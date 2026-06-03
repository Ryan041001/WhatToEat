# 前后端对接缺口清单

日期：2026-05-27

本文档基于当前仓库代码重新全局扫描，记录“后端已经实现，但前端尚未完整对接”的能力。它不是 API 契约源；接口字段、错误码和返回结构仍以 `docs/api.md` 与 `docs/api.yaml` 为准。

## 1. 扫描依据

### 后端扫描范围

- `backend/src/main/java/com/zjgsu/whattoeat/controller/*`
- `docs/api.md`
- `docs/api.yaml`
- `docs/frontend-ai-review-integration.md`

### 前端扫描范围

- `frontend/api/*.js`
- `frontend/app.js`
- `frontend/pages/**/*`
- `frontend/components/**/*`
- `frontend/app.json`

### 当前前端页面

`frontend/app.json` 当前注册页面：

- `pages/index/index`
- `pages/detail/detail`
- `pages/home/home`
- `pages/ai-chat/ai-chat`
- `pages/mine/mine`
- `pages/restaurants/restaurants`
- `pages/spin/spin`
- `pages/swipe/swipe`

---

## 2. 总览结论

| 能力 | 后端状态 | 前端状态 | 对接结论 |
|---|---|---|---|
| 微信登录 / 登出 | 已实现 | 登录、登出已接 | 基本已接 |
| 当前用户 `/auth/me` | 已实现 | 有封装，页面未直接调用 | 部分缺口 |
| 附近餐厅 `/restaurants/nearby` | 已实现 | 首页缓存、列表页已接 | 已接 |
| 关键词搜索 `/restaurants/search` | 已实现 | 有封装，无页面调用 | 未落地 |
| 推荐随机 `/recommendations/random` | 已实现 | 无封装，前端本地随机 | 未落地 |
| 推荐卡片 `/recommendations/cards` | 已实现 | 无封装，滑卡用本地缓存 | 未落地 |
| AI 同步问答 `/recommendations/ask` | 已实现 | 未接，文档建议不用作正式入口 | 可不接 |
| AI 流式问答 `/recommendations/ask/stream` | 已实现 | AI 聊天页已接 | 已接 |
| 黑名单新增 / 删除 / 列表 | 已实现 | 已接 | 已接 |
| 黑名单详情 / 修改 reason | 已实现 | 无封装或 UI | 未落地 |
| 用户备注 notes CRUD | 已实现 | 无 API 封装，无页面 | 未落地 |
| 当前用户单店评论 CRUD | 已实现 | 详情页已接 | 已接 |
| 公开评论列表 | 已实现 | 详情页已接 | 已接 |
| 评论聚合摘要 / AI 标签 / 场景 | 已实现 | 详情页已接 | 已接 |
| 最近吃过 choice-history 写入 | 已实现 | AI 页“就吃这家”已写，其他入口未写 | 部分缺口 |
| 最近吃过 choice-history 查询 | 已实现 | 有封装，无页面 | 未落地 |
| 推荐反馈写入 | 已实现 | AI 页仅接“不想吃这个” | 部分缺口 |
| 推荐反馈查询 | 已实现 | 有封装，无页面 | 未落地 |
| 口味画像 preference-profile | 已实现 | AI 页已读取并展示摘要 | 已接 |

---

## 3. 已经对接较完整的能力

### 3.1 登录与登出

后端：

