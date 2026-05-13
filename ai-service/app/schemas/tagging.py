from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, Field


class ReviewText(BaseModel):
    content: str
    rating_score: Optional[Decimal] = Field(default=None, alias="ratingScore")
    per_capita_price: Optional[int] = Field(default=None, alias="perCapitaPrice")

    model_config = {"populate_by_name": True}


class ReviewTagRequest(BaseModel):
    poi_id: str = Field(alias="poiId")
    poi_name: Optional[str] = Field(default=None, alias="poiName")
    reviews: List[ReviewText]

    model_config = {"populate_by_name": True}


class ReviewTagResponse(BaseModel):
    tag1: Optional[str] = None
    tag2: Optional[str] = None
    summary: Optional[str] = None
