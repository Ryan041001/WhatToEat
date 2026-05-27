# 监控配置说明

## 目标
为后端提供结构化日志、健康检查与基础指标，以便验证可观测性配置。

## 结构化日志
- 日志格式：JSON
- 配置文件：backend/src/main/resources/logback-spring.xml
- 关键字段：timestamp、level、message、logger（由 LogstashEncoder 输出）

## 健康检查
- 端点：`GET /health`
- 返回示例：
```json
{
  "status": "healthy",
  "timestamp": "2026-05-27T10:00:00Z",
  "version": "1.0.0"
}
```

## 基础指标
- Actuator 基础路径：`/actuator`
- 关键指标：
  - 请求计数/响应时间：`GET /actuator/metrics/http.server.requests`
  - 错误率：在 `http.server.requests` 中按 `status` 标签统计 5xx 占比
- Prometheus（可选）：`GET /actuator/prometheus`

## 运行提示
- 本地/测试环境：使用 `application-dev.yml` / `application-test.yml`
- Docker 环境：使用 `application-docker.yml`
