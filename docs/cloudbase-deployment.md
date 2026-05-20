# CloudBase 免自有域名备案部署说明

本文档描述当前 `deploy-cloudbase` 分支的推荐部署方式：小程序备案照常做，但小程序不再请求自有公网域名或公网 IP，而是通过 `wx.cloud.callContainer` 访问微信云托管后端。

## 目标架构

```text
微信小程序
  -> wx.cloud.callContainer
  -> whattoeat-backend (Spring Boot, 云托管)
      -> CloudBase MySQL / TDSQL-C MySQL
      -> whattoeat-ai (FastAPI, 云托管内网访问)
          -> OPENAI_BASE_URL + OPENAI_API_KEY
      -> 高德 Web 服务 API
```

关键点：

- 小程序正式链路不使用 `https://api.xxx.com`，也不使用公网 IP。
- 小程序只调用 `whattoeat-backend`。
- `whattoeat-ai` 不暴露给小程序；建议关闭公网访问，开启内网访问。
- `OPENAI_BASE_URL` 是 `whattoeat-ai` 的出站目标，不是外部访问 `whattoeat-ai` 的入口；关闭 AI 服务公网入口不会阻止它调用外部模型 API。

## 前端配置

1. 在微信开发者工具打开 `frontend/`。
2. 在 `frontend/api/cloudbase-config.js` 中填写云开发环境 ID：

```js
const CLOUDBASE_ENV_ID = '你的云开发环境 ID';
const CLOUDBASE_BACKEND_SERVICE = 'whattoeat-backend';
```

3. 生产默认传输模式是 `cloudbase`，普通 API 通过 `wx.cloud.callContainer` 调用 `/api/v1/*`。
4. 本地调试后端时，可在开发者工具控制台临时切回 `wx.request`：

```js
wx.setStorageSync('apiTransportMode', 'request')
wx.removeStorageSync('apiBaseUrl')
```

如需恢复云托管链路：

```js
wx.setStorageSync('apiTransportMode', 'cloudbase')
```

## 后端云托管服务

服务名建议固定为：

```text
whattoeat-backend
```

配置：

- 源码目录：`backend/`
- Dockerfile：`backend/Dockerfile`
- 服务端口：`8080`，或使用平台注入的 `PORT`
- Spring profile：`cloudbase`
- 如果仅小程序调用，建议关闭公网访问；`wx.cloud.callContainer` 走微信与腾讯云之间的私有链路，不依赖公网域名。

环境变量：

```text
SPRING_PROFILES_ACTIVE=cloudbase
PORT=8080
DB_URL=jdbc:mysql://<CloudBase MySQL 内网地址>:3306/<数据库名>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USER=<数据库账号>
DB_PASSWORD=<数据库密码>
AMAP_KEY=<高德 Web 服务 Key>
WECHAT_APP_ID=<小程序 AppID>
WECHAT_APP_SECRET=<小程序 AppSecret>
WECHAT_MOCK_LOGIN_ENABLED=false
AI_SERVICE_BASE_URL=http://<whattoeat-ai 内网域名>
AI_SERVICE_TIMEOUT_SECONDS=5
```

## AI 云托管服务

服务名建议固定为：

```text
whattoeat-ai
```

配置：

- 源码目录：`ai-service/`
- Dockerfile：`ai-service/Dockerfile`
- 服务端口：`8000`，或使用平台注入的 `PORT`
- 开启内网访问
- 可关闭公网访问
- 后端的 `AI_SERVICE_BASE_URL` 应填写同环境下 `whattoeat-ai` 的内网域名，而不是公网域名。

环境变量：

```text
PORT=8000
OPENAI_API_KEY=<模型服务 API Key>
OPENAI_BASE_URL=<OpenAI-compatible base URL>
OPENAI_MODEL=<模型名>
OPENAI_TIMEOUT_SECONDS=30
```

## 数据库

当前后端通过 Flyway 自动建表，CloudBase MySQL 初始化后首次启动 `whattoeat-backend` 会执行 `backend/src/main/resources/db/migration/` 下的迁移。

注意：

- 不要手工改表结构绕过 Flyway。
- 不要把数据库开放到公网。
- `restaurant` 主数据仍来自高德 POI，本地数据库只保存用户侧数据、评论和聚合快照。

## 官方最佳实践核对表

- 小程序端在 `app.js` 启动时初始化一次 `wx.cloud`，业务请求统一走 `wx.cloud.callContainer`。
- `callContainer` 请求头必须带 `X-WX-SERVICE=whattoeat-backend`，路径保持 `/api/v1/*`，避免前端直接访问高德或 AI Service。
- 仅小程序访问的服务关闭公网访问；服务间访问需要开启被调用服务的内网访问。
- 云托管版本的服务端口必须与容器实际监听端口一致；本分支通过 `PORT` 环境变量让 Spring Boot 与 Uvicorn 监听平台配置的端口。
- 后端仍保留项目自有 Bearer Token 会话；云托管会额外注入 `x-wx-openid` / `x-wx-unionid` / `x-cloudbase-context` 等请求头，后续如切到 CloudBase 身份认证可在网关侧开启鉴权后再接入。
- 容器日志应输出到 stdout/stderr，方便在云托管日志中查询；服务实例应保持无状态，上传文件、会话持久化等不要依赖本地磁盘。
- 镜像构建保持多阶段 Dockerfile，并通过 `.dockerignore` 排除测试缓存、依赖缓存和本地配置，减少构建上下文和镜像体积。

## 验证顺序

1. 先部署 `whattoeat-ai`，确认 `/health` 正常。
2. 给 `whattoeat-ai` 配好 `OPENAI_BASE_URL` 和 `OPENAI_API_KEY`，确认它可以出站调用模型服务。
3. 部署 `whattoeat-backend`，确认 `/health` 正常。
4. 在 backend 环境变量中把 `AI_SERVICE_BASE_URL` 配成 AI 服务内网域名。
5. 用小程序 `callContainer` 调 `GET /api/v1/restaurants/nearby`。
6. 再验证 `POST /api/v1/recommendations/ask`。
7. 最后验证流式 `POST /api/v1/recommendations/ask/stream`；若目标环境不稳定支持分块，前端应临时降级到同步问答。
