import json

from app.clients.openai_compatible import StructuredModelClient
from app.schemas import ReviewTagRequest, ReviewTagResponse


class ReviewTaggingService:
    def __init__(self, model_client: StructuredModelClient) -> None:
        self.model_client = model_client

    def summarize(self, request: ReviewTagRequest) -> ReviewTagResponse:
        reviews = [review for review in request.reviews if review.content.strip()]
        if not reviews:
            return ReviewTagResponse(tag1=None, tag2=None, summary=None)

        payload = {
            "poiId": request.poi_id,
            "poiName": request.poi_name,
            "reviews": [
                {
                    "content": review.content.strip(),
                    "ratingScore": str(review.rating_score) if review.rating_score is not None else None,
                    "perCapitaPrice": review.per_capita_price,
                }
                for review in reviews
            ],
        }
        result = self.model_client.generate_json(
            system_prompt=(
                "你是餐厅点评摘要助手。"
                "请根据用户评论提炼 1 到 2 个适合前端展示的中文短标签，并给出一句简短摘要。"
                "输出必须是 JSON 对象，字段只有 tag1、tag2、summary。"
                "tag1 和 tag2 必须是 2 到 8 个汉字，不要使用标点，不要重复，不要太空泛。"
                "summary 必须是 1 句中文，总结评论共识，长度控制在 12 到 40 个字。"
                "如果评论信息不足，tag2 可以为 null。"
            ),
            user_prompt=json.dumps(payload, ensure_ascii=False),
        )
        return ReviewTagResponse(
            tag1=self._normalize_tag(result.get("tag1")),
            tag2=self._normalize_tag(result.get("tag2")),
            summary=self._normalize_summary(result.get("summary")),
        )

    def _normalize_tag(self, value):
        if value is None:
            return None
        tag = str(value).strip()
        return tag[:8] if tag else None

    def _normalize_summary(self, value):
        if value is None:
            return None
        summary = str(value).strip()
        return summary[:80] if summary else None
