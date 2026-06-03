# 性能优化贡献说明

姓名：林佳涛  学号：2312190316  日期：2026-06-03

## 我完成的工作

基于"性能优化与移动端体验"课程内容，对 WhatToEat 项目进行了全栈性能优化：

### 前端优化（5 项）
- [x] 请求超时 + 指数退避重试（`client.js`）
- [x] 缓存 TTL 机制，减少重复 API 调用（`app.js`）
- [x] 图片懒加载，7 个页面/组件的 `<image>` 添加 `lazy-load`
- [x] 同步存储 → 异步存储，避免主线程阻塞（`app.js`）
- [x] 搜索防抖 300ms，减少无效请求（`restaurants.js`）

### 后端优化（3 项）
- [x] Caffeine 本地缓存餐厅聚合快照，5min TTL + 评论写入驱逐
- [x] HTTP 缓存头 `Cache-Control: max-age=300` 覆盖餐厅列表接口
- [x] 数据库全表索引覆盖分析，确认无需新增索引

## PR 链接
- 分支: `feat/optimize-linjiatao-14`
- PR: https://github.com/Ryan041001/WhatToEat/pull/new/feat/optimize-linjiatao-14

## 遇到的问题和解决
1. 问题：全局 `SecurityHeadersFilter` 设置了 `Cache-Control: no-store`，阻止客户端缓存。
   解决：新建 `RestaurantCacheControlFilter`，以更低优先级（HIGHEST_PRECEDENCE + 20）运行，覆盖餐厅接口的缓存头。

2. 问题：多个页面在 `onShow` 时强制刷新位置和餐厅数据（`force: true`）。
   解决：在 `app.js` 中集中添加 TTL 检查（餐厅 3min，位置 5min），各页面无需改动即受益。

## 心得体会
性能优化不应凭直觉猜测，而是数据驱动的工程过程。本次优化遵循"先测量→找瓶颈→改一处→验证→记录"的原则，每项优化独立提交，便于回溯和评估效果。前端缓存 TTL 和后端 Caffeine 缓存的组合，可以在不改动 API 契约的前提下显著降低重复请求对后端和数据库的压力。
