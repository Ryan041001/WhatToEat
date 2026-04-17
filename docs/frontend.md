# 前端说明文档

> 说明：本文档描述的是 **当前仓库里微信小程序前端的真实实现基线**。如果涉及评论、聚合摘要、AI 推荐等联调，请优先再看：
>
> - `docs/api.md`
> - `docs/api.yaml`
> - `docs/frontend-ai-review-integration.md`
>
> 修改前端联调代码前，必须先阅读当前分支/当前提交里相关的 `backend + docs` 改动，不要只根据旧前端页面猜接口。

**负责人**: 林佳涛 (2312190316)

---

## 1. 当前前端定位

当前 `frontend/` 是微信小程序原生工程，不是 Web 端 React 工程。

它目前已经具备：

- 登录页
- 首页
- 餐厅列表页
- 详情页
- 大转盘页面
- 卡片滑选页面
- 个人中心页面
- 基础请求封装与全局餐厅缓存

但需要明确：

- 当前前端仍处于“**部分真实后端接入 + 部分本地 fallback/mock**”混合阶段
- 评论、聚合摘要、AI 推荐增强等能力的联调说明，应以 `docs/frontend-ai-review-integration.md` 为主

---

## 2. 当前功能现状

### 2.1 已经接入或具备页面骨架的能力

1. **登录页**
   - 已调用后端 `POST /api/v1/auth/wechat-login`
   - 失败时会切换到本地 mock 会话，保证开发流程不中断

2. **首页**
   - 展示总餐厅数 / 可选数 / 已拉黑数
   - 支持大转盘入口、摇一摇、卡片滑选入口
   - 展示热门推荐列表

3. **餐厅列表页**
   - 已能从全局缓存读取餐厅列表并展示
   - 当前仍主要使用旧字段展示（如 `rating` / `priceLevel` / `tags`）

4. **详情页**
   - 已能展示餐厅基础信息
   - 当前评论区仍是本地缓存 demo 方案

5. **卡片滑选页**
   - 已支持左右滑动、喜欢/跳过、结果展示

6. **个人中心页**
   - 展示当前用户昵称、统计信息、退出登录入口

### 2.2 当前尚未真正完成的联调项

以下能力后端已经具备，但前端尚未完整落地：

- 当前用户单店评论 CRUD
- 公开评论列表
- 评论聚合摘要（评分 / 评论数 / 人均 / AI 标签 / AI 摘要）
- 列表增强排序
- AI 同步问答
- AI 流式问答

---

## 3. 当前目录结构（真实）

```text
frontend/
├── app.js
├── app.json
├── app.wxss
├── api/
│   ├── auth.js
│   ├── client.js
│   └── restaurants.js
├── assets/
├── components/
│   ├── bottom-nav/
│   ├── loading-spinner/
│   ├── navigation-bar/
│   └── restaurant-card/
├── mock/
│   └── restaurants.js
├── pages/
│   ├── detail/
│   ├── home/
│   ├── index/
│   ├── mine/
│   ├── restaurants/
│   ├── spin/
│   └── swipe/
├── scripts/
├── styles/
├── tests/
└── utils/
```

### 3.1 关键文件职责

- `app.js`
  - 管理全局用户态与餐厅缓存
  - 启动时恢复本地缓存
  - 提供 `bootstrapRestaurants()` / `getRestaurants()` / `toggleBlacklist()` 等全局方法

- `api/client.js`
  - 封装 `wx.request`
  - 自动注入 Bearer Token
  - 对 401 做统一处理

- `api/auth.js`
  - 登录 / 登出 / 当前用户接口封装

- `api/restaurants.js`
  - 附近餐厅 / 搜索接口封装
  - 当前还承担“把后端餐厅对象映射成前端卡片数据”的适配职责

---

## 4. 当前页面说明（按真实代码）

### 4.1 `pages/index/`
登录页。

当前能力：
- 调 `wx.login()` 获取 code
- 调用后端登录接口
- 登录成功后缓存 token / user
- 登录失败时回退到 mock 会话

### 4.2 `pages/home/`
首页。

当前能力：
- 加载全局餐厅缓存
- 统计总数 / 可选数 / 已拉黑数
- 热门推荐（当前基于前端现有 `rating` 排序）
- 跳转到 spin / swipe / restaurants / mine / detail
- 支持摇一摇交互

### 4.3 `pages/restaurants/`
餐厅列表页。

当前能力：
- 从全局缓存读取餐厅
- 本地分类筛选 / 价格筛选 / 排序
- 跳详情页
- 本地切换不感兴趣状态

