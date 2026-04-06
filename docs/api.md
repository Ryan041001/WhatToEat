# API 设计文档

## 1. 文档范围

本文档描述 WhatToEat / 今天吃什么 项目当前已经实现的 API 契约，供课程作业提交、后端实现对齐，以及后续前端联调用。

接口统一前缀：

```text
/api/v1
```

当前已实现五组资源：

- `auth`：登录、登出、当前用户
- `restaurants`：餐厅查询
- `recommendations`：随机推荐与卡片候选列表
- `users/{userId}/blacklist`：用户黑名单新增、删除、分页查询
- `users/{userId}/notes`：用户备注 CRUD、分页查询与内容筛选

---

## 2. 统一返回格式

所有接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

字段说明：

- `code`：业务码，`0` 表示成功
- `message`：结果说明
- `data`：业务数据，失败时通常为 `null`

失败示例：

```json
{
  "code": 1003,
  "message": "未登录或token无效",
  "data": null
}
```

---

## 3. 鉴权方式

当前作业阶段采用 **mock 微信登录 + Bearer Token 会话**。

### 3.1 登录

调用：

- `POST /api/v1/auth/wechat-login`

请求体示例：

```json
{
  "code": "mock-code-001",
  "nickname": "Alice"
}
```

登录成功后返回 `token`，后续受保护接口通过请求头传递：

```http
Authorization: Bearer <token>
```

### 3.2 当前受保护接口

- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `POST /api/v1/users/{userId}/blacklist`
- `DELETE /api/v1/users/{userId}/blacklist/{poiId}`
- `GET /api/v1/users/{userId}/blacklist`
- `POST /api/v1/users/{userId}/notes`
- `GET /api/v1/users/{userId}/notes`
- `GET /api/v1/users/{userId}/notes/{noteId}`
- `PUT /api/v1/users/{userId}/notes/{noteId}`
- `DELETE /api/v1/users/{userId}/notes/{noteId}`

说明：当前 blacklist 创建接口仍以 `userId` 作为资源定位参数，但服务端会校验 Bearer Token 对应用户与路径参数一致；后续如收敛到“当前用户上下文”，可在不改变资源语义的前提下进一步演进。

---

## 4. HTTP 状态码约定

- `200 OK`：查询、更新、删除、登出成功
- `201 Created`：创建成功，例如登录建会话、加入黑名单
- `400 Bad Request`：参数错误、校验失败
- `401 Unauthorized`：未登录或 token 无效
- `404 Not Found`：用户或目标资源不存在
- `409 Conflict`：重复创建或资源冲突
- `502 Bad Gateway`：高德上游失败
- `504 Gateway Timeout`：高德上游超时
- `500 Internal Server Error`：系统内部异常

---

## 5. 业务错误码

### 5.1 参数与认证

- `1001`：参数校验失败
- `1002`：用户不存在
- `1003`：未登录或 token 无效
- `1004`：登录 code 非法

### 5.2 黑名单

- `2001`：重复拉黑
- `2002`：黑名单记录不存在

### 5.3 备注

- `2003`：备注内容非法
- `2004`：备注不存在
- `2005`：备注已存在

### 5.4 高德上游

- `3001`：高德上游失败
- `3002`：高德上游超时
- `3003`：高德无结果

### 5.5 系统兜底

- `9000`：系统异常

---

## 6. 分页与筛选约定

当前已实现的分页查询接口使用：

- `page`：页码，从 `1` 开始
- `size`：每页条数
- `keyword`：可选筛选条件，适用于餐厅搜索与备注内容筛选

分页返回统一结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "size": 10,
    "total": 35
  }
}
```

---

## 7. 餐厅查询接口

### 7.1 查询附近餐厅

- 方法：`GET`
- 路径：`/api/v1/restaurants/nearby`

查询参数：

- `longitude`：经度，必填，坐标系 GCJ-02
- `latitude`：纬度，必填，坐标系 GCJ-02
- `radius`：搜索半径（米），默认 `1000`
- `page`：页码，默认 `1`
- `size`：每页条数，默认 `10`

调用提示：

- `longitude`、`latitude` 必须传具体数值，不能只写成 `?longitude&latitude`；坐标系须为 **GCJ-02**（火星坐标系），传入 WGS-84 会导致位置偏移
- `radius`、`page`、`size` 不传时会走默认值；如果要传，也必须传具体数值

请求示例：

```bash
curl 'http://127.0.0.1:8080/api/v1/restaurants/nearby?longitude=120.35&latitude=30.31&radius=1000&page=1&size=10'
```

结果语义说明：
- 当 `total == 0` 时，返回 `404 Not Found`，业务码 `3003`（高德无结果）
- 当 `total > 0` 且当前页 `items` 为空时，返回 `200 OK`，`items: []`

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "poiId": "B0FF123456",
        "name": "沙县小吃",
        "address": "学林街xx号",
        "longitude": 120.3502,
        "latitude": 30.3154,
        "category": "餐饮服务;中餐厅;快餐厅",
        "distance": 180
      }
    ],
    "page": 1,
    "size": 10,
    "total": 35
  }
}
```

