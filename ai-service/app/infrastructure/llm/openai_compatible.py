import json
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, Dict, List, Optional

from openai import APIConnectionError, APITimeoutError, APIStatusError, OpenAI

from app.core.config import AISettings
from app.core.exceptions import ModelResponseError, ModelServiceError, ModelTimeoutError


@dataclass(frozen=True)
class ModelToolDefinition:
    name: str
    description: str
    parameters: Dict[str, Any]


@dataclass(frozen=True)
class ModelToolCall:
    id: str
    name: str
    arguments: Dict[str, Any]


class StructuredModelClient(ABC):
    @abstractmethod
    def generate_json(self, system_prompt: str, user_prompt: str) -> Dict[str, Any]:
        raise NotImplementedError

    def generate_tool_calls(
        self,
        system_prompt: str,
        user_prompt: str,
        tools: List[ModelToolDefinition],
    ) -> List[ModelToolCall]:
        raise NotImplementedError

    def generate_text(
        self,
        system_prompt: str,
        user_prompt: str,
        tool_calls: Optional[List[ModelToolCall]] = None,
        tool_outputs: Optional[List[Dict[str, Any]]] = None,
    ) -> str:
        raise NotImplementedError

    def stream_text(
        self,
        system_prompt: str,
        user_prompt: str,
        tool_calls: Optional[List[ModelToolCall]] = None,
        tool_outputs: Optional[List[Dict[str, Any]]] = None,
    ):
        raise NotImplementedError


