from app.domain.recommendation.prompts import (
    answer_system_prompt,
    json_fallback_system_prompt,
    streaming_answer_system_prompt,
    tool_selection_system_prompt,
)


def test_tool_selection_prompt_should_prefer_three_cards_when_available():
    prompt = tool_selection_system_prompt()

    assert "优先推荐 3 家" in prompt
    assert "候选不足 3 家" in prompt
    assert "推荐几家就调用几次" in prompt


def test_json_fallback_prompt_should_match_candidate_count_when_less_than_three():
    prompt = json_fallback_system_prompt()

    assert "优先给出 3 项" in prompt
    assert "候选不足 3 家" in prompt
    assert "有几家候选就只给几项" in prompt


def test_streaming_answer_prompt_should_not_expand_beyond_selected_cards():
    prompt = streaming_answer_system_prompt()

    assert "只围绕已选餐厅" in prompt
    assert "不要额外补充未选中的餐厅" in prompt


def test_answer_prompts_should_encourage_readable_markdown():
    prompts = [
        answer_system_prompt(),
        streaming_answer_system_prompt(),
        json_fallback_system_prompt(),
    ]

    for prompt in prompts:
        assert "善用 Markdown" in prompt
        assert "**重点**" in prompt
        assert "列表" in prompt
