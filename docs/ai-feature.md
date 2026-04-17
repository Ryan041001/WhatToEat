# AI 功能说明

## 1. 文档目标

本文档说明 WhatToEat 当前仓库中已经落地的 AI 能力，包括：

1. 使用了什么模型接入方式
2. 当前实现了哪些 AI 功能
3. AI 能力在系统中的调用链路
4. 当前能力边界与对前端可见契约

本文档描述的是 **当前真实实现**，不是未来规划。

---

## 2. 当前 AI 架构

当前项目不是在 Spring Boot 后端里直接调用大模型，而是采用两层结构：

1. 微信小程序前端只调用 `backend/`
2. `backend/` 调用内部 `ai-service/`
3. `ai-service/` 再调用 OpenAI-compatible 模型服务

对应仓库位置：

- `backend/src/main/java/com/zjgsu/whattoeat/integration/ai/`
- `ai-service/app/`

这样做的目的有三个：

1. 前端不需要知道模型协议细节
2. 后端不需要直接处理复杂的模型响应解析
3. 后续切换模型供应商时，只需要收口在 `ai-service/`

---

## 3. 使用模型与接入方式

### 3.1 模型接入方式

当前 `ai-service/` 使用的是 **OpenAI-compatible API** 接入方式。

对应实现文件：

- `ai-service/app/clients/openai_compatible.py`

### 3.2 当前配置项

AI Service 当前通过环境变量控制模型侧配置：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `OPENAI_TIMEOUT_SECONDS`

对应实现文件：

- `ai-service/app/config.py`

### 3.3 当前默认模型

当前默认模型配置为：

```text
gpt-4.1-mini
```

说明：

- 这是 `ai-service` 的默认值
- 实际运行时可以通过环境变量替换为兼容 OpenAI Chat Completions 的其他模型
- 因此当前项目的真实表达应是：
  - **模型接入协议：OpenAI-compatible**
  - **默认模型：`gpt-4.1-mini`**
  - **可通过环境变量替换**

---

## 4. 当前已实现的 AI 功能

当前 AI 功能已经进入主链路，不再只是实验代码。

### 4.1 评论标签摘要

功能入口：

- `POST /internal/review-tags`（AI Service 内部接口）
- 由 backend 的 `RestaurantReviewAiApplicationService` 调用

功能说明：

- 输入某家餐厅的评论集合
- 模型提取 1 到 2 个标签
- 生成一句简短摘要
- 回写到 `restaurant_metric_snapshot`

当前生成结果字段：

- `ai_tag_1`
- `ai_tag_2`
- `ai_summary`

对应代码：

