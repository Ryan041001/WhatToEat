# AI 功能集成贡献说明

姓名：林佳涛
学号：2312190316
日期：2026-04-19

## 我完成的工作

### 1. AI 功能
- 功能类型：智能客服 / 推荐问答（流式）
- 使用模型：由后端 `ai-service` 通过 OpenAI-compatible 接入（模型由环境变量配置）

### 2. 实现内容
- [x] 后端 API（已由团队已有实现提供：`/api/v1/recommendations/ask/stream`）
- [x] 前端调用（新增 `frontend/pages/ai-chat/*` + `frontend/api/recommendation-chat.js`）
- [x] 错误处理（流中断、错误事件、重试）
- [x] 推荐闭环接入（`choice-history`、`recommendation-feedback`、`preference-profile`）

## PR 链接
- PR #X: https://github.com/xxx/xxx/pull/X

## 心得体会
本次实践重点是把 AI 能力从“后端可用”落到“前端可感知”。我学习到了流式事件解析、结构化卡片渲染、以及错误态保留部分结果的处理方式。后续会继续完善多轮 refine 和会话持久化能力。
