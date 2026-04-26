import unittest
from decimal import Decimal

from app.domain.recommendation.service import RecommendationService
from app.domain.tagging.service import ReviewTaggingService
from app.infrastructure.llm.openai_compatible import ModelToolCall, StructuredModelClient
from app.schemas.recommendation import RecommendationCandidate, RecommendationContext, RecommendationRequest
from app.schemas.tagging import ReviewTagRequest, ReviewText


class FakeStructuredModelClient(StructuredModelClient):
    def __init__(
        self,
        json_response=None,
        tool_calls=None,
        text_response=None,
        json_error=None,
        tool_call_error=None,
        text_error=None,
        stream_error=None,
        stream_chunks=None,
    ):
        self.json_response = json_response or {}
        self.tool_calls = tool_calls or []
        self.text_response = text_response or ""
        self.json_error = json_error
        self.tool_call_error = tool_call_error
        self.text_error = text_error
        self.stream_error = stream_error
        self.stream_chunks = stream_chunks
        self.last_json_user_prompt = None
        self.last_tool_user_prompt = None
        self.last_text_user_prompt = None
        self.last_stream_user_prompt = None

    def generate_json(self, system_prompt: str, user_prompt: str):
        self.last_json_user_prompt = user_prompt
        if self.json_error is not None:
            raise self.json_error
        return self.json_response

    def generate_tool_calls(self, system_prompt: str, user_prompt: str, tools):
        self.last_tool_user_prompt = user_prompt
        if self.tool_call_error is not None:
            raise self.tool_call_error
        return self.tool_calls

    def generate_text(self, system_prompt: str, user_prompt: str, tool_calls=None, tool_outputs=None):
        self.last_text_user_prompt = user_prompt
        if self.text_error is not None:
            raise self.text_error
        return self.text_response

    def stream_text(self, system_prompt: str, user_prompt: str, tool_calls=None, tool_outputs=None):
        self.last_stream_user_prompt = user_prompt
        if self.stream_error is not None:
            raise self.stream_error
        if self.stream_chunks is not None:
            yield from self.stream_chunks
            return
        if self.text_response:
            yield self.text_response


class ReviewTaggingServiceTest(unittest.TestCase):
    def test_summarize_should_extract_top_tags(self) -> None:
        service = ReviewTaggingService(
            FakeStructuredModelClient(
                json_response={
                    "tag1": "性价比高",
                    "tag2": "汤底稳",
                    "summary": "评论普遍提到性价比高，汤底表现稳定。",
                }
            )
        )

        result = service.summarize(
            ReviewTagRequest(
                poiId="poi-1",
                poiName="兰州拉面",
                reviews=[
                    ReviewText(content="汤底稳，性价比高"),
                    ReviewText(content="价格实惠，出餐也快"),
                ],
            )
        )

        self.assertEqual("性价比高", result.tag1)
        self.assertEqual("汤底稳", result.tag2)
        self.assertEqual("评论普遍提到性价比高，汤底表现稳定。", result.summary)


