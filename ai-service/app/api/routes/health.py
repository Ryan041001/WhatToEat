from datetime import datetime, timezone
from importlib.metadata import PackageNotFoundError, version

from fastapi import APIRouter

from app.core.metrics import metrics
from app.schemas.health import HealthResponse, MetricsResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="healthy",
        timestamp=datetime.now(timezone.utc).isoformat(),
        version=service_version(),
    )


@router.get("/metrics", response_model=MetricsResponse)
def metrics_snapshot() -> MetricsResponse:
    return MetricsResponse(**metrics.snapshot())


def service_version() -> str:
    try:
        return version("whattoeat-ai-service")
    except PackageNotFoundError:
        return "0.1.0"
