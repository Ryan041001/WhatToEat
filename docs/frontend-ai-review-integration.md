# 前端对接文档：评论、聚合摘要与 AI 推荐增强

日期：2026-04-18

本文档面向微信小程序前端开发，基于当前仓库里的 **后端接口变更** 与 **前端实际代码进度**，补充评论、评分、人均价格、排序增强、AI 推荐问答的对接要求，并重点补齐联调阶段的鲁棒性约束。

> 前置要求：在修改任何前端联调代码前，先把当前分支/当前提交里与本次能力相关的 **backend + docs 改动完整看一遍**，先理解真实接口契约、错误语义、空态语义、排序语义和 AI 返回边界，再开始写代码；不要只看旧前端代码猜接口。

---

## 0. 当前小程序实际进度基线（按仓库代码）

下面这部分不是规划，而是当前代码现状，方便前后端联调时对齐。

### 0.1 已有能力

1. **登录页已接后端登录接口**
   - 文件：`frontend/pages/index/index.js`
   - 已调用：`POST /api/v1/auth/wechat-login`
   - 但当前在登录失败时会直接切到本地 mock 会话，属于“开发不中断”策略。

2. **全局已有统一请求封装和餐厅缓存**
   - 文件：`frontend/api/client.js`
   - 文件：`frontend/app.js`
   - 已有 token 注入、基础错误提示、全局 `restaurants` 缓存。

3. **首页 / 列表页 / 卡片滑选 / 详情页已经能跑通基础浏览流程**
   - `frontend/pages/home/*`
   - `frontend/pages/restaurants/*`
   - `frontend/pages/swipe/*`
   - `frontend/pages/detail/*`

### 0.2 当前仍是旧数据模型的页面

以下页面当前仍主要依赖旧字段或本地缓存：

另外还有一个很容易被忽视、但会直接破坏联调真实性的问题：

- `frontend/app.js` 里的 `mergeWithMockRestaurants()` 会在真实后端返回数量不足 12 条时，**把 mock 餐厅混入真实列表**
- 这会导致列表、首页、卡片页、详情页里同时存在“真实 POI + 本地假数据”
- 一旦详情页、评论页、AI 推荐页继续沿用这份混合列表，就会出现：
  - 页面能点开，但后端没有这个 `poiId` 的真实上下文
  - 前端看得到的餐厅和后端推荐候选不一致
  - 黑名单 / 评论 / AI 推荐结果出现“前端看起来对，实际服务端不一致”的错觉

所以在真正开始评论 / AI / 排序联调前，建议把“成功请求后仍混入 mock 数据”也纳入 P0 必修项。

- `frontend/pages/restaurants/restaurants.js`
  - 仍使用旧字段：`rating`、`priceLevel`、`tags`
  - 当前排序只有：`rating`、`distance`
  - 当前筛选仍是本地分类/价位筛选，不是后端增强排序

- `frontend/pages/detail/detail.js`
  - 评论仍保存在 `wx.setStorageSync`
  - 只有纯文本评论输入，没有评分、人均、聚合摘要
  - 未接当前用户评论接口 / 公开评论接口 / 聚合摘要接口

- `frontend/app.js`
  - `bootstrapRestaurants()` 已尝试调用 `/restaurants/nearby`
  - **但当前有两个关键兼容问题：**
    1. 传参使用的是 `pageSize`，不是后端当前文档约定的 `size`
    2. `extractRestaurantList()` 没处理当前分页结构 `data.items`
  - 这意味着：即使后端成功返回分页数据，前端也可能把结果识别为空，然后落回 mock 数据

### 0.3 当前尚未接入的后端能力

以下能力后端已经在本次 diff 中补齐，但前端尚未真正落地：

