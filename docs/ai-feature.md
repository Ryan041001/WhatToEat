# AI 功能说明

## 1. 文档目标

本文档描述 WhatToEat 当前仓库里已经真实落地的 AI 架构、能力、推荐契约与工程边界，不讨论未来规划。

如果你关心的是：

- AI 在系统里的调用链路
- `backend/` 和 `ai-service/` 各自负责什么
- 当前到底已经实现了哪些 AI 能力
- 前端现在能依赖什么、不能依赖什么

优先读这份文档。

另外与前端正式联调最相关的配套文档是：

- [`docs/api.md`](./api.md)
- [`docs/api.yaml`](./api.yaml)
- [`docs/frontend-ai-review-integration.md`](./frontend-ai-review-integration.md)

---

## 2. 当前 AI 架构

当前项目不是在 Spring Boot 后端里直接调用大模型，而是采用两层收口：

1. 微信小程序前端只调用 `backend/`
2. `backend/` 调用内部 `ai-service/`
3. `ai-service/` 再调用 OpenAI-compatible 模型服务

当前关键目录：

- `backend/src/main/java/com/zjgsu/whattoeat/infrastructure/ai/`
- `backend/src/main/java/com/zjgsu/whattoeat/application/recommendation/`
- `backend/src/main/java/com/zjgsu/whattoeat/domain/recommendation/`
- `ai-service/app/api/`
- `ai-service/app/core/`
- `ai-service/app/domain/`
- `ai-service/app/infrastructure/llm/`
- `ai-service/app/schemas/`

这样分层的价值：

1. 前端不需要理解模型协议和错误映射。
2. backend 只负责候选池、权限边界、业务约束和对外契约。
3. 模型供应商变化时，主要改动被收敛在 `ai-service/`。

---

## 3. 模型接入方式

### 3.1 协议与入口

当前 `ai-service/` 通过 OpenAI-compatible API 接入模型。

对应实现：

- `ai-service/app/infrastructure/llm/openai_compatible.py`
- `ai-service/app/infrastructure/llm/client_factory.py`

### 3.2 配置项

AI Service 当前通过环境变量控制模型侧配置：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `OPENAI_TIMEOUT_SECONDS`

对应实现：

- `ai-service/app/core/config.py`

### 3.3 当前默认模型

当前 `ai-service/` 的默认模型值仍是：

```text
gpt-4.1-mini
```

但这是默认值，不是硬绑定。运行时可以通过环境变量替换成任意兼容 OpenAI Chat Completions 的模型。

---

## 4. 当前已实现的 AI 功能

### 4.1 评论标签摘要

功能入口：

- `POST /internal/review-tags`

backend 调用方：

- `RestaurantReviewAiApplicationService`

AI Service 相关实现：

- `ai-service/app/api/routes/tagging.py`
- `ai-service/app/domain/tagging/service.py`
- `ai-service/app/domain/tagging/prompts.py`

效果：

- 输入某家餐厅的评论集合
- 生成 1 到 2 个标签
- 生成一句简短摘要
- 写回 `restaurant_metric_snapshot`

### 4.2 AI 推荐问答（同步）

功能入口：

- `POST /api/v1/recommendations/ask`

backend 相关实现：

- `backend/src/main/java/com/zjgsu/whattoeat/application/recommendation/RecommendationApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/application/recommendation/RecommendationCandidateLoader.java`
- `backend/src/main/java/com/zjgsu/whattoeat/application/recommendation/RecommendationCardAssembler.java`

AI Service 相关实现：

- `ai-service/app/api/routes/recommendation.py`
- `ai-service/app/domain/recommendation/service.py`
- `ai-service/app/domain/recommendation/parser.py`
- `ai-service/app/domain/recommendation/prompts.py`

流程：

1. backend 从高德拿候选
2. 合并本地聚合快照
3. 补充 AI 可消费的增强字段
4. backend 基于服务端 `Clock` 自动补充日期、星期和当前时段语境
5. 把候选集合和问题传给 AI Service
6. backend 内部通过流式推荐链路聚合同步结果
7. 对外返回 `answer` 和结构化推荐结果

