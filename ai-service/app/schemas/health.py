from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    timestamp: str
    version: str


class MetricsResponse(BaseModel):
    requestCount: int
    errorCount: int
    averageResponseTimeMs: float
    errorRate: float
