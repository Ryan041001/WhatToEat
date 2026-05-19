# Docker 部署贡献说明

姓名：林佳涛  学号：2312190316  角色：前端  日期：2026-05-19

## 我完成的工作

### 1. Dockerfile 编写

- [x] 前端 Dockerfile 多阶段构建
- [x] 前端 `.dockerignore` 覆盖 node_modules、环境文件和无关缓存
- [ ] 后端 Dockerfile 多阶段构建（团队已完成，非本人提交）
- [ ] AI Service Dockerfile 多阶段构建（团队已完成，非本人提交）

### 2. Compose 配置

- [x] 开发环境 `compose.yaml` 增加前端服务与健康检查
- [x] `.env.example` 增加 `FRONTEND_PORT` 配置项
- [ ] 生产环境 `compose.prod.yaml` 前端服务（本次未修改）

### 3. 自动化部署

- 选项 A / B：团队已完成，本次未修改。

## PR 链接

- PR 待创建：`feat/docker-homework-linjiatao-11`

## 遇到的问题和解决

1. 问题：微信小程序前端没有传统 Web build 产物，容器化时无法直接走 SPA 发布流程。
   解决：前端 Dockerfile 采用多阶段构建，运行时使用 nginx-unprivileged 静态托管源码并添加健康检查。

2. 问题：开发环境需要一键启动前端服务并可观测健康状态。
   解决：在 `compose.yaml` 中增加前端服务，挂载源码目录并配置 healthcheck。

## AI 使用情况

- 使用了哪些 Prompt：
  - 生成前端多阶段 Dockerfile 与 `.dockerignore` 模板。
  - 对齐作业要求（非 root、健康检查、compose 开发配置）。
- AI 帮助解决了哪些问题：
  - 选择合适的运行时镜像与健康检查方式。
  - 补齐环境变量与 compose 前端服务配置。

## 心得体会

这次补齐前端容器化后，前后端与 AI 服务的启动方式更统一，便于本地联调与作业验收。对微信小程序项目而言，容器化更偏向“开发编排与可观察性”，需要在可运行和作业要求之间找到平衡。