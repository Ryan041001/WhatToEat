# 安全审查（AI 辅助）
日期：2026-05-06
范围：前端小程序页面组件、后端配置文件（backend/src/main/resources）
审查人：林佳涛

## 审查范围与文件
- 前端页面与组件（pages/index，components/navigation-bar）
- 后端运行配置（application-*.yml / application.properties）
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

## OWASP 重点检查说明
- 注入（SQL/命令）：本次未逐条审查后端 DAO/Service 代码（待补充）。
- 失效访问控制：本次未逐条审查认证/授权逻辑（待补充）。
- 敏感信息泄露：已修复配置明文凭据和第三方 Key。
- 密码存储：本次未核查用户密码哈希逻辑（待补充）。
- 错误信息暴露：本次未逐条审查异常处理（待补充）。

## CI 自动化安全扫描
- 已添加 gitleaks 密钥泄露扫描工作流。
- 已添加 CodeQL 静态分析工作流（加分项）。

## 备注
- 若需要，我可以继续对后端 controller/service/repository 进行逐项审查并补充报告。
