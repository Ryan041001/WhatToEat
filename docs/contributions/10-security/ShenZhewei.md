# 安全审查贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-05-06

## 我完成的工作

### AI 安全审查

- 审查了认证、评论、推荐和内部 AI 调用相关代码。
- 发现并修复了评论正文、AI 推荐问题和上下文中 HTML / script 原样进入业务链路的问题。
- 发现并收紧了 AI 推荐 `question` 与 `context` 的输入长度边界。
- 合并团队审查结论，补充了后端配置明文凭据、Amap Key 占位、CI 安全扫描等交付内容。

### 后端深度审查记录

- 审查范围：`AuthController`、`AuthApplicationService`、`InMemorySessionStore`、`RestaurantReviewController`、`RestaurantReviewApplicationService`、`RecommendationController`、`RecommendationApplicationService`、`AiHttpClient`。
- XSS：登录昵称、评论正文、AI 推荐问题和上下文都来自用户输入，已在进入业务、持久化或内部 AI 前按纯文本净化。
- 输入边界：`POST /recommendations/ask` 增加 `question <= 500`、`previousQuestion <= 500`、上下文数组最多 20 项、POI ID 单项不超过 128、用户信号不超过 64。
- Prompt 注入：HTML / script 净化不能完全解决自然语言 prompt injection，当前依靠后端候选池与结构化推荐卡片约束 AI 只能从候选 POI 中推荐。

### 安全检查清单

- 认证与授权：使用 Bearer session；受保护用户接口校验 token 用户与路径 `userId` 一致。
- Session：token 有过期时间，logout 后失效。
- SQL 注入：使用 Spring Data JPA / repository 查询，无字符串拼接 SQL。
- XSS：登录昵称、评论正文、AI 推荐输入按纯文本净化。
- 敏感信息：后端 dev / docker 配置改为读取环境变量；`.env.example` 只提供空值占位模板。
- 依赖安全：Dependabot 已配置依赖更新提醒。
- 安全 HTTP 头：后端统一注入基础安全响应头。
- CSRF：无 Bearer 的状态变更请求要求 `X-CSRF-Token`。
- 错误信息暴露：业务异常通过统一 `ApiResponse` 返回业务码和固定消息，安全测试未发现堆栈直接暴露到响应体。

### CI 安全扫描

- 配置选项 A：已添加 `.github/workflows/security.yml`，使用 gitleaks 做密钥泄露扫描。
- 选做项 C：已添加 `.github/workflows/codeql.yml`，对 Java / JavaScript 执行 CodeQL 静态分析。
- 当前仓库已配置 Dependabot 分生态依赖更新提醒。
- 本轮验证重点为后端安全回归测试、全量 Maven 测试和 GitHub Actions 安全扫描配置。

### 验证记录

- 红灯测试：新增 `ReviewSecurityTest`、`RecommendationSecurityTest`、`AiRecommendationSecurityTest` 后，分别暴露评论 HTML 入库、超长 question 未拦截、AI 输入原样转发的问题。
- 修复后目标测试：`JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -Dtest=ReviewSecurityTest,RecommendationSecurityTest,AiRecommendationSecurityTest,AuthSecurityTest,XssSanitizerTest test`
- 修复后全量测试：`JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean test`

### 剩余风险

- CSRF filter 当前只检查 `X-CSRF-Token` 是否存在，未绑定 token 来源；如果未来改成 cookie session，应实现正式的 token 生成与校验。
- Prompt injection 不能只靠 HTML sanitizer 彻底解决；后续应在 AI service 的 system prompt、工具调用和输出校验层继续加固。
- `AiHttpClient` 调用内部 AI service 当前未加服务间 API key；部署到共享网络时建议增加内部鉴权。

## 遇到的问题和解决

1. 问题：评论内容可以原样保存 `<script>` 和 HTML 标签。
   解决：在 `RestaurantReviewApplicationService` 入库前调用纯文本净化，并新增回归测试。

2. 问题：AI 推荐 `question` 只有非空校验，且 HTML/script 会原样传给内部 AI。
   解决：补充长度限制，进入推荐服务前净化 `question` 和 `context`。

3. 问题：登录昵称仍允许安全 HTML。
   解决：改为纯文本保存，避免未来前端误用 rich-text 时形成存储型 XSS。

4. 问题：配置文件中存在默认数据库凭据和 Amap Key 占位，且单独查看本 PR 时缺少 CI 安全扫描交付。
   解决：将 dev / docker 配置改为环境变量注入，补充 gitleaks 与 CodeQL workflow，并在安全审查记录中汇总团队完成情况。

## 心得体会

Vibe Coding 可以快速补齐安全措施，但安全审查不能只看“加了 sanitizer / filter”这种表面动作。更重要的是把输入最终会保存到哪里、展示在哪里、传给哪个上游服务想清楚，并用回归测试把这些边界锁住。
