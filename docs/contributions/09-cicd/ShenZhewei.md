# CI/CD 配置贡献说明

姓名：沈哲伟
学号：2312190313
角色：后端
日期：2026-04-29

## 我完成的工作

### 1. 后端 CI 工作流
- [x] 新增 `.github/workflows/ci.yml`
- [x] 在 `push`、`pull_request`、`workflow_dispatch` 时自动运行
- [x] 使用 Java 17 和 Maven 缓存
- [x] 执行 backend 测试并生成 JaCoCo 覆盖率报告
- [x] 上传 `backend/target/site/jacoco/jacoco.xml` 到 Codecov，flag 为 `backend`
- [x] 上传后端测试覆盖率产物与 JAR 包产物

### 2. 后端质量检查
- [x] 增加 `Backend Lint And Compile Check` 任务
- [x] 执行 `./mvnw -B -DskipTests validate`
- [x] 执行 `./mvnw -B -DskipTests compile`
- [x] 当前项目未引入 Checkstyle / Spotless，先用 Maven validation + compile 作为无新增依赖的后端 lint/静态质量门禁

### 3. 后端打包与 Docker 构建校验
- [x] 增加 `Backend Package` 任务，执行 `./mvnw -B -DskipTests package`
- [x] 增加 `Backend Docker Build Check` 任务
- [x] 在 CI 中校验 `docker compose config`
- [x] 使用 `docker/build-push-action` 构建 `backend/Dockerfile`，确保后端镜像可构建

### 4. 后端 CD 工作流
- [x] 新增 `.github/workflows/backend-deploy.yml`
- [x] `main` 分支、`v*` tag、手动触发时构建并推送后端镜像到 GHCR
- [x] 手动触发时可勾选 `deploy_to_server`，通过 SSH 拉取镜像并重启 `whattoeat-backend` 容器
- [x] 部署环境变量从 GitHub Secrets 注入，不在仓库中硬编码数据库、高德或服务器密钥

### 5. Codecov 配置与文档
- [x] 在 `codecov.yml` 中补齐 `frontend` flag，保持现有前端覆盖率上传配置可被 Codecov 正确识别
- [x] 新增 `.github/workflows/ai-service-coverage.yml`，保留原有 AI service 覆盖率上传能力
- [x] 恢复 `.github/workflows/frontend-coverage.yml`，不影响前端同学已有测试流水线
- [x] 更新 `README.md` 的 CI/CD、覆盖率和 Secrets 说明

## 需要配置的 GitHub Secrets

后端远程部署需要：

- `GHCR_USER`
- `GHCR_TOKEN`
- `BACKEND_DEPLOY_HOST`
- `BACKEND_DEPLOY_USER`
- `BACKEND_DEPLOY_KEY`
- `BACKEND_DEPLOY_PORT`
- `BACKEND_PORT`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `AMAP_KEY`
- `AI_SERVICE_BASE_URL`

私有仓库上传 Codecov 时还需要：

- `CODECOV_TOKEN`

## 涉及的主要文件

- `.github/workflows/ci.yml`
- `.github/workflows/backend-deploy.yml`
- `.github/workflows/ai-service-coverage.yml`
- `.github/workflows/frontend-coverage.yml`
- `codecov.yml`
- `README.md`
- `docs/contributions/09-cicd/ShenZhewei.md`

## 验证方式

本地验证命令：

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test jacoco:report
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -DskipTests package

cd ..
docker compose config
docker build -t whattoeat-backend:ci backend
```

GitHub 端验证：

- 提交 PR 后查看 `Backend CI`
- 合并到 `main` 后查看 `Backend CD` 是否成功推送 GHCR 镜像
- 如需部署，手动运行 `Backend CD` 并勾选 `deploy_to_server`

## 心得体会

这次 CI/CD 配置的重点是把后端从“本地能跑”推进到“提交后自动验证”。CI 负责测试、覆盖率、编译、打包和 Docker 构建，CD 负责镜像发布与可选服务器部署。后续如果团队决定引入 Checkstyle 或 Spotless，可以直接把对应 Maven 插件挂到当前 `Backend Lint And Compile Check` 任务中，不需要重新设计流水线。