1. 当前用户单店评论 CRUD
2. 餐厅公开评论列表
3. 餐厅评论聚合摘要（评分 / 人均 / AI 标签 / AI 摘要）
4. 列表增强字段：`avgRating`、`reviewCount`、`avgPerCapitaPrice`、`aiTags`
5. 列表增强排序：`avgRating`、`reviewCount`、`avgPriceAsc`、`avgPriceDesc`、`smart`
6. AI 同步问答：`POST /api/v1/recommendations/ask`
7. AI 流式问答：`POST /api/v1/recommendations/ask/stream`
8. 最近选择历史：`POST/GET /api/v1/users/{userId}/choice-history`
9. 推荐反馈闭环：`POST/GET /api/v1/users/{userId}/recommendation-feedback`
10. 轻量口味画像：`GET /api/v1/users/{userId}/preference-profile`

### 0.3.1 与新增后端能力相关的前端风险（本轮不改前端，只记录风险）

1. **如果前端不在“确认吃这家”时写入 `choice-history`，后端的“最近吃过，先别推”不会真正生效。**
2. **如果前端不把“太贵了 / 太远了 / 今天不想吃这个 / 已经吃过了”接到 `recommendation-feedback`，推荐闭环仍然只是服务端空转能力。**
3. **如果前端继续只传 `question`，不传 `context.previousQuestion / rejectedPoiIds / userSignals`，就无法真正发挥 refine / 连续追问能力。**
4. **`review-summary` 现在新增了 `recommendedScenarios`，但前端若忽略该字段，详情页仍然只会停留在“有摘要”而不是“解释这家店适合什么场景”。**

### 0.4 联调前必须先修的底座问题

如果不先处理下面两点，后面的评论 / AI / 聚合增强会一直处于“看起来接了，实际没真正吃到后端数据”的状态：

1. **餐厅列表提取逻辑必须支持分页结构 `data.items`**
2. **附近餐厅查询参数统一改为 `size`，不要再传 `pageSize`**

建议先把“餐厅列表真正吃到后端数据”作为 P0，再继续做详情页评论与 AI 问答接入。

---

## 1. 总体对接原则

1. 前端只调用 `backend/`，不直接调用 AI 服务。
2. 餐厅主信息仍来自餐厅查询接口，评论、摘要、AI 推荐都只是增强层。
3. 评论不再保存在本地缓存中，统一改为调用后端接口。
4. 前端必须把“接口空态”和“接口失败”区分开，不能把所有 404 / 空数组都当异常。
5. 流式场景中，前端只消费后端定义的结构化事件，不直接消费模型私有协议。
6. 页面渲染应优先使用 `poiId` 作为稳定主键，而不是临时生成的本地 id。

---

## 2. 建议接入顺序（按风险从低到高）

### P0：先修基础列表链路

目标：保证首页 / 列表页 / 卡片页真的在消费后端餐厅数据。

至少完成：

1. `extractRestaurantList()` 支持 `data.items`
2. `bootstrapRestaurants()` 统一传 `size`
3. 餐厅映射函数兼容新字段：`avgRating`、`reviewCount`、`avgPerCapitaPrice`、`aiTags`
4. 页面内部尽量基于 `poiId` 跳转详情

### P1：详情页评论与聚合摘要

目标：把详情页从“本地评论 demo”升级成“真实后端评论页”。

至少完成：

1. 当前用户评论回显
2. 公开评论列表展示
3. 评论表单支持评分 + 人均 + 文本
4. 聚合摘要展示
5. 评论成功后刷新相关数据

### P2：列表页增强字段与排序

目标：列表页真正展示评分、人均、评论数和 AI 标签，并接入后端排序。

### P3：AI 同步 / 流式问答

目标：提供真正可用的推荐问答，而不是只在前端拼文案。

### P4：把“最近吃过 / 用户反馈 / 口味画像”真正接上

目标：把后端已经具备的 AI native 闭环能力，真正变成用户可感知体验。

至少完成：

1. 用户“确认吃这家”后调用 `POST /api/v1/users/{userId}/choice-history`
2. 在 AI 推荐页提供快捷反馈并调用 `POST /api/v1/users/{userId}/recommendation-feedback`
3. 在进入 AI 推荐页前，按需拉取 `GET /api/v1/users/{userId}/preference-profile`
4. 把画像结果作为页面上的“当前偏好提示”，而不是只静默留在后端

