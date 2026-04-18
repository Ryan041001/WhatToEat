# 今天吃什么 (WhatToEat)

## 团队成员
| 姓名 | 学号 | 分工 |
| :--- | :--- | :--- |
| 沈哲伟 | 2312190313 | 项目负责人 / 后端服务开发 (Spring Boot) 与数据库设计 |
| 林佳涛 | 2312190316 | 前端负责人 / 前端开发 (微信小程序) 与 UI 交互设计 |

## 项目简介
本项目是一款基于微信小程序的“干饭防纠结”应用，主要面向在校大学生及周边职场人群。针对用户日常就餐时“不知道吃什么”的痛点，应用提供随机推荐（大转盘 / 摇一摇）、卡片滑选、列表筛选、评论与 AI 推荐等能力。

当前阶段的整体方案是：

- **高德 POI** 作为餐厅主数据来源
- **Spring Boot 后端** 统一代理地图、推荐、评论与用户侧数据能力
- **微信小程序前端** 只对接 backend，不直接调用高德或 AI 服务
- **内部 AI Service** 负责评论标签摘要与推荐问答增强

---

## 当前技术栈

- **前端：** 微信小程序原生开发
- **后端：** Spring Boot 4 + Java 17 + Spring Data JPA
- **数据库：** MySQL 8（dev / docker） / H2（test）
- **地图与位置服务：** 高德 Web 服务 API（由 backend 调用）
- **AI 能力：** `ai-service/`（FastAPI + OpenAI-compatible）
- **数据库迁移：** Flyway

---

## 当前已实现能力

### 用户与用户侧数据
- mock 微信登录
- 当前用户信息
- 黑名单 CRUD
- 备注 CRUD
- 当前用户单店评论 CRUD

### 餐厅与推荐
- 附近餐厅查询
- 关键词搜索
- 列表增强字段：评分 / 评论数 / 人均 / AI 标签
- 列表增强排序：`distance` / `avgRating` / `reviewCount` / `avgPriceAsc` / `avgPriceDesc` / `smart`
- 随机推荐
- 卡片候选列表
- AI 推荐问答（同步）
- AI 推荐问答（流式）

### 评论与聚合摘要
- 公开评论列表
- 评论聚合摘要
- AI 标签摘要

---

## 仓库结构

```text
WhatToEat/
├── frontend/                     # 微信小程序前端
├── backend/                      # Spring Boot 后端
│   └── src/main/java/com/zjgsu/whattoeat/
│       ├── controller/
│       ├── service/
│       │   ├── application/
│       │   └── domain/
│       ├── integration/
│       │   ├── amap/
│       │   ├── ai/
│       │   └── wechat/
│       ├── repository/
│       ├── model/entity/
│       ├── common/
│       └── config/
├── ai-service/                   # 内部 AI Service（FastAPI）
├── docs/                         # 架构、数据库、API、前端对接文档
│   ├── architecture.md
│   ├── database.md
│   ├── api.md
│   ├── api.yaml
│   ├── backend.md
│   ├── frontend.md
│   ├── frontend-ai-review-integration.md
│   └── design-spec.md
├── docker-compose.yml
└── README.md
```

---

## 开发与运行

### 后端（dev 环境，需要本地 MySQL）
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run
```

### 后端（test 环境，使用 H2）
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
```

### 后端测试
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test
```

### AI Service 本地运行
```bash
cd ai-service
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### AI Service 测试
```bash
cd ai-service
uv run --with pytest pytest -q
```

### Docker Compose 联调
```bash
cp .env.example .env
# 配置 AMAP_KEY / OPENAI_API_KEY / OPENAI_BASE_URL / OPENAI_MODEL
docker compose --env-file .env up --build
```

### 前端开发
- 使用微信开发者工具打开 `frontend/`
- 小程序本地调试默认后端地址：`http://127.0.0.1:8080`
- 真机调试请使用宿主机局域网 IP，而不是 `localhost`

---

## 文档入口

联调、开发、校验时请优先看下面这些文档：

- `docs/architecture.md`：后端架构真实来源
- `docs/database.md`：数据库与 migration 真实来源
- `docs/api.md`：人类可读 API 契约
- `docs/api.yaml`：OpenAPI 主契约
- `docs/backend.md`：后端模块说明
- `docs/frontend-ai-review-integration.md`：前端对接评论 / 摘要 / AI 推荐的联调说明

> 注意：`docs/frontend.md` 与 `docs/design-spec.md` 中包含部分规划态 / 设计态内容，不应直接当成当前接口契约真相源。

---

## 说明与约束

- 餐厅主数据来源于高德，不在本地维护完整餐厅主表
- 本地数据库当前保存：用户、黑名单、备注、评论、聚合快照、预留历史
- `user_choice_history` 当前仍是预留态，尚未真正接入推荐主链路
- `review-summary` 当前对外不暴露 `aiStatus`；前端应把 `aiTags=[]`、`aiSummary=null` 统一理解为“当前无可展示的 AI 结果”

---

## Figma 链接

https://www.figma.com/make/O3ROt1nNk2TfEyq6cJ2D2g/%E5%B9%B2%E9%A5%AD%E9%98%B2%E7%BA%A0%E7%BB%93%E5%B0%8F%E7%A8%8B%E5%BA%8F?p=f&t=ICiEQPjXqTttrzhE-0
