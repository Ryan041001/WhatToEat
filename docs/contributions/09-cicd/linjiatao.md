# CI/CD 配置贡献说明

姓名：林佳涛  学号：2312190316  角色：前端  日期：2026-04-29

## 完成的工作

### 工作流相关
- [x] 参与编写 / 审查 `.github/workflows/ci.yml`
- [x] 配置 Codecov 覆盖率上传（frontend flag）
- [x] 添加 README 状态徽章

### 代码适配
- [x] 本地测试命令与 CI 一致，无需额外配置
- [x] 代码通过 Lint 检查（ESLint）
- [x] 核心覆盖率达标（> 60%，沿用上次作业覆盖范围）

### 可选项
- [ ] 配置 Dependabot 自动更新依赖
- [ ] 集成 CodeRabbit AI 代码审查
- [ ] 使用 act 本地验证工作流

## PR 链接
- PR #X: https://github.com/xxx/xxx/pull/X

## CI 运行链接
- https://github.com/xxx/xxx/actions/runs/XXX

## 遇到的问题和解决
1. 问题：Jest 在 CI 中可能进入 watch 模式导致流程阻塞。解决：将 `frontend/package.json` 中 `test` 脚本改为 `jest --ci --coverage`。
2. 问题：前端未提供严格 Lint 脚本。解决：补充 `lint` 脚本为 `eslint . --ext .js --max-warnings 0`，并在 CI 中执行。
3. 问题：需要可上传的覆盖率报告。解决：CI 使用 `frontend/coverage/lcov.info` 上传 Codecov，并打上 `frontend` flag。

## 心得体会
这次配置把“本地可跑”升级到了“提交即验证”，减少了协作时因环境差异导致的问题。将 lint、test、coverage 三者固定在同一条工作流后，前端质量门禁更清晰，也更适合后续团队并行开发。
