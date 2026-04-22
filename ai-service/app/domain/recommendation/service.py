import json
import logging
import re
from typing import List, Tuple

from app.domain.recommendation.parser import parse_json_choices, parse_ranked_tool_calls
from app.domain.recommendation.prompts import (
    answer_system_prompt,
    json_fallback_system_prompt,
    tool_selection_system_prompt,
)
from app.infrastructure.llm.openai_compatible import ModelToolCall, ModelToolDefinition, StructuredModelClient
from app.schemas.recommendation import RecommendationAdvice, RecommendationChoice, RecommendationRequest

logger = logging.getLogger(__name__)


class RecommendationService:
    CARD_TOOL_NAME = "show_restaurant_card"
    THINK_OPEN_TAG = "<think>"
    THINK_CLOSE_TAG = "</think>"

    def __init__(self, model_client: StructuredModelClient) -> None:
        self.model_client = model_client

    def recommend(self, request: RecommendationRequest) -> RecommendationAdvice:
        tool_choices, tool_calls = self._recommend_with_tool_calls(request)
        if tool_choices:
            answer = self._generate_answer_from_tool_calls(request, tool_choices, tool_calls)
            return RecommendationAdvice(answer=answer[:200], choices=tool_choices)
        return self._recommend_with_json_fallback(request)

    def stream_recommend(self, request: RecommendationRequest):
        choices, tool_calls = self._resolve_stream_choices(request)
        yield from self._emit_tool_calls(choices, tool_calls)
        final_answer = yield from self._stream_answer_from_choices(request, choices, tool_calls)
        yield ("answer.done", {"answer": final_answer[:200]})
        yield ("done", {"finishReason": "stop"})

    def _recommend_with_tool_calls(
        self,
        request: RecommendationRequest,
    ) -> Tuple[List[RecommendationChoice], List[ModelToolCall]]:
        payload = self._build_request_payload(request)
        tool_calls = self.model_client.generate_tool_calls(
            system_prompt=tool_selection_system_prompt(),
            user_prompt=json.dumps(payload, ensure_ascii=False),
            tools=[
                ModelToolDefinition(
                    name=self.CARD_TOOL_NAME,
                    description="选择一间应该展示给用户的推荐餐厅",
                    parameters={
                        "type": "object",
                        "properties": {
                            "poiId": {
                                "type": "string",
                                "description": "当前候选餐厅中的 poiId",
                            },
                            "reason": {
                                "type": "string",
                                "description": "这家餐厅适合当前用户需求的简短中文理由",
                            },
                            "rank": {
                                "type": "integer",
                                "description": "推荐顺位，1 表示最推荐",
                                "minimum": 1,
                                "maximum": 3,
                            },
                        },
                        "required": ["poiId", "reason", "rank"],
                        "additionalProperties": False,
                    },
                )
            ],
        )

        return parse_ranked_tool_calls(
            {candidate.poi_id for candidate in request.candidates},
            tool_calls,
        )

    def _generate_answer_from_tool_calls(
        self,
        request: RecommendationRequest,
        choices: List[RecommendationChoice],
        tool_calls: List[ModelToolCall],
    ) -> str:
        if not choices or not tool_calls:
            return self._build_fallback_answer(request, choices)

        tool_outputs = self._build_tool_outputs(request, choices, tool_calls)
        if not tool_outputs:
            return self._build_fallback_answer(request, choices)

        try:
            answer = self.model_client.generate_text(
                system_prompt=answer_system_prompt(),
                user_prompt=self._build_answer_user_prompt(request, choices),
                tool_calls=tool_calls,
                tool_outputs=tool_outputs,
            )
        except Exception:
            return self._build_fallback_answer(request, choices)

        normalized = self._sanitize_answer_text(answer)
        return normalized[:200] if normalized else self._build_fallback_answer(request, choices)

    def _resolve_stream_choices(
        self,
        request: RecommendationRequest,
    ) -> Tuple[List[RecommendationChoice], List[ModelToolCall]]:
        try:
            tool_choices, tool_calls = self._recommend_with_tool_calls(request)
        except Exception:
            logger.exception("stream_recommend tool selection failed")
            tool_choices, tool_calls = [], []
        if tool_choices:
            return tool_choices, tool_calls

        try:
            fallback = self._recommend_with_json_fallback(request)
            fallback_choices = fallback.choices or self._fallback_choices(request)
        except Exception:
            logger.exception("stream_recommend json fallback failed")
            fallback_choices = self._fallback_choices(request)
        return fallback_choices, self._build_synthetic_tool_calls(fallback_choices)

    def _stream_answer_from_choices(
        self,
        request: RecommendationRequest,
        choices: List[RecommendationChoice],
        tool_calls: List[ModelToolCall],
    ) -> str:
        if not choices:
            return self._build_fallback_answer(request, choices)[:200]

        tool_outputs = self._build_tool_outputs(request, choices, tool_calls)
        if not tool_outputs:
            return self._build_fallback_answer(request, choices)[:200]

        accumulated_answer = ""
        sanitizer = StreamingAnswerSanitizer()
        try:
            for delta in self.model_client.stream_text(
                system_prompt=answer_system_prompt(),
                user_prompt=self._build_answer_user_prompt(request, choices),
                tool_calls=tool_calls,
                tool_outputs=tool_outputs,
            ):
                if not delta:
                    continue
                visible_delta = sanitizer.feed(delta)
                if not visible_delta:
                    continue
                accumulated_answer += visible_delta
                yield ("answer.delta", {"delta": visible_delta})
        except Exception:
            accumulated_answer = ""
            sanitizer = StreamingAnswerSanitizer()

        tail = sanitizer.finish()
        if tail:
            accumulated_answer += tail
            yield ("answer.delta", {"delta": tail})

        final_answer = self._sanitize_answer_text(accumulated_answer)
        if final_answer:
            return final_answer[:200]

        fallback_answer = self._build_fallback_answer(request, choices)
        for delta in self._chunk_text(fallback_answer):
            yield ("answer.delta", {"delta": delta})
        return fallback_answer[:200]

    def _emit_tool_calls(
        self,
        choices: List[RecommendationChoice],
        tool_calls: List[ModelToolCall] | None = None,
    ):
        if tool_calls is None:
            for index, choice in enumerate(choices, start=1):
                yield (
                    "tool.call",
                    {
                        "toolName": self.CARD_TOOL_NAME,
                        "arguments": {
                            "poiId": choice.poi_id,
                            "reason": choice.reason,
                            "rank": index,
                        },
                    },
                )
            return

        for choice, tool_call in zip(choices, tool_calls):
            yield (
                "tool.call",
                {
                    "toolName": tool_call.name,
                    "arguments": {
                        "poiId": choice.poi_id,
                        "reason": choice.reason,
                        "rank": tool_call.arguments.get("rank"),
                    },
                },
            )

    def _build_tool_outputs(
        self,
        request: RecommendationRequest,
        choices: List[RecommendationChoice],
        tool_calls: List[ModelToolCall],
    ) -> List[dict]:
        candidate_by_poi_id = {candidate.poi_id: candidate for candidate in request.candidates}
        tool_outputs = []
        for choice, tool_call in zip(choices, tool_calls):
            candidate = candidate_by_poi_id.get(choice.poi_id)
            if candidate is None:
                continue
            tool_outputs.append(
                {
                    "toolCallId": tool_call.id,
                    "poiId": candidate.poi_id,
                    "name": candidate.name,
                    "address": candidate.address,
                    "category": candidate.category,
                    "distance": candidate.distance,
                    "avgRating": str(candidate.avg_rating) if candidate.avg_rating is not None else None,
                    "reviewCount": candidate.review_count,
                    "avgPerCapitaPrice": candidate.avg_per_capita_price,
                    "aiTags": candidate.ai_tags,
                    "aiSummary": candidate.ai_summary,
                    "derivedTags": candidate.derived_tags,
                    "matchReason": choice.reason,
                }
            )
        return tool_outputs

    def _build_synthetic_tool_calls(self, choices: List[RecommendationChoice]) -> List[ModelToolCall]:
        synthetic_calls: List[ModelToolCall] = []
        for index, choice in enumerate(choices, start=1):
            synthetic_calls.append(ModelToolCall(
                id=f"fallback-call-{index}",
                name=self.CARD_TOOL_NAME,
                arguments={
                    "poiId": choice.poi_id,
                    "reason": choice.reason,
                    "rank": index,
                },
            ))
        return synthetic_calls

    def _build_answer_user_prompt(
        self,
        request: RecommendationRequest,
        choices: List[RecommendationChoice],
    ) -> str:
        return json.dumps(
            {
                "question": request.question,
                "selectedPoiIds": [choice.poi_id for choice in choices],
                "context": request.context.model_dump(by_alias=True) if request.context else None,
            },
            ensure_ascii=False,
        )

    def _recommend_with_json_fallback(self, request: RecommendationRequest) -> RecommendationAdvice:
        payload = self._build_request_payload(request)
        result = self.model_client.generate_json(
            system_prompt=json_fallback_system_prompt(),
            user_prompt=json.dumps(payload, ensure_ascii=False),
        )
        parsed_choices = parse_json_choices(
            {candidate.poi_id for candidate in request.candidates},
            result.get("choices", []),
        )

        if not parsed_choices:
            parsed_choices = self._fallback_choices(request)

        answer = self._sanitize_answer_text(result.get("answer", ""))
        if not answer:
            answer = self._build_fallback_answer(request, parsed_choices)
        return RecommendationAdvice(answer=answer[:200], choices=parsed_choices)

    def _fallback_choices(self, request: RecommendationRequest) -> List[RecommendationChoice]:
        return [
            RecommendationChoice(
                poiId=candidate.poi_id,
                reason="候选列表里和你当前需求更匹配。",
            )
            for candidate in request.candidates[:3]
        ]

    def _build_fallback_answer(self, request: RecommendationRequest, choices: List[RecommendationChoice]) -> str:
        candidate_by_poi_id = {candidate.poi_id: candidate for candidate in request.candidates}
        selected_names = [
            candidate_by_poi_id[choice.poi_id].name
            for choice in choices
            if choice.poi_id in candidate_by_poi_id
        ]
        if not selected_names:
            return "我先按你当前的描述，从附近候选里挑了更匹配的几家。"
        if len(selected_names) == 1:
            return f"按你现在的需求，优先可以考虑{selected_names[0]}。"
        return "按你现在的需求，优先可以考虑{}，其次是{}。".format(
            selected_names[0],
            "、".join(selected_names[1:]),
        )

    def _chunk_text(self, text: str) -> List[str]:
        if not text:
            return []
        normalized = text.strip()
        if not normalized:
            return []
        chunk_size = 24
        return [
            normalized[index:index + chunk_size]
            for index in range(0, len(normalized), chunk_size)
        ]

    def _build_request_payload(self, request: RecommendationRequest) -> dict:
        return {
            "question": request.question,
            "candidates": [
                {
                    "poiId": candidate.poi_id,
                    "name": candidate.name,
                    "address": candidate.address,
                    "category": candidate.category,
                    "distance": candidate.distance,
                    "avgRating": str(candidate.avg_rating) if candidate.avg_rating is not None else None,
                    "reviewCount": candidate.review_count,
                    "avgPerCapitaPrice": candidate.avg_per_capita_price,
                    "aiTags": candidate.ai_tags,
                    "aiSummary": candidate.ai_summary,
                    "derivedTags": candidate.derived_tags,
                }
                for candidate in request.candidates
            ],
            "context": request.context.model_dump(by_alias=True) if request.context else None,
        }

    def _sanitize_answer_text(self, text: str | object) -> str:
        normalized = re.sub(
            r"<think>.*?</think>",
            "",
            str(text),
            flags=re.DOTALL | re.IGNORECASE,
        )
        normalized = normalized.replace(self.THINK_OPEN_TAG, "").replace(self.THINK_CLOSE_TAG, "")
        return normalized.strip()