- `POST /api/v1/auth/wechat-login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

前端现状：

- `frontend/api/auth.js` 封装了 `WechatLogin`、`Logout`、`GetMe`
- `frontend/pages/index/index.js` 调用 `WechatLogin`
- `frontend/pages/mine/mine.js` 调用 `Logout`

缺口：

- `GetMe` 目前只有封装，未看到页面实际调用。当前用户主要依赖登录返回结果和本地缓存。

建议：

- 如果需要启动时校验 token 是否仍有效，可在 app 启动或进入受保护页时调用 `GetMe`。
- 如果保持当前轻量缓存方案，也可以暂不接 `GetMe`，但要接受 token 过期只在请求 401 后被动发现。

### 3.2 附近餐厅、列表增强字段与排序

后端：

- `GET /api/v1/restaurants/nearby`
- 支持 `sort`、`category`、`minAvgPerCapitaPrice`、`maxAvgPerCapitaPrice`
- 返回增强字段：`avgRating`、`reviewCount`、`avgPerCapitaPrice`、`aiTags`

前端现状：

- `frontend/api/restaurants.js` 封装 `GetNearbyRestaurants`
- `frontend/app.js` 的 `bootstrapRestaurants()` 调用附近餐厅接口
- `frontend/pages/restaurants/restaurants.js` 调用附近餐厅接口，并传排序、分类、价格筛选参数
- `mapApiRestaurantToCard()` 已适配增强字段

结论：

- 这部分已经是前端主链路之一。

### 3.3 评论、公开评论与聚合摘要

后端：

- `GET /api/v1/users/{userId}/restaurant-reviews/{poiId}`
- `PUT /api/v1/users/{userId}/restaurant-reviews/{poiId}`
- `DELETE /api/v1/users/{userId}/restaurant-reviews/{poiId}`
- `GET /api/v1/restaurants/{poiId}/reviews`
- `GET /api/v1/restaurants/{poiId}/review-summary`

前端现状：

- `frontend/api/reviews.js` 已封装上述接口
- `frontend/pages/detail/detail.js` 已加载摘要、公开评论、当前用户评论
- 详情页已支持提交、更新、删除自己的评论
- `recommendedScenarios` 已在 `frontend/pages/detail/detail.wxml` 展示

结论：

- 详情页评论主链路已经对接完整。

### 3.4 AI 流式推荐与口味画像

后端：

- `POST /api/v1/recommendations/ask/stream`
- `GET /api/v1/users/{userId}/preference-profile`

前端现状：

- `frontend/api/recommendation-chat.js` 单独处理流式 `wx.request(enableChunked)`
- `frontend/pages/ai-chat/ai-chat.js` 调用 `startRecommendationStream`
- AI 页进入时调用 `GetPreferenceProfile`
- AI 页会把画像摘要展示成“你的近期偏好”

结论：

- AI 流式入口和口味画像摘要已经接上。

---

## 4. 后端已实现但前端未落地

### 4.1 用户备注 notes CRUD

后端接口：

- `POST /api/v1/users/{userId}/notes`
- `GET /api/v1/users/{userId}/notes`
- `GET /api/v1/users/{userId}/notes/{noteId}`
- `PUT /api/v1/users/{userId}/notes/{noteId}`
- `DELETE /api/v1/users/{userId}/notes/{noteId}`

后端位置：

- `backend/src/main/java/com/zjgsu/whattoeat/controller/UserNoteController.java`

前端现状：

- 没有 `frontend/api/notes.js`
- 页面代码中未发现 notes/备注相关接口调用
- 当前详情页已有评论能力，但没有“私人备注”入口

建议接入方式：

1. 新增 `frontend/api/notes.js`
2. 在详情页增加“我的备注”模块，和公开评论区分开
3. 支持创建 / 修改 / 删除当前餐厅备注
4. 在“我的”页可选增加备注列表入口，调用分页查询与 `keyword` 筛选

注意：

- notes 是用户私有数据，必须带 Bearer Token。
- notes 与 restaurant-reviews 语义不同：备注可以是私人备忘，不参与公开评论与聚合评分。

### 4.2 餐厅关键词搜索

后端接口：

- `GET /api/v1/restaurants/search`

后端能力：

- 支持 `keyword`
- 支持 `longitude`、`latitude`、`radius`
- 支持 `sort`、`category`、价格筛选
- 返回字段与 nearby 对齐

前端现状：

- `frontend/api/restaurants.js` 已封装 `SearchRestaurants`
- 没有页面实际调用 `SearchRestaurants`
- 当前餐厅页只调用 `GetNearbyRestaurants`

建议接入方式：

1. 在餐厅列表页顶部增加搜索输入
2. keyword 为空时走 `/restaurants/nearby`
3. keyword 非空时走 `/restaurants/search`
4. 搜索结果继续复用 `mapApiRestaurantToCard()`、黑名单状态合并、筛选摘要与排序 UI

注意：

- 搜索仍应传当前定位，用于距离和候选排序。
- 不要在前端只对已有 nearby 缓存做本地搜索，否则搜索范围会被当前页候选池限制。

### 4.3 后端随机推荐 `/recommendations/random`

后端接口：

- `GET /api/v1/recommendations/random`

后端能力：

- 可传 `userId`
- 会应用黑名单硬过滤
- 会对近 3 天吃过、近 7 天负反馈做软过滤
- 软过滤无候选时自动回退到仅黑名单过滤

前端现状：

- 没有 API 封装
- 首页摇一摇从 `app.getActiveRestaurants()` 本地随机
- 转盘页从 `app.getActiveRestaurants()` 本地抽样，本地随机 winner

建议接入方式：

1. 新增 `frontend/api/recommendations.js`
2. 封装 `GetRandomRecommendation(params)`
3. 首页摇一摇优先调用后端 random
4. 转盘可二选一：
   - 仍保留前端动画，但最终结果由后端 random 决定
   - 或先拉 cards 候选池，再在前端转盘中展示候选

注意：

- 如果继续本地随机，后端的“近 3 天吃过 / 负反馈软过滤”不会对摇一摇、转盘生效。
- 如果要让转盘视觉上展示多个候选，单独 random 不够，需要结合 `/recommendations/cards`。

### 4.4 后端卡片候选 `/recommendations/cards`

后端接口：

- `GET /api/v1/recommendations/cards`

后端能力：

- 可传 `userId`
- 会应用黑名单、最近吃过、近期负反馈过滤
- 返回一组候选 POI

前端现状：

- 没有 API 封装
- `frontend/pages/swipe/swipe.js` 使用 `app.getActiveRestaurants()` 作为滑卡数据
- 该数据来自 nearby 缓存，只做前端黑名单状态过滤

建议接入方式：

1. 在 `frontend/api/recommendations.js` 封装 `GetRecommendationCards(params)`
2. 滑卡页 `loadData()` 改为优先调用 `/recommendations/cards`
3. 返回项复用 `mapApiRestaurantToCard()` 转成前端卡片模型
4. 传 `userId`，让后端过滤真正生效

注意：

- 滑卡左滑“不喜欢”如果只影响本地当前轮，不会进入后端反馈闭环。
- 可以在左滑时调用 `recommendation-feedback`，反馈类型用 `DONT_WANT_THIS_TODAY` 或更细粒度原因。

### 4.5 黑名单详情与 reason 修改

后端接口：

- `GET /api/v1/users/{userId}/blacklist/{poiId}`
- `PUT /api/v1/users/{userId}/blacklist/{poiId}`

前端现状：

- `frontend/api/blacklist.js` 只封装了列表、新增、删除
- 列表页和 app 全局能力只传空 `reason`
- 没有编辑黑名单原因的 UI

建议接入方式：

1. 按需新增 `GetBlacklistItem` 与 `UpdateBlacklist`
2. 在“我的”页增加“不感兴趣餐厅”管理入口
3. 支持查看、编辑 reason，或恢复展示

注意：

- 如果项目不需要展示拉黑原因，这一块可以延后。
- 但 `preference-profile` 会利用 `user_blacklist.reason`，所以 reason 越真实，画像越有价值。

---

## 5. 已部分接入但体验不完整

### 5.1 最近吃过 choice-history

后端接口：

- `POST /api/v1/users/{userId}/choice-history`
- `GET /api/v1/users/{userId}/choice-history`

后端行为：

- 记录用户选择过的 `poiId`
- 推荐时优先避开近 3 天内吃过的餐厅
- 软过滤把候选排空时自动回退

前端现状：

- `frontend/api/user-signals.js` 已封装 `CreateChoiceHistory`、`ListChoiceHistory`
- 只有 AI 聊天页“就吃这家”调用了 `CreateChoiceHistory`
- 首页摇一摇结果、转盘结果、滑卡喜欢/选中、详情页普通浏览都没有写入
- 没有最近吃过历史页

建议接入方式：

1. 只在“明确决定吃这家”的动作写入，不要在普通进入详情时写入
2. 首页摇一摇结果页增加“就吃这家”按钮并写入
3. 转盘结果页增加“就吃这家”按钮并写入
4. 滑卡结算页对最终选择写入，而不是每次右滑都写入
5. “我的”页可增加最近吃过列表，调用 `ListChoiceHistory`

注意：

- 普通查看详情不等于吃过，不能自动写入。
- 如果不写入 choice-history，后端无法判断“近 3 天吃过”。

### 5.2 推荐反馈 recommendation-feedback

后端接口：

- `POST /api/v1/users/{userId}/recommendation-feedback`
- `GET /api/v1/users/{userId}/recommendation-feedback`

后端支持反馈类型：

- `TOO_EXPENSIVE`
- `TOO_FAR`
- `DONT_WANT_THIS_TODAY`
- `LOOKS_UNHYGIENIC`
- `ALREADY_ATE`

后端行为：

- `ALREADY_ATE` 会同步写入 choice-history
- 其他带 `poiId` 的近期反馈会参与短期软过滤
- 反馈信号会进入口味画像和 AI 推荐上下文

前端现状：

- `frontend/api/user-signals.js` 已封装写入与查询
- AI 聊天页卡片“不想吃这个”只写 `DONT_WANT_THIS_TODAY`
- 没有“太贵 / 太远 / 不卫生 / 已吃过”的快捷反馈 UI
- 没有反馈历史页
- 滑卡左滑当前只影响本地当前轮，不写后端反馈

建议接入方式：

1. AI 卡片增加反馈菜单：太贵、太远、不想吃、看起来不卫生、已经吃过
2. 滑卡左滑可默认写 `DONT_WANT_THIS_TODAY`，或弹出原因选择
3. 如果用户点“已经吃过”，调用 `CreateRecommendationFeedback`，不要再重复手写 choice-history，因为后端会同步
4. “我的”页可增加推荐反馈历史入口

注意：

- 反馈最好携带 `poiId` 与 `poiNameSnapshot`，否则无法参与候选过滤。
- 不要把所有负反馈都当黑名单；黑名单是长期硬过滤，feedback 是短期软信号。

### 5.3 AI 推荐上下文

后端接口：

- `POST /api/v1/recommendations/ask`
- `POST /api/v1/recommendations/ask/stream`

后端支持上下文：

- `previousQuestion`
- `rejectedPoiIds`
- `selectedPoiIds`
- `userSignals`

前端现状：

- AI 页会传 `previousQuestion`
- AI 页会传本轮 `lastRejectedPoiIds`
- AI 页会把 `preferenceSummary` 放进 `userSignals`
- `selectedPoiIds` 当前固定为空数组

建议接入方式：

1. 当用户点“就吃这家”后，把该 `poiId` 加入 `selectedPoiIds` 或写入本轮上下文
2. 连续追问时保留上轮选择和拒绝信息
3. 不需要接同步 `/ask`；正式链路继续使用 `/ask/stream`

---

## 6. 可以不优先接的能力

### 6.1 AI 同步问答 `/recommendations/ask`

后端有同步接口，但现有前端文档明确建议小程序正式链路使用 `/ask/stream`。

建议：

- 不新增正式页面入口
- 可仅作为调试接口保留在 API 文档或 Apifox 中

### 6.2 健康检查 `/health`

后端提供健康检查接口，但小程序业务页面不需要直接调用。

建议：

- 用于部署、监控、联调排障
- 不纳入小程序功能对接清单

---

## 7. 建议接入优先级

### P0：补齐真实推荐闭环

目标：让后端已经实现的过滤和反馈能力真正影响用户推荐结果。

包含：

1. 滑卡页接 `/recommendations/cards`
2. 摇一摇或转盘接 `/recommendations/random`
3. “就吃这家”统一写 `choice-history`
4. 左滑 / 不想吃写 `recommendation-feedback`

理由：

- 这是“过滤黑名单和近 3 天吃过餐厅”的核心体验。
- 当前如果继续使用本地缓存随机，后端软过滤不会覆盖所有推荐入口。

### P1：补齐搜索

目标：让用户能主动找店，而不是只能看 nearby 候选。

包含：

1. 列表页搜索框
2. keyword 非空时调用 `/restaurants/search`
3. 复用当前排序、分类、价格筛选

理由：

- 后端接口和前端封装都已存在，页面接入成本相对低。

### P2：补齐用户备注

目标：把后端 notes CRUD 变成用户可见的私人记录能力。

包含：

1. `frontend/api/notes.js`
2. 详情页私人备注模块
3. “我的”页备注列表与搜索

理由：

- 后端完整实现但前端完全没有入口。
- 备注与评论分工明确，适合提升用户侧数据闭环。

### P3：补齐历史与管理页

目标：让用户能看到和管理自己的长期行为数据。

包含：

1. 最近吃过历史
2. 推荐反馈历史
3. 黑名单 reason 编辑

理由：

- 这些不是主流程必需，但能解释推荐为什么变化。

---

## 8. 推荐新增前端 API 封装

### 8.1 `frontend/api/recommendations.js`

建议新增：

```js
import client from './client';