---

## 3. 前端统一数据适配层要求

当前前端多个页面共享 `app.globalData.restaurants`，所以建议先定义一层统一适配，不要让每个页面直接读后端原始返回。

### 3.1 建议的前端标准餐厅模型

建议前端内部统一为：

```js
{
  id,                 // 建议直接等于 poiId
  poiId,
  name,
  address,
  category,
  distance,           // 文案，例如 260m / 1.2km
  distanceValue,      // 数值，便于排序
  avgRating,          // number | null
  reviewCount,        // number
  avgPerCapitaPrice,  // number | null
  aiTags,             // string[]
  rating,             // 兼容旧页面展示，可由 avgRating 回填
  priceLevel,         // 兼容旧页面展示，可由 avgPerCapitaPrice 推导
  tags,               // 兼容旧页面展示，可由 aiTags 回填
  image,
  description,
  isBlacklisted,
  isUserAdded
}
```

### 3.2 字段兼容映射建议

| 后端字段 | 前端标准字段 | 兼容策略 |
|---|---|---|
| `poiId` | `id` + `poiId` | `id` 直接等于 `poiId`，避免本地 id 与后端主键分离 |
| `avgRating` | `avgRating` + `rating` | `rating = avgRating ?? null`，旧页面可临时继续读 `rating` |
| `avgPerCapitaPrice` | `avgPerCapitaPrice` | 同时可推导 `priceLevel` 仅用于旧 UI |
| `aiTags` | `aiTags` + `tags` | `tags` 可临时回填 `aiTags.slice(0, 3)` |
| `distance` | `distanceValue` + `distance` | 保留数值和文案两份，避免反复 parse |
| `reviewCount` | `reviewCount` | 默认值必须是 `0` |

### 3.3 兼容回填规则

建议按下面顺序取值：

- 评分展示：`avgRating ?? rating ?? null`
- 标签展示：`aiTags.length > 0 ? aiTags : tags`
- 人均展示：`avgPerCapitaPrice ?? null`
- 价格等级展示（旧 UI 兼容）：
  - `<= 20` -> `¥`
  - `21 ~ 40` -> `¥¥`
  - `> 40` -> `¥¥¥`
  - 空值 -> `--`

### 3.4 当前必须补上的兼容逻辑

`frontend/app.js` 里的列表提取逻辑建议至少兼容以下几种返回：

```js
payload.data.items
payload.data.records
payload.data.restaurants
payload.data
payload
```

其中当前后端正式契约优先以 `payload.data.items` 为准。

---

## 4. 请求层鲁棒性要求

当前 `frontend/api/client.js` 能满足简单 CRUD，但不足以支撑本次所有联调场景，需要补充“可控失败”能力。

### 4.1 不要把所有非 2xx 都做成全局错误 Toast

至少要区分三类：

1. **预期空态**：页面自己消化
2. **鉴权失效**：统一处理
3. **真正错误**：提示 + 重试

### 4.2 这些状态不能按普通错误处理

#### A. 当前用户未评论

- 接口：`GET /api/v1/users/{userId}/restaurant-reviews/{poiId}`
- 返回：`404 / 2104`
- 处理：视为正常空态，展示空表单

#### B. 当前餐厅没有公开评论

- 接口：`GET /api/v1/restaurants/{poiId}/reviews`
- 返回：`200` + `items: []`
- 处理：展示“暂无评论”

#### C. 周边餐厅无结果

- 接口：`GET /api/v1/restaurants/nearby`
- 返回：`404 / 3003`
- 处理：展示空态页，不要直接认为网络错误

### 4.3 建议给请求层增加的能力

建议 `client.request()` 支持以下可选配置：

```js
{
  silent: true,                 // 不自动 toast
  allowHttpStatus: [404],       // 允许页面自己处理
  returnFullResponse: true,     // 返回 statusCode + body
  skipAuthRedirect: true        // 某些场景先不跳登录页
}
```

