import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


def load_environment() -> None:
    service_root = Path(__file__).resolve().parents[2]
    repo_root = service_root.parent
    load_dotenv(repo_root / ".env")
    load_dotenv(service_root / ".env", override=True)


@dataclass(frozen=True)
class AISettings:
    api_key: str
    base_url: str
    model: str
    timeout_seconds: float

    @classmethod
    def from_env(cls) -> "AISettings":
        load_environment()
        return cls(
            api_key=os.getenv("OPENAI_API_KEY", "").strip(),
            base_url=normalize_openai_base_url(
                os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1").strip()
            ),
            model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini").strip(),
            timeout_seconds=float(os.getenv("OPENAI_TIMEOUT_SECONDS", "30").strip()),
        )


def normalize_openai_base_url(value: str) -> str:
    normalized = value.rstrip("/")
    if not normalized:
        return ""
    if normalized.endswith("/v1"):
        return normalized
    return normalized + "/v1"