### 7.2 按关键词搜索餐厅

- 方法：`GET`
- 路径：`/api/v1/restaurants/search`

查询参数：

- `keyword`：关键词，必填
- `longitude`：经度，必填，坐标系 GCJ-02
- `latitude`：纬度，必填，坐标系 GCJ-02
- `radius`：搜索半径，默认 `1000`
- `page`：页码
- `size`：每页条数

请求示例：

```bash
curl 'http://127.0.0.1:8080/api/v1/restaurants/search?keyword=拉面&longitude=120.36&latitude=30.32&radius=1000&page=1&size=10'
```

结果语义说明：
- 当 `total == 0` 时，返回 `404 Not Found`，业务码 `3003`（高德无结果）
- 当 `total > 0` 且当前页 `items` 为空时，返回 `200 OK`，`items: []`

---

## 8. 推荐接口

推荐接口当前不要求 Bearer Token；`userId` 为可选参数，仅用于应用黑名单过滤。
若传入 `userId`：

- 必须为正整数，否则返回 `400 Bad Request` / `1001`
- 对应用户必须存在，否则返回 `404 Not Found` / `1002`

### 8.1 随机推荐餐厅

- 方法：`GET`
- 路径：`/api/v1/recommendations/random`

查询参数：

- `longitude`：经度，必填，坐标系 GCJ-02
- `latitude`：纬度，必填，坐标系 GCJ-02
- `radius`：搜索半径（米），默认 `1000`
- `userId`：可选；传入后会过滤该用户黑名单中的 `poiId`

调用提示：

- `userId` 不需要时请省略整个参数，不要传成 `userId=`
- `longitude`、`latitude` 必须传具体数值；坐标系须为 **GCJ-02**（火星坐标系），传入 WGS-84 会导致位置偏移

请求示例：

```bash
curl 'http://127.0.0.1:8080/api/v1/recommendations/random?longitude=120.35&latitude=30.31&radius=1000'
```

结果语义说明：
- 使用高德 nearby 结果作为候选池，内部会按页继续拉取并补足最多 `20` 条可用候选，再随机返回一条
- 过滤黑名单后若候选为空，返回 `404 Not Found`，业务码 `3003`
- 高德上游失败 / 超时分别返回 `502 / 504`

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "poiId": "B0FF123456",
    "name": "老地方盖浇饭",
    "address": "学林街xx号",
    "longitude": 120.3502,
    "latitude": 30.3154,
    "category": "餐饮服务;中餐厅;快餐厅",
    "distance": 180,
    "reason": "符合筛选条件的随机结果"
  }
}
```

### 8.2 候选卡片列表

- 方法：`GET`
- 路径：`/api/v1/recommendations/cards`

查询参数：

- `longitude`：经度，必填，坐标系 GCJ-02
- `latitude`：纬度，必填，坐标系 GCJ-02
- `radius`：搜索半径（米），默认 `1000`
- `size`：候选数量，默认 `20`
- `userId`：可选；传入后会过滤该用户黑名单中的 `poiId`

请求示例：

```bash
curl 'http://127.0.0.1:8080/api/v1/recommendations/cards?longitude=120.35&latitude=30.31&radius=1000&size=20'
```

结果语义说明：
- 会按页继续拉取高德结果，直到收集满请求的 `size` 条可用候选或上游结果耗尽
- 返回过滤后的候选列表，顺序与高德返回顺序一致
- 过滤黑名单后若候选为空，返回 `404 Not Found`，业务码 `3003`
- 高德上游失败 / 超时分别返回 `502 / 504`

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "poiId": "B0FF123456",
      "name": "兰州拉面",
      "address": "文泽路xx号",
      "longitude": 120.3601,
      "latitude": 30.3205,
      "category": "餐饮服务;中餐厅;快餐厅",
      "distance": 220
    }
  ]
}
```

---

## 9. 黑名单接口

### 9.1 加入黑名单

- 方法：`POST`
- 路径：`/api/v1/users/{userId}/blacklist`

