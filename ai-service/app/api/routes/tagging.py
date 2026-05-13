from fastapi import APIRouter, Depends, HTTPException

from app.api.dependencies import get_review_tagging_service
from app.core.exceptions import ModelResponseError, ModelServiceError, ModelTimeoutError
from app.domain.tagging.service import ReviewTaggingService
from app.schemas.tagging import ReviewTagRequest, ReviewTagResponse

router = APIRouter(prefix="/internal/review-tags", tags=["tagging"])


@router.post("", response_model=ReviewTagResponse)
def review_tags(
    request: ReviewTagRequest,
    service: ReviewTaggingService = Depends(get_review_tagging_service),
) -> ReviewTagResponse:
    try:
        return service.summarize(request)
    except ModelTimeoutError as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except (ModelServiceError, ModelResponseError) as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