class StreamingAnswerSanitizer:
    def __init__(self) -> None:
        self.buffer = ""
        self.inside_think = False

    def feed(self, chunk: str) -> str:
        if not chunk:
            return ""
        self.buffer += chunk
        visible_parts: List[str] = []

        while self.buffer:
            if self.inside_think:
                end_index = self.buffer.find(RecommendationService.THINK_CLOSE_TAG)
                if end_index < 0:
                    self.buffer = self._retain_possible_prefix_tail(
                        self.buffer,
                        RecommendationService.THINK_CLOSE_TAG,
                    )
                    break
                self.buffer = self.buffer[end_index + len(RecommendationService.THINK_CLOSE_TAG):]
                self.inside_think = False
                continue

            start_index = self.buffer.find(RecommendationService.THINK_OPEN_TAG)
            if start_index < 0:
                visible, tail = self._split_visible_and_tail(
                    self.buffer,
                    RecommendationService.THINK_OPEN_TAG,
                )
                visible_parts.append(visible)
                self.buffer = tail
                break

            visible_parts.append(self.buffer[:start_index])
            self.buffer = self.buffer[start_index + len(RecommendationService.THINK_OPEN_TAG):]
            self.inside_think = True

        return "".join(visible_parts)

    def finish(self) -> str:
        if self.inside_think:
            self.buffer = ""
            return ""
        visible = self.buffer
        self.buffer = ""
        return visible

    def _split_visible_and_tail(self, text: str, tag: str) -> Tuple[str, str]:
        tail_length = 0
        max_prefix_length = min(len(text), len(tag) - 1)
        for prefix_length in range(max_prefix_length, 0, -1):
            if text.endswith(tag[:prefix_length]):
                tail_length = prefix_length
                break
        if tail_length == 0:
            return text, ""
        return text[:-tail_length], text[-tail_length:]

    def _retain_possible_prefix_tail(self, text: str, tag: str) -> str:
        _, tail = self._split_visible_and_tail(text, tag)
        return tail
