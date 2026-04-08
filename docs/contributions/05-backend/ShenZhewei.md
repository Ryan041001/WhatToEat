# 后端开发贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-04-08

## 我完成的工作

### 1. API 实现
- [x] 用户认证 API（mock 微信登录 / 登出 / 当前用户）
- [x] 业务资源1 CRUD：餐厅查询（附近查询、关键词搜索）
- [x] 业务资源2 CRUD：随机推荐与候选卡片列表
- [x] 业务资源3 CRUD：用户黑名单（创建、删除、分页查询）
- [x] 业务资源4 CRUD：用户备注（创建、分页、详情、更新、删除）
- [x] 统一错误响应格式（`ApiResponse<T>` + `GlobalExceptionHandler`）
- [x] 统一 CORS 配置，支持浏览器与 Apifox Web 预检请求

### 2. 数据库
- [x] 数据模型定义（ER 图与 `docs/database.md`）
- [x] ORM 配置（Spring Data JPA + Hibernate）
- [x] 数据库迁移脚本（Flyway，`backend/src/main/resources/db/migration/`）
- [x] 用户侧数据表设计：`users`、`user_blacklist`、`user_restaurant_note`、`user_choice_history`
- [x] 唯一约束、外键与索引设计

### 3. 部署
- [x] Dockerfile 编写（`backend/Dockerfile`）
- [x] docker-compose.yml 配置（含 MySQL 服务）
- [x] 本地联调验证（dev / test / docker 三套环境配置）
- [x] `.env.example` 配置模板与敏感信息注入说明

### 4. 可观测性与工程化
- [x] 接入 Spring Boot Actuator 与 Prometheus 指标导出
- [x] 为餐厅查询、推荐、高德调用补充成功/失败维度的业务指标埋点
- [x] 新增 TraceIdFilter，统一写入请求 traceId 并跳过 `/actuator` 探活请求
- [x] 收紧默认与 docker 环境的 Actuator 暴露范围，仅保留 `health` 端点
- [x] 补充 AmapHttpClient、TraceIdFilter、RecommendationController、RestaurantController 等相关测试覆盖

### 5. 测试
- [x] 后端单元测试 / 集成测试（82 个测试用例，`./mvnw test` 全量通过）
- [x] 为认证、餐厅查询、推荐、黑名单、备注接口补充控制器测试与应用服务测试
- [x] 新增 OpenAPI 契约测试，校验路径参数、查询参数和成功响应结构与实现一致
- [x] 新增 CORS 预检测试，覆盖 `OPTIONS /api/v1/restaurants/nearby`
- [x] 新增空 `userId` 路径段回归测试，覆盖 `/api/v1/users//blacklist`
- [x] 新增可观测性指标与链路追踪测试，覆盖 TraceIdFilter、业务指标埋点、AmapHttpClient 指标记录

## PR 链接

- PR #8: https://github.com/Ryan041001/WhatToEat/pull/8（API 作业）
- 目标分支：`develop`
- 当前功能分支：`feat/backend-homework-ShenZhewei-05`

## 关键提交记录

- `6a98f88`：完善后端可观测性埋点与链路追踪
- `de3ddee`：新增餐厅附近查询接口
- `badf0bc`：完善黑名单功能并统一餐厅查询接口
- `1d2fcf2`：清理旧偏好接口残留并同步架构文档
- `91fdddf`：完成备注 CRUD 并补齐备注 API 契约
- `eb70f61`：补全黑名单删除与分页查询接口
- `5dc4e46`：完成推荐接口并同步收紧推荐 API 契约
- `dbee9bc`：完善本地联调配置并修复 API 调试问题
- `6f19a1f`：add auth and restaurant query APIs
- `61e4845`：完成后端架构设计与数据库基础搭建

## 遇到的问题和解决

1. 问题：推荐接口最初只依赖一页高德结果，在过滤黑名单后容易把"还有后续可选餐厅"的场景误判成无结果。
   解决：改成按页继续拉取候选结果，在达到目标数量或上游结果耗尽前持续补足，再执行随机推荐或卡片返回。

2. 问题：黑名单和备注列表都涉及分页，如果只按时间字段排序，在相同时间戳下会出现跨页重复或漏项。
   解决：把分页排序统一收口为稳定排序，增加 `id` 作为二级排序字段，并补充 tied timestamp 回归测试。

3. 问题：使用 Apifox Web 和浏览器调试时，后端在没有 CORS 配置的情况下会被预检请求拦住，表现为 `TypeError: Failed to fetch`，不容易第一时间定位到是跨域问题。
   解决：新增 `/api/**` 的全局 CORS 配置，并补充预检测试，确认跨域头和允许方法能够正确返回；同时把联调文档补充到 `docs/backend.md`、`docs/api.md` 和 `.env.example` 中，降低本地联调门槛。

4. 问题：Docker Compose 模式下，后端连接 MySQL 8 时出现 `Public Key Retrieval is not allowed`，导致容器不断重启，联调请求表面看起来像是接口不可用。
   解决：为 `dev` 和 `docker` 配置补充 `allowPublicKeyRetrieval=true`，并把 Docker profile、Compose 文件和本地启动说明一并收口，保证本机与容器环境都能稳定启动。

5. 问题：在 Apifox 中，如果只创建了 `userId` 环境变量但没有真正绑定到路径参数，最终请求 URL 会变成 `/api/v1/users//blacklist` 或 `/api/v1/users//notes`。早期后端会把这种非法路径错误包装成 `500 系统异常`，误导联调方向。
   解决：将 `NoResourceFoundException` 单独映射为客户端错误，并在 `docs/api.yaml`、`docs/api.md` 中明确区分 OpenAPI 的 `{userId}` 占位符与 Apifox 的 `{{userId}}` 变量替换写法，同时补充 `/api/v1/auth/me -> userId -> 用户资源接口` 的调试链路说明。

6. 问题：业务指标缺少结果维度，无法区分成功与失败请求；Actuator 端点在默认配置下直接暴露 prometheus，存在安全风险；TraceIdFilter 会记录 `/actuator` 探活流量，污染业务日志。
   解决：为 `recommendation.requests` 和 `restaurant.query.requests` 增加 `result` tag（success / errorCode / SYSTEM_ERROR）；收紧默认与 docker 配置的 Actuator 暴露范围，仅保留 `health`；TraceIdFilter 通过 `shouldNotFilter` 跳过 `/actuator/**` 请求；补充相关测试覆盖。

## 心得体会

这次后端开发作业让我更深刻地体会到，后端工程不只是把接口写出来，更重要的是把"实现、测试、文档、运行环境、联调方式、可观测性"一起收口。尤其是在分页、鉴权、路径参数、错误码、本地 Docker 启动和业务指标埋点这些细节上，只要文档和代码稍微偏一点，联调时就会反复返工。

通过这次从架构设计、数据库建模、API 实现、测试覆盖、Docker 部署到可观测性埋点的完整过程，我对"先收紧契约，再补回归测试，最后把运行与调试说明补全"这套后端开发节奏有了更清晰的理解。同时，接入 Actuator 与 Prometheus 指标、补充 TraceIdFilter 链路追踪，也让我意识到可观测性不是"上线后再说"，而是应该在开发阶段就融入日常迭代，这样才能在联调、测试和生产环境中快速定位问题。
