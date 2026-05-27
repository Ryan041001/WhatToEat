import logging
import time

from fastapi import FastAPI

from app.api.routes.health import router as health_router
from app.api.routes.recommendation import router as recommendation_router
from app.api.routes.tagging import router as tagging_router
from app.core.metrics import metrics
from app.utils.logger import configure_logging

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    configure_logging()
    app = FastAPI(title="WhatToEat AI Service", version="0.1.0")
    app.include_router(health_router)
    app.include_router(tagging_router)
    app.include_router(recommendation_router)

    @app.middleware("http")
    async def collect_metrics(request, call_next):
        start_at = time.perf_counter()
        response = await call_next(request)
        response_time_ms = (time.perf_counter() - start_at) * 1000
        metrics.record(response.status_code, response_time_ms)
        logger.info(
            "request",
            extra={
                "method": request.method,
                "path": request.url.path,
                "status_code": response.status_code,
                "response_time_ms": round(response_time_ms, 3),
            },
        )
        return response

    return app


app = create_app()
