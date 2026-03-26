# API 设计文档

## 1. 文档范围

本文档描述 WhatToEat / 今天吃什么 项目当前 API 契约，供课程作业提交、后端实现对齐，以及后续前端联调用。

接口统一前缀：

```text
/api/v1
```

当前覆盖五组资源：

- `auth`：登录、登出、当前用户
- `restaurants`：餐厅查询
- `recommendations`：推荐结果
- `users/{userId}/blacklist`：用户黑名单
- `users/{userId}/notes`：用户备注 CRUD

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

说明：当前 blacklist / notes / recommendations 文档仍以 `userId` 作为资源定位参数；后续如收敛到“当前用户上下文”，可在不改变资源语义的前提下进一步演进。

---

## 4. HTTP 状态码约定

- `200 OK`：查询、更新、删除、登出成功
- `201 Created`：创建成功，例如登录建会话、创建备注、加入黑名单
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

### 5.2 黑名单与备注

- `2001`：重复拉黑
- `2002`：黑名单记录不存在
- `2003`：备注内容非法
- `2004`：备注不存在

### 5.3 高德上游

- `3001`：高德上游失败
- `3002`：高德上游超时
- `3003`：高德无结果

### 5.4 系统兜底

- `9000`：系统异常

---

## 6. 分页与筛选约定

所有列表接口统一使用：

- `page`：页码，从 `1` 开始
- `size`：每页条数
- `keyword`：可选筛选条件，适用于搜索与备注筛选

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

## 7. 餐厅查询与推荐接口

### 7.1 查询附近餐厅

- 方法：`GET`
- 路径：`/api/v1/restaurants/nearby`

查询参数：

- `longitude`：经度，必填
- `latitude`：纬度，必填
- `radius`：搜索半径（米），默认 `1000`
- `page`：页码，默认 `1`
- `size`：每页条数，默认 `10`

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
- `longitude`：经度，必填
- `latitude`：纬度，必填
- `radius`：搜索半径，默认 `1000`
- `page`：页码
- `size`：每页条数

结果语义说明：
- 当 `total == 0` 时，返回 `404 Not Found`，业务码 `3003`（高德无结果）
- 当 `total > 0` 且当前页 `items` 为空时，返回 `200 OK`，`items: []`

### 7.3 获取随机推荐结果

- 方法：`GET`
- 路径：`/api/v1/recommendations/random`

查询参数：

- `userId`：用户 ID，必填
- `longitude`：经度，必填
- `latitude`：纬度，必填
- `radius`：搜索半径，默认 `1000`

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
    "distance": 180,
    "reason": "符合筛选条件的随机结果"
  }
}
```

### 7.4 获取推荐候选卡片

- 方法：`GET`
- 路径：`/api/v1/recommendations/cards`

查询参数：

- `userId`：用户 ID，必填
- `longitude`：经度，必填
- `latitude`：纬度，必填
- `radius`：搜索半径，默认 `1000`
- `size`：候选数量，默认 `10`

---

## 8. 黑名单接口

### 8.1 查询黑名单列表

- 方法：`GET`
- 路径：`/api/v1/users/{userId}/blacklist`

查询参数：

- `page`
- `size`

### 8.2 加入黑名单

- 方法：`POST`
- 路径：`/api/v1/users/{userId}/blacklist`

请求体：

```json
{
  "poiId": "B0FF123456"
}
```

- `poiId`：餐厅 POI ID，必填，长度不超过 `64`

成功返回 `201 Created`。

### 8.3 删除黑名单项

- 方法：`DELETE`
- 路径：`/api/v1/users/{userId}/blacklist/{poiId}`

成功返回：

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 9. 备注 CRUD 接口

### 9.1 创建备注

- 方法：`POST`
- 路径：`/api/v1/users/{userId}/notes`

请求体：

```json
{
  "poiId": "B0FF123456",
  "content": "中午排队久，但味道不错"
}
```

### 9.2 查询备注列表

- 方法：`GET`
- 路径：`/api/v1/users/{userId}/notes`

查询参数：

- `page`
- `size`
- `keyword`：按备注内容模糊筛选

### 9.3 查询备注详情

- 方法：`GET`
- 路径：`/api/v1/users/{userId}/notes/{noteId}`

### 9.4 更新备注

- 方法：`PUT`
- 路径：`/api/v1/users/{userId}/notes/{noteId}`

请求体：

```json
{
  "content": "晚高峰排队久，建议错峰"
}
```

### 9.5 删除备注

- 方法：`DELETE`
- 路径：`/api/v1/users/{userId}/notes/{noteId}`

备注详情示例：

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

---

## 10. OpenAPI 导入说明

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
2. 选择本地服务地址 `http://localhost:8080`
3. 先调用 `POST /api/v1/auth/wechat-login` 获取 token
4. 在受保护接口中配置 `Authorization: Bearer <token>`

---

## 11. 实现说明

- 当前认证实现为 mock 微信登录，接口形状贴近真实小程序登录流程
- 餐厅主数据来源于高德，不在本地维护完整餐厅主表
- 黑名单与备注属于用户侧业务数据，由本地数据库持久化
- 推荐结果在返回前应执行黑名单过滤

---

## 12. 与旧版文档的差异

本版相较于早期草稿，做了以下收敛：

1. 将黑名单新增接口从路径参数式 `POST /blacklist/{poiId}` 调整为更 RESTful 的 body 创建式 `POST /blacklist`
2. 将备注接口扩展为完整 CRUD，而不是仅提交备注
3. 补充认证资源 `auth`
4. 补充分页结构、错误码、Bearer Token 说明
5. 以 `docs/api.yaml` 作为后续实现与联调的主契约