export const GetRandomRecommendation = async (params) => {
  return await client.get('/recommendations/random', params);
};

export const GetRecommendationCards = async (params) => {
  return await client.get('/recommendations/cards', params);
};

export const AskRecommendation = async (payload) => {
  return await client.post('/recommendations/ask', payload);
};
```

说明：

- `AskRecommendation` 只建议调试使用，正式 AI 页面继续走 `recommendation-chat.js`。

### 8.2 `frontend/api/notes.js`

建议新增：

```js
import client from './client';

export const CreateNote = async (userId, payload) => {
  return await client.post(`/users/${userId}/notes`, payload);
};

export const ListNotes = async (userId, params = {}) => {
  return await client.get(`/users/${userId}/notes`, params, { silent: true });
};

export const GetNote = async (userId, noteId) => {
  return await client.get(`/users/${userId}/notes/${noteId}`, {}, { silent: true });
};

export const UpdateNote = async (userId, noteId, payload) => {
  return await client.put(`/users/${userId}/notes/${noteId}`, payload);
};

export const DeleteNote = async (userId, noteId) => {
  return await client.delete(`/users/${userId}/notes/${noteId}`);
};
```

### 8.3 扩展 `frontend/api/blacklist.js`

建议补充：

```js
export const GetBlacklistItem = async (userId, poiId) => {
  return await client.get(`/users/${userId}/blacklist/${encodeURIComponent(poiId)}`, {}, { silent: true });
};

