# CI/CD 配置贡献说明

姓名：林佳涛  学号：2312190316  角色：前端  日期：2026-04-29

## 完成的工作

### 工作流相关
- [x] 参与编写 / 审查 `.github/workflows/frontend-ci.yml`
- [x] 配置 Codecov 覆盖率上传（frontend flag）
- [x] 与后端 CI/CD 分支对齐，避免覆盖后端 `.github/workflows/ci.yml`

### 代码适配
- [x] 保留本地 `npm test` 交互习惯，新增 `npm run test:ci` 供 CI 使用
- [x] 代码通过 Lint 检查（ESLint）
- [x] 核心覆盖率达标（> 60%，沿用上次作业覆盖范围）

### 可选项
- [ ] 配置 Dependabot 自动更新依赖
- [ ] 集成 CodeRabbit AI 代码审查
- [ ] 使用 act 本地验证工作流

## PR 链接
- PR #21: https://github.com/Ryan041001/WhatToEat/pull/21

## CI 运行链接
- 合并前以 PR Checks 为准；工作流入口为 `.github/workflows/frontend-ci.yml`

## 遇到的问题和解决
1. 问题：Jest 在 CI 中可能进入 watch 模式导致流程阻塞。解决：保留本地 `test` 脚本，新增 `test:ci` 为 `jest --ci --coverage`。
2. 问题：前端未提供严格 Lint 脚本。解决：补充 `lint` 脚本，对 `app.js`、`api`、`components`、`pages`、`utils`、`tests` 执行 ESLint，并在 CI 中运行。
3. 问题：前端 PR 原先新增 `.github/workflows/ci.yml`，会和后端 CI/CD PR 的后端流水线冲突。解决：改为使用独立 `frontend-ci.yml`，保持前后端工作流职责分离。
4. 问题：需要可上传的覆盖率报告。解决：CI 使用 `frontend/coverage/lcov.info` 上传 Codecov，并打上 `frontend` flag。

## 心得体会
这次配置把“本地可跑”升级到了“提交即验证”，减少了协作时因环境差异导致的问题。将 lint、test、coverage 三者固定在同一条工作流后，前端质量门禁更清晰，也更适合后续团队并行开发。