class RecommendationServiceTest(unittest.TestCase):
    def test_recommend_should_prioritize_budget_and_tag_match_via_tool_calls(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-2",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-rice", "reason": "更近，出餐也比较快", "rank": 2},
                    ),
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-noodle", "reason": "评分更高，人均也在预算内", "rank": 1},
                    ),
                ],
                text_response="优先可以考虑兰州拉面，其次是桂香卤味拌饭。",
            )
        )

        result = service.recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-rice",
                        name="桂香卤味拌饭",
                        address="学林街",
                        category="餐饮",
                        distance=180,
                        avgRating=Decimal("4.1"),
                        reviewCount=12,
                        avgPerCapitaPrice=18,
                        aiTags=["出餐快", "学生友好"],
                    ),
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        )

        self.assertEqual("poi-noodle", result.choices[0].poi_id)
        self.assertEqual("poi-rice", result.choices[1].poi_id)
        self.assertIn("兰州拉面", result.answer)

    def test_recommend_should_fallback_to_json_when_tool_calls_missing(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                json_response={
                    "answer": "优先可以考虑兰州拉面，其次是桂香卤味拌饭。",
                    "choices": [
                        {"poiId": "poi-noodle", "reason": "评分更高，人均也在预算内"},
                        {"poiId": "poi-rice", "reason": "更近，出餐也比较快"},
                    ],
                }
            )
        )

        result = service.recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-rice",
                        name="桂香卤味拌饭",
                        address="学林街",
                        category="餐饮",
                        distance=180,
                        avgRating=Decimal("4.1"),
                        reviewCount=12,
                        avgPerCapitaPrice=18,
                        aiTags=["出餐快", "学生友好"],
                    ),
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        )

        self.assertEqual("poi-noodle", result.choices[0].poi_id)
        self.assertIn("兰州拉面", result.answer)

    def test_recommend_should_accept_refine_context_and_derived_tags(self) -> None:
        model_client = FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-salad", "reason": "更贴近高蛋白和清淡需求", "rank": 1},
                    ),
                ],
                text_response="如果你在健身，优先考虑轻食能量碗会更稳。",
            )
        service = RecommendationService(model_client)

        result = service.recommend(
            RecommendationRequest(
                question="换一家，还是想吃高蛋白",
                context=RecommendationContext(
                    previousQuestion="预算35以内，想吃轻一点",
                    rejectedPoiIds=["poi-fried"],
                    userSignals=["健身"],
                    temporalContext="4月18日，星期六，当前时间 15:30，更像下午茶或轻食场景。",
                    preferredTags=["清淡", "高蛋白"],
                    recentFeedbackSignals=["更在意预算"],
                ),
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-salad",
                        name="轻食能量碗",
                        address="学林街",
                        category="轻食",
                        distance=220,
                        avgRating=Decimal("4.7"),
                        reviewCount=18,
                        avgPerCapitaPrice=32,
                        aiTags=["清淡", "高蛋白"],
                        aiSummary="鸡胸肉和热汤都很受欢迎",
                        derivedTags=["健身友好", "高蛋白"],
                    ),
                ],
            )
        )

        self.assertEqual("poi-salad", result.choices[0].poi_id)
        self.assertIn("健身", result.answer)
        self.assertIn("星期六", model_client.last_tool_user_prompt)
        self.assertIn("15:30", model_client.last_tool_user_prompt)

    def test_recommend_should_strip_reasoning_tags_from_answer(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-noodle", "reason": "热汤更稳", "rank": 1},
                    ),
                ],
                text_response="<think>先分析预算和距离</think>兰州拉面更合适。",
            )
        )

        result = service.recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        )

        self.assertEqual("兰州拉面更合适。", result.answer)

    def test_recommend_should_fallback_when_text_generation_fails(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-noodle", "reason": "热汤更稳", "rank": 1},
                    ),
                ],
                text_error=RuntimeError("llm unavailable"),
            )
        )

        result = service.recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        )

        self.assertEqual("**首选** 可以考虑兰州拉面 😋", result.answer)

    def test_recommend_should_fallback_when_json_answer_and_choices_invalid(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                json_response={
                    "answer": "   ",
                    "choices": [
                        {"poiId": "poi-missing", "reason": "不在候选列表里"},
                    ],
                }
            )
        )

        result = service.recommend(
            RecommendationRequest(
                question="随便来点",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                    RecommendationCandidate(
                        poiId="poi-rice",
                        name="桂香卤味拌饭",
                        address="学林街",
                        category="餐饮",
                        distance=180,
                        avgRating=Decimal("4.1"),
                        reviewCount=12,
                        avgPerCapitaPrice=18,
                        aiTags=["出餐快", "学生友好"],
                    ),
                ],
            )
        )

        self.assertEqual(["poi-noodle", "poi-rice"], [choice.poi_id for choice in result.choices])
        self.assertEqual("**首选** 可以考虑兰州拉面，其次是桂香卤味拌饭 ✨", result.answer)

    def test_stream_recommend_should_emit_ranked_cards_and_final_answer(self) -> None:
        model_client = FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-2",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-rice", "reason": "更近，出餐也比较快", "rank": 2},
                    ),
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-noodle", "reason": "评分更高，人均也在预算内", "rank": 1},
                    ),
                ],
                text_response="优先可以考虑兰州拉面，其次是桂香卤味拌饭。",
            )
        service = RecommendationService(model_client)

        events = list(service.stream_recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                context=RecommendationContext(
                    temporalContext="4月18日，星期六，当前时间 15:30，更像下午茶或轻食场景。",
                ),
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-rice",
                        name="桂香卤味拌饭",
                        address="学林街",
                        category="餐饮",
                        distance=180,
                        avgRating=Decimal("4.1"),
                        reviewCount=12,
                        avgPerCapitaPrice=18,
                        aiTags=["出餐快", "学生友好"],
                    ),
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        ))

        event_names = [event[0] for event in events]
        first_tool_call_index = event_names.index("tool.call")
        answer_done_index = event_names.index("answer.done")

        self.assertEqual("tool.call", event_names[0])
        self.assertLess(first_tool_call_index, answer_done_index)
        self.assertIn("兰州拉面", events[answer_done_index][1]["answer"])
        self.assertEqual("poi-noodle", events[first_tool_call_index][1]["arguments"]["poiId"])
        self.assertEqual(1, events[first_tool_call_index][1]["arguments"]["rank"])
        self.assertEqual("poi-rice", events[first_tool_call_index + 1][1]["arguments"]["poiId"])
        self.assertEqual(2, events[first_tool_call_index + 1][1]["arguments"]["rank"])
        self.assertEqual(("done", {"finishReason": "stop"}), events[-1])
        self.assertIn("星期六", model_client.last_stream_user_prompt)
        self.assertIn("15:30", model_client.last_stream_user_prompt)

    def test_stream_recommend_should_hide_reasoning_chunks(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-noodle", "reason": "热汤更稳", "rank": 1},
                    ),
                ],
                text_response="<think>先分析预算和距离</think>兰州拉面更合适。",
            )
        )

        events = list(service.stream_recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        ))

        answer_deltas = [payload["delta"] for name, payload in events if name == "answer.delta"]
        self.assertEqual(["兰州拉面更合适。"], answer_deltas)
        first_tool_call = next(payload for name, payload in events if name == "tool.call")
        self.assertEqual("poi-noodle", first_tool_call["arguments"]["poiId"])
        answer_done = next(payload for name, payload in events if name == "answer.done")
        self.assertEqual("兰州拉面更合适。", answer_done["answer"])

    def test_stream_recommend_should_fallback_to_json_choices_when_tool_calls_missing(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                json_response={
                    "answer": "优先可以考虑兰州拉面，其次是桂香卤味拌饭。",
                    "choices": [
                        {"poiId": "poi-noodle", "reason": "评分更高，人均也在预算内"},
                        {"poiId": "poi-rice", "reason": "更近，出餐也比较快"},
                    ],
                }
            )
        )

        events = list(service.stream_recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-rice",
                        name="桂香卤味拌饭",
                        address="学林街",
                        category="餐饮",
                        distance=180,
                        avgRating=Decimal("4.1"),
                        reviewCount=12,
                        avgPerCapitaPrice=18,
                        aiTags=["出餐快", "学生友好"],
                    ),
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        ))

        event_names = [event[0] for event in events]
        first_tool_call_index = event_names.index("tool.call")
        answer_done_index = event_names.index("answer.done")

        self.assertEqual("tool.call", event_names[0])
        self.assertLess(first_tool_call_index, answer_done_index)
        self.assertIn("兰州拉面", events[answer_done_index][1]["answer"])
        self.assertEqual("poi-noodle", events[first_tool_call_index][1]["arguments"]["poiId"])
        self.assertEqual("poi-rice", events[first_tool_call_index + 1][1]["arguments"]["poiId"])
        self.assertEqual(("done", {"finishReason": "stop"}), events[-1])

    def test_stream_recommend_should_still_emit_cards_when_post_answer_selection_fails(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                text_response="现在更适合先吃点热乎的正餐。",
                tool_call_error=RuntimeError("tool selection failed"),
                json_error=RuntimeError("json fallback failed"),
            )
        )

        events = list(service.stream_recommend(
            RecommendationRequest(
                question="现在适合吃什么",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                    RecommendationCandidate(
                        poiId="poi-rice",
                        name="桂香卤味拌饭",
                        address="学林街",
                        category="餐饮",
                        distance=180,
                        avgRating=Decimal("4.1"),
                        reviewCount=12,
                        avgPerCapitaPrice=18,
                        aiTags=["出餐快", "学生友好"],
                    ),
                ],
            )
        ))

        event_names = [event[0] for event in events]
        first_tool_call_index = event_names.index("tool.call")
        answer_done_index = event_names.index("answer.done")

        self.assertLess(first_tool_call_index, answer_done_index)
        self.assertEqual("poi-noodle", events[first_tool_call_index][1]["arguments"]["poiId"])
        self.assertEqual("poi-rice", events[first_tool_call_index + 1][1]["arguments"]["poiId"])
        self.assertEqual(("done", {"finishReason": "stop"}), events[-1])

    def test_stream_recommend_should_fallback_to_chunked_answer_when_stream_fails(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-noodle", "reason": "热汤更稳", "rank": 1},
                    ),
                ],
                stream_error=RuntimeError("stream failed"),
            )
        )

        events = list(service.stream_recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        ))

        answer_deltas = [payload["delta"] for name, payload in events if name == "answer.delta"]
        self.assertEqual(["**首选** 可以考虑兰州拉面 😋"], answer_deltas)
        answer_done = next(payload for name, payload in events if name == "answer.done")
        self.assertEqual("**首选** 可以考虑兰州拉面 😋", answer_done["answer"])

    def test_stream_recommend_should_fallback_when_stream_has_only_hidden_reasoning(self) -> None:
        service = RecommendationService(
            FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-noodle", "reason": "热汤更稳", "rank": 1},
                    ),
                ],
                stream_chunks=["<think>先分析预算", "和距离</think>"],
            )
        )

        events = list(service.stream_recommend(
            RecommendationRequest(
                question="预算30以内，想吃点带汤的",
                candidates=[
                    RecommendationCandidate(
                        poiId="poi-noodle",
                        name="兰州拉面",
                        address="文泽路",
                        category="餐饮",
                        distance=220,
                        avgRating=Decimal("4.8"),
                        reviewCount=25,
                        avgPerCapitaPrice=29,
                        aiTags=["性价比高", "汤底稳"],
                    ),
                ],
            )
        ))

        answer_deltas = [payload["delta"] for name, payload in events if name == "answer.delta"]
        self.assertEqual(["**首选** 可以考虑兰州拉面 😋"], answer_deltas)
        answer_done = next(payload for name, payload in events if name == "answer.done")
        self.assertEqual("**首选** 可以考虑兰州拉面 😋", answer_done["answer"])

    def test_emit_tool_calls_should_generate_ranks_without_model_calls(self) -> None:
        service = RecommendationService(FakeStructuredModelClient())
        request = RecommendationRequest(
            question="随便来点",
            candidates=[
                RecommendationCandidate(
                    poiId="poi-noodle",
                    name="兰州拉面",
                    address="文泽路",
                    category="餐饮",
                    distance=220,
                    avgRating=Decimal("4.8"),
                    reviewCount=25,
                    avgPerCapitaPrice=29,
                    aiTags=["性价比高", "汤底稳"],
                ),
                RecommendationCandidate(
                    poiId="poi-rice",
                    name="桂香卤味拌饭",
                    address="学林街",
                    category="餐饮",
                    distance=180,
                    avgRating=Decimal("4.1"),
                    reviewCount=12,
                    avgPerCapitaPrice=18,
                    aiTags=["出餐快", "学生友好"],
                ),
            ],
        )

        events = list(service._emit_tool_calls(service._fallback_choices(request)))

        self.assertEqual(1, events[0][1]["arguments"]["rank"])
        self.assertEqual(2, events[1][1]["arguments"]["rank"])
        self.assertEqual("show_restaurant_card", events[0][1]["toolName"])

    def test_streaming_answer_sanitizer_should_handle_split_tags(self) -> None:
        from app.domain.recommendation.service import StreamingAnswerSanitizer

        target = StreamingAnswerSanitizer()
        self.assertEqual("兰州拉面", target.feed("兰州拉面<th"))
        self.assertEqual("更稳", target.feed("ink>隐藏</think>更稳"))
        self.assertEqual("", target.finish())

    def test_streaming_answer_sanitizer_should_drop_unclosed_reasoning_tail(self) -> None:
        from app.domain.recommendation.service import StreamingAnswerSanitizer

        target = StreamingAnswerSanitizer()
        self.assertEqual("", target.feed("<think>隐藏</thi"))
        self.assertEqual("", target.finish())


if __name__ == "__main__":
    unittest.main()
