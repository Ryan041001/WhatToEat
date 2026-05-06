# 安全审查记录

日期：2026-05-06

## 审查范围

- 认证与会话：`AuthController`、`AuthApplicationService`、`InMemorySessionStore`
- 评论写入：`RestaurantReviewController`、`RestaurantReviewApplicationService`
- 推荐与 AI 问答：`RecommendationController`、`RecommendationApplicationService`、`AiHttpClient`
- 公共安全组件：`XssSanitizer`、`CsrfTokenFilter`、`SecurityHeadersFilter`
- API 契约：`docs/api.md`、`docs/api.yaml`

## AI 辅助安全审查结论

审查重点按 OWASP Top 10 覆盖注入、鉴权、敏感信息、错误暴露和安全配置。

### 已发现并处理的问题

1. XSS：登录昵称、评论正文、AI 推荐问题和上下文都来自用户输入。原任务 1 已对登录昵称和头像做基础消毒，但昵称仍允许安全 HTML；本轮改为纯文本保存，并把评论正文、AI `question`、`previousQuestion`、`userSignals` 等进入业务或内部 AI 前统一移除 HTML / script 标签。
2. 输入边界：`POST /recommendations/ask` 的 `question` 原先只有 `@NotBlank`，超长输入会进入推荐候选查询链路。本轮补充 `question <= 500`、`previousQuestion <= 500`、上下文数组最多 20 项、POI ID 和 user signal 单项长度限制。
3. 评论存储型 XSS：`RestaurantReviewApplicationService` 原先直接 `content.trim()` 入库。本轮改为先纯文本净化，再执行空值和长度校验。
4. Prompt 注入风险：HTML/脚本净化不能彻底解决自然语言 prompt injection，但可以阻断把前端脚本或 HTML 片段原样传入内部 AI 上下文。本项目的核心约束仍由后端候选池和结构化推荐卡片保证：AI 只能从候选 POI 中推荐。

### 当前适用或已覆盖项

- SQL 注入：当前数据库访问使用 Spring Data JPA repository 和参数化查询，没有发现字符串拼接 SQL。
- 越权访问：用户侧评论、黑名单、备注、历史、反馈、画像接口均通过 Bearer token 校验路径 `userId` 与当前用户一致。
- Session：内存 token 已有 TTL，logout 后删除 token。
- CSRF：当前策略为无 Bearer 的状态变更请求要求 `X-CSRF-Token`；Bearer token 请求放行。该策略适合当前小程序 mock 会话阶段，但不是完整 cookie-session CSRF 双提交方案。
- 安全响应头：`SecurityHeadersFilter` 已添加 `nosniff`、`DENY frame`、`Referrer-Policy`、`no-store` 等响应头。
- 敏感信息：未发现硬编码 Amap/OpenAI 密钥；`.env.example` 仅提供占位配置。

## 验证记录

- 新增红灯测试后确认失败：
  - `ReviewSecurityTest` 暴露评论正文原样保存 HTML/script。
  - `RecommendationSecurityTest` 暴露超长 `question` 未被请求校验拦截。
  - `AiRecommendationSecurityTest` 暴露 `question/context` 原样传给内部 AI。
- 修复后目标测试通过：
  - `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -Dtest=ReviewSecurityTest,RecommendationSecurityTest,AiRecommendationSecurityTest,AuthSecurityTest,XssSanitizerTest test`

## 剩余风险

- CSRF filter 当前只检查头是否存在，不校验 token 来源或绑定关系；若后续改为 cookie session，应实现正式的 CSRF token 生成与校验。
- Prompt injection 不能靠 HTML sanitizer 完全解决；后续可在 AI service 的 system prompt、结构化工具调用和输出校验层继续加固。
- `AiHttpClient` 调用内部 AI service 当前未加内部 API key。本地 Docker 网络内风险较低，但部署到共享网络时建议增加服务间鉴权。
