# CloudBase 免自有域名备案部署说明

本文档描述当前 `deploy-cloudbase` 分支的推荐部署方式：小程序备案照常做，但小程序不再请求自有公网域名或公网 IP，而是通过 `wx.cloud.callContainer` 访问微信云托管服务。

当前最省钱路线是 **CloudBase 代理到 VPS**：VPS 继续运行 Spring Boot backend、MySQL 和 AI Service；CloudBase 只部署一个轻量 `whattoeat-backend` 代理服务，作为小程序正式入口。

## 目标架构

### 低成本 VPS 代理模式（当前推荐）

```text
微信小程序
  -> wx.cloud.callContainer
  -> whattoeat-backend (cloudbase-proxy, 云托管)
      -> https://38.65.93.54/api/v1/* (VPS Nginx -> Spring Boot backend)
          -> VPS MySQL
          -> VPS ai-service
          -> 高德 Web 服务 API
```

关键点：

- 小程序正式链路不使用 `wx.request` 直连 `38.65.93.54`，避免小程序合法域名 / 自有域名备案问题。
- CloudBase 个人版即可部署代理，不需要购买标准版私有网络。
- `whattoeat-backend` 这个服务名保留给 CloudBase 代理，所以前端无需改服务名。
- VPS 仍是实际业务后端和数据库所在位置；CloudBase 代理只做请求转发，不保存业务数据。

### 全 CloudBase 私网模式（付费后可选）

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
- 该模式需要 CloudBase 私有网络 / 云数据库等付费能力；当前若预算有限，先使用低成本 VPS 代理模式。

## 前端配置

1. 在微信开发者工具打开 `frontend/`。
2. 确认小程序 AppID 是当前云开发环境关联的小程序：

```text
wx395056b9fe3de58e
```

如果开发者工具打开的是 `frontend/`，它会读取 `frontend/project.config.json`；如果打开的是仓库根目录，它会读取根目录 `project.config.json` 并使用 `miniprogramRoot=frontend/`。两个配置里的 AppID 必须一致，否则 `wx.cloud.callContainer` 可能在 CloudBase 网关层直接返回 `Invalid host`。

官方说明：

- `wx.cloud.callContainer` 默认只能访问本小程序已关联云开发环境里的云托管服务。
- 跨环境访问默认不支持；只有目标环境开启“云开发环境共享”后，才能通过 `resourceAppid/resourceEnv` 访问。

参考：https://docs.cloudbase.net/run/develop/access/mini

3. 确认 `frontend/api/cloudbase-config.js` 中的云开发环境 ID 与控制台环境一致：

```js
const CLOUDBASE_ENV_ID = 'cloud1-d0gendp5i219d4f5f';
const CLOUDBASE_BACKEND_SERVICE = 'whattoeat-backend';
```

如果后续换云开发环境，需要同步修改 `CLOUDBASE_ENV_ID` 并重新上传小程序版本。

4. 生产默认传输模式是 `cloudbase`，普通 API 通过 `wx.cloud.callContainer` 调用 `/api/v1/*`。
5. 本地调试后端时，可在开发者工具控制台临时切回 `wx.request`：

```js
wx.setStorageSync('apiTransportMode', 'request')
wx.removeStorageSync('apiBaseUrl')
```

如需恢复云托管链路：

```js
wx.setStorageSync('apiTransportMode', 'cloudbase')
```

## 低成本模式：CloudBase 代理服务

服务名建议固定为：

```text
whattoeat-backend
```

配置：

- 源码目录：`cloudbase-proxy/`
- Dockerfile：`cloudbase-proxy/Dockerfile`
- 服务端口：`8080`，或使用平台注入的 `PORT`
- 公网访问：可关闭；小程序通过 `callContainer` 访问
- 私有网络：不需要购买

环境变量：

```text
PORT=8080
UPSTREAM_BASE_URL=https://38.65.93.54
PROXY_TIMEOUT_MS=120000
```

说明：

