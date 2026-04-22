# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

“今天吃什么 / WhatToEat” 是一个以微信小程序为前端入口的餐厅推荐项目。当前仓库包含三条需要联动理解的实现面：

- `frontend/`：微信小程序原生工程，负责登录、首页、列表、详情、滑选、AI 对话等页面。
- `backend/`：Spring Boot 4 + Java 17 后端，统一提供鉴权、餐厅查询、评论、推荐、黑名单、AI 增强能力。
- `ai-service/`：内部 FastAPI 服务，供后端调用，用于评论标签/摘要与推荐问答增强；前端不直接调用它。

核心约束：
- 餐厅主数据来源于高德 POI，不在本地维护完整餐厅主表。
- 前端只对接 `backend/`，不直接调用高德，也不直接调用 AI Service。
- 本地数据库除用户侧数据外，还保存评论事实与聚合快照，用于列表增强和摘要能力。

## Common Commands

### Backend

在 `backend/` 目录下执行：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run
```
启动 dev 环境后端（默认连接本地 MySQL）。

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.useTestClasspath=true
```
启动 test 环境后端（使用 H2）。

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test
```
运行全部后端测试。

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -Dtest=ClassName test
```
运行单个后端测试类。

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -Dtest=ClassName#methodName test
```
运行单个后端测试方法。

### AI Service

在 `ai-service/` 目录下执行：

```bash
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000
```
启动内部 AI Service。

```bash
uv run --with pytest pytest -q
```
运行 AI Service 全部测试。

```bash
uv run --with pytest pytest path/to/test_file.py -q
```
运行单个 AI Service 测试文件。

### Full-stack Local Integration

在仓库根目录执行：

```bash
cp .env.example .env
docker compose --env-file .env up --build
```
启动 MySQL、backend、ai-service 的本地联调环境；先在 `.env` 中配置 `AMAP_KEY`、`OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_MODEL`。

### Frontend

- 使用微信开发者工具打开 `frontend/`。
- 开发者工具默认后端地址是 `http://127.0.0.1:8080/api/v1`。
- 真机默认后端地址在 `frontend/api/base-url.js` 中维护；切换网络后需要同步更新，或通过该模块提供的方法覆写。
- 当前前端没有独立的 npm build / lint / test 命令；主要通过微信开发者工具联调。

## Source of Truth Documents

开始改动前先读与任务最相关的文档；其中以下文件优先级最高：

- `README.md`：项目范围、运行方式与整体能力概览。
- `docs/architecture.md`：后端真实架构来源。
- `docs/database.md`：数据库与 migration 的真实来源。
- `docs/api.md`：人类可读 API 契约。
- `docs/api.yaml`：OpenAPI 主契约。
- `docs/frontend-ai-review-integration.md`：前端联调评论、聚合摘要、AI 推荐时的真实约束与接入顺序。

注意：
- `docs/frontend.md` 描述的是前端当前实现基线，但不应单独作为接口真相源。
- `docs/design-spec.md` 更偏设计/规划语义，不能替代当前代码与 API 契约。

## High-level Architecture

### Request/Data Flow

典型链路是：

1. 微信小程序页面通过 `frontend/api/client.js` 发起请求。
2. `client.js` 统一拼接 `frontend/api/base-url.js` 中的 base URL，并自动注入 Bearer Token。
3. Spring Boot 后端对外暴露 `/api/v1/**`，负责鉴权、业务编排、聚合与错误语义。
4. 后端按场景调用：
   - 高德 Web 服务 API：获取附近餐厅与搜索候选。
   - MySQL / H2：读取用户、黑名单、评论、聚合快照等本地数据。
   - `ai-service/`：生成评论标签/摘要与推荐问答增强。
5. 前端消费统一响应，并把餐厅列表缓存到 `App.globalData` 与本地存储。

### Frontend Shape

前端不是 React/Vue，而是微信小程序原生工程：

- `frontend/app.js`：全局用户态、餐厅缓存、黑名单状态、`bootstrapRestaurants()` 等核心入口。
- `frontend/api/client.js`：统一请求封装，处理 token 注入、401、可控静默错误、返回完整响应等。
- `frontend/api/base-url.js`：区分开发者工具与真机的 API 基地址，并支持运行时覆写。
- `frontend/api/*.js`：按资源拆分接口模块，如 `auth.js`、`restaurants.js`、`blacklist.js`、`reviews.js`、`recommendation-chat.js`、`user-signals.js`。
- `frontend/pages/*`：页面层直接消费全局缓存和 API 模块，不存在独立状态管理库；状态主要依赖 `App.globalData`、页面 `setData()` 与 `wx` 本地存储。

前端联调时要特别注意：
- 真实后端优先，已移除显式 mock 登录和 mock 餐厅 fallback。
- AI 正式聊天入口应优先使用流式接口 `/api/v1/recommendations/ask/stream`。
- 真机环境不能使用 `127.0.0.1` / `localhost`；优先检查 `frontend/api/base-url.js`。

### Backend Shape

后端是分层 Spring Boot 应用，但“推荐”与“AI 集成”已经从早期结构演进出更明确的包边界：

- `controller/`：薄控制器，接收参数并返回统一 `ApiResponse`。
- `service/application/`：评论、聚合、画像、反馈等应用编排。
- `application/recommendation/`：推荐主链路编排。
- `domain/recommendation/`：推荐领域规则，如候选过滤、随机策略。
- `integration/amap/`：高德调用、DTO 清洗、错误转换。
- `infrastructure/ai/`：内部 AI Service 适配，同步/流式推荐与评论摘要能力。
- `repository/`：JPA 持久化。
- `model/entity/` 与 `model/dto/`：实体与传输模型。
- `common/`、`config/`：统一响应、异常、配置绑定。

理解后端时，不要只看旧的 `service/application` 目录；推荐主链路和 AI 集成的真实重心分别在：
- `backend/src/main/java/com/zjgsu/whattoeat/application/recommendation/`
- `backend/src/main/java/com/zjgsu/whattoeat/domain/recommendation/`
- `backend/src/main/java/com/zjgsu/whattoeat/infrastructure/ai/`

### AI Service Role

`ai-service/` 是内部依赖，不是前端对外接口层：

- 技术栈：FastAPI + Pydantic + OpenAI-compatible SDK。
- 作用：根据后端传入的候选餐厅和评论语料生成结构化增强结果。
- 边界：只能在后端给定候选集和语料范围内工作，不能脱离现有候选自由生成餐厅数据。

## Important Implementation Constraints

- 前端改动联调代码前，必须同时核对当前分支上的 `backend/` 与 `docs/` 改动，不要仅凭旧页面猜接口。
- 餐厅列表、详情、AI 对话共用同一套后端契约；字段兼容和空态语义优先参考 `docs/frontend-ai-review-integration.md`。
- 后端接口统一前缀是 `/api/v1`，受保护接口通过 `Authorization: Bearer <token>` 传递会话。
- 当前认证仍是 mock 微信登录形状，但前端链路已经走真实后端登录接口。
- `user_choice_history` 已进入推荐软过滤链路；如果改推荐相关前端交互，要留意是否需要同步写入 choice history / recommendation feedback。
- 数据库结构变更应以 Flyway migration 为准；`docs/database.md` 需要与 migration 保持一致。