### 4.4 401 的建议处理

当前 `401` 会清 token 并 `redirectTo('/pages/index/index')`，这对简单页面可以，但对并发加载场景有两个风险：

1. 多个请求同时 401，可能重复跳转
2. 页面初始化中被中断，局部状态可能已渲染一半

建议：

- 统一清 token
- 只做一次跳转
- 页面侧保留一次“本页请求因登录失效被中断”的状态，避免用户误以为是空数据

---

## 5. AI refine / 反馈闭环 / 画像接入清单

这一节是**新增能力的前端最小接入清单**。本轮不改前端代码，但后续联调建议直接照这份清单落。

### 5.1 AI refine（换一家，但保持条件）

同步 ask 与流式 ask 都统一按下面方式组织请求：

```json
{
  "userId": 1,
  "question": "换一家，还是想吃热汤，但别太贵",
  "longitude": 120.35,
  "latitude": 30.31,
  "radius": 3000,
  "size": 3,
  "context": {
    "previousQuestion": "预算 35 以内，想吃清淡一点",
    "rejectedPoiIds": ["B0FFOLD001"],
    "selectedPoiIds": [],
    "userSignals": ["健身"]
  }
}
```

前端建议：

1. 维护一个最近一轮推荐状态对象：
   - `lastQuestion`
   - `lastRecommendedPoiIds`
   - `lastRejectedPoiIds`
   - `userSignals`
2. 用户点快捷 refine 时，不要重造系统能力，只是：
   - 把上一轮 `lastQuestion` 放进 `context.previousQuestion`
   - 把刚刚明确拒绝的餐厅放进 `context.rejectedPoiIds`
   - 把本轮快捷语句作为新的 `question`
3. 用户说“在健身”“最近减脂”“想吃高蛋白”时，前端可把这些词直接塞进 `userSignals`

### 5.2 推荐反馈闭环

推荐页建议至少给出这些快捷反馈按钮：

- 太贵了
- 太远了
- 今天不想吃这个
- 看起来不太卫生
- 已经吃过了

对应请求：

```json
{
  "poiId": "B0FF123456",
  "poiNameSnapshot": "轻食工坊",
  "feedbackType": "TOO_EXPENSIVE",
  "detail": "最近在健身，想吃更轻一点的人均 30 内",
  "requestQuestion": "在健身，想吃高蛋白"
}
```

鲁棒性要求：

1. 有 `poiId` 时尽量总是带上 `poiId`
2. “已经吃过了”优先用 `ALREADY_ATE`
3. 反馈成功后，不要只 toast，应该立即：
   - 更新前端本地 `lastRejectedPoiIds`
   - 触发下一轮 refine 或提示“已降低同类推荐概率”

### 5.3 口味画像接入

接口：

- `GET /api/v1/users/{userId}/preference-profile`

建议前端不要把它做成重页面，先做两个轻量入口：

1. AI 推荐页顶部提示条
   - 示例：`你最近偏向清淡 / 高蛋白，也更在意预算`
2. 个人页里的“口味画像”入口

前端使用建议：

- `summary` 用来做一句话提示
- `preferredTags / avoidedTags` 用于标签 UI
- `recentFeedbackSignals` 用于辅助解释“为什么这次推荐会变”

### 5.4 详情页“适合什么场景”

接口：

- `GET /api/v1/restaurants/{poiId}/review-summary`

新增字段：

- `recommendedScenarios: string[]`

前端建议展示方式：

- 放在 AI 摘要下面，作为二级信息块
- 若为空则直接隐藏，不要占位
- 最多展示前 3~4 个标签型场景

推荐文案样式：

- 适合工作日午餐
- 适合一个人快吃
- 适合预算 30 左右
- 适合想吃热汤时

### 5.5 与当前前端实现相关的高风险误区

1. **不要把快捷反馈只做成前端本地状态。**
   - 如果不调 `recommendation-feedback`，后端不会记住这轮偏好变化。
2. **不要只在聊天文本里体现“换一家”，却不传 `context.previousQuestion`。**
   - 否则模型无法稳定继承上一轮条件。