- `UPSTREAM_BASE_URL` 必须填写 VPS backend 的服务端入口，不要带 `/api/v1` 也可以；代理会保留小程序传来的 `/api/v1/*` 路径。
- 当前 VPS 的 Spring Boot backend 只绑定在 `127.0.0.1:8080`，公网入口是 Nginx 的 `https://38.65.93.54/api/*`，所以 CloudBase 代理必须使用 `https://38.65.93.54`。
- VPS 防火墙需要允许 CloudBase 代理访问 HTTPS 443。若无法限制来源，至少确保后端接口仍靠 Bearer Token 与 CSRF 规则保护状态变更请求。
- 代理本身提供 `/health`，用于云托管健康检查。

部署后验证：

1. 在云托管控制台确认 `whattoeat-backend` 健康检查通过。
2. 打开小程序体验版，确认 `wx.cloud.callContainer` 能调用 `/api/v1/auth/wechat-login`。
3. 再测 `/api/v1/restaurants/nearby` 和 `/api/v1/recommendations/ask/stream`。

## 付费模式：Spring Boot 后端云托管服务

如果后续升级 CloudBase 标准版并使用私有网络，可把 Spring Boot backend 直接部署到云托管。

服务名仍建议固定为：

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

## 付费模式：AI 云托管服务

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

低成本 VPS 代理模式下，数据库继续使用 VPS 上已经部署好的 MySQL，不需要 CloudBase MySQL。

全 CloudBase 私网模式下，后端通过 Flyway 自动建表，CloudBase MySQL 初始化后首次启动 `whattoeat-backend` 会执行 `backend/src/main/resources/db/migration/` 下的迁移。

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

低成本 VPS 代理模式：

1. 先确认 VPS 上的 backend、MySQL、AI service 正常。
2. 在 CloudBase 部署 `cloudbase-proxy/`，服务名填 `whattoeat-backend`。
3. 配置 `UPSTREAM_BASE_URL=https://38.65.93.54`。
4. 确认云托管 `whattoeat-backend` 的 `/health` 正常。
5. 用小程序 `callContainer` 调 `GET /api/v1/restaurants/nearby`。
6. 再验证 `POST /api/v1/recommendations/ask`。
7. 最后验证流式 `POST /api/v1/recommendations/ask/stream`；若目标环境不稳定支持分块，前端应临时降级到同步问答。

## `Invalid host` 排查

如果小程序端报：

```text
cloud.callContainer:fail Error: errCode: -501000 | errMsg: Invalid host
```

优先按下面顺序排查：

1. 微信开发者工具右上角的 AppID 必须是 `wx395056b9fe3de58e`。
2. 云开发环境 `cloud1-d0gendp5i219d4f5f` 必须归属或关联到同一个小程序 AppID。
3. 云托管服务名必须是 `whattoeat-backend`。
4. 前端请求头必须包含 `X-WX-SERVICE=whattoeat-backend`。
5. 如果云环境属于另一个小程序，先在控制台开启“云开发环境共享”，再改成 `new wx.cloud.Cloud({ resourceAppid, resourceEnv })` 的跨环境调用方式。

开发者工具 Console 可先跑最小验证：

```js
wx.cloud.init({
  env: 'cloud1-d0gendp5i219d4f5f',
  traceUser: true
})

wx.cloud.callContainer({
  config: { env: 'cloud1-d0gendp5i219d4f5f' },
  path: '/health',
  method: 'GET',
  header: { 'X-WX-SERVICE': 'whattoeat-backend' },
  dataType: 'text'
}).then(console.log).catch(console.error)
```

如果这个最小验证仍然是 `Invalid host`，请求没有到达代理服务，继续处理 AppID / 云环境关联或环境共享；如果最小验证成功但业务接口失败，再排查代理、VPS 或后端接口。

全 CloudBase 私网模式：

1. 先部署 `whattoeat-ai`，确认 `/health` 正常。
2. 给 `whattoeat-ai` 配好 `OPENAI_BASE_URL` 和 `OPENAI_API_KEY`，确认它可以出站调用模型服务。
3. 部署 Spring Boot `whattoeat-backend`，确认 `/health` 正常。
4. 在 backend 环境变量中把 `AI_SERVICE_BASE_URL` 配成 AI 服务内网域名。
5. 按上面的业务接口顺序验证。
