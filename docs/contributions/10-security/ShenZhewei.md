# 安全审查贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-05-06

## 我完成的工作

### AI 安全审查

- 审查了认证、评论、推荐和内部 AI 调用相关代码。
- 发现并修复了评论正文、AI 推荐问题和上下文中 HTML / script 原样进入业务链路的问题。
- 发现并收紧了 AI 推荐 `question` 与 `context` 的输入长度边界。

### 安全检查清单

- 认证与授权：使用 Bearer session；受保护用户接口校验 token 用户与路径 `userId` 一致。
- Session：token 有过期时间，logout 后失效。
- SQL 注入：使用 Spring Data JPA / repository 查询，无字符串拼接 SQL。
- XSS：登录昵称、评论正文、AI 推荐输入按纯文本净化。
- 敏感信息：未硬编码 API Key；`.env.example` 提供占位模板。
- 依赖安全：Dependabot 已配置依赖更新提醒。
- 安全 HTTP 头：后端统一注入基础安全响应头。
- CSRF：无 Bearer 的状态变更请求要求 `X-CSRF-Token`。

### CI 安全扫描

- 当前仓库已配置 Dependabot 分生态扫描依赖更新。
- 本轮本地验证重点为后端安全回归测试和全量 Maven 测试。

## 遇到的问题和解决

1. 问题：评论内容可以原样保存 `<script>` 和 HTML 标签。
   解决：在 `RestaurantReviewApplicationService` 入库前调用纯文本净化，并新增回归测试。

2. 问题：AI 推荐 `question` 只有非空校验，且 HTML/script 会原样传给内部 AI。
   解决：补充长度限制，进入推荐服务前净化 `question` 和 `context`。

3. 问题：登录昵称仍允许安全 HTML。
   解决：改为纯文本保存，避免未来前端误用 rich-text 时形成存储型 XSS。

## 心得体会

Vibe Coding 可以快速补齐安全措施，但安全审查不能只看“加了 sanitizer / filter”这种表面动作。更重要的是把输入最终会保存到哪里、展示在哪里、传给哪个上游服务想清楚，并用回归测试把这些边界锁住。