export const UpdateBlacklist = async (userId, poiId, payload) => {
  return await client.put(`/users/${userId}/blacklist/${encodeURIComponent(poiId)}`, payload);
};
```

---

## 9. 关键产品语义

### 普通浏览不等于吃过

不要在以下动作写 `choice-history`：

- 点击餐厅卡片查看详情
- 浏览 AI 推荐卡片
- 滑卡右滑但尚未最终确认

建议写入的动作：

- “就吃这家”
- “确认选择”
- “已经吃过”反馈

### 黑名单不等于短期不想吃

- 黑名单：长期硬过滤，除非用户主动恢复，否则一直排除。
- 推荐反馈：短期软过滤，适合表达“今天不想吃”“太远”“太贵”等临时偏好。

### 后端 random/cards 比本地随机更懂用户

如果前端用本地 `Math.random()`，只能避开当前本地黑名单状态，无法完整使用：

- 近 3 天吃过
- 近 7 天负反馈
- 后端软过滤回退策略
- 后端候选分页补偿

---

## 10. 下一步最小落地方案

如果只做一轮小改动，建议按这个顺序：

1. 新增 `frontend/api/recommendations.js`
2. 滑卡页改用 `/recommendations/cards?userId=...`
3. 首页摇一摇改用 `/recommendations/random?userId=...`
4. 抽中 / 选中后统一调用 `CreateChoiceHistory`
5. 滑卡左滑或 AI 卡片反馈统一调用 `CreateRecommendationFeedback`

这能让“黑名单 + 近 3 天吃过 + 近期反馈”的后端推荐闭环真正覆盖主要推荐入口。
