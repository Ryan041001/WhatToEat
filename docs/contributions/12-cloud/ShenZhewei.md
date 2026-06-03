# 云服务部署贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-06-03

## 课程内容对应

本次文档对应“云服务部署与小程序上线联调”部分。基于 `deploy-vps` 与 `deploy-cloudbase` 分支推进的小程序云部署方案：

- `deploy-vps`：先固定 VPS HTTPS 后端入口，完成小程序到服务器的联调快照。
- `deploy-cloudbase`：再切到微信云托管 / CloudBase，让小程序通过 `wx.cloud.callContainer` 访问后端入口。
- 已实现但未最终采用的方案：**CloudBase 轻量代理 + VPS 实际承载业务后端、MySQL、AI Service**。

## 我的分工和分支记录

### 1. VPS 联调入口

对应分支：`deploy-vps`

对应提交：

- `435d07d feat(deploy): 固定小程序VPS联调入口`

我完成的工作：

- [x] 将小程序默认请求入口切到 VPS HTTPS 后端。
- [x] 清理早期本地联调残留的 API base 缓存，避免体验版继续请求 `localhost` 或局域网地址。
- [x] 扩大首页和餐厅列表候选数量，方便 VPS 联调时验证筛选和展示效果。
- [x] 明确该分支只是 VPS 部署快照，不作为最终 CloudBase 免备案路径继续扩展。

### 2. CloudBase 云托管正式链路设计

对应分支：`deploy-cloudbase`

对应提交：

- `68b6361 feat(deploy): 通过 CloudBase 移除正式链路域名依赖`
- `894c239 feat(deploy): 用 CloudBase 代理承接 VPS 后端入口`
- `9db56b2 fix(deploy): 指向 VPS 的 HTTPS 代理入口`
- `92d9825 fix(deploy): 放宽 CloudBase 代理流式超时`
- `4806f22 fix(frontend): 固定小程序云托管环境`
- `f202ee9 修正云托管调用的开发者工具身份`

我完成的工作：

- [x] 设计小程序正式链路：小程序不再直连公网 IP，而是通过 `wx.cloud.callContainer` 进入云托管。
- [x] 保留项目约束：小程序只访问 backend，不直接调用 AI Service。
- [x] 新增 CloudBase 前端传输层，默认 `apiTransportMode=cloudbase`，本地调试时可切回 `request`。
- [x] 固定云开发环境 ID：`cloud1-d0gendp5i219d4f5f`。
- [x] 固定云托管服务名：`whattoeat-backend`。
- [x] 在 `app.js` 启动时初始化 `wx.cloud`。
- [x] 对普通 API 和流式接口都接入 `wx.cloud.callContainer`。
- [x] 补充 CloudBase 部署说明和 `Invalid host` 排查步骤。

## 最终部署方案

### 实现状态与未采用原因

这套 CloudBase 代理方案已经在 `deploy-cloudbase` 分支中实现，包括小程序 `wx.cloud.callContainer` 传输层、CloudBase 代理服务、VPS HTTPS 上游配置、流式超时配置和排查文档。

但是它没有作为最终采用的线上部署方案，主要原因是当前网络环境无法稳定支撑这条链路：

- 小程序正式链路依赖 `wx.cloud.callContainer` 进入 CloudBase 云托管。
- CloudBase 代理再访问 VPS 的 HTTPS 入口。
- 当前环境下这条“微信云托管 -> VPS HTTPS 后端”的链路无法稳定完成实际验证。
- AI 推荐流式接口还依赖长连接 / 分块传输，网络不稳定时更容易出现代理层或上游链路提前中断。

因此，这套方案保留为**已实现的备选云部署方案**，用于说明小程序免自有域名备案的部署思路；最终没有采用，是因为部署环境和网络链路可靠性不足，而不是代码方案没有完成。

### 低成本模式：CloudBase 代理到 VPS

这是已经实现过的低成本方案，但受网络环境影响没有最终采用。

```text
微信小程序
  -> wx.cloud.callContainer
  -> whattoeat-backend (cloudbase-proxy，微信云托管)
      -> https://38.65.93.54/api/v1/* (VPS Nginx)
          -> Spring Boot backend
          -> VPS MySQL
          -> VPS ai-service
          -> 高德 Web 服务 API
```

选择这个方案的原因：

- 小程序正式版不能稳定依赖公网 IP 或未备案自有域名。
- CloudBase 个人版可以低成本部署一个轻量代理服务。
- VPS 继续承载实际业务服务和数据库，不需要立即购买 CloudBase 标准版私有网络或云数据库。
- CloudBase 代理只做请求转发，不保存业务数据。

### CloudBase 代理服务配置

服务名固定为：

```text
whattoeat-backend
```

配置项：

- 源码目录：`cloudbase-proxy/`
- Dockerfile：`cloudbase-proxy/Dockerfile`
- 服务端口：`8080`
- 健康检查：`GET /health`
- 公网访问：可关闭，小程序通过 `callContainer` 访问
- 私有网络：低成本代理模式下不需要购买

环境变量：

```text
PORT=8080
UPSTREAM_BASE_URL=https://38.65.93.54
PROXY_TIMEOUT_MS=120000
```

注意：

- `UPSTREAM_BASE_URL` 填 VPS 的 HTTPS origin，不要填 `http://38.65.93.54:8080`。
- VPS 上 Spring Boot 后端只绑定在 `127.0.0.1:8080`，云托管无法直接访问该端口。
- VPS 对外入口是 Nginx 暴露的 `https://38.65.93.54/api/*`。
- `PROXY_TIMEOUT_MS=120000` 是为了支撑 AI 推荐的 SSE 流式响应，避免 30 秒默认超时提前断流。

### 小程序端配置

CloudBase 配置：

