# 后端模块说明

## 1. 模块目标

`backend/` 目录承载 WhatToEat 当前真实服务端能力，职责已经从“高德代理 + 基础用户数据 CRUD”扩展为：

1. 餐厅查询与排序增强
2. 随机推荐与卡片候选
3. 评论写入、公开评论、聚合摘要
4. AI 标签摘要
5. AI 推荐问答（同步 / 流式）

因此现在的后端是一个 **以高德 POI 为主数据源、以本地评论快照做增强层、以内部 AI Service 做推理增强的单体服务**。

---

## 2. 当前技术框架

- 开发语言：Java 17
- 核心框架：Spring Boot 4.0.3
- Web 层：Spring Web MVC
- 数据访问：Spring Data JPA + Hibernate
- 构建工具：Maven Wrapper
- 数据库：MySQL 8（dev / docker）、H2（test）
- 外部餐厅数据：高德 Web 服务 API
- 内部 AI 能力：独立 `ai-service/`（FastAPI + OpenAI-compatible）

---

## 3. 当前后端结构

```text
backend/src/main/java/com/zjgsu/whattoeat/
├── controller/       # 对外 REST API
├── service/
│   ├── application/  # 业务编排
│   └── domain/       # 推荐规则
├── integration/
│   ├── amap/         # 高德集成
│   └── ai/           # AI Service 集成
├── repository/       # JPA Repository
├── model/
│   ├── dto/
│   └── entity/
├── common/           # 统一响应 / 异常 / Web 支撑
└── config/           # 配置绑定与 Web 配置
```

当前新增的重要模块：

- `controller/RestaurantReviewController.java`
- `controller/RestaurantReviewSummaryController.java`
- `integration/ai/*`
- `service/application/RestaurantReview*`
- `service/application/RestaurantMetricAggregationService`
- `model/entity/RestaurantReviewEntity`
- `model/entity/RestaurantMetricSnapshotEntity`

---

## 4. 当前已实现能力

### 4.1 餐厅与推荐

- `/restaurants/nearby`：附近餐厅分页查询
- `/restaurants/search`：关键词搜索
- `/recommendations/random`：随机推荐
- `/recommendations/cards`：卡片候选列表
- `/recommendations/ask`：AI 同步问答推荐
- `/recommendations/ask/stream`：AI 流式问答推荐

### 4.2 餐厅增强信息

- 列表查询会自动合并：
  - `avgRating`
  - `reviewCount`
  - `avgPerCapitaPrice`
  - `aiTags`
- 支持增强排序：
  - `distance`
  - `avgRating`
  - `reviewCount`
  - `avgPriceAsc`
  - `avgPriceDesc`
  - `smart`

### 4.3 用户侧数据

- 黑名单 CRUD
- 备注 CRUD
- 当前用户单店评论 CRUD

### 4.4 评论与聚合

- 餐厅公开评论分页查询
- 餐厅评论聚合摘要查询
- 评论写入后自动刷新：
  - 均分
  - 均价
  - 评论数
  - AI 标签 / 摘要

---

## 5. 关键服务职责

### 5.1 `RestaurantQueryApplicationService`

- 调高德 nearby / search
- 合并 `restaurant_metric_snapshot`
- 在非 `distance` 排序时做本地候选池排序

### 5.2 `RecommendationApplicationService`

- 负责随机推荐、卡片候选、AI 推荐问答
- 会先校验 `userId`
- 会应用服务端黑名单过滤
- `ask` / `askStream` 会合并本地快照增强候选卡

### 5.3 `RestaurantReviewApplicationService`

- 负责当前用户评论查询 / upsert / 删除
- 写入后触发：
  - `RestaurantMetricAggregationService`
  - `RestaurantReviewAiApplicationService`

### 5.4 `RestaurantReviewQueryApplicationService`

- 查询某店公开评论列表
- 查询某店聚合摘要

### 5.5 `RestaurantMetricAggregationService`

- 按当前评论重算：
  - `reviewCount`
  - `avgRating`
  - `avgPerCapitaPrice`
  - `lastReviewAt`
- 同时推进 `aiStatus` 为 `pending` / `idle`

### 5.6 `RestaurantReviewAiApplicationService`

- 拉取某店全部评论
- 调 AI Service 总结标签与摘要
- 回写 `restaurant_metric_snapshot`

---

## 6. 当前数据库落地范围

当前 backend 真实依赖以下本地表：

1. `users`
2. `user_blacklist`
3. `user_restaurant_note`
4. `user_choice_history`
5. `restaurant_review`
6. `restaurant_metric_snapshot`

其中：

- `user_choice_history` 已进入推荐软过滤链路，用于“最近吃过，先别推”
- `recommendation_feedback` 已进入推荐软过滤与 AI 上下文
- `restaurant_review` 与 `restaurant_metric_snapshot` 已进入主链路

---

## 7. 高德与 AI 的边界

### 7.1 高德边界

- 高德仍是餐厅主数据唯一来源
- backend 负责代理、清洗、超时处理、错误映射
- 当前不维护本地餐厅主表

### 7.2 AI 边界

- backend 只调用内部 AI Service
- AI Service 当前提供：
  - `/internal/review-tags`
  - `/internal/recommend`
  - `/internal/recommend/stream`
- AI 只做“给定语料 / 给定候选”的推理增强，不直接替代主数据源

### 7.3 当前 AI Service 形态

`ai-service/` 当前是一个独立 Python 服务：

- FastAPI
- OpenAI-compatible client
- 使用结构化 JSON 输出与 tool calling
- 流式推荐通过 SSE 返回结构化事件

> 当前仓库里 Python 依赖管理已经是 `uv` 路线（`pyproject.toml` + `uv.lock`）。

---

## 8. 运行方式

### 8.1 Backend 单独运行

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run
```

### 8.2 Test Profile

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
```

### 8.3 Docker Compose 联调

```bash
cp .env.example .env
# 配置 AMAP_KEY / OPENAI_API_KEY 等变量
docker compose --env-file .env up --build
```

当前 Compose 会拉起：

- `mysql`
- `ai-service`
- `backend`

---

## 9. 当前鲁棒性注意点

### 9.1 已做的稳定性处理

- 高德 / AI 上游异常统一映射
- 评论参数校验集中在 service
- 评论接口严格校验 token 与 `userId`
- 流式推荐只对前端暴露结构化事件

### 9.2 现阶段仍需注意的实现限制

1. **增强排序是候选池内局部排序**
   - 不是高德全量结果的全局排序
   - 前端翻页不能只依赖 `total`

2. **评论摘要当前不返回 `aiStatus`**
   - 前端无法严格区分“摘要生成中 / 失败 / 暂无”
   - `aiSummary == null` 只能当成“暂不可用”

3. **前端还没有真正调用 choice-history / recommendation-feedback / preference-profile**
   - 后端能力已在，但如果前端不接，这些能力不会对真实用户生效

4. **前端当前本地黑名单与服务端黑名单可能不一致**
   - 后端推荐只认服务端真实黑名单

---

## 10. 当前最适合继续扩展的方向

如果继续在当前代码上演进，最自然的不是直接上重型 RAG，而是：

1. 继续细化轻量口味画像，而不是马上引入重型画像表或向量库
2. 让前端真正接上 `context` 做 AI refine / 换一家 / 太贵了 / 太远了
3. 让确认选店动作显式写入 `user_choice_history`
4. 仅在前端确有需要时，再考虑是否暴露 `review-summary.aiStatus`

这些都与现有数据模型和服务边界自然衔接。
