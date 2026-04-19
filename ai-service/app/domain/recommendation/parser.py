from typing import Any, List, Tuple

from app.infrastructure.llm.openai_compatible import ModelToolCall
from app.schemas.recommendation import RecommendationChoice


def parse_ranked_tool_calls(
    valid_poi_ids: set[str],
    tool_calls: List[ModelToolCall],
) -> Tuple[List[RecommendationChoice], List[ModelToolCall]]:
    parsed_with_rank: List[tuple[int, RecommendationChoice, ModelToolCall]] = []
    seen_poi_ids = set()
    seen_ranks = set()
    for tool_call in tool_calls:
        if tool_call.name != "show_restaurant_card":
            continue
        poi_id = str(tool_call.arguments.get("poiId", "")).strip()
        reason = str(tool_call.arguments.get("reason", "")).strip()
        rank_value = tool_call.arguments.get("rank")
        if not isinstance(rank_value, int):
            continue
        if poi_id not in valid_poi_ids or not reason or poi_id in seen_poi_ids or rank_value in seen_ranks:
            continue
        parsed_with_rank.append(
            (
                rank_value,
                RecommendationChoice(poiId=poi_id, reason=reason[:80]),
                tool_call,
            )
        )
        seen_poi_ids.add(poi_id)
        seen_ranks.add(rank_value)
        if len(parsed_with_rank) == 3:
            break

    parsed_with_rank.sort(key=lambda item: item[0])
    return (
        [item[1] for item in parsed_with_rank],
        [item[2] for item in parsed_with_rank],
    )


def parse_json_choices(
    valid_poi_ids: set[str],
    raw_choices: List[dict[str, Any]],
) -> List[RecommendationChoice]:
    parsed_choices: List[RecommendationChoice] = []
    for choice in raw_choices:
        poi_id = str(choice.get("poiId", "")).strip()
        reason = str(choice.get("reason", "")).strip()
        if poi_id in valid_poi_ids and reason:
            parsed_choices.append(RecommendationChoice(poiId=poi_id, reason=reason[:80]))
        if len(parsed_choices) == 3:
            break
    return parsed_choices
