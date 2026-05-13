from fastapi import FastAPI

from app.api.routes.health import router as health_router
from app.api.routes.recommendation import router as recommendation_router
from app.api.routes.tagging import router as tagging_router


def create_app() -> FastAPI:
    app = FastAPI(title="WhatToEat AI Service", version="0.1.0")
    app.include_router(health_router)
    app.include_router(tagging_router)
    app.include_router(recommendation_router)
    return app


app = create_app()