```js
const CLOUDBASE_ENV_ID = 'cloud1-d0gendp5i219d4f5f';
const CLOUDBASE_BACKEND_SERVICE = 'whattoeat-backend';
```

小程序 AppID：

```text
wx395056b9fe3de58e
```

正式模式：

- 默认传输模式：`cloudbase`
- 普通 API：`wx.cloud.callContainer`
- 流式 API：`wx.cloud.callContainer` + 分块解析
- 请求路径保持 `/api/v1/*`
- 请求头带 `X-WX-SERVICE=whattoeat-backend`

本地调试时可在开发者工具 Console 切回 `wx.request`：

```js
wx.setStorageSync('apiTransportMode', 'request')
wx.removeStorageSync('apiBaseUrl')
```

恢复云托管链路：

```js
wx.setStorageSync('apiTransportMode', 'cloudbase')
```

## 可选方案：全 CloudBase 私网部署

如果后续预算允许购买 CloudBase 标准版私有网络，可以把业务服务全部迁入云托管：

```text
微信小程序
  -> wx.cloud.callContainer
  -> whattoeat-backend (Spring Boot，云托管)
      -> CloudBase MySQL / TDSQL-C MySQL
      -> whattoeat-ai (FastAPI，云托管内网访问)
          -> OpenAI-compatible 模型服务
```

该方案的注意点：

- 后端服务名仍建议保留 `whattoeat-backend`。
- AI 服务名建议为 `whattoeat-ai`。
- 小程序仍只调用 backend，不能直接调用 AI Service。
- AI Service 可关闭公网访问，只给 backend 内网访问。
- 后端使用 `cloudbase` profile，并通过环境变量注入数据库、高德、微信和 AI 服务配置。

后端云托管关键环境变量：

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

AI Service 云托管关键环境变量：

```text
PORT=8000
OPENAI_API_KEY=<模型服务 API Key>
OPENAI_BASE_URL=<OpenAI-compatible base URL>
OPENAI_MODEL=<模型名>
OPENAI_TIMEOUT_SECONDS=30
```

## 验证顺序

低成本 CloudBase 代理模式：

1. 先确认 VPS 上的 backend、MySQL、AI Service 正常。
2. 在 CloudBase 云托管部署 `cloudbase-proxy/`。
3. 服务名填 `whattoeat-backend`。
4. 配置 `UPSTREAM_BASE_URL=https://38.65.93.54`。
5. 配置 `PROXY_TIMEOUT_MS=120000`。
6. 确认云托管服务 `/health` 正常。
7. 小程序体验版验证 `GET /api/v1/auth/me` 或 `POST /api/v1/auth/wechat-login`。
8. 验证 `GET /api/v1/restaurants/nearby`。
9. 验证 `POST /api/v1/recommendations/ask`。
10. 最后验证 `POST /api/v1/recommendations/ask/stream`。

如果流式接口在目标环境里分块不稳定，前端应临时降级到同步问答接口。

## `Invalid host` 排查

如果小程序端报：

```text
cloud.callContainer:fail Error: errCode: -501000 | errMsg: Invalid host
```

优先排查：

1. 微信开发者工具右上角 AppID 必须是 `wx395056b9fe3de58e`。
2. 云开发环境 `cloud1-d0gendp5i219d4f5f` 必须归属或关联到同一个小程序 AppID。
3. 根目录 `project.config.json` 和 `frontend/project.config.json` 的 AppID 要一致。
4. 云托管服务名必须是 `whattoeat-backend`。
5. 前端请求头必须包含 `X-WX-SERVICE=whattoeat-backend`。
6. 如果云环境属于另一个小程序，需要先开启云开发环境共享，再改成跨环境调用。

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

如果这个最小验证仍然报 `Invalid host`，说明请求没有到达代理服务，应继续排查 AppID、云环境关联或环境共享；如果最小验证成功但业务接口失败，再排查代理、VPS 或后端接口。

## 验证记录

从 `deploy-vps` / `deploy-cloudbase` 分支提交记录看，相关验证包括：

- `frontend npm test -- --runInBand`
- `frontend npm run lint`
- `backend ./mvnw test`
- `uv run --with pytest pytest -q`
- `node --test cloudbase-proxy/server.test.js`
- `docker build -t whattoeat-cloudbase-proxy:check cloudbase-proxy`
- `docker build -t whattoeat-backend:cloudbase-check backend`
- `docker build -t whattoeat-ai:cloudbase-check ai-service`
- `git diff --check`

未验证项：

- 当前网络环境下，CloudBase 到 VPS HTTPS 后端链路未能稳定作为最终线上入口。
- 小程序真机经 `wx.cloud.callContainer` 访问 VPS 后端未作为最终方案采用。
- 云托管环境下 AI 流式接口的分块稳定性仍有风险。

## PR 链接

- 相关分支：`deploy-vps`
- 相关分支：`deploy-cloudbase`
- 12-cloud 贡献文档补充在当前 `feat/optimize-ShenZhewei-14` 分支中。

## 在线地址

- CloudBase 服务名：`whattoeat-backend`
- CloudBase 环境：`cloud1-d0gendp5i219d4f5f`
- VPS HTTPS 入口：`https://38.65.93.54`
- 线上体验版地址：TBD

## 心得体会

这次部署方案让我更清楚地区分了“服务真的部署在哪里”和“小程序合法入口在哪里”。VPS 已经能承载 Spring Boot、MySQL 和 AI Service，但小程序正式链路不能简单依赖公网 IP。CloudBase 代理模式把入口合规问题和业务运行位置拆开：小程序走 `wx.cloud.callContainer`，CloudBase 负责转发，VPS 继续跑真实业务。这个方案成本低、改动集中，也为后续升级到全 CloudBase 私网部署保留了路径。