3. **不要在“确认选中餐厅”后只跳详情，不写 `choice-history`。**
   - 否则“最近吃过，先别推”永远不会变成真实能力。
4. **不要忽略 `recommendedScenarios`。**
   - 否则详情页失去最自然的 AI native 解释层。
- 页面侧收到 401 后尽快停止后续依赖请求

### 4.5 流式请求不要复用当前通用 client

`ask/stream` 需要：

- `enableChunked: true`
- `responseType: 'arraybuffer'`
- `onChunkReceived()`

这类请求建议做独立模块，例如：

- `frontend/api/recommendation-chat.js`

不要强塞进现有 CRUD 风格的 `client.get/post`。

---

## 5. 详情页改造

当前页面参考：

- `frontend/pages/detail/detail.js`
- `frontend/pages/detail/detail.wxml`

### 5.1 当前现状

当前详情页仍是本地 demo 方案：

- 评论从 `wx.setStorageSync` 读取
- 评论只支持纯文本
- 没有评分、人均、聚合摘要
- 没有“当前用户评论”和“公开评论”区分

### 5.2 目标能力

详情页应改成四块数据：

1. 餐厅基础详情
2. 当前用户评论
3. 公开评论列表
4. 当前餐厅聚合摘要

### 5.3 推荐加载顺序

建议：

1. 先根据 `id / poiId` 拿到当前餐厅
2. 餐厅拿到后，并行请求：
   - `GET /api/v1/restaurants/{poiId}/review-summary`
   - `GET /api/v1/restaurants/{poiId}/reviews?page=1&size=10`
   - 登录态下再请求 `GET /api/v1/users/{userId}/restaurant-reviews/{poiId}`
3. 三块数据分别落状态，不要相互阻塞

### 5.4 当前用户评论回显

接口：

- `GET /api/v1/users/{userId}/restaurant-reviews/{poiId}`

回填字段：

- `ratingScore`
- `perCapitaPrice`
- `content`

按钮文案可切换为：

- 无评论：`发布评论`
- 已有评论：`更新评论`

接口空态约定：

- `404 / 2104` = 正常未评论
- 不弹系统错误
- 只清空表单并标记 `hasReviewed = false`

### 5.5 评论表单改造

现有表单只有文本输入，需要改为三部分：

1. 星级评分组件
2. 人均价格输入框
3. 评论内容输入框

提交前校验：

- `ratingScore` 必填
- `ratingScore` 范围 `0.5 ~ 5.0`
- `ratingScore` 步进 `0.5`
- `perCapitaPrice` 必填
- `perCapitaPrice` 为正整数
- `content` 必填
- `content.trim().length <= 1000`

### 5.6 星级组件要求

- 支持半星
- 建议前端内部直接使用数字值：`0.5 / 1.0 / 1.5 / ... / 5.0`
- 编辑态展示可点击星级
- 评论列表展示只读星级

### 5.7 人均价格输入要求

- 字段名：`perCapitaPrice`
- 单位：元
- 数据类型：整数
- 使用数字键盘
- 不允许负数、小数、空值
- 建议占位文案：`例如 28`

### 5.8 评论提交接口

`PUT /api/v1/users/{userId}/restaurant-reviews/{poiId}`

请求体：

```json
{
  "poiNameSnapshot": "沙县小吃",
  "ratingScore": 4.5,
  "perCapitaPrice": 28,
  "content": "出餐快，适合中午随便吃一顿。"
}
```

前端要求：

- `Authorization: Bearer <token>`
- `poiNameSnapshot` 建议传当前详情页餐厅名，便于后端快照补齐
- 首次创建与更新当前都返回 `200 OK`
- 成功后至少刷新：
  1. 当前用户评论
  2. 公开评论列表
  3. 当前餐厅聚合摘要

### 5.9 删除当前用户评论

`DELETE /api/v1/users/{userId}/restaurant-reviews/{poiId}`

建议：

