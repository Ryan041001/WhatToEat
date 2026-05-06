# 安全审查（AI 辅助）
日期：2026-05-06
范围：前端小程序页面组件、后端配置文件、认证/Session、评论、推荐和内部 AI 调用链路
审查人：林佳涛、沈哲伟

## 审查范围与文件
- 前端页面与组件（pages/index，components/navigation-bar）
- 后端运行配置（application-*.yml / application.properties）
- 认证与 Session（AuthController、AuthApplicationService、InMemorySessionStore）
- 用户评论与公开摘要（RestaurantReviewController、RestaurantReviewApplicationService、RestaurantReviewQueryApplicationService）
- 推荐与 AI 问答（RecommendationController、RecommendationApplicationService、AiHttpClient）
- CI 配置（GitHub Actions）

## 发现的问题与修复
### 1) 配置中明文数据库凭据（高）
风险：泄露后可直接访问数据库，导致数据泄露或篡改。

证据：
- backend/src/main/resources/application-dev.yml 中存在明文 username/password。
- backend/src/main/resources/application-docker.yml 中存在默认账号密码。

修复：
- 改为从环境变量读取（DB_URL/DB_USERNAME/DB_PASSWORD/DB_USER）。
- 提供 .env.example 作为配置模板。

状态：已修复

### 2) 配置中硬编码第三方 API Key（中）
风险：API Key 泄露会导致配额被滥用或产生费用。

证据：
- backend/src/main/resources/application-dev.yml、application-test.yml 中存在 AMAP Key。

修复：
- 改为从环境变量读取（AMAP_KEY），并在 .env.example 中标注。

状态：已修复

### 3) 用户输入可能形成存储型 XSS（高）
风险：登录昵称、评论正文、AI 推荐问题和上下文来自用户输入，若原样保存、返回或转发，未来前端误用 rich-text/innerHTML 时可能触发存储型 XSS。

证据：
- 评论正文可携带 `<script>`、HTML 标签和事件属性。
- AI 推荐 `question` 与 `context` 会进入内部 AI 链路，原始 HTML 会增加展示和 prompt 污染风险。
- 登录昵称会随用户信息返回给前端，属于可复用展示字段。

修复：
- 新增 `XssSanitizer`，按纯文本语义去除 HTML 标签、script/style 内容、事件属性和危险协议。
- 登录昵称、评论正文、AI 推荐问题与上下文进入业务链路前统一净化。
- 增加 `XssSanitizerTest`、`ReviewSecurityTest`、`RecommendationSecurityTest`、`AiRecommendationSecurityTest` 回归测试。

状态：已修复

### 4) AI 推荐输入缺少长度边界（中）
风险：超长问题或上下文数组可能造成资源消耗、日志噪声，或放大 prompt 注入与异常路径。

证据：
- `POST /recommendations/ask` 原先主要校验非空，缺少对 question、previousQuestion、context 数组及条目长度的统一边界。

修复：
- `question` / `previousQuestion` 限制为 500 字符。
- `rejectedPoiIds` / `selectedPoiIds` / `userSignals` 每组最多 20 项。
- POI ID 单项最多 128 字符，用户信号单项最多 64 字符。
- 对净化后为空的上下文条目拒绝处理。

状态：已修复

### 5) Session 与状态变更请求防护不足（中）
风险：若 token 无明确过期或 logout 后仍可复用，会扩大会话泄露影响；无 Bearer 的状态变更请求缺少最小防护头时，容易被误接入 cookie session 后暴露 CSRF 风险。

证据：
- 审查认证、logout、当前用户与状态变更接口时，发现需要用测试锁定 token 过期、登出失效和状态变更保护语义。

修复：
- Session token 明确过期时间，logout 后失效。
- 无 Bearer 的非安全方法要求携带 `X-CSRF-Token` 防护头，并使用统一 `ApiResponse` 返回错误。
- 增加 `AuthSecurityTest`、`InMemorySessionStoreTest`、`CsrfTokenFilterTest`。

状态：已修复

### 6) 基础安全响应头缺失（低）
风险：缺少 `X-Content-Type-Options`、`X-Frame-Options`、Referrer-Policy 等响应头时，浏览器侧防护能力不足。

修复：
- 新增 `SecurityHeadersFilter` 统一注入基础安全响应头。
- 增加 `SecurityHeadersFilterTest`。

状态：已修复

## OWASP 重点检查说明
- 注入（SQL/命令）：后端数据访问使用 Spring Data JPA / repository 查询，未发现字符串拼接 SQL；本轮重点修复 XSS 与 AI 输入净化。
- 失效访问控制：受保护用户接口使用 Bearer session，并校验 token 用户与路径 `userId` 一致；登出后 token 失效。
- 敏感信息泄露：已修复配置明文凭据和第三方 Key。
- 密码存储：当前项目为 mock WeChat 登录形态，不存储本地用户密码；不适用 bcrypt/argon2 密码哈希项。
- 错误信息暴露：业务异常通过统一 `ApiResponse` 返回业务码和固定消息；未知异常返回系统错误码，未向响应体暴露堆栈。
- Prompt 注入：HTML/script 净化已覆盖输入层；AI 结果仍依靠后端候选池约束，不能推荐候选 POI 之外的餐厅。自然语言 prompt injection 属于剩余风险，后续应继续在 AI service system prompt、工具调用与输出校验层加固。

## CI 自动化安全扫描
- 已添加 gitleaks 密钥泄露扫描工作流。
- 已添加 CodeQL 静态分析工作流（加分项），覆盖 main / master / develop 分支。
- 已配置 Dependabot 依赖更新提醒。

## 验证记录
- 后端目标安全测试：`JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -Dtest=XssSanitizerTest,RecommendationSecurityTest,CsrfTokenFilterTest test`
- 后端全量测试：`JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test`
- PR #55 远端检查：gitleaks、CodeQL、后端、前端和 AI service 检查均通过。
- PR #56 远端检查：gitleaks、后端、前端和 AI service 检查均通过。

## 剩余风险
- `X-CSRF-Token` 当前是状态变更防护头存在性检查，尚未实现正式 token 生成与绑定校验；若未来改成 cookie session，应升级为标准 CSRF token。
- Prompt injection 不能只靠 HTML sanitizer 彻底解决；后续应继续加强 AI service 的系统提示词、工具调用边界和输出校验。
- 内部 AI service 调用当前未加服务间 API key；若部署到共享网络，建议增加内部鉴权。
