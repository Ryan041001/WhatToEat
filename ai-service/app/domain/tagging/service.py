import json

from app.domain.tagging.prompts import review_tagging_system_prompt
from app.infrastructure.llm.openai_compatible import StructuredModelClient
from app.schemas.tagging import ReviewTagRequest, ReviewTagResponse


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
            system_prompt=review_tagging_system_prompt(),
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