补充说明：

- `/api/v1/recommendations/ask` 当前是对外同步兼容接口
- frontend 正式聊天接入不建议把它作为主入口
- 即使调用同步接口，backend -> ai-service 仍然是**只请求流式**

### 4.3 AI 推荐问答（流式）

功能入口：

- `POST /api/v1/recommendations/ask/stream`

流程特征：

- backend 通过 SSE 对前端输出过程事件
- AI Service 先确定推荐卡片，再输出对应答案文本流
- backend 再把上游事件翻译为前端可消费的稳定事件
- `recommendation.card` 与最终 `answer` 来自同一批已选餐厅
- 为了保证卡片和文案严格对应，`recommendation.card` 可能先于 `answer.delta` 到达

关键事件：

- `session.created`
- `retrieval.started`
- `retrieval.completed`
- `recommendation.card`
- `answer.delta`
- `answer.done`
- `done`
- `error`

前端对接注意：

- 小程序正式聊天链路应优先接 `POST /api/v1/recommendations/ask/stream`
- 前端不需要自行传日期、星期、早中晚餐或天气字段
- 当前已接入独立 AI 对话页与流式模块：
   - `frontend/pages/ai-chat/*`
   - `frontend/api/recommendation-chat.js`

### 4.4 轻量 refine

当前推荐接口已经支持轻量多轮 refine，不需要额外的复杂会话系统。

请求体里的可选 `context` 支持：

- `previousQuestion`
- `rejectedPoiIds`
- `selectedPoiIds`
- `userSignals`

这允许前端表达：

- 太贵了
- 太远了
- 想吃热汤
- 不要快餐
- 我在健身

### 4.5 轻量口味画像

当前没有独立长期画像表，而是基于现有业务数据做实时聚合：

- `restaurant_review`
- `user_restaurant_note`
- `user_blacklist.reason`
- `user_choice_history`
- `recommendation_feedback`
- `restaurant_metric_snapshot`

对外接口：

- `GET /api/v1/users/{userId}/preference-profile`

返回重点：

- `summary`
- `preferredTags`
- `avoidedTags`
- `avgPerCapitaBudget`
- `budgetRange`
- `lifestyleSignals`
- `recentFeedbackSignals`

### 4.6 最近吃过过滤与反馈闭环

当前推荐链路已经接入两类用户行为信号：

1. `choice-history`
2. `recommendation-feedback`

作用：

- 优先避开最近吃过的店
- 根据近期拒绝原因调整推荐上下文
- 把“太贵了 / 太远了 / 今天不想吃这个 / 已经吃过了”转成更稳定的模型输入信号

---

## 5. 当前推荐实现方式与契约

### 5.1 不是自由生成餐厅

当前推荐能力严格限制在后端给出的候选池内，不允许模型脱离候选编造餐厅。

约束链路：

1. 候选餐厅先由 backend 决定
2. AI 只能从候选集合中选择
3. backend 会再次校验 `poiId`
4. 只有校验通过的结果才会返回给前端

当前 AI 候选不只是高德原始字段，还包含增强信息：

- `avgRating`
- `reviewCount`
- `avgPerCapitaPrice`
- `aiTags`
- `aiSummary`
- `derivedTags`

其中 `derivedTags` 是后端根据类别、AI 标签和摘要派生出来的轻量信号，例如：

- 高蛋白
- 清淡
- 热汤
- 健身友好
- 快餐

### 5.2 工具调用驱动卡片推荐

推荐链路不是直接让模型吐一段自然语言，而是先确定结构化推荐卡片，再生成与这些卡片一致的回答：

```text
show_restaurant_card
```

工具字段：

- `poiId`
- `reason`
- `rank`

处理方式：

