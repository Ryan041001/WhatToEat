import json
from typing import List, Tuple

from app.clients.openai_compatible import ModelToolCall, ModelToolDefinition, StructuredModelClient
from app.schemas import RecommendationAdvice, RecommendationChoice, RecommendationRequest


class RecommendationService:
    CARD_TOOL_NAME = "show_restaurant_card"

    def __init__(self, model_client: StructuredModelClient) -> None:
        self.model_client = model_client

    def recommend(self, request: RecommendationRequest) -> RecommendationAdvice:
        tool_choices, tool_calls = self._recommend_with_tool_calls(request)
        if tool_choices:
            answer = self._generate_answer_from_tool_calls(request, tool_choices, tool_calls)
            return RecommendationAdvice(answer=answer[:200], choices=tool_choices)
        return self._recommend_with_json_fallback(request)

    def stream_recommend(self, request: RecommendationRequest):
        tool_choices, tool_calls = self._recommend_with_tool_calls(request)
        if not tool_choices:
            fallback = self._recommend_with_json_fallback(request)
            for index, choice in enumerate(fallback.choices, start=1):
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
            for delta in self._chunk_text(fallback.answer):
                yield ("answer.delta", {"delta": delta})
            yield ("answer.done", {"answer": fallback.answer})
            yield ("done", {"finishReason": "stop"})
            return

        for choice, tool_call in zip(tool_choices, tool_calls):
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

        candidate_by_poi_id = {candidate.poi_id: candidate for candidate in request.candidates}
        tool_outputs = []
        for choice, tool_call in zip(tool_choices, tool_calls):
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

        accumulated_answer = ""
        try:
            for delta in self.model_client.stream_text(
                system_prompt=self._answer_system_prompt(),
                user_prompt=json.dumps(
                    {
                        "question": request.question,
                        "selectedPoiIds": [choice.poi_id for choice in tool_choices],
                        "context": request.context.model_dump(by_alias=True) if request.context else None,
                    },
                    ensure_ascii=False,
                ),
                tool_calls=tool_calls,
                tool_outputs=tool_outputs,
            ):
                if not delta:
                    continue
                accumulated_answer += delta
                yield ("answer.delta", {"delta": delta})
        except Exception:
            accumulated_answer = self._build_fallback_answer(request, tool_choices)
            for delta in self._chunk_text(accumulated_answer):
                yield ("answer.delta", {"delta": delta})

        final_answer = accumulated_answer.strip() or self._build_fallback_answer(request, tool_choices)
        yield ("answer.done", {"answer": final_answer[:200]})
        yield ("done", {"finishReason": "stop"})

    def _recommend_with_tool_calls(
        self,
        request: RecommendationRequest,
    ) -> Tuple[List[RecommendationChoice], List[ModelToolCall]]:
        payload = self._build_request_payload(request)
        tool_calls = self.model_client.generate_tool_calls(
            system_prompt=self._tool_selection_system_prompt(),
            user_prompt=json.dumps(payload, ensure_ascii=False),
            tools=[
                ModelToolDefinition(
                    name=self.CARD_TOOL_NAME,
                    description="选择一间应该展示给用户的推荐餐厅卡片。",
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

        valid_poi_ids = {candidate.poi_id for candidate in request.candidates}
        parsed_with_rank: List[tuple[int, RecommendationChoice, ModelToolCall]] = []
        seen_poi_ids = set()
        seen_ranks = set()
        for tool_call in tool_calls:
            if tool_call.name != self.CARD_TOOL_NAME:
                continue
            poi_id = str(tool_call.arguments.get("poiId", "")).strip()
            reason = str(tool_call.arguments.get("reason", "")).strip()
            rank_value = tool_call.arguments.get("rank")
            if not isinstance(rank_value, int):
                continue
            if poi_id not in valid_poi_ids or not reason or poi_id in seen_poi_ids or rank_value in seen_ranks:
                continue
            parsed_with_rank.append(
                (
                    rank_value,
                    RecommendationChoice(poiId=poi_id, reason=reason[:80]),
                    tool_call,
                )
            )
            seen_poi_ids.add(poi_id)
            seen_ranks.add(rank_value)
            if len(parsed_with_rank) == 3:
                break

        parsed_with_rank.sort(key=lambda item: item[0])
        return (
            [item[1] for item in parsed_with_rank],
            [item[2] for item in parsed_with_rank],
        )

    def _generate_answer_from_tool_calls(
        self,
        request: RecommendationRequest,
        choices: List[RecommendationChoice],
        tool_calls: List[ModelToolCall],
    ) -> str:
        if not choices or not tool_calls:
            return self._build_fallback_answer(request, choices)

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

        if not tool_outputs:
            return self._build_fallback_answer(request, choices)

        try:
            answer = self.model_client.generate_text(
                system_prompt=self._answer_system_prompt(),
                user_prompt=json.dumps(
                    {
                        "question": request.question,
                        "selectedPoiIds": [choice.poi_id for choice in choices],
                        "context": request.context.model_dump(by_alias=True) if request.context else None,
                    },
                    ensure_ascii=False,
                ),
                tool_calls=tool_calls,
                tool_outputs=tool_outputs,
            )
        except Exception:
            return self._build_fallback_answer(request, choices)

        normalized = str(answer).strip()
        return normalized[:200] if normalized else self._build_fallback_answer(request, choices)

    def _recommend_with_json_fallback(self, request: RecommendationRequest) -> RecommendationAdvice:
        payload = self._build_request_payload(request)
        result = self.model_client.generate_json(
            system_prompt=(
                "你是餐厅推荐助手。"
                "请严格基于给定候选餐厅，回答用户现在吃什么更合适。"
                "如果 context 中有连续追问、用户画像和近期反馈，也要把这些因素考虑进去。"
                "连续追问时要默认继承 previousQuestion 里的约束，再叠加本轮新条件。"
                "推荐时优先保证硬约束匹配，其次再比较距离、预算、评分、评论数与标签契合度。"
                "输出必须是 JSON 对象，字段只有 answer 和 choices。"
                "answer 是 1 到 3 句中文建议。"
                "choices 是数组，最多 3 项，每项必须包含 poiId 和 reason。"
                "poiId 必须来自输入候选列表，reason 必须是简短中文理由。"
                "不允许编造输入中不存在的餐厅。"
            ),
            user_prompt=json.dumps(payload, ensure_ascii=False),
        )
        valid_poi_ids = {candidate.poi_id for candidate in request.candidates}
        parsed_choices = []
        for choice in result.get("choices", []):
            poi_id = str(choice.get("poiId", "")).strip()
            reason = str(choice.get("reason", "")).strip()
            if poi_id in valid_poi_ids and reason:
                parsed_choices.append(RecommendationChoice(poiId=poi_id, reason=reason[:80]))
            if len(parsed_choices) == 3:
                break

        if not parsed_choices:
            parsed_choices = self._fallback_choices(request)

        answer = str(result.get("answer", "")).strip()
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

    def _tool_selection_system_prompt(self) -> str:
        return (
            "你是餐厅推荐助手。"
            "请严格基于给定候选餐厅，为用户挑选最合适的 1 到 3 家。"
            "如果 context 中有 previousQuestion、userSignals、preferredTags、avoidedTags、recentFeedbackSignals，"
            "说明这是连续追问或已有用户画像，必须一并考虑。"
            "连续追问时要默认继承 previousQuestion 的有效约束，再叠加本轮新要求。"
            "优先级从高到低："
            "1) 不要违反显式否定约束；"
            "2) 先满足口味/场景/生活方式信号；"
            "3) 再综合距离、预算、评分、评论数；"
            "4) 如果条件冲突，优先保留用户刚刚强调的新条件。"
            "例如用户说健身/减脂/高蛋白时，优先看 derivedTags 或 aiTags 中的高蛋白、清淡、健身友好；"
            "用户说想喝热汤时，优先看热汤；用户说不要快餐时，要避开快餐；"
            "用户说太贵/太远时，要明显降低价格和距离权重不匹配的候选。"
            "你必须通过工具 show_restaurant_card 输出推荐结果。"
            "每次工具调用只推荐一家店，poiId 必须来自输入候选列表。"
            "reason 必须是简短中文理由，rank 必须是 1 到 3 的整数且不能重复。"
            "不要编造输入中不存在的餐厅。"
        )

    def _answer_system_prompt(self) -> str:
        return (
            "你是餐厅推荐助手。"
            "你已经通过工具挑出了推荐餐厅。"
            "请严格基于工具结果，用 1 到 3 句中文给出最终建议。"
            "如果上下文显示用户在健身、控制预算、在意距离、想喝热汤或想换口味，要把这些因素自然融入建议。"
            "优先解释“为什么这几家更贴近当前约束”，而不是泛泛夸店。"
            "不要输出 JSON，不要提及未被选中的餐厅。"
        )
