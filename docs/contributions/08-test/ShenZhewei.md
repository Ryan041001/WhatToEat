# 测试与质量保障贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-04-22

## 我完成的工作

### 1. 自动化测试补充
- [x] 为 `ai-service` 的 `RecommendationService` 补充回退逻辑测试
- [x] 为 `ai-service` 的流式推荐补充异常回退与隐藏推理内容兜底测试
- [x] 为 `ai-service` 新增内部 API 契约测试，覆盖 `/health`、`/internal/review-tags`、`/internal/recommend`、`/internal/recommend/stream`
- [x] 为 OpenAI-compatible 客户端新增兼容性测试，覆盖 JSON mode fallback、tool call 解析、stream delta 聚合、timeout / upstream error 映射
- [x] 为 backend 新增 `AiHttpClient` 测试，覆盖成功响应映射、SSE 多行 data 解析、流式聚合与 timeout 错误映射
- [x] 为 backend 新增 `UserChoiceHistoryApplicationService`、`UserRecommendationFeedbackApplicationService` 的时间窗口回归测试

### 2. 覆盖率与 CI 收口
- [x] backend 接入 JaCoCo 覆盖率报告生成
- [x] ai-service 配置 `pytest` 测试入口与 `coverage` 报告选项
- [x] 新增 GitHub Actions 工作流 `.github/workflows/test-and-coverage.yml`
- [x] 接入 Codecov，分别上传 backend 与 ai-service 覆盖率报告
- [x] 新增 `scripts/update_coverage_badges.py`，生成仓库内 SVG 覆盖率徽章
- [x] 在 `README.md` 中补充覆盖率徽章与本地刷新说明

### 3. 为可测试性做的代码收口
- [x] 为 `UserChoiceHistoryApplicationService` 注入 `Clock`，让最近吃过窗口可稳定测试
- [x] 为 `UserRecommendationFeedbackApplicationService` 注入 `Clock`，让最近反馈窗口可稳定测试
- [x] 保持 AI 推荐与推荐反馈链路的时间相关行为可复现、可回归

### 4. 测试验证
- [x] `cd ai-service && UV_CACHE_DIR=../.uv-cache uv run --with pytest --with pytest-cov pytest -q`
- [x] 结果：`35 passed in 0.71s`
- [x] `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -q -Dmaven.repo.local=../.m2 -Dtest=AiHttpClientTest,UserChoiceHistoryApplicationServiceTest,UserRecommendationFeedbackApplicationServiceTest test`
- [x] 结果：命令退出码为 `0`，本次测试作业直接相关的 backend 测试通过

## PR 链接

- PR：待提交（创建后补充实际链接）
- 当前功能分支：`feat/test-homework-ShenZhewei-08`

## 涉及的主要文件

### ai-service

- `ai-service/pyproject.toml`
- `ai-service/tests/test_services.py`
- `ai-service/tests/test_api_contracts.py`
- `ai-service/tests/test_openai_compatible.py`

### backend

- `backend/pom.xml`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/UserChoiceHistoryApplicationService.java`
- `backend/src/main/java/com/zjgsu/whattoeat/service/application/UserRecommendationFeedbackApplicationService.java`
- `backend/src/test/java/com/zjgsu/whattoeat/infrastructure/ai/AiHttpClientTest.java`
- `backend/src/test/java/com/zjgsu/whattoeat/service/application/UserChoiceHistoryApplicationServiceTest.java`
- `backend/src/test/java/com/zjgsu/whattoeat/service/application/UserRecommendationFeedbackApplicationServiceTest.java`

### 工程化

- `.github/workflows/test-and-coverage.yml`
- `.github/badges/backend-coverage.svg`
- `.github/badges/ai-coverage.svg`
- `scripts/update_coverage_badges.py`
- `README.md`

## 遇到的问题和解决

1. 问题：推荐链路里有不少回退逻辑，尤其是 LLM 文本生成失败、流式输出失败、返回内容不完整时，如果只测主路径，很容易让 fallback 分支长期失真。
   解决：在 `ai-service/tests/test_services.py` 中补充“文本生成失败回退”“JSON 无效回退”“流式失败回退”“仅隐藏推理内容回退”等测试，确保服务在异常上游下仍能输出稳定答案。

2. 问题：内部 AI 接口虽然主要由 backend 调用，但如果没有独立契约测试，后续改接口时很容易把状态码、SSE 事件形状或 OpenAPI 描述改偏。
   解决：新增 `ai-service/tests/test_api_contracts.py`，直接校验内部接口的 HTTP 状态、错误映射、SSE 事件名称以及 OpenAPI schema 引用。

3. 问题：时间窗口逻辑使用 `LocalDateTime.now()` 时，测试对系统当前时间敏感，最近吃过与最近反馈的查询边界不稳定。
   解决：将 backend 两个应用服务改为注入 `Clock`，再用固定时间构造回归测试，保证 3 天与 7 天窗口都能稳定断言。

4. 问题：只在本地执行测试还不够，缺少统一覆盖率产物和可视化反馈时，后续很难持续跟踪测试质量变化。
   解决：在 backend 接入 JaCoCo，在 ai-service 接入 `pytest-cov`，再通过 GitHub Actions + Codecov 上传报告，并新增本地 SVG 徽章脚本，把覆盖率结果直接展示到仓库首页。

## 心得体会

这次测试作业让我更明确地感受到，测试的价值不只是“让 CI 变绿”，而是把系统里那些最容易在重构中被忽略的边界条件固定下来。尤其是 AI 推荐这种既有结构化工具调用、又有文本生成和流式输出的链路，如果只覆盖 happy path，后面任何一次兼容性调整都可能悄悄破坏真实行为。

另外一个收获是，测试工作和工程化收口其实是同一件事的两面。单元测试、契约测试、覆盖率报告、CI 上传、README 徽章这些内容单看都不大，但串起来之后，项目的“可验证性”就会明显提升。相比只补几个测试文件，这次更重要的是把“测试怎么跑、覆盖率怎么看、出问题时如何定位”一起收口成可重复的团队协作流程。
