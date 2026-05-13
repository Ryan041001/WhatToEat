from types import SimpleNamespace

import httpx
import pytest

from app.core.config import AISettings
from app.core.exceptions import ModelResponseError, ModelServiceError, ModelTimeoutError
from app.infrastructure.llm import openai_compatible as module
from app.infrastructure.llm.openai_compatible import ModelToolCall, ModelToolDefinition, OpenAICompatibleClient


class FakeCompletions:
    def __init__(self):
        self.calls = []
        self.responses = []

    def create(self, **kwargs):
        self.calls.append(kwargs)
        response = self.responses.pop(0)
        if isinstance(response, Exception):
            raise response
        return response


class FakeOpenAI:
    def __init__(self, **kwargs):
        self.kwargs = kwargs
        self.chat = SimpleNamespace(completions=FakeCompletions())


def make_completion(content=None, tool_calls=None):
    return SimpleNamespace(
        choices=[
            SimpleNamespace(
                message=SimpleNamespace(
                    content=content,
                    tool_calls=tool_calls,
                )
            )
        ]
    )


def make_tool_call(name, arguments, tool_call_id=None):
    return SimpleNamespace(
        id=tool_call_id,
        function=SimpleNamespace(
            name=name,
            arguments=arguments,
        ),
    )


def make_stream_chunk(content):
    return SimpleNamespace(
        choices=[SimpleNamespace(delta=SimpleNamespace(content=content))]
    )


def make_client(monkeypatch, settings=None):
    fake_openai_instances = []

    def factory(**kwargs):
        instance = FakeOpenAI(**kwargs)
        fake_openai_instances.append(instance)
        return instance

    monkeypatch.setattr(module, "OpenAI", factory)
    client = OpenAICompatibleClient(
        settings or AISettings(
            api_key="test-key",
            base_url="https://example.com/v1",
            model="gpt-test",
            timeout_seconds=12.0,
        )
    )
    return client, fake_openai_instances[0].chat.completions


def status_error(status_code):
    request = httpx.Request("POST", "https://example.com/v1/chat/completions")
    response = httpx.Response(status_code, request=request)
    return module.APIStatusError(f"status {status_code}", response=response, body={})


def test_generate_json_should_parse_json_fragment_when_wrapped_by_text(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.append(make_completion("prefix {\"answer\":\"ok\"} suffix"))

    result = client.generate_json("system", "user")

    assert result == {"answer": "ok"}
    assert completions.calls[0]["response_format"] == {"type": "json_object"}


def test_generate_json_should_fallback_without_json_mode_when_api_rejects_json_mode(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.extend([
        status_error(400),
        make_completion("{\"answer\":\"fallback\"}"),
    ])

    result = client.generate_json("system", "user")

    assert result == {"answer": "fallback"}
    assert len(completions.calls) == 2
    assert "response_format" in completions.calls[0]
    assert "response_format" not in completions.calls[1]


def test_generate_json_should_raise_when_no_json_can_be_extracted(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.append(make_completion("not-json"))

    with pytest.raises(ModelResponseError):
        client.generate_json("system", "user")


def test_generate_tool_calls_should_parse_missing_id_and_json_fragment(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.append(
        make_completion(
            tool_calls=[
                make_tool_call("show_restaurant_card", "prefix {\"poiId\":\"poi-1\",\"rank\":1} suffix"),
                make_tool_call("ignored_tool", "\"string\"", "call-2"),
            ]
        )
    )

    result = client.generate_tool_calls(
        "system",
        "user",
        [ModelToolDefinition(name="dummy", description="", parameters={})],
    )

    assert result == [ModelToolCall(id="tool_call_1", name="show_restaurant_card", arguments={"poiId": "poi-1", "rank": 1})]


def test_generate_tool_calls_should_return_empty_for_supported_client_errors(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.append(status_error(422))

    result = client.generate_tool_calls("system", "user", [SimpleNamespace(name="tool", description="", parameters={})])

    assert result == []


def test_generate_tool_calls_should_raise_for_upstream_errors(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.append(status_error(500))

    with pytest.raises(ModelServiceError):
        client.generate_tool_calls("system", "user", [SimpleNamespace(name="tool", description="", parameters={})])


def test_generate_text_should_include_tool_messages(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.append(make_completion("final answer"))

    result = client.generate_text(
        "system",
        "user",
        tool_calls=[ModelToolCall(id="call-1", name="show_restaurant_card", arguments={"poiId": "poi-1"})],
        tool_outputs=[{"toolCallId": "call-1", "poiId": "poi-1", "name": "轻食"}],
    )

    assert result == "final answer"
    messages = completions.calls[0]["messages"]
    assert messages[2]["role"] == "assistant"
    assert messages[3]["role"] == "tool"
    assert messages[3]["tool_call_id"] == "call-1"


def test_stream_text_should_merge_list_and_object_deltas(monkeypatch):
    client, completions = make_client(monkeypatch)
    completions.responses.append(
        [
            make_stream_chunk(["热", {"text": "汤"}, SimpleNamespace(text="面")]),
            make_stream_chunk(None),
            make_stream_chunk("好"),
        ]
    )

    result = list(client.stream_text("system", "user"))

    assert result == ["热汤面", "好"]
    assert completions.calls[0]["stream"] is True


def test_stream_text_should_map_timeout_and_connection_errors(monkeypatch):
    client, completions = make_client(monkeypatch)
    request = httpx.Request("POST", "https://example.com/v1/chat/completions")
    completions.responses.append(module.APITimeoutError(request=request))

    with pytest.raises(ModelTimeoutError):
        list(client.stream_text("system", "user"))

    completions.responses.append(module.APIConnectionError(message="offline", request=request))
    with pytest.raises(ModelServiceError):
        list(client.stream_text("system", "user"))


def test_generate_json_should_fail_fast_when_configuration_missing(monkeypatch):
    client, _ = make_client(
        monkeypatch,
        AISettings(api_key="", base_url="https://example.com/v1", model="gpt-test", timeout_seconds=5.0),
    )

    with pytest.raises(ModelServiceError):
        client.generate_json("system", "user")