- `ai-service/app/services/tagging.py`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/RestaurantReviewAiApplicationService.java`

### 4.2 AI 推荐问答（同步）

功能入口：

- `POST /api/v1/recommendations/ask`

功能说明：

- 后端先从高德获取候选餐厅
- 合并本地快照字段：
  - `avgRating`
  - `reviewCount`
  - `avgPerCapitaPrice`
  - `aiTags`
- 把增强后的候选集合与用户问题交给 AI Service
- AI 返回：
  - `answer`
  - 结构化推荐列表 `recommendations`

对应代码：

- `backend/src/main/java/com/zjgsu/whattoeat/service/application/RecommendationApplicationService.java`
- `ai-service/app/services/recommendation.py`

### 4.3 AI 推荐问答（流式）

功能入口：

- `POST /api/v1/recommendations/ask/stream`

功能说明：

- 使用 SSE（`text/event-stream`）返回推荐过程
- 前端可接收结构化事件：
  - `session.created`
  - `retrieval.started`
  - `retrieval.completed`
  - `recommendation.card`
  - `answer.delta`
  - `answer.done`
  - `done`
  - `error`

对应代码：

- `backend/src/main/java/com/zjgsu/whattoeat/controller/RecommendationController.java`
- `backend/src/main/java/com/zjgsu/whattoeat/integration/ai/AiHttpClient.java`
- `ai-service/app/main.py`
- `ai-service/app/services/recommendation.py`

### 4.4 AI refine（换一家，但保持条件）

当前推荐接口并不是一次性问答，已经支持**轻量多轮 refine**。

对外入口仍然是：

- `POST /api/v1/recommendations/ask`
- `POST /api/v1/recommendations/ask/stream`

但请求体现在支持可选 `context`：

- `previousQuestion`
- `rejectedPoiIds`
- `selectedPoiIds`
- `userSignals`

这意味着前端后续可以非常轻量地做连续追问，例如：

- 太贵了
- 太远了
- 想吃热汤
- 不要快餐
- 我在健身

而不需要为此再额外发明一套新模型工作流。

### 4.5 轻量口味画像

当前没有单独的“长期画像表”，而是采用**实时聚合式画像**：

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

这属于当前阶段非常适合的 AI native 形态：

- 不重
- 解释性强
- 与现有数据自然衔接

### 4.6 最近吃过过滤与推荐反馈闭环

当前推荐系统已经接入两类用户行为信号：

1. **最近吃过**
   - 接口：
     - `POST /api/v1/users/{userId}/choice-history`
     - `GET /api/v1/users/{userId}/choice-history`
   - 作用：
     - 推荐会优先避开近 3 天最近吃过的店
     - 若过滤后完全无候选，才会回退到较宽松候选集

2. **推荐反馈闭环**
   - 接口：
     - `POST /api/v1/users/{userId}/recommendation-feedback`
     - `GET /api/v1/users/{userId}/recommendation-feedback`
   - 当前支持反馈类型：
     - `TOO_EXPENSIVE`
     - `TOO_FAR`
     - `DONT_WANT_THIS_TODAY`
     - `LOOKS_UNHYGIENIC`
     - `ALREADY_ATE`
   - 其中 `ALREADY_ATE` 会同步写入 `choice-history`

---

## 5. 当前 AI 推荐实现方式

### 5.1 不是“自由生成餐厅”

当前推荐能力不是让模型自由输出“你去吃什么”，而是严格限制在当前候选池内。

也就是说：

1. 候选餐厅先由后端决定
2. AI 只能从这些候选里选择
3. 后端会校验 AI 给出的 `poiId`
4. 只有校验通过的结果才会发给前端

此外，现在候选本身也已经不只是“高德原始字段”，而是增强候选：

- `avgRating`
- `reviewCount`
- `avgPerCapitaPrice`
- `aiTags`
- `aiSummary`
- `derivedTags`

其中 `derivedTags` 是后端根据类别、AI 摘要、标签做的轻量派生信号，例如：

- 高蛋白
- 清淡
- 热汤
- 健身友好
- 快餐

### 5.2 使用了 tool call 思路

推荐服务里定义了结构化工具动作：

```text
show_restaurant_card
```

模型需要通过这个工具动作输出：

- `poiId`
- `reason`
- `rank`

然后：

1. AI Service 解析 tool call
2. backend 再把它翻译成前端可见的 `recommendation.card` 事件

这套设计的好处是：

- 不需要从自然语言里猜餐厅名
- 推荐卡片与回答文本解耦
- 更容易做前端稳定渲染

### 5.3 Prompt 当前的强化方向

当前推荐 prompt 已经针对 AI native 场景做了强化，核心不是“更会聊天”，而是**更会继承上下文与约束**：

1. 连续追问时，默认继承 `previousQuestion`
2. 新一轮明确约束优先级高于旧约束
3. 优先满足显式否定条件
4. 再综合预算、距离、评分、评论数与标签契合度
5. 对下面几类信号做了明确偏向：
   - 健身 / 减脂 / 高蛋白
   - 热汤
   - 不要快餐
   - 太贵了
   - 太远了

---

## 6. 当前对外接口契约

### 6.1 前端可见 AI 能力

前端当前能感知到的 AI 结果主要有两类：

1. **餐厅聚合摘要**
   - `aiTags`
   - `aiSummary`
   - `recommendedScenarios`
2. **AI 推荐问答**
   - `answer`
   - `recommendations`
   - 流式事件
3. **用户 AI native 信号接口**
   - `choice-history`
   - `recommendation-feedback`
   - `preference-profile`

### 6.2 `review-summary` 的重要约定

当前后端内部保存了 `aiStatus`，但 **不对前端暴露**。

因此当前对前端的真实契约是：

- 只有当 AI 摘要状态为 `ready` 时，才返回：
  - `aiTags`
  - `aiSummary`
- 其他状态统一返回：
  - `aiTags=[]`
  - `aiSummary=null`

这意味着前端不需要也不能依赖内部 AI 状态做细分判断。

---

## 7. 当前能力边界

虽然 AI 功能已经落地，但当前仍有明确边界。

### 7.1 已经具备的能力

- 评论标签抽取
- 评论摘要生成
- 基于候选餐厅集合的推荐回答
- 流式推荐输出
- 工具调用驱动的结构化推荐卡片

### 7.2 还没有实现的能力

- 长对话式会话记忆（目前是轻量 refine，不是完整聊天 session）
- 独立长期用户画像表
- 面向前端暴露 `aiStatus`
- 单店自由问答接口
- 基于向量库或 RAG 的评论检索增强

---

## 8. 当前工程落地情况

### 8.1 backend 已接入

当前 backend 已完整接入 AI Service：

- `AiServiceProperties`
- `AiAssistantClient`
- `UserChoiceHistoryApplicationService`
- `UserRecommendationFeedbackApplicationService`
- `UserPreferenceProfileApplicationService`
- `AiHttpClient`
- 推荐问答调用链
- 评论摘要调用链

### 8.2 ai-service 已独立成服务

当前仓库中的 `ai-service/` 已包含：

- FastAPI 服务入口
- OpenAI-compatible client
- 推荐服务
- 评论摘要服务
- 测试代码
- Dockerfile

说明当前 AI 能力已经不是草稿，而是实际工程组成部分。

---

## 9. 总结

当前项目中的 AI 功能可以概括为：

1. **模型接入方式：** OpenAI-compatible API
2. **默认模型：** `gpt-4.1-mini`
3. **已实现能力：**
   - 评论标签摘要
   - 评论摘要生成
   - AI 推荐问答（同步）
   - AI 推荐问答（流式）
4. **实现特点：**
   - 前端不直连模型
   - backend 不直出模型私有协议
   - 推荐只在候选池内进行
   - tool call 驱动结构化推荐卡片

因此，当前 AI 模块已经是 WhatToEat 的正式增强能力，而不是单纯的概念验证代码。