路径参数：

- `userId`：用户 ID，必填。不要留空；应先登录，再调用 `GET /api/v1/auth/me` 获取当前用户的 `id`
- 在 Apifox 中，推荐把路径写成 `/api/v1/users/{{userId}}/blacklist`，并将环境变量 `userId` 绑定到这个路径参数；如果最终生成的 URL 仍然是 `/users//blacklist`，说明变量没有真正替换成功

请求体：

```json
{
  "poiId": "B0FF123456"
}
```

- `poiId`：餐厅 POI ID，必填，长度不超过 `64`

成功返回 `201 Created`。

正确调用示例：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/v1/users/1/blacklist' \
--header 'Authorization: Bearer <token>' \
--header 'Content-Type: application/json' \
--data-raw '{
  "poiId": "B0FF123456"
}'
```

调试顺序建议：

1. `POST /api/v1/auth/wechat-login` 获取 `token`
2. `GET /api/v1/auth/me` 获取当前用户 `id`
3. 用拿到的 `id` 替换路径里的 `{userId}`，再调用黑名单接口
4. 如果在 Apifox 里用环境变量，确认最终请求 URL 已经变成 `/api/v1/users/1/blacklist` 这种实际值，而不是 `/api/v1/users//blacklist`

错误示例：

- `POST /api/v1/users//blacklist`
  `userId` 为空，属于错误路径，不是合法请求

### 9.2 移除黑名单

- 方法：`DELETE`
- 路径：`/api/v1/users/{userId}/blacklist/{poiId}`

路径参数：

- `userId`：用户 ID，必填
- `poiId`：餐厅 POI ID，必填，长度不超过 `64`

成功返回 `200 OK`。

错误语义：

- 未登录、token 无效或 token 对应用户与路径 `userId` 不一致时，返回 `401 Unauthorized` / `1003`
- 目标黑名单记录不存在时，返回 `404 Not Found` / `2002`

### 9.3 分页查询黑名单

- 方法：`GET`
- 路径：`/api/v1/users/{userId}/blacklist`

查询参数：

- `page`：页码，默认 `1`
- `size`：每页条数，默认 `10`，最大 `100`

返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "poiId": "B0FF123456",
        "createdAt": "2026-03-26T10:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 1
  }
}
```

字段说明：

- `items[].poiId`：被拉黑的餐厅 POI ID
- `items[].createdAt`：加入黑名单时间，ISO-8601 本地日期时间字符串

错误语义：

- 未登录、token 无效或 token 对应用户与路径 `userId` 不一致时，返回 `401 Unauthorized` / `1003`
- 参数校验失败时，返回 `400 Bad Request` / `1001`

---

## 10. 备注接口

### 10.1 创建备注

- 方法：`POST`
- 路径：`/api/v1/users/{userId}/notes`

路径参数：

- `userId`：用户 ID，必填。不要留空；应先登录，再调用 `GET /api/v1/auth/me` 获取当前用户的 `id`
- 在 Apifox 中，推荐把路径写成 `/api/v1/users/{{userId}}/notes`，并将环境变量 `userId` 绑定到这个路径参数；如果最终生成的 URL 仍然是 `/users//notes`，说明变量没有真正替换成功

请求体：

```json
{
  "poiId": "B0FF123456",
  "content": "中午排队久，但味道不错"
}
```

- `poiId`：餐厅 POI ID，必填，长度不超过 `64`
- `content`：备注内容，必填；字段缺失时返回 `400 Bad Request` / `1001`，提供后若去除首尾空白仍为空或长度超过 `1000`，返回 `400 Bad Request` / `2003`

成功返回 `201 Created`。

正确调用示例：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/v1/users/1/notes' \
--header 'Authorization: Bearer <token>' \
--header 'Content-Type: application/json' \
--data-raw '{
  "poiId": "B0FF123456",
  "content": "中午排队久，但味道不错"
}'
```

错误语义：

- 未登录、token 无效或 token 对应用户与路径 `userId` 不一致时，返回 `401 Unauthorized` / `1003`
- 参数校验失败时，返回 `400 Bad Request` / `1001`
- 备注内容非法（如去除首尾空白后为空、长度超过 `1000`）时，返回 `400 Bad Request` / `2003`
- 同一用户对同一 `poiId` 重复创建备注时，返回 `409 Conflict` / `2005`

### 10.2 分页查询备注

- 方法：`GET`
- 路径：`/api/v1/users/{userId}/notes`

查询参数：

- `page`：页码，默认 `1`
- `size`：每页条数，默认 `10`，最大 `100`
- `keyword`：可选；按备注内容做包含筛选

返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 10,
        "poiId": "B0FF123456",
        "content": "晚高峰排队久，建议错峰",
        "createdAt": "2026-03-26T18:00:00",
        "updatedAt": "2026-03-26T18:10:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 1
  }
}
```