class OpenAICompatibleClient(StructuredModelClient):
    def __init__(self, settings: AISettings) -> None:
        self.settings = settings
        self.client = OpenAI(
            api_key=settings.api_key,
            base_url=settings.base_url,
            timeout=settings.timeout_seconds,
        )

    def generate_json(self, system_prompt: str, user_prompt: str) -> Dict[str, Any]:
        self._ensure_configured()
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

        try:
            completion = self._create_chat_completion(
                messages=messages,
                response_format={"type": "json_object"},
            )
        except (APIStatusError, TypeError, ValueError):
            completion = self._create_without_json_mode(messages)

        content = self._extract_content(completion)
        try:
            return json.loads(content)
        except json.JSONDecodeError:
            extracted = self._extract_json_fragment(content)
            if extracted is None:
                raise ModelResponseError("Model response is not valid JSON")
            return json.loads(extracted)

    def generate_tool_calls(
        self,
        system_prompt: str,
        user_prompt: str,
        tools: List[ModelToolDefinition],
    ) -> List[ModelToolCall]:
        if not tools:
            return []
        self._ensure_configured()
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]
        try:
            completion = self._create_chat_completion(
                messages=messages,
                tools=[
                    {
                        "type": "function",
                        "function": {
                            "name": tool.name,
                            "description": tool.description,
                            "parameters": tool.parameters,
                        },
                    }
                    for tool in tools
                ],
                tool_choice="auto",
            )
        except APIStatusError as exc:
            if exc.status_code in {400, 404, 422}:
                return []
            raise ModelServiceError(str(exc)) from exc
        except (TypeError, ValueError):
            return []

        return self._extract_tool_calls(completion)

    def generate_text(
        self,
        system_prompt: str,
        user_prompt: str,
        tool_calls: Optional[List[ModelToolCall]] = None,
        tool_outputs: Optional[List[Dict[str, Any]]] = None,
    ) -> str:
        self._ensure_configured()
        messages = self._build_messages(system_prompt, user_prompt, tool_calls, tool_outputs)
        completion = self._create_chat_completion(messages=messages)
        return self._extract_content(completion)

    def stream_text(
        self,
        system_prompt: str,
        user_prompt: str,
        tool_calls: Optional[List[ModelToolCall]] = None,
        tool_outputs: Optional[List[Dict[str, Any]]] = None,
    ):
        self._ensure_configured()
        messages = self._build_messages(system_prompt, user_prompt, tool_calls, tool_outputs)

        try:
            stream = self.client.chat.completions.create(
                model=self.settings.model,
                temperature=0.2,
                messages=messages,
                stream=True,
            )
            for chunk in stream:
                if not chunk.choices:
                    continue
                delta = getattr(chunk.choices[0], "delta", None)
                if delta is None:
                    continue
                content = getattr(delta, "content", None)
                text = self._extract_stream_text(content)
                if text:
                    yield text
        except APITimeoutError as exc:
            raise ModelTimeoutError(str(exc)) from exc
        except APIConnectionError as exc:
            raise ModelServiceError(str(exc)) from exc
        except Exception as exc:
            raise ModelServiceError(str(exc)) from exc

    def _build_messages(
        self,
        system_prompt: str,
        user_prompt: str,
        tool_calls: Optional[List[ModelToolCall]] = None,
        tool_outputs: Optional[List[Dict[str, Any]]] = None,
    ) -> List[Dict[str, Any]]:
        messages: List[Dict[str, Any]] = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]
        if tool_calls:
            messages.append(
                {
                    "role": "assistant",
                    "content": "",
                    "tool_calls": [
                        {
                            "id": tool_call.id,
                            "type": "function",
                            "function": {
                                "name": tool_call.name,
                                "arguments": json.dumps(tool_call.arguments, ensure_ascii=False),
                            },
                        }
                        for tool_call in tool_calls
                    ],
                }
            )
        if tool_calls and tool_outputs:
            output_by_call_id = {output["toolCallId"]: output for output in tool_outputs if output.get("toolCallId")}
            for tool_call in tool_calls:
                output = output_by_call_id.get(tool_call.id)
                if output is None:
                    continue
                messages.append(
                    {
                        "role": "tool",
                        "tool_call_id": tool_call.id,
                        "content": json.dumps(output, ensure_ascii=False),
                    }
                )
        return messages

    def _create_without_json_mode(self, messages: List[Dict[str, str]]) -> Any:
        return self._create_chat_completion(messages=messages)

    def _create_chat_completion(self, **kwargs: Any) -> Any:
        self._ensure_configured()
        try:
            return self.client.chat.completions.create(
                model=self.settings.model,
                temperature=0.2,
                **kwargs,
            )
        except APITimeoutError as exc:
            raise ModelTimeoutError(str(exc)) from exc
        except APIConnectionError as exc:
            raise ModelServiceError(str(exc)) from exc
        except APIStatusError:
            raise
        except Exception as exc:
            raise ModelServiceError(str(exc)) from exc

    def _ensure_configured(self) -> None:
        if not self.settings.api_key:
            raise ModelServiceError("OPENAI_API_KEY is not configured")
        if not self.settings.base_url:
            raise ModelServiceError("OPENAI_BASE_URL is not configured")
        if not self.settings.model:
            raise ModelServiceError("OPENAI_MODEL is not configured")

    def _extract_content(self, completion: Any) -> str:
        if not completion.choices:
            raise ModelResponseError("Model returned no choices")
        message = completion.choices[0].message
        content = getattr(message, "content", None)
        if isinstance(content, str) and content.strip():
            return content.strip()
        if isinstance(content, list):
            parts = []
            for item in content:
                if isinstance(item, str) and item.strip():
                    parts.append(item.strip())
                    continue
                if isinstance(item, dict):
                    text = item.get("text")
                    if isinstance(text, str) and text.strip():
                        parts.append(text.strip())
                        continue
                text = getattr(item, "text", None)
                if isinstance(text, str) and text.strip():
                    parts.append(text.strip())
            if parts:
                return "".join(parts)
        raise ModelResponseError("Model returned empty content")

    def _extract_tool_calls(self, completion: Any) -> List[ModelToolCall]:
        if not completion.choices:
            return []
        message = completion.choices[0].message
        tool_calls = getattr(message, "tool_calls", None) or []
        parsed: List[ModelToolCall] = []
        for index, tool_call in enumerate(tool_calls, start=1):
            function = getattr(tool_call, "function", None)
            name = self._read_attr(function, "name")
            arguments_text = self._read_attr(function, "arguments")
            if not name or not arguments_text:
                continue
            try:
                arguments = json.loads(arguments_text)
            except json.JSONDecodeError:
                extracted = self._extract_json_fragment(arguments_text)
                if extracted is None:
                    continue
                arguments = json.loads(extracted)
            if not isinstance(arguments, dict):
                continue
            tool_call_id = self._read_attr(tool_call, "id") or f"tool_call_{index}"
            parsed.append(ModelToolCall(id=str(tool_call_id), name=str(name), arguments=arguments))
        return parsed

    def _read_attr(self, obj: Any, key: str) -> Optional[str]:
        if obj is None:
            return None
        if isinstance(obj, dict):
            value = obj.get(key)
        else:
            value = getattr(obj, key, None)
        return value if isinstance(value, str) else None

    def _extract_json_fragment(self, content: str) -> Optional[str]:
        start = content.find("{")
        end = content.rfind("}")
        if start == -1 or end == -1 or end <= start:
            return None
        return content[start:end + 1]

    def _extract_stream_text(self, content: Any) -> str:
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts: List[str] = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                    continue
                if isinstance(item, dict):
                    text = item.get("text")
                    if isinstance(text, str):
                        parts.append(text)
                        continue
                text = getattr(item, "text", None)
                if isinstance(text, str):
                    parts.append(text)
            return "".join(parts)
        return ""
