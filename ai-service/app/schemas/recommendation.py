from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, Field


class RecommendationCandidate(BaseModel):
    poi_id: str = Field(alias="poiId")
    name: str
    address: str
    category: str
    distance: float
    avg_rating: Optional[Decimal] = Field(default=None, alias="avgRating")
    review_count: int = Field(alias="reviewCount")
    avg_per_capita_price: Optional[int] = Field(default=None, alias="avgPerCapitaPrice")
    ai_tags: List[str] = Field(default_factory=list, alias="aiTags")
    ai_summary: Optional[str] = Field(default=None, alias="aiSummary")
    derived_tags: List[str] = Field(default_factory=list, alias="derivedTags")

    model_config = {"populate_by_name": True}


class RecommendationContext(BaseModel):
    previous_question: Optional[str] = Field(default=None, alias="previousQuestion")
    rejected_poi_ids: List[str] = Field(default_factory=list, alias="rejectedPoiIds")
    selected_poi_ids: List[str] = Field(default_factory=list, alias="selectedPoiIds")
    user_signals: List[str] = Field(default_factory=list, alias="userSignals")
    temporal_context: Optional[str] = Field(default=None, alias="temporalContext")
    preference_profile_summary: Optional[str] = Field(default=None, alias="preferenceProfileSummary")
    preferred_tags: List[str] = Field(default_factory=list, alias="preferredTags")
    avoided_tags: List[str] = Field(default_factory=list, alias="avoidedTags")
    recent_feedback_signals: List[str] = Field(default_factory=list, alias="recentFeedbackSignals")

    model_config = {"populate_by_name": True}


class RecommendationRequest(BaseModel):
    question: str
    candidates: List[RecommendationCandidate]
    context: Optional[RecommendationContext] = None


class RecommendationChoice(BaseModel):
    poi_id: str = Field(alias="poiId")
    reason: str

    model_config = {"populate_by_name": True}


class RecommendationAdvice(BaseModel):
    answer: str
    choices: List[RecommendationChoice]
