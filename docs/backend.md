# 后端模块说明

## 1. 模块目标

`backend/` 目录承载“今天吃什么”项目的服务端能力，负责向微信小程序提供 RESTful API。

本项目后端当前阶段计划采用**“高德数据源优先”**方案：

- 餐厅主数据来自高德 Web 服务 API
- 后端负责调用、清洗、筛选并返回统一结构
- 本地数据库只保存用户侧数据（黑名单、备注、历史）

这样可以更快落地“附近找店 + 随机推荐”核心功能，降低前期开发复杂度。

---

## 2. 当前技术框架

- 开发语言：Java 17
- 核心框架：Spring Boot 4.0.3
- Web 层：Spring Web MVC
- 构建工具：Maven
- 启动入口：`backend/src/main/java/com/zjgsu/whattoeat/WhatToEatApplication.java`
- 配置文件：`backend/src/main/resources/application.properties`

---

## 3. 当前后端分层

```text
backend/src/main/java/com/zjgsu/whattoeat/
├── controller/       # 对外 API
├── service/          # 业务逻辑（application/domain 分层）
├── integration/
│   └── amap/         # 高德 API 封装
├── repository/       # 用户侧数据访问（黑名单、备注、历史）
├── model/
│   ├── dto/          # 请求/响应对象
│   └── entity/       # 本地数据库实体
├── common/           # 统一返回体、异常处理
└── config/           # CORS、配置绑定
```

---

## 4. 当前阶段计划实现的核心能力

### 4.1 餐厅与推荐

- 根据定位查询附近餐厅（来自高德）
- 按关键词搜索餐厅（来自高德）
- 随机推荐餐厅（高德结果池中随机）
- 左滑右滑候选卡片列表（高德结果池）

### 4.2 用户侧数据

- 用户黑名单创建、删除与分页查询（按 `userId + poiId`）
- 用户备注创建、分页查询、详情、更新、删除（按 `userId + poiId` 约束唯一）
- 推荐时过滤当前用户黑名单

---

## 5. 高德 Web 服务集成说明

### 5.1 接入方式

后端统一调用高德 Web 服务 API，前端原则上不直接调用高德接口。

### 5.2 接入收益

- 保护高德 Key，不暴露到小程序端
- 统一返回结构，便于前端开发
- 可在后端做超时、限流、错误兜底

### 5.3 建议封装结构

```text
integration/amap/
├── AmapClient.java
├── AmapProperties.java
├── AmapPlaceService.java
└── dto/
```

### 5.4 建议配置项

```properties
amap.key=你的高德Web服务Key
amap.base-url=https://restapi.amap.com
amap.search-radius=3000
```

坐标系约定：统一使用 **GCJ-02**。

---

## 6. 数据库最小化设计（仅高德主数据）

按当前阶段实现，本地库暂不存完整餐厅主表，当前保留：

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

3. （可选，预留）`user_choice_history`
   - `id`
   - `user_id`
   - `poi_id`
   - `chosen_at`

---

## 7. 当前依赖说明（按现状）

- MySQL Connector/J
- Spring Data JPA + Hibernate
- Spring Validation
- Spring Boot Configuration Processor
- HTTP 客户端（RestClient / WebClient）

---

## 8. 运行方式

```bash
cd backend
./mvnw spring-boot:run
```

或：

```bash
cd backend
./mvnw clean package
java -jar target/WhatToEat-0.0.1-SNAPSHOT.jar
```

### 8.1 无 MySQL 时本地启动

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
```

说明：

- `dev`：默认本地开发模式，连接宿主机 MySQL `localhost:3306/whattoeat_dev`
- `test`：H2 内存库模式，适合无 MySQL 的功能验证
- `docker`：容器化本地开发模式，连接 Compose 内的 `mysql` 服务

### 8.2 Docker Compose 本地联调

在项目根目录执行：

```bash
cp .env.example .env
docker compose --env-file .env up --build
```

首次使用前请将 `.env` 中的 `AMAP_KEY` 替换为真实可用的高德 Web 服务 Key；默认的 `test-key` 仅用于让容器配置完整，不保证餐厅查询与推荐接口可用。

常用命令：

```bash
docker compose --env-file .env ps
docker compose --env-file .env down
```

联调说明：

- 微信开发者工具使用 `http://127.0.0.1:8080`
- 真机同局域网调试使用 `http://<你的宿主机局域网IP>:8080`
- 容器模式下数据库由 Compose 管理，表结构仍由 Flyway 自动迁移
- 当前小程序侧只补充了 API 地址配置边界，真正的 `wx.request` 接口接入仍需后续前端联调时补齐

---

## 9. 当前阶段建议实施顺序

1. 补充基础工程能力（统一返回体、配置绑定、异常处理）
2. 封装高德 nearby/search 接口
3. 打通随机推荐接口
4. 落地黑名单与备注表及对应 CRUD 接口
5. 在推荐流程中接入黑名单过滤
6. 与小程序前端联调
