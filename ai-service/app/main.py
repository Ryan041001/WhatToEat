import json

from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse

from app.clients.openai_compatible import (
    ModelResponseError,
    ModelServiceError,
    ModelTimeoutError,
    OpenAICompatibleClient,
)
from app.config import AISettings
from app.schemas import (
    HealthResponse,
    RecommendationAdvice,
    RecommendationRequest,
    ReviewTagRequest,
    ReviewTagResponse,
)
from app.services.recommendation import RecommendationService
from app.services.tagging import ReviewTaggingService

app = FastAPI(title="WhatToEat AI Service", version="0.1.0")

settings = AISettings.from_env()
model_client = OpenAICompatibleClient(settings)
tagging_service = ReviewTaggingService(model_client)
recommendation_service = RecommendationService(model_client)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.post("/internal/review-tags", response_model=ReviewTagResponse)
def review_tags(request: ReviewTagRequest) -> ReviewTagResponse:
    try:
        return tagging_service.summarize(request)
    except ModelTimeoutError as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except (ModelServiceError, ModelResponseError) as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@app.post("/internal/recommend", response_model=RecommendationAdvice)
def recommend(request: RecommendationRequest) -> RecommendationAdvice:
    try:
        return recommendation_service.recommend(request)
    except ModelTimeoutError as exc:
        raise HTTPException(status_code=504, detail=str(exc)) from exc
    except (ModelServiceError, ModelResponseError) as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@app.post("/internal/recommend/stream")
def recommend_stream(request: RecommendationRequest) -> StreamingResponse:
    def event_stream():
        try:
            for event_name, payload in recommendation_service.stream_recommend(request):
                yield f"event:{event_name}\n"
                yield f"data:{payload.model_dump_json(by_alias=True, ensure_ascii=False) if hasattr(payload, 'model_dump_json') else json.dumps(payload, ensure_ascii=False)}\n\n"
        except ModelTimeoutError as exc:
            yield f"event:error\n"
            yield f"data:{{\"code\":3005,\"message\":{json.dumps(str(exc), ensure_ascii=False)}}}\n\n"
        except (ModelServiceError, ModelResponseError) as exc:
            yield f"event:error\n"
            yield f"data:{{\"code\":3004,\"message\":{json.dumps(str(exc), ensure_ascii=False)}}}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
