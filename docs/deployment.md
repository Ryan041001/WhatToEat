# 云服务部署说明（Railway）

## 部署范围
- 后端 Spring Boot + AI Service + MySQL（Railway 多服务）
- 微信小程序前端不在云平台部署，使用后端公网地址

## 部署配置文件
- railway.toml（后端）
- ai-service/railway.toml（AI Service）

## 部署步骤
1. Railway 新建 Project。
2. 添加 MySQL 服务（Railway 提供的数据库服务）。
3. 添加 Backend 服务：
   - 连接 GitHub 仓库。
   - 使用 `railway.toml` 构建（Dockerfile: backend/Dockerfile）。
   - Healthcheck 路径设为 `/health`。
4. 添加 AI Service：
   - 连接 GitHub 仓库。
   - Service Root 指向 `ai-service/`，或使用 `ai-service/railway.toml`。
   - Healthcheck 路径设为 `/health`。
5. 配置环境变量：
   - Backend：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`AMAP_KEY`、`AI_SERVICE_BASE_URL`
   - AI Service：`OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_MODEL`
   - 如平台需要，可补充 `NODE_ENV=production`
6. 自动部署：
   - 绑定 GitHub 仓库并选择部署分支（如 `main`）。
   - 开启自动部署。
7. 验证：
   - Backend：`GET <backend_url>/health` 返回 `status`。
   - AI Service：`GET <ai_service_url>/health` 返回 `status`。
8. 前端联调：
   - 将小程序后端地址切换到 `backend_url`（frontend/api/base-url.js 或运行时覆写）。

## 线上地址
- Backend：TBD
- AI Service：TBD