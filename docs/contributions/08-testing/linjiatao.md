# 软件测试贡献说明

姓名：林佳涛  学号：2312190316  角色：前端  日期：2026-04-22

## 完成的测试工作

### 测试文件
- frontend/__tests__/component-interaction.test.js
- frontend/__tests__/api-client.mock.test.js
- frontend/__tests__/spin-logic.test.js
- frontend/pages/spin/spin-layout.test.js（已有测试继续保留）

### 测试清单
- [x] 正常情况测试（17 个）
- [x] 边界 / 异常情况测试（8 个）
- [x] Mock 使用（API 请求与小程序 wx 环境）

### 覆盖率
- 运行命令：`npm run test:coverage`
- 总体行覆盖率：79.33%
- 核心模块覆盖率：
   - api/client.js：94.87%
   - components/bottom-nav/bottom-nav.js：100%
   - components/loading-spinner/loading-spinner.js：100%
   - components/navigation-bar/navigation-bar.js：100%
   - components/restaurant-card/restaurant-card.js：100%
   - pages/spin/spin-logic.js：60.34%

### AI 辅助
- 使用工具：GitHub Copilot（GPT-5.3-Codex）
- Prompt 示例：
  - “请基于微信小程序前端代码，补齐组件交互测试与 Mock API 测试，满足组件测试>=8、Mock API>=4（含失败场景）并可直接运行覆盖率命令。”
  - “请优先覆盖关键路径：导航跳转、卡片点击事件、摇一摇行为、401 鉴权失效、网络失败提示。”
- AI 生成 + 人工修改的测试数量：25 个

## PR 链接
- PR #X: 待提交

## 遇到的问题和解决
1. 问题：仓库前端缺少统一测试框架配置（无 Jest 配置）
   解决：新增 `frontend/package.json`、`frontend/jest.config.cjs`、`frontend/babel.config.cjs`，统一入口为 `npm test` 与 `npm run test:coverage`。
2. 问题：小程序 API（wx.request、wx.navigateTo）无法在 Node 环境直接运行
   解决：在测试中对 `wx` 对象做 mock，按用户视角验证行为与状态变化。
3. 问题：ESM 模块与 Node 测试环境兼容性
   解决：使用 `babel-jest` 转译前端模块，保证测试可在组员机器直接执行。

## 心得体会
本次测试实践重点不是“只追求覆盖率数字”，而是优先覆盖关键交互与失败路径。AI 工具可以显著提升测试样例产出速度，但边界条件和断言质量仍需要人工审查与修订。后续会在功能迭代时同步更新测试，保持测试与业务代码一致。