- 二次确认后调用
- 删除成功后清空编辑表单
- 同时刷新评论列表与聚合摘要

### 5.10 公开评论列表

接口：

- `GET /api/v1/restaurants/{poiId}/reviews`

评论项建议展示：

- 用户昵称
- 星级评分
- 人均价格
- 评论内容
- 更新时间

空态约定：

- 没有评论时返回 `200 OK`
- `data.items = []`
- 展示“暂无评论”

### 5.11 聚合摘要接口

`GET /api/v1/restaurants/{poiId}/review-summary`

详情页建议展示：

- 平均评分
- 评论数
- 平均人均
- AI 标签
- AI 总结

空态稳定返回：

```json
{
  "reviewCount": 0,
  "avgRating": null,
  "avgPerCapitaPrice": null,
  "aiTags": [],
  "aiSummary": null
}
```

另外要注意一个很重要的对外契约：

- 后端内部存在 AI 摘要生成状态，但 **当前接口不会把 `aiStatus` 暴露给前端**
- 因此前端应直接把 `aiTags=[]`、`aiSummary=null` 视为“当前没有可展示的 AI 结果”
- 这既包括“暂无评论/暂无快照”，也包括“AI 生成失败或尚未 ready”
- 前端不要自行推断失败原因，也不要假设数据库里一定存在旧摘要可兜底

前端要求：

- `aiTags` 为空时直接隐藏标签区
- `aiSummary` 为空时显示“暂无 AI 总结”或直接隐藏
- 不能因为 `aiSummary` 为空而整块摘要不渲染
- 当前前端无需额外处理 `aiStatus`，因为接口对外已经把非 ready 状态收敛成空 AI 字段
- 当 `reviewCount > 0` 但 `aiSummary == null` 时，前端不要把它解释成“没有评论”，而应解释成“当前没有可展示的 AI 摘要结果”
- 更稳妥的文案建议：
  - `reviewCount === 0`：`暂无评论`
  - `reviewCount > 0 && !aiSummary`：`AI 摘要暂不可用`

---

## 6. 列表页改造

当前页面参考：

- `frontend/pages/restaurants/restaurants.js`
- `frontend/pages/restaurants/restaurants.wxml`

### 6.1 当前现状

当前列表页：

- 使用旧字段 `rating / priceLevel / tags`
- 当前排序只有 `rating / distance`
- 筛选主要依赖本地 mock / 全局缓存数据
- 黑名单切换仍是本地状态切换

### 6.2 新增展示字段

`/api/v1/restaurants/nearby` 和 `/api/v1/restaurants/search` 现在都支持增强字段，列表页和搜索结果页应统一按同一套逻辑处理。

建议新增展示：

- 平均评分 `avgRating`
- 评论数 `reviewCount`
- 平均人均 `avgPerCapitaPrice`
- AI 标签 `aiTags`

空值展示建议：

- `avgRating == null`：显示 `暂无评分`
- `reviewCount === 0`：显示 `暂无评论`
- `avgPerCapitaPrice == null`：显示 `人均待补充`
- `aiTags.length === 0`：不展示标签区域

### 6.3 排序入口改造

列表页排序枚举建议改为后端受控值：

- `distance`
- `avgRating`
- `reviewCount`
- `avgPriceAsc`
- `avgPriceDesc`
- `smart`

前端传参示例：

```text
/api/v1/restaurants/nearby?longitude=120.35&latitude=30.31&radius=3000&page=1&size=20&sort=avgRating
```

约束：

- `sort` 大小写敏感
- 不传时默认 `distance`
- 不要拼派生值，例如 `avg_rating`、`price_desc`
- 传未支持值时，后端返回 `400 / 1001`

### 6.4 分页停止条件

为兼容当前增强排序实现，分页建议同时满足以下两种停止条件之一就停止继续翻页：

1. 已达到 `total`
2. 当前页 `items.length === 0`

### 6.5 当前页面改造建议

建议不要继续在页面里直接 parse 文案距离或推导旧字段，而是：

