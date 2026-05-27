# 监控配置贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-05-27

## 我完成的工作

### 1. 日志配置

- [x] 后端 Spring Boot 控制台日志改为 JSON 行格式
- [x] 后端请求日志保留 `traceId`、请求路径、方法、状态码和耗时
- [x] AI Service 使用 Python logging 输出 JSON 行格式
- [x] AI Service 请求日志记录方法、路径、状态码和响应耗时
- [x] 健康检查端点跳过后端请求追踪日志，减少探活噪声

### 2. 健康检查

- [x] 后端新增 `GET /health`
- [x] AI Service `/health` 返回作业要求的 `healthy`、`timestamp`、`version`
- [x] 保留后端 Actuator `/actuator/health`
- [x] Docker 健康检查可继续使用根 `/health`

### 3. 指标收集

- [x] 后端通过 Actuator / Micrometer / Prometheus 暴露请求和运行时指标
- [x] AI Service 新增请求计数
- [x] AI Service 新增平均响应时间
- [x] AI Service 新增错误计数和错误率
- [x] AI Service 新增 `GET /metrics` 查询轻量指标

### 4. 文档与测试

- [x] 新增 `docs/monitoring.md`
- [x] 新增后端 `/health` 回归测试
- [x] 新增 AI Service `/health` 与 `/metrics` 契约测试
- [x] 更新 `TraceIdFilterTest` 覆盖根健康检查跳过逻辑

## PR 链接

- PR 待创建：`feat/monitoring-homework-ShenZhewei-13`

## 遇到的问题和解决

1. 问题：后端已有 `/actuator/health`，但作业和 Docker 健康检查要求根路径 `/health`。
   解决：新增独立 `HealthController`，返回 `status`、`timestamp`、`version`，同时保留 Actuator 端点。

2. 问题：AI Service 原 `/health` 只返回 `{"status":"ok"}`，不满足作业格式。
   解决：扩展响应 schema，返回 `healthy` 状态、UTC 时间戳和服务版本。

3. 问题：AI Service 没有轻量指标查询入口。
   解决：新增请求指标中间件和线程安全的内存计数器，通过 `/metrics` 输出请求数、错误数、平均响应时间和错误率。

4. 问题：健康检查轮询会产生大量后端请求日志。
   解决：`TraceIdFilter` 跳过 `/health` 与 `/actuator/**`，只记录真实业务请求。

## 心得体会

这次监控配置让我把“服务能启动”和“服务可观察”区分开来：健康检查负责告诉部署平台服务是否可用，结构化日志负责快速定位单次请求，指标负责观察整体趋势。后端复用 Actuator/Micrometer 能减少重复造轮子，AI Service 则用轻量中间件补齐了课程作业需要的请求计数、响应时间和错误率。