当前限制：
- 仍主要基于旧字段：`rating`、`priceLevel`、`tags`
- 当前排序只有 `rating` / `distance`
- 还没有完整接上后端增强排序和增强字段

### 4.4 `pages/detail/`
餐厅详情页。

当前能力：
- 展示餐厅图片、地址、简介
- 展示本地标签
- 本地评论输入与展示
- 本地不感兴趣切换

当前限制：
- 评论仍来自 `wx.setStorageSync`
- 还没接当前用户评论接口
- 还没接公开评论列表
- 还没接聚合摘要

### 4.5 `pages/spin/`
大转盘随机抽取页面。

当前能力：
- 支持转盘逻辑与展示
- 使用当前全局可选餐厅数据

### 4.6 `pages/swipe/`
卡片滑选页面。

当前能力：
- 左右滑动
- 喜欢 / 跳过
- 结果结算
- 支持从结果页跳详情

### 4.7 `pages/mine/`
个人中心。

当前能力：
- 展示当前用户昵称
- 展示统计信息
- 提供退出登录
- 提供关于弹窗

---

## 5. 当前真实数据流

### 5.1 登录流

1. `pages/index/index.js` 调 `wx.login()`
2. 调 `api/auth.js -> WechatLogin()`
3. `app.js -> setAuth()` 保存 token / user
4. 后续请求通过 `api/client.js` 自动带 `Authorization`

### 5.2 餐厅列表流

1. 页面调用 `app.bootstrapRestaurants()`
2. `app.js` 调 `api/restaurants.js -> GetNearbyRestaurants()`
3. 后端返回餐厅分页数据
4. 前端做本地适配并写入全局缓存

### 5.3 当前需要特别注意的现实问题

以下几个问题会直接影响联调真实性：

1. `bootstrapRestaurants()` 之前存在 `pageSize` / `size` 的契约差异风险
2. 列表提取逻辑需要兼容后端分页结构 `data.items`
3. 当前前端会在部分场景把 mock 餐厅混入真实列表，可能造成“页面能看见，但后端并没有对应真实上下文”的假象
4. 本地黑名单和服务端黑名单当前并不完全一致

因此做真实联调时，不能只看“页面上有没有数据显示”，而要确认数据是不是来自真实接口、是不是和后端候选一致。

---

## 6. 当前技术说明

### 6.1 小程序技术栈
- WXML
- WXSS
- JavaScript
- 微信小程序 API

### 6.2 当前工程辅助模块
- `loading-spinner`：统一 loading 组件
- `restaurant-card`：餐厅卡片展示组件
- `bottom-nav` / `navigation-bar`：通用导航组件

### 6.3 状态管理方式
当前不是 React/Redux 类方案，而是：

- `app.globalData`
- 页面 `setData()`
- 本地缓存 `wx.getStorageSync / wx.setStorageSync`

---

## 7. 开发与调试

### 7.1 启动方式
- 用微信开发者工具打开 `frontend/`
- 本地联调默认后端地址：`http://127.0.0.1:8080`
- 真机请使用宿主机局域网 IP

### 7.2 调试重点
做前后端联调时，优先检查：

1. 是否真的命中了 backend 接口
2. token 是否存在且有效
3. 当前页面数据是否来自真实后端，而不是本地 fallback
4. 页面所用字段是否还是旧字段兼容层
5. 空态 / 404 / 401 是否按文档契约处理

### 7.3 当前最重要的联调文档
- `docs/api.md`
- `docs/api.yaml`
- `docs/frontend-ai-review-integration.md`

---

## 8. 不要误用的旧认知

下面这些说法在当前仓库里都不够准确：

- “前端只有首页 / 大转盘 / 卡片滑，没有列表和详情”
- “前端还没接后端登录”
- “前端评论已经是服务端评论”
- “列表排序已经是真实后端增强排序”
- “当前前端展示的数据全部来自真实后端”

这些都与当前代码现状不完全一致。

---

## 9. 当前最合适的后续前端工作

如果继续往前推进，最值得优先做的是：

1. 修稳真实餐厅列表接入链路
2. 把详情页评论从本地缓存切到后端评论接口
3. 接入聚合摘要展示
4. 接入后端增强排序
5. 最后再做 AI 问答入口

这也是当前最符合代码现状的推进顺序。

---

**文档维护**: 林佳涛 (2312190316)  
**当前基线整理**: 2026-04-18  
**说明**: 本文档已按当前仓库真实代码结构重写，后续若页面或数据流发生变化，应同步更新。