1. 统一在适配层生成 `distanceValue`
2. 统一在适配层兼容 `rating / priceLevel / tags`
3. 页面只做展示，不做接口字段兼容判断

这样首页 / 列表页 / 卡片页 / 详情页可以共用同一个餐厅模型。

---

## 7. 与现有黑名单 / 登录态的兼容提醒

虽然本文件重点是评论和 AI 增强，但当前前端还有两个会影响联调稳定性的现实问题。

### 7.1 黑名单当前仍是本地态

当前：

- `app.toggleBlacklist()` 只改本地缓存
- 没有真正调用后端黑名单接口

这会导致：

- 列表页和详情页切换时看起来一致
- 但和后端真实用户黑名单并不一致
- AI 推荐 / 随机推荐如果走后端 `userId` 过滤，结果可能和前端本地看到的不一致

建议：

- 如果本轮联调只做评论与 AI，可先在文档里明确“当前前端黑名单仍为本地态，后端推荐过滤以真实服务端黑名单为准”
- 如果要追求真实一致性，应尽快把 `toggleBlacklist()` 切到后端接口

### 7.2 登录失败时的 mock fallback 只适合开发期

当前 `frontend/pages/index/index.js` 在登录失败时会自动切 mock 会话。

这适合本地开发，但联调时要注意：

- mock 用户通常没有真实后端 userId
- 评论 / 黑名单 / AI 个性化过滤依赖真实 userId
- 所以进入评论或个性化 AI 流程前，应优先保证登录成功并拿到真实用户信息

建议：

- 开发模式允许 mock fallback
- 联调 / 验收模式下，对需要真实用户身份的功能关闭 mock fallback 或显式提示“当前为体验模式，评论能力不可用”

---

## 8. AI 推荐问答接口

### 8.1 同步接口

`POST /api/v1/recommendations/ask`

请求体建议：

```json
{
  "userId": 1,
  "question": "想吃点清淡的，预算 30 以内，不要太远",
  "longitude": 120.35,
  "latitude": 30.31,
  "radius": 3000,
  "size": 3
}
```

请求约束：

- `userId` 可选；不传时不做黑名单过滤
- 若传 `userId`，必须为正整数；不存在用户返回 `404 / 1002`
- `size` 当前最大 `10`，前端建议限制在 `1 ~ 3` 或 `1 ~ 5`
- 同步 `ask` 与流式 `ask/stream` 入参保持一致

