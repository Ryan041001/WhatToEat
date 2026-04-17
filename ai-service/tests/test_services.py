import unittest
from decimal import Decimal

from app.clients.openai_compatible import ModelToolCall, StructuredModelClient
from app.schemas import RecommendationCandidate, RecommendationContext, RecommendationRequest, ReviewTagRequest, ReviewText
from app.services.recommendation import RecommendationService
from app.services.tagging import ReviewTaggingService


class FakeStructuredModelClient(StructuredModelClient):
    def __init__(self, json_response=None, tool_calls=None, text_response=None):
        self.json_response = json_response or {}
        self.tool_calls = tool_calls or []
        self.text_response = text_response or ""

    def generate_json(self, system_prompt: str, user_prompt: str):
        return self.json_response

    def generate_tool_calls(self, system_prompt: str, user_prompt: str, tools):
        return self.tool_calls

    def generate_text(self, system_prompt: str, user_prompt: str, tool_calls=None, tool_outputs=None):
        return self.text_response

    def stream_text(self, system_prompt: str, user_prompt: str, tool_calls=None, tool_outputs=None):
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
        service = RecommendationService(
            FakeStructuredModelClient(
                tool_calls=[
                    ModelToolCall(
                        id="call-1",
                        name="show_restaurant_card",
                        arguments={"poiId": "poi-salad", "reason": "更贴近高蛋白和清淡需求", "rank": 1},
                    ),
                ],
                text_response="如果你在健身，优先考虑轻食能量碗会更稳。",
            )
        )

        result = service.recommend(
            RecommendationRequest(
                question="换一家，还是想吃高蛋白",
                context=RecommendationContext(
                    previousQuestion="预算35以内，想吃轻一点",
                    rejectedPoiIds=["poi-fried"],
                    userSignals=["健身"],
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


if __name__ == "__main__":
    unittest.main()
