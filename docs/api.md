# API 设计文档

## 1. 设计目标

本项目 API 面向微信小程序前端，提供“找附近餐厅 + 随机推荐 + 用户偏好（拉黑/备注）”能力。

餐厅主数据统一来源于**高德 Web 服务 API**，后端负责封装与清洗结果，不自建完整餐厅主表。

接口统一前缀：

```text
/api/v1
```

---

## 2. 统一返回格式

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

字段说明：

- `code`: 业务状态码，`0` 表示成功
- `message`: 返回信息
- `data`: 业务数据

---

## 3. 数据来源与坐标约定

- 餐厅数据来源：高德 Web 服务 API（后端调用）
- 坐标系：统一使用 **GCJ-02**
- 前端仅传定位与筛选条件，不直接调用高德服务

> 说明：当前阶段采用“仅高德数据”，不维护本地餐厅主数据 CRUD。

---

## 4. 餐厅查询与推荐 API

### 4.1 附近餐厅查询

- 方法：`GET`
- 路径：`/api/v1/restaurants/nearby`

查询参数：

- `longitude`: 经度（GCJ-02）
- `latitude`: 纬度（GCJ-02）
- `radius`: 搜索半径（米），默认 `3000`
- `keyword`: 可选，关键词（如“米线”“汉堡”）
- `page`: 可选，页码
- `size`: 可选，每页条数

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "poiId": "B0FF123456",
      "name": "沙县小吃",
      "address": "学林街xx号",
      "longitude": 120.3502,
      "latitude": 30.3154,
      "distance": 180
    }
  ]
}
```

### 4.2 关键词搜索地点

- 方法：`GET`
- 路径：`/api/v1/restaurants/search`

查询参数：

- `keywords`
- `city`: 可选
- `page`: 可选
- `size`: 可选

### 4.3 随机推荐餐厅

- 方法：`GET`
- 路径：`/api/v1/recommendations/random`

查询参数：

- `longitude`: 可选（有定位时推荐附近）
- `latitude`: 可选
- `radius`: 可选，默认 `3000`
- `keyword`: 可选
- `userId`: 可选，用于过滤该用户黑名单

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "poiId": "B0FF123456",
    "name": "老地方盖浇饭",
    "reason": "符合筛选条件的随机结果"
  }
}
```

### 4.4 候选卡片列表（左滑右滑）

- 方法：`GET`
- 路径：`/api/v1/recommendations/cards`

查询参数：

- `longitude`
- `latitude`
- `radius`
- `size`: 候选数量，默认 `20`
- `userId`: 可选

---

## 5. 用户偏好与互动 API

### 5.1 拉黑餐厅

- 方法：`POST`
- 路径：`/api/v1/users/{userId}/blacklist/{poiId}`

### 5.2 取消拉黑

- 方法：`DELETE`
- 路径：`/api/v1/users/{userId}/blacklist/{poiId}`

### 5.3 查看用户黑名单

- 方法：`GET`
- 路径：`/api/v1/users/{userId}/blacklist`

### 5.4 提交备注/避雷

- 方法：`POST`
- 路径：`/api/v1/users/{userId}/notes/{poiId}`

请求体：

```json
{
  "content": "晚高峰排队较久"
}
```

---

## 6. 后端内部映射的高德能力

建议后端封装以下高德接口：

- 逆地理编码：`/v3/geocode/regeo`
- 关键字搜索/周边搜索：`/v3/place/*`
- 输入提示：`/v3/assistant/inputtips`

调用链：

- 小程序获取用户定位（GCJ-02）
- 前端请求你们后端 API
- 后端请求高德 Web 服务 API
- 后端清洗/过滤后返回前端

---

## 7. 数据库最小化方案（仅高德数据）

因为餐厅主数据来自高德，本地数据库可简化为用户侧数据：

1. `user_blacklist`
   - `id`
   - `user_id`
   - `poi_id`
   - `created_at`

2. `user_restaurant_note`
   - `id`
   - `user_id`
   - `poi_id`
   - `content`
   - `created_at`

可选：`user_choice_history`（记录抽到/选择过的 poiId，用于去重推荐）。

---

## 8. 建议优先实现顺序

1. `GET /api/v1/restaurants/nearby`
2. `GET /api/v1/recommendations/random`
3. `POST /api/v1/users/{userId}/blacklist/{poiId}`
4. `GET /api/v1/recommendations/cards`
5. `POST /api/v1/users/{userId}/notes/{poiId}`

---

## 9. 错误码与错误语义（补充）

建议在 `code != 0` 时采用分层错误码：

- `1001`：参数校验失败（缺失/格式错误）
- `1002`：用户不存在或不可用
- `2001`：重复拉黑（`userId + poiId` 已存在）
- `2002`：备注内容非法（为空或超长）
- `3001`：高德上游请求失败
- `3002`：高德上游请求超时
- `9000`：系统内部异常

示例：

```json
{
  "code": 3001,
  "message": "amap service unavailable",
  "data": null
}
```

---

## 10. 接口一致性约定（补充）

1. 分页参数统一为 `page`、`size`，缺省值由后端统一处理。
2. 所有坐标参数统一按 **GCJ-02** 解释。
3. 用户偏好数据遵循 `(user_id, poi_id)` 唯一语义：
   - 黑名单：同一用户同一 POI 不可重复新增；
   - 备注：同一用户同一 POI 建议采用“覆盖更新”。
4. 推荐接口在返回前必须执行黑名单过滤；可选结合 `user_choice_history` 做短期去重。