字段说明：

- `items[].id`：备注主键 ID
- `items[].poiId`：餐厅 POI ID
- `items[].content`：备注内容
- `items[].createdAt`：创建时间，ISO-8601 本地日期时间字符串
- `items[].updatedAt`：更新时间，ISO-8601 本地日期时间字符串

结果语义说明：

- 列表按 `updatedAt DESC, id DESC` 排序，优先返回最近更新的备注
- `keyword` 为空或只包含空白时，按未筛选列表处理

### 10.3 查询备注详情

- 方法：`GET`
- 路径：`/api/v1/users/{userId}/notes/{noteId}`

路径参数：

- `noteId`：备注主键 ID，必须为正整数

返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10,
    "userId": 1,
    "poiId": "B0FF123456",
    "content": "晚高峰排队久，建议错峰",
    "createdAt": "2026-03-26T18:00:00",
    "updatedAt": "2026-03-26T18:10:00"
  }
}
```

字段说明：

- `userId`：所属用户 ID

### 10.4 更新备注

- 方法：`PUT`
- 路径：`/api/v1/users/{userId}/notes/{noteId}`

请求体：

```json
{
  "content": "已改成错峰去，人少很多"
}
```

- `content`：备注内容，必填；字段缺失时返回 `400 Bad Request` / `1001`，提供后若去除首尾空白仍为空或长度超过 `1000`，返回 `400 Bad Request` / `2003`

成功返回 `200 OK`，响应 `data` 结构与备注详情一致。

错误语义：

- `noteId` 非正整数等参数校验失败时，返回 `400 Bad Request` / `1001`
- 目标备注不存在时，返回 `404 Not Found` / `2004`
- 备注内容非法（如去除首尾空白后为空、长度超过 `1000`）时，返回 `400 Bad Request` / `2003`

### 10.5 删除备注

- 方法：`DELETE`
- 路径：`/api/v1/users/{userId}/notes/{noteId}`

成功返回 `200 OK`。

错误语义：

- `noteId` 非正整数等参数校验失败时，返回 `400 Bad Request` / `1001`
- 目标备注不存在时，返回 `404 Not Found` / `2004`

### 10.6 备注接口通用错误语义

- 未登录、token 无效或 token 对应用户与路径 `userId` 不一致时，返回 `401 Unauthorized` / `1003`
- 参数校验失败时，返回 `400 Bad Request` / `1001`

---

## 11. OpenAPI 导入说明

OpenAPI 主契约文件位于：

```text
docs/api.yaml
```

可直接导入：

- Apifox
- Postman
- Swagger Editor

建议流程：

1. 导入 `docs/api.yaml`
2. 微信开发者工具调试时选择 `http://127.0.0.1:8080`
3. 真机同局域网调试时选择 `http://<你的宿主机局域网IP>:8080`
4. 先调用 `POST /api/v1/auth/wechat-login` 获取 token
5. 在受保护接口中配置 `Authorization: Bearer <token>`

说明：

- Docker Compose 本地联调默认将后端暴露在宿主机 `8080` 端口
- `localhost` 只适合开发者工具，不适合真机；真机应使用宿主机局域网 IP
- 若使用 Compose，请先把 `.env` 里的 `AMAP_KEY` 换成真实高德 Key，再测试餐厅与推荐接口

---

## 12. 实现说明

- 当前认证实现为 mock 微信登录，接口形状贴近真实小程序登录流程
- 餐厅主数据来源于高德，不在本地维护完整餐厅主表
- 当前已实现的用户侧写接口包括黑名单与备注 CRUD、推荐查询

---

## 13. 与旧版文档的差异

本版相较于早期草稿，做了以下收敛：

1. 将黑名单新增接口从路径参数式 `POST /blacklist/{poiId}` 调整为更 RESTful 的 body 创建式 `POST /blacklist`
2. 补充认证资源 `auth`
3. 补充分页结构、错误码、Bearer Token 说明
4. 补充随机推荐与卡片候选列表接口契约
5. 补充备注 CRUD 契约、错误码与筛选语义
6. 当前文档只保留已落地接口，未实现资源不再作为现行契约发布
7. 以 `docs/api.yaml` 作为后续实现与联调的主契约
