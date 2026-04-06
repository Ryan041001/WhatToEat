# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

"今天吃什么 / WhatToEat" 是一款基于微信小程序的餐厅随机推荐应用，帮助用户解决"不知道吃什么"的选择困难。

**技术栈：**
- 前端：微信小程序原生开发
- 后端：Spring Boot 4 + Java 17 + Spring Data JPA
- 数据库：MySQL 8（dev 环境）/ H2（test 环境）
- 外部服务：高德 Web 服务 API（餐厅主数据来源）
- 数据库迁移：Flyway

**仓库结构：**
```
WhatToEat/
├── frontend/          # 微信小程序前端
├── backend/           # Spring Boot 后端
│   └── src/main/java/com/zjgsu/whattoeat/
│       ├── controller/          # REST endpoints
│       ├── service/
│       │   ├── application/     # 流程编排层
│       │   └── domain/          # 业务规则层
│       ├── integration/amap/    # 高德 API 集成
│       ├── repository/          # JPA repositories
│       ├── model/entity/        # 持久化实体
│       ├── common/              # 统一响应、异常处理
│       └── config/              # 配置绑定
├── docs/              # 架构、数据库、API 文档
└── docs/design/       # UI 原型（React + Vite + Tailwind）
```

---

## Common Commands

### Backend Development

**运行后端（dev 环境，需要本地 MySQL）：**
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run
```

**运行后端（test 环境，使用 H2 内存数据库）：**
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
```

**运行测试：**
```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test
```

**Docker Compose 本地联调：**
```bash
cd backend
cp .env.example .env
# 编辑 .env 文件，将 AMAP_KEY 替换为真实高德 Key
docker compose --env-file .env up --build
```

### Frontend Development

- 使用微信开发者工具打开 `frontend/` 目录
- 本地调试时后端地址：`http://127.0.0.1:8080`
- 真机同局域网调试时：`http://<宿主机局域网IP>:8080`

### UI Prototype

```bash
cd docs/design
npm install
npm run dev      # 开发服务器
npm run build    # 构建生产版本
```

---

## Architecture & Design Principles

### Backend Architecture

**分层职责：**
- **Controller**：参数接收、校验、返回统一响应结构
- **Application Service**：编排业务流程（例如推荐时先取候选再过滤）
- **Domain Service**：核心业务规则（随机策略、过滤策略）
- **Repository（JPA）**：用户侧数据持久化访问
- **Integration/Amap**：高德接口调用、超时与错误转换、DTO 映射
- **Common**：统一返回体、全局异常处理、业务错误码

**关键约束：**
- 餐厅主数据不落地为本地主表（统一来源于高德 POI）
- 本地数据库只保存用户侧数据（黑名单、备注、历史）
- 前端不直接调用高德 API，由后端统一代理与清洗

### Database Design

**用户侧数据表：**
- `users`：用户基本信息（openid、nickname）
- `user_blacklist`：用户黑名单（user_id + poi_id 唯一约束）
- `user_restaurant_note`：用户备注（user_id + poi_id 唯一约束）
- `user_choice_history`：用户选择历史（预留，当前未接入推荐链路）

**迁移管理：**
- 使用 Flyway 管理数据库版本
- 迁移脚本位于 `backend/src/main/resources/db/migration/`
- 禁止手改线上库结构，统一走 migration

### API Design

**接口统一前缀：** `/api/v1`

**已实现资源：**
- `auth`：登录、登出、当前用户
- `restaurants`：餐厅查询（附近查询、关键词搜索）
- `recommendations`：随机推荐与卡片候选列表
- `users/{userId}/blacklist`：用户黑名单 CRUD
- `users/{userId}/notes`：用户备注 CRUD

**统一返回格式：**
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

**鉴权方式：**
- 当前采用 mock 微信登录 + Bearer Token 会话
- 受保护接口通过请求头传递：`Authorization: Bearer <token>`

**坐标系约定：**
- 所有经纬度参数使用 **GCJ-02**（火星坐标系）
- 传入 WGS-84 会导致位置偏移

---

## Development Rules

### Backend Coding Rules

- **保持 Controller 薄**：业务流程属于 application/domain services
- **不要绕过 repository 或 integration 边界**
- **API 响应必须包装在统一响应模型中**
- **优先使用显式异常和集中式错误处理**
- **保持文档与实际代码、迁移脚本对齐**

### Database Rules

- **不要硬编码敏感信息**到源文件
- **不要在没有新迁移的情况下修改数据库结构**
- **不要将计划中的行为记录为已实现**

### Documentation Rules

- `docs/architecture.md` 是后端架构的真实来源
- `docs/database.md` 必须与 Flyway SQL 保持一致
- `docs/api.md` 应反映已暴露的后端接口
- `docs/api.yaml` 是 OpenAPI 主契约文件，可导入 Apifox/Postman/Swagger Editor
- 个人贡献记录放在 `docs/contributions/` 下

### Frontend Rules

- **仅在请求后端工作时不要更改前端约定**

---

## Environment Configuration

### Profiles

- `dev`：本地开发，连接本机 MySQL `whattoeat_dev`
- `test`：本地无 MySQL 验证或测试环境，使用 H2 内存数据库
- `docker`：Docker Compose 环境，启动 MySQL + 后端

### Configuration Files

- `application.properties` + `application-{profile}.yml`
- 敏感信息（如高德 key）通过环境变量或 `.env` 文件注入
- `.env.example` 提供了配置模板

---

## Testing & Debugging

### Backend Testing

- 测试使用 H2 内存数据库
- 运行全部测试：`JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test`

### API Testing

1. 导入 `docs/api.yaml` 到 Apifox/Postman
2. 先调用 `POST /api/v1/auth/wechat-login` 获取 token
3. 在受保护接口中配置 `Authorization: Bearer <token>`
4. 注意：`localhost` 只适合开发者工具，真机应使用宿主机局域网 IP

### Common Issues

- **高德接口调用失败**：检查 `.env` 中的 `AMAP_KEY` 是否为真实 Key
- **坐标偏移**：确认传入的经纬度使用 GCJ-02 坐标系
- **路径参数为空**：在 Apifox 中使用环境变量时，确认最终 URL 已正确替换

---

## Key Business Flows

### Random Recommendation Flow

1. 前端请求 `GET /api/v1/recommendations/random`（传入经纬度、半径、可选 userId）
2. 后端调用高德 nearby 接口获取候选餐厅
3. 如果传入 userId，查询该用户的黑名单
4. 过滤黑名单后的候选列表
5. 执行随机策略并返回单个推荐结果
6. 可选记录到 `user_choice_history`

### User Blacklist Flow

1. 前端请求 `POST /api/v1/users/{userId}/blacklist`
2. 后端校验 Bearer Token 与路径 `userId` 一致
3. 检查唯一键（`user_id + poi_id`）
4. 持久化写入 `user_blacklist`
5. 返回统一成功响应

---

## Important Notes

- **当前认证实现为 mock 微信登录**，接口形状贴近真实小程序登录流程
- **餐厅主数据来源于高德**，不在本地维护完整餐厅主表
- **当前已实现的用户侧写接口**包括黑名单与备注 CRUD、推荐查询
- **`user_choice_history` 当前只在数据模型中预留**，尚未接入现行推荐链路