1. `ai-service` 先解析并清洗 tool call，确定本轮选中的餐厅
2. backend 将其映射为前端事件 `recommendation.card`
3. `ai-service` 再基于同一批已选餐厅流式生成回答文本
4. 前端再根据事件稳定渲染卡片和文案

这样做的好处：

- 不需要从自然语言里反推餐厅
- 卡片渲染与答案文本解耦
- 更容易做流式体验和排序控制
- 更容易保证“卡片和文本说的是同几家店”

### 5.3 Prompt 的当前偏向

当前 prompt 重点不在“聊天感”，而在约束继承和可解释推荐：

1. 连续追问默认继承 `previousQuestion`
2. 新一轮明确约束优先于旧约束
3. 先满足显式否定条件
4. 再综合预算、距离、评分、评论数和标签契合度
5. 明确强化以下信号：
   - 健身 / 减脂 / 高蛋白
   - 热汤
   - 不要快餐
   - 太贵了
   - 太远了

### 5.4 前端当前可依赖的 AI 能力

前端现在能稳定依赖的 AI 结果主要有三类：

1. 餐厅聚合摘要
   - `aiTags`
   - `aiSummary`
   - `recommendedScenarios`
2. AI 推荐问答
   - `answer`
   - `recommendations`
   - 流式 SSE 事件
3. 用户 AI-native 信号接口
   - `choice-history`
   - `recommendation-feedback`
   - `preference-profile`

### 5.5 `review-summary` 的关键约定

backend 内部维护 `aiStatus`，但当前不对前端暴露。

因此前端的真实契约是：

- 当 AI 结果 ready 时，返回 `aiTags` / `aiSummary`
- 非 ready 状态统一表现为：
  - `aiTags=[]`
  - `aiSummary=null`

前端不能基于内部状态做分支逻辑，只能基于对外字段判断“当前有没有可展示结果”。

---

## 6. 当前能力边界

### 6.1 已经具备的能力

- 评论标签抽取
- 评论摘要生成
- 基于候选池的推荐回答
- 流式推荐输出
- 工具调用驱动的结构化推荐卡片
- 服务端自动注入日期、星期和时段语境
- 同步对外接口内部复用流式推荐链路

### 6.2 还没有实现的能力

- 长对话式会话记忆
- 独立长期用户画像表
- 面向前端暴露 `aiStatus`
- 单店自由问答接口
- 基于向量库或 RAG 的评论检索增强
- AI 对话页的会话持久化与多轮 refine 快捷操作（当前仍是轻量版本）

---

## 7. 当前工程落地情况

### 7.1 backend 已接入的 AI 关键模块

- `AiServiceProperties`
- `AiAssistantClient`
- `AiHttpClient`
- `RecommendationApplicationService`
- `RecommendationCandidateLoader`
- `RecommendationCardAssembler`
- `UserChoiceHistoryApplicationService`
- `UserRecommendationFeedbackApplicationService`
- `UserPreferenceProfileApplicationService`
- `RestaurantReviewAiApplicationService`

### 7.2 ai-service 当前真实结构

- `app/main.py`：应用装配入口
- `app/api/`：FastAPI 路由与依赖注入
- `app/core/`：配置与异常
- `app/domain/recommendation/`：推荐 prompt、解析与服务
- `app/domain/tagging/`：评论标签与摘要服务
- `app/infrastructure/llm/`：模型客户端实现
- `app/schemas/`：Pydantic 请求/响应模型

这说明当前 AI 模块已经是正式工程组成部分，不是临时 demo。

---

## 8. 一句话结论

当前项目中的 AI 能力可以概括为：

1. 模型接入方式是 OpenAI-compatible API
2. 默认模型值是 `gpt-4.1-mini`
3. 推荐严格受候选池约束
4. 对前端正式聊天体验，应优先使用 `/api/v1/recommendations/ask/stream`
5. 流式推荐通过结构化工具调用驱动卡片事件，并保证卡片和文案来自同一批候选
6. 画像、最近吃过和反馈闭环已经进入主链路，但仍属于轻量 AI-native 形态，而不是重型 agent / RAG 系统
