import json

from fastapi.testclient import TestClient

from app.api.dependencies import get_recommendation_service, get_review_tagging_service
from app.core.exceptions import ModelResponseError, ModelServiceError, ModelTimeoutError
from app.main import create_app
from app.schemas.recommendation import RecommendationAdvice, RecommendationChoice
from app.schemas.tagging import ReviewTagResponse


class StubTaggingService:
    def __init__(self, response=None, error=None):
        self.response = response or ReviewTagResponse(tag1="性价比高", tag2="热汤稳", summary="适合预算内吃热的。")
        self.error = error

    def summarize(self, request):
        if self.error is not None:
            raise self.error
        return self.response


class StubRecommendationService:
    def __init__(self, response=None, stream_events=None, error=None):
        self.response = response or RecommendationAdvice(
            answer="优先考虑鸡胸肉能量碗。",
            choices=[RecommendationChoice(poiId="poi-1", reason="更贴近高蛋白和清淡需求")],
        )
        self.stream_events = stream_events or [
            ("tool.call", {"toolName": "show_restaurant_card", "arguments": {"poiId": "poi-1", "reason": "高蛋白", "rank": 1}}),
            ("answer.delta", {"delta": "优先考虑鸡胸肉能量碗。"}),
            ("answer.done", {"answer": "优先考虑鸡胸肉能量碗。"}),
            ("done", {"finishReason": "stop"}),
        ]
        self.error = error

    def recommend(self, request):
        if self.error is not None:
            raise self.error
        return self.response

    def stream_recommend(self, request):
        if self.error is not None:
            raise self.error
        for event in self.stream_events:
            yield event


def create_test_client(tagging_service=None, recommendation_service=None):
    app = create_app()
    app.dependency_overrides[get_review_tagging_service] = lambda: tagging_service or StubTaggingService()
    app.dependency_overrides[get_recommendation_service] = lambda: recommendation_service or StubRecommendationService()
    return TestClient(app)


def recommendation_payload():
    return {
        "question": "想吃高蛋白",
        "candidates": [
            {
                "poiId": "poi-1",
                "name": "鸡胸肉能量碗",
                "address": "学林街",
                "category": "轻食",
                "distance": 220,
                "avgRating": "4.7",
                "reviewCount": 18,
                "avgPerCapitaPrice": 31,
                "aiTags": ["高蛋白", "清淡"],
                "aiSummary": "鸡胸肉和热汤都很受欢迎",
                "derivedTags": ["健身友好"],
            }
        ],
    }


def test_health_endpoint_should_return_ok_status():
    client = create_test_client()

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_review_tags_should_map_model_timeout_to_504():
    client = create_test_client(tagging_service=StubTaggingService(error=ModelTimeoutError("timeout")))

    response = client.post(
        "/internal/review-tags",
        json={
            "poiId": "poi-1",
            "poiName": "兰州拉面",
            "reviews": [{"content": "热汤稳", "ratingScore": "4.6", "perCapitaPrice": 28}],
        },
    )

    assert response.status_code == 504
    assert response.json() == {"detail": "timeout"}


def test_recommend_should_return_structured_advice():
    client = create_test_client()

    response = client.post("/internal/recommend", json=recommendation_payload())

    assert response.status_code == 200
    assert response.json() == {
        "answer": "优先考虑鸡胸肉能量碗。",
        "choices": [{"poiId": "poi-1", "reason": "更贴近高蛋白和清淡需求"}],
    }


def test_recommend_should_map_model_service_error_to_502():
    client = create_test_client(recommendation_service=StubRecommendationService(error=ModelServiceError("upstream failed")))

    response = client.post("/internal/recommend", json=recommendation_payload())

    assert response.status_code == 502
    assert response.json() == {"detail": "upstream failed"}


def test_recommend_stream_should_emit_sse_events_in_contract_shape():
    client = create_test_client()

    with client.stream("POST", "/internal/recommend/stream", json=recommendation_payload()) as response:
        chunks = [chunk for chunk in response.iter_text() if chunk]

    body = "".join(chunks)
    assert response.status_code == 200
    assert "event:tool.call" in body
    assert "event:answer.delta" in body
    assert "event:answer.done" in body
    assert "event:done" in body
    assert "鸡胸肉能量碗" in body


def test_recommend_stream_should_map_model_response_error_to_error_event():
    client = create_test_client(recommendation_service=StubRecommendationService(error=ModelResponseError("bad payload")))

    with client.stream("POST", "/internal/recommend/stream", json=recommendation_payload()) as response:
        body = "".join(chunk for chunk in response.iter_text() if chunk)

    assert response.status_code == 200
    assert "event:error" in body
    assert json.loads(body.split("data:", 1)[1].strip()) == {"code": 3004, "message": "bad payload"}


def test_openapi_should_document_internal_ai_contracts():
    client = create_test_client()
    schema = client.get("/openapi.json").json()

    assert "/internal/review-tags" in schema["paths"]
    assert "/internal/recommend" in schema["paths"]
    assert "/internal/recommend/stream" in schema["paths"]
    assert schema["paths"]["/internal/recommend"]["post"]["responses"]["200"]["content"]["application/json"]["schema"] == {
        "$ref": "#/components/schemas/RecommendationAdvice"
    }
    assert schema["paths"]["/internal/review-tags"]["post"]["responses"]["200"]["content"]["application/json"]["schema"] == {
        "$ref": "#/components/schemas/ReviewTagResponse"
    }
