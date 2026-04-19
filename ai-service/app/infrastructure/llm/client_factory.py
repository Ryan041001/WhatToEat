from app.core.config import AISettings
from app.infrastructure.llm.openai_compatible import OpenAICompatibleClient, StructuredModelClient


def create_model_client(settings: AISettings) -> StructuredModelClient:
    return OpenAICompatibleClient(settings)
