# 数据库索引分析报告

日期: 2026-06-03
分支: feat/optimize-linjiatao-14

## 分析目标

根据"性能优化与移动端体验"课程中的慢查询分析原则，检查当前数据库索引是否覆盖所有高频查询。

## 分析方法

对照所有 JPA Repository 的 finder 方法与对应的 Flyway migration DDL，逐表检查 WHERE + ORDER BY 列是否有索引覆盖。

## 结果

| 表 | 查询 | 索引 | 覆盖 |
|----|------|------|------|
| users | findByOpenid | uk_openid(openid) | ✅ |
| user_blacklist | findByUserId / findByUserIdAndPoiId | uk_user_poi(user_id, poi_id) | ✅ |
| user_restaurant_note | findByUserId / findByUserIdAndPoiId / findByUserIdAndNoteContaining | uk_user_poi(user_id, poi_id) | ✅ |
| user_choice_history | findByUserIdOrderByChosenAtDesc | idx_user_chosen(user_id, chosen_at) | ✅ |
| restaurant_review | findByPoiIdOrderByUpdatedAtDescIdDesc | idx_review_poi_updated(poi_id, updated_at, id) | ✅ |
| restaurant_review | findByUserIdAndPoiId | uk_review_user_poi(user_id, poi_id) | ✅ |
| restaurant_metric_snapshot | findAllById (batch) | PK(poi_id) | ✅ |
| recommendation_feedback | findByUserIdOrderByCreatedAtDescIdDesc | idx_feedback_user_created(user_id, created_at, id) | ✅ |

### 低优先提醒

- `restaurant_review.findByUserIdOrderByUpdatedAtDescIdDesc` — 使用 `uk_review_user_poi(user_id, poi_id)` 过滤，但 `updated_at` 排序不在索引中，理论上会产生 filesort。**实际影响可忽略**：单用户评论量通常 0-10 条，filesort 成本极低。
- 若未来数据量大幅增长，可考虑加 `(user_id, updated_at, id)` 复合索引。

## 结论

**当前无需新增索引。** 所有高频查询的 WHERE 条件都已被现有索引覆盖，排序字段在大部分场景中也被包含在复合索引中。

这体现了幻灯片的原则："先测量，再优化"——分析后再决定是否加索引，而不是猜测。
