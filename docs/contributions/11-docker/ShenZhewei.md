# Docker 部署贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-05-13

## 我完成的工作

### 1. Dockerfile 编写

- [x] 后端 Dockerfile 多阶段构建
- [x] AI Service Dockerfile 多阶段构建
- [x] 后端与 AI Service 以非 root 用户运行
- [x] 后端与 AI Service 配置容器健康检查
- [x] 后端与 AI Service `.dockerignore` 覆盖构建产物、环境文件和无关缓存
- [ ] 前端 Dockerfile（本次个人职责不包含前端）

### 2. Compose 配置

- [x] 开发环境 `compose.yaml`
- [x] 兼容入口 `docker-compose.yml`
- [x] 生产环境 `compose.prod.yaml`
- [x] MySQL 数据卷持久化
- [x] MySQL、AI Service、后端健康检查
- [x] 后端依赖 MySQL 与 AI Service healthy 后启动
- [x] 生产环境通过 `.env` 注入密钥，不在配置文件中硬编码生产密码
- [x] 生产部署脚本等待服务 healthy，异常时返回失败

### 3. 自动化部署

选择了选项 A + B：

- 选项 A：新增 `.github/workflows/docker.yml`，自动构建后端和 AI Service 镜像并推送到 GHCR，推送后执行 Trivy 高危漏洞扫描。
- 选项 B：新增 `deploy.sh`，可用 `compose.prod.yaml` 一键构建并启动生产编排。

## PR 链接

- PR 待创建：`feat/docker-homework-ShenZhewei-11`

## 遇到的问题和解决

1. 问题：`eclipse-temurin:17-jre-alpine` 在当前 Docker 平台没有可用 manifest。
   解决：后端运行时镜像改为 `amazoncorretto:17-alpine`，保留 Java 17 与 Alpine 运行时约束。

2. 问题：AI Service 生产运行时移除 `uv` 后，原 Compose 健康检查仍调用 `uv run`。
   解决：健康检查改为直接使用运行时 Python 标准库访问 `/health`。

3. 问题：本人的作业职责是后端和 AI 开发，不应提交前端容器化实现。
   解决：Docker 编排只覆盖 `backend`、`ai-service` 和 MySQL，前端 Dockerfile 不纳入本次个人提交。

4. 问题：Spring Boot 4 下仅引入 `flyway-core` 和 `flyway-mysql` 不会触发 Boot Flyway 自动配置，生产空库启动时 Hibernate validate 会先报缺表。
   解决：后端改用 `spring-boot-starter-flyway` 加 `flyway-mysql`，确认空库首次启动会执行全部迁移并创建 `recommendation_feedback`。

5. 问题：部署脚本如果只打印 `docker compose ps`，服务 unhealthy 或 crash-loop 时仍可能返回成功。
   解决：`deploy.sh` 使用 `docker compose up --wait --wait-timeout`，服务未 healthy 会返回非零退出码。

## AI 使用情况

- 使用了哪些 Prompt：
  - 根据 `hw.pdf` 拆解 Docker 作业要求。
  - 生成并修正后端、AI Service 的多阶段 Dockerfile。
  - 生成开发/生产 Compose、部署脚本和 GitHub Actions 镜像流水线。
- AI 帮助解决了哪些问题：
  - 识别镜像 manifest 不可用并切换到可用 Java 17 Alpine 运行时。
  - 对齐非 root、健康检查、持久化、密钥注入和镜像扫描要求。
  - 整理阶段性提交与个人贡献说明。

## 心得体会

本次 Docker 部署工作让我把后端、AI Service 和数据库的运行依赖显式化。开发环境重点是可一键启动和可观察的健康状态，生产环境重点是密钥不入库、资源限制、持久化和镜像安全扫描。相比只写 Dockerfile，Compose 的健康检查和依赖条件更能反映服务是否真正可用。
