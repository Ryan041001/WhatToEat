import json
import logging

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from app.api.dependencies import get_recommendation_service
from app.core.exceptions import ModelResponseError, ModelServiceError, ModelTimeoutError
from app.domain.recommendation.service import RecommendationService
from app.schemas.recommendation import RecommendationAdvice, RecommendationRequest

router = APIRouter(prefix="/internal/recommend", tags=["recommendation"])
logger = logging.getLogger(__name__)


@router.post("", response_model=RecommendationAdvice)
def recommend(
    request: RecommendationRequest,
    service: RecommendationService = Depends(get_recommendation_service),
) -> RecommendationAdvice:
    try:
        return service.recommend(request)
    except ModelTimeoutError as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except (ModelServiceError, ModelResponseError) as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.post("/stream")
def recommend_stream(
    request: RecommendationRequest,
    service: RecommendationService = Depends(get_recommendation_service),
) -> StreamingResponse:
    def event_stream():
        try:
            for event_name, payload in service.stream_recommend(request):
                yield f"event:{event_name}\n"
                serialized = (
                    payload.model_dump_json(by_alias=True, ensure_ascii=False)
                    if hasattr(payload, "model_dump_json")
                    else json.dumps(payload, ensure_ascii=False)
                )
                yield f"data:{serialized}\n\n"
        except ModelTimeoutError as exc:
            yield "event:error\n"
            yield f"data:{{\"code\":3005,\"message\":{json.dumps(str(exc), ensure_ascii=False)}}}\n\n"
        except (ModelServiceError, ModelResponseError) as exc:
            yield "event:error\n"
            yield f"data:{{\"code\":3004,\"message\":{json.dumps(str(exc), ensure_ascii=False)}}}\n\n"
        except Exception as exc:
            logger.exception("Unexpected streaming recommendation failure")
            yield "event:error\n"
            yield f"data:{{\"code\":3004,\"message\":{json.dumps(str(exc), ensure_ascii=False)}}}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
