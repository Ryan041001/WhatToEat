from functools import lru_cache

from app.core.config import AISettings
from app.domain.recommendation.service import RecommendationService
from app.domain.tagging.service import ReviewTaggingService
from app.infrastructure.llm.client_factory import create_model_client
from app.infrastructure.llm.openai_compatible import StructuredModelClient


@lru_cache
def get_ai_settings() -> AISettings:
    return AISettings.from_env()


@lru_cache
def get_model_client() -> StructuredModelClient:
    return create_model_client(get_ai_settings())


@lru_cache
def get_recommendation_service() -> RecommendationService:
    return RecommendationService(get_model_client())


@lru_cache
def get_review_tagging_service() -> ReviewTaggingService:
    return ReviewTaggingService(get_model_client())
