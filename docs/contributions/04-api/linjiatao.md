# API 设计与实现贡献说明

姓名：林佳涛
学号：2312190316
日期：2026-03-30

## 我完成的工作

### 1. API 设计
- [x] 参与餐厅查询接口前端对接（附近查询、关键词搜索）
- [x] 确认分页查询参数契约（page、size）与前端实现一致
- [ ] 用户认证 API 联调完成（登录流程待后端环境稳定后继续）

### 2. 文档编写
- [x] 补充 API 使用文档中的前端联调说明
- [x] 明确前端 API 访问层文件位置与页面接入位置
- [ ] OpenAPI 主文档维护（由后端同学主导）

### 3. 前端实现
- [x] HTTP 客户端配置（统一 baseURL、请求封装、Token 注入）
- [x] API 调用函数封装（auth、restaurants）
- [x] 餐厅查询页面接入后端接口（nearby / search）
- [x] 实现分页加载（加载更多）
- [x] 实现筛选与搜索联动
- [x] 增加接口失败兜底（回退本地数据）
- [ ] Mock 数据独立方案（MSW / JSON Server）

### 4. 后端实现
- [ ] API 路由定义
- [ ] 业务逻辑处理
- [ ] 错误处理

说明：后端 API 由队友完成，我主要负责微信小程序前端 API 访问层与页面联调。

### 5. 测试
- [x] 微信开发者工具中完成页面联调验证（首页、餐厅页）
- [x] 验证查询参数与分页行为
- [ ] 登录鉴权联调验证（进行中）
- [ ] Postman/Apifox 测试集合（由后端同学主导）
- [ ] 后端单元测试（由后端同学主导）
- [x] 测试用例数量：前端手工联调场景 6 个

## 本次关键改动文件

- frontend/api/client.js
- frontend/api/auth.js
- frontend/api/restaurants.js
- frontend/pages/mine/mine.js
- frontend/pages/mine/mine.wxml
- frontend/pages/restaurants/restaurants.js
- frontend/pages/restaurants/restaurants.wxml
- frontend/pages/restaurants/restaurants.wxss
- frontend/pages/home/home.js
- docs/api.md

## PR 链接

- PR #X：待提交（创建后替换为实际链接）
- 仓库地址：https://github.com/Ryan041001/WhatToEat
- 当前工作分支：feat/architecture-homework-linjiatao-03

## Git 记录证明

- 建议截图命令：`git log --author="hiko-1" --oneline --graph --decorate`
- 当前分支最新提交：`9a7131c feat: architecture homework linjiatao 03`
- 说明：若后续以中文姓名配置 Git author，请同步修改截图命令中的 author 参数。

## 遇到的问题和解决

1. 问题：微信开发者工具提示请求不在合法域名，导致登录和接口调用失败。
   解决：关闭开发环境 URL 校验，并在项目私有配置中设置 urlCheck=false。

2. 问题：接口联调时出现 ERR_CONNECTION_REFUSED。
   解决：排查为后端未启动导致，确认本地 8080 端口监听状态后继续联调。

3. 问题：登录入口在页面中不明显，影响功能验证。
   解决：调整“我的”页未登录态 UI，增加明显的“微信一键登录”按钮。

4. 问题：远端接口偶发失败会影响页面演示。
   解决：增加本地数据兜底策略，保证页面可用性，同时保留远端优先逻辑。

## 心得体会

这次 API 作业让我对前端联调流程有了更完整的认识：不仅要写请求代码，还要处理鉴权、异常兜底、分页、筛选和页面交互一致性。通过把首页和餐厅页接到真实接口，我理解了“接口契约先行”的价值，也意识到文档同步和可复现的联调步骤同样重要。