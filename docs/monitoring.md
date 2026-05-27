# 监控配置说明

本文档记录 WhatToEat 后端与 AI Service 的基础监控配置，用于课程作业和本地部署验收。

## 1. 结构化日志

### 后端 Spring Boot

后端通过 `backend/src/main/resources/logback-spring.xml` 将控制台日志输出为 JSON 行格式，字段包括：

- `time`
- `level`
- `application`
- `thread`
- `logger`
- `traceId`
- `message`

业务请求会经过 `TraceIdFilter`，自动生成或复用 `X-Trace-Id`，并记录请求路径、方法、状态码和耗时。`/health` 与 `/actuator/**` 会跳过请求追踪日志，避免健康检查轮询污染业务日志。

示例：

```json
{"time":"2026-05-27T10:30:28.790+08:00","level":"INFO","application":"WhatToEat","thread":"main","logger":"c.z.w.common.web.TraceIdFilter","traceId":"trace-fixed-001","message":"request path=/api/v1/restaurants/nearby method=GET status=200 latencyMs=0 traceId=trace-fixed-001"}
```

### AI Service

AI Service 通过 `ai-service/app/utils/logger.py` 配置 Python 标准库 logging，输出 JSON 行格式，字段包括：

- `time`
- `level`
- `message`
- `module`
- `method`
- `path`
- `status_code`
- `response_time_ms`

FastAPI 中间件会为每个请求记录状态码和响应耗时。

## 2. 健康检查端点

### 后端

端点：

```text
GET /health
```

返回示例：

```json
{
  "status": "healthy",
  "timestamp": "2026-05-27T10:30:28.687922+08:00",
  "version": "0.0.1-SNAPSHOT"
}
```

后端仍保留 Spring Boot Actuator 健康检查：

```text
GET /actuator/health
```

### AI Service

端点：

```text
GET /health
```

返回示例：

```json
{
  "status": "healthy",
  "timestamp": "2026-05-27T02:30:28.687922+00:00",
  "version": "0.1.0"
}
```

## 3. 基础指标

### 后端指标

后端使用 Spring Boot Actuator + Micrometer + Prometheus，开发与测试环境暴露：

```text
GET /actuator/metrics
GET /actuator/prometheus
```

可观测的关键指标包括：

- HTTP 请求计数
- HTTP 响应时间
- 5xx 错误请求数量与比例
- JVM、线程、数据库连接池等运行时指标

生产默认只暴露健康检查；如果需要接入 Prometheus，应在受控网络内显式开放 `prometheus` 端点。

### AI Service 指标

AI Service 暴露轻量 JSON 指标：

```text
GET /metrics
```

返回示例：

```json
{
  "requestCount": 12,
  "errorCount": 1,
  "averageResponseTimeMs": 8.342,
  "errorRate": 0.0833
}
```

指标含义：

- `requestCount`：服务启动以来处理的请求总数
- `errorCount`：HTTP 状态码大于等于 500 的请求数
- `averageResponseTimeMs`：平均响应时间
- `errorRate`：错误率

## 4. 本地验证命令

后端测试：

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -Dtest=WhatToEatApplicationTests,TraceIdFilterTest test
```

AI Service 测试：

```bash
cd ai-service
uv run --with pytest pytest -q tests/test_api_contracts.py
```

运行服务后手动验证：

```bash
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/actuator/prometheus
curl http://127.0.0.1:8000/health
curl http://127.0.0.1:8000/metrics
```

## 5. 告警建议

当前作业未接入外部告警服务，推荐部署时按以下规则配置：

- 服务不可用：连续 3 次 `/health` 失败触发告警
- 错误率过高：5 分钟内 5xx 错误率超过 5% 触发告警
- 响应变慢：5 分钟内 P95 响应时间超过 2 秒触发告警