返回体示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "如果你想吃清淡、预算控制在 30 元以内，港记云吞面会更合适。",
    "recommendations": [
      {
        "poiId": "B0FF1",
        "name": "港记云吞面",
        "distance": 260,
        "avgRating": 4.7,
        "avgPerCapitaPrice": 28,
        "aiTags": ["清淡", "带汤"],
        "matchReason": "预算内、离得近、评论里普遍提到口味清淡。"
      }
    ]
  }
}
```

### 8.2 前端呈现建议

- 提供一个简短输入框或问答入口
- 用户提交后显示：
  - AI 推荐文案
  - 对应推荐餐厅卡片
- 卡片以 `recommendations` 为准，不要从回答文本里提取店名
- 若页面本身已有餐厅列表，可优先按 `poiId` 做本地命中并复用已有卡片样式

### 8.3 流式接口

`POST /api/v1/recommendations/ask/stream`

当前建议协议为 `text/event-stream`，但小程序端不要依赖浏览器 `EventSource`，而是用 `wx.request + enableChunked` 手动解析。

```js
const requestTask = wx.request({
  url: `${BASE_URL}/api/v1/recommendations/ask/stream`,
  method: 'POST',
  enableChunked: true,
  responseType: 'arraybuffer',
  header: {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    'Authorization': `Bearer ${token}`
  },
  data: payload
});
```

### 8.4 解析要求

前端必须自己维护两个缓冲区：

- `rawBuffer`：原始字符串缓冲，处理 chunk 截断
- `eventQueue`：已解析但尚未渲染的事件

解析规则：

1. 单个 chunk 可能不是完整事件，不能收到一块就直接 `JSON.parse`
2. 以 `\n\n` 作为事件帧结束
3. 一帧内部解析 `event:` 与 `data:`
4. JSON 不完整时必须继续留在 `rawBuffer`
5. 收到未知事件类型时应忽略而不是报错，保证向前兼容

### 8.5 当前前端可见事件

- `session.created`
- `retrieval.started`
- `retrieval.completed`
- `recommendation.card`
- `answer.delta`
- `answer.done`
- `done`
- `error`

### 8.6 推荐卡片渲染原则

推荐卡片字段建议：

- `poiId`
- `name`
- `address`
- `category`
- `distance`
- `avgRating`
- `reviewCount`
- `avgPerCapitaPrice`
- `aiTags`
- `matchReason`

前端渲染原则：

- 卡片必须直接用流式事件里的结构化字段渲染
- 不要从 `answer` 文本里正则匹配餐厅名称
- 同一个 `poiId` 如果重复收到，按更新处理，不重复插卡
- 如果只收到了文本、没有卡片，则展示纯文本回答并保留“查看附近餐厅”兜底按钮

### 8.7 失败回退策略

前端必须支持以下回退：

- 流式接口失败：回退到同步 `POST /api/v1/recommendations/ask`
- 流式中途收到 `error` 事件：
  - 读取 `code`、`message`
  - 保留已输出的文案和卡片
  - 标记当前消息为“已中断”
  - 提供“重新生成”按钮
- 流式中途断开但已有部分内容：
  - 保留已输出内容
  - 标记为“已中断”
- 只收到了卡片没收完文本：
  - 卡片保留
  - 文案区提示“推荐理由生成中断”
- 只收到了文本没收到卡片：
  - 显示文本
  - 提供“查看候选餐厅列表”按钮
- 同步接口返回的 `recommendations.length < size`：
  - 按实际返回数渲染
  - 不要前端自行补卡

### 8.8 对话状态管理建议

每条 AI 消息建议维护：

```js
{
  messageId,
  requestId,
  status,        // streaming / final / interrupted / error
  answerText,
  cards,
  startedAt,
  completedAt
}
```

这样后续做“重新生成”“复制回答”“继续追问”会更稳。

---

## 9. 必做清单（按当前仓库现实落地）

### 9.1 P0 必做

1. `frontend/app.js` 兼容 `data.items`
2. `frontend/app.js` 请求参数改为 `size`
3. `mapApiRestaurantToCard()` 兼容增强字段
4. 全局餐厅对象统一使用 `poiId` 作为稳定主键
5. 成功拿到真实后端列表后，不再用 `mergeWithMockRestaurants()` 把 mock 数据混入真实列表

### 9.2 P1 必做

1. 详情页删除本地评论存储读写逻辑
2. 新增半星评分组件
3. 新增人均价格输入
4. 评论提交改为 `PUT /users/{userId}/restaurant-reviews/{poiId}`
5. 评论列表改为 `/restaurants/{poiId}/reviews`
6. 详情页新增 `/restaurants/{poiId}/review-summary`

### 9.3 P2 必做

1. 列表页支持后端排序枚举
2. 列表页展示 `avgRating / reviewCount / avgPerCapitaPrice / aiTags`
3. 分页与空态按后端真实语义处理

### 9.4 P3 必做

1. AI 同步问答接入
2. AI 流式问答接入
3. 流式失败自动回退同步接口
4. 结构化卡片渲染，不解析自然语言猜店名

---

## 10. 一句话结论

当前小程序已经具备登录、基础列表、详情、卡片滑选等页面骨架，但 **真实后端联调仍停留在“餐厅列表半接通、评论和 AI 尚未接入”的阶段**。本轮最重要的不是继续堆页面，而是先把：

1. **分页列表真正吃到后端 `data.items`**
2. **详情页从本地评论切到真实评论接口**
3. **统一前端餐厅适配层**

这三件事做稳。底座稳了，后面的评分、人均、AI 标签、流式问答才不会反复返工。
