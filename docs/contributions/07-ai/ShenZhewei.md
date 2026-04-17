# AI 功能集成贡献说明

姓名：沈哲伟  
学号：2312190313  
日期：2026-04-18

## 我完成的工作

### 1. AI 功能

- 功能类型：评论标签摘要、评论摘要生成、AI 推荐问答、AI 流式推荐问答、AI refine、轻量口味画像、最近吃过过滤、推荐反馈闭环
- 使用模型：OpenAI-compatible 模型接入，默认模型为 `gpt-4.1-mini`

### 2. 实现内容

- [x] 后端 API
- [x] 前端对接文档
- [x] 错误处理

### 3. 具体完成项

- [x] 新增内部 `ai-service/` 服务，使用 FastAPI 承载 AI 能力
- [x] 实现 OpenAI-compatible 模型客户端封装
- [x] 实现评论标签与摘要生成能力
- [x] 实现 AI 推荐问答同步接口
- [x] 实现 AI 推荐问答流式接口
- [x] 在 backend 中新增 AI 集成层（`integration/ai/*`）
- [x] 在推荐链路中接入 AI 推荐能力
- [x] 在评论链路中接入 AI 标签摘要能力
- [x] 为 AI 推荐补充 refine / 连续追问上下文（`context`）
- [x] 接入最近吃过记录（`choice-history`）并进入推荐软过滤
- [x] 接入推荐反馈表（`recommendation_feedback`）并进入推荐闭环
- [x] 实现轻量口味画像接口（`preference-profile`）
- [x] 给详情页补充 `recommendedScenarios` 场景解释字段
- [x] 补充 AI 失败映射与超时映射（`3004 / 3005`）
- [x] 修复 AI 摘要失败时旧摘要残留的一致性问题
- [x] 补充 API 文档、前端对接文档与项目级 AGENTS 说明

### 4. 关键实现说明

#### 4.1 评论摘要能力

- 后端在评论写入或删除后，会触发：
  - 评论聚合刷新
  - AI 标签摘要刷新
- AI Service 会根据当前餐厅评论集合生成：
  - `tag1`
  - `tag2`
  - `summary`
- backend 再把结果写回 `restaurant_metric_snapshot`

#### 4.2 AI 推荐能力

- backend 先从高德拉取候选餐厅
- 再合并本地聚合快照：
  - `avgRating`
  - `reviewCount`
  - `avgPerCapitaPrice`
  - `aiTags`
- `aiSummary`
- `derivedTags`
- 然后把增强候选集发给 AI Service
- AI Service 使用 tool call 思路输出结构化推荐结果

这次我进一步把推荐从“一次性问答”推进到“轻量 AI native 推荐闭环”：

- 支持 `context.previousQuestion`
- 支持 `context.rejectedPoiIds`
- 支持 `context.userSignals`
- 后端会自动叠加：
  - 最近吃过记录
  - 推荐反馈信号
  - 轻量口味画像

这样前端后续只要补少量接入，就能自然支持：

- 换一家，但保持条件
- 太贵了 / 太远了
- 想吃热汤
- 不要快餐
- 在健身 / 减脂 / 高蛋白

#### 4.3 流式推荐能力

- 新增 `POST /api/v1/recommendations/ask/stream`
- 对前端输出结构化 SSE 事件：
  - `session.created`
  - `retrieval.started`
  - `retrieval.completed`
  - `recommendation.card`
  - `answer.delta`
  - `answer.done`
  - `done`
  - `error`

#### 4.4 一致性与错误处理

- AI 上游失败映射为 `3004`
- AI 上游超时映射为 `3005`
- `review-summary` 当前不暴露 `aiStatus`
- 只有 AI 状态 `ready` 时，才对外返回：
  - `aiTags`
  - `aiSummary`
- 非 `ready` 状态统一对外收敛成：
  - `aiTags=[]`
  - `aiSummary=null`

#### 4.5 详情页与用户侧 AI native 能力

本轮新增了三类特别贴合当前项目阶段的能力：

1. **详情页“适合什么场景”**
   - 在 `review-summary` 中新增 `recommendedScenarios`
   - 用于表达：
     - 工作日午餐
     - 一个人快吃
     - 想吃热汤时
     - 预算 30 左右

2. **最近吃过，先别推**
   - 新增 `choice-history` 接口
   - 推荐链路优先避开近 3 天吃过的店
   - 如果过滤到完全无候选，再自动回退，保证鲁棒性

3. **推荐反馈闭环**
   - 新增 `recommendation_feedback` 表与接口
   - `ALREADY_ATE` 会同步写入 `choice-history`
   - 其他反馈进入 AI 上下文与短期软过滤

4. **轻量口味画像**
   - 不建重表，不上向量库
   - 直接聚合评论、备注、黑名单原因、选择历史、反馈信号
   - 形成 `preference-profile`

---

## 涉及的主要文件

### backend

- `backend/src/main/java/com/zjgsu/whattoeat/config/AiServiceProperties.java`
- `backend/src/main/java/com/zjgsu/whattoeat/integration/ai/AiAssistantClient.java`
- `backend/src/main/java/com/zjgsu/whattoeat/integration/ai/AiHttpClient.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/RecommendationApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/RecommendationInsightHeuristics.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/RestaurantReviewAiApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/RestaurantReviewQueryApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/UserChoiceHistoryApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/UserRecommendationFeedbackApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/UserPreferenceProfileApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/controller/RecommendationController.java`
- `backend/src/main/java/com/zjgsu/whattoeat/controller/UserChoiceHistoryController.java`
- `backend/src/main/java/com/zjgsu/whattoeat/controller/UserRecommendationFeedbackController.java`
- `backend/src/main/java/com/zjgsu/whattoeat/controller/UserPreferenceProfileController.java`
- `backend/src/main/java/com/zjgsu/whattoeat/model/entity/RecommendationFeedbackEntity.java`
- `backend/src/main/resources/db/migration/V9__init_recommendation_feedback.sql`

### ai-service

- `ai-service/app/main.py`
- `ai-service/app/config.py`
- `ai-service/app/schemas.py`
- `ai-service/app/clients/openai_compatible.py`
- `ai-service/app/services/recommendation.py`
- `ai-service/app/services/tagging.py`

### 文档

- `docs/ai-feature.md`
- `docs/api.md`
- `docs/api.yaml`
- `docs/frontend-ai-review-integration.md`
- `AGENTS.md`

---

## PR 链接

- PR #16：https://github.com/Ryan041001/WhatToEat/pull/16

---

## 个人体会

这次 AI 集成最大的收获，不是“把模型调通”，而是把模型能力收口成可维护、可联调、可测试的工程结构。相比直接在后端里硬写 prompt，我这次把 AI 功能拆成了独立 `ai-service`，同时让 backend 只负责候选构造、权限边界、错误映射和前端契约收口，这样结构更稳定，也更适合后续替换模型或扩展能力。

另外一个更重要的体会是：**AI native 不一定等于重型系统**。对这个项目来说，更合适的路线不是一上来做 RAG、向量库、复杂 agent，而是把已有数据——评论、备注、黑名单原因、选择历史、推荐反馈——真正织进推荐主链路。这样做出来的 refine、场景解释、最近吃过过滤、轻量画像，既贴合当前数据库和后端状态，也更容易在课程项目阶段做出“自然、可信、能落地”的体验差异。
