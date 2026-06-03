# 性能优化贡献说明

姓名：沈哲伟
学号：2312190313
日期：2026-06-03

## 课程内容对应

本次优化参考“性能优化与移动端体验”幻灯片中的后端优化原则：

- 先观察高频链路，再针对瓶颈优化
- 热点数据使用缓存降低数据库压力
- 批量读取时避免重复查询
- 优化后通过自动化测试形成回归保护

## 我完成的工作

### 1. 后端热点快照读取复用

- [x] 新增 `RestaurantMetricSnapshotLookup`，统一封装餐厅聚合快照的批量读取逻辑
- [x] 先查 Caffeine 本地缓存，缓存未命中的 POI 再批量查数据库
- [x] 对同一批请求中的重复 POI ID 去重，避免重复进入 `findAllById`
- [x] 数据库读取成功后回填缓存，后续列表和推荐链路可复用

### 2. 推荐链路接入缓存

- [x] `RestaurantQueryApplicationService` 改为使用共享快照读取组件
- [x] `RecommendationCardAssembler` 改为使用共享快照读取组件
- [x] AI 推荐卡片不再绕过餐厅快照缓存，减少推荐问答高频场景下的重复 DB 访问

### 3. 附近餐厅候选池扩容

- [x] 前端首页全局餐厅池从 `30` 扩到 `100`
- [x] 餐厅列表页默认查询数量从 `30` 扩到 `100`
- [x] 后端在业务请求 `size > 50` 时自动跨高德页拼接，避免受高德单页返回上限影响
- [x] `size <= 50` 的小页请求保持原有单次上游调用路径

### 4. AI 推荐流式体验优化

- [x] AI 服务优先选择 3 家候选餐厅，候选不足时不重复、不编造
- [x] 流式回答保留完整最终答案，不再在 `answer.done` 阶段提前截断
- [x] 提高短推荐回答 token 预算，避免 3 家推荐的自然语言结论被截断
- [x] 前端聊天页支持 Markdown 加粗、列表、引用和行内代码渲染
- [x] 本地恢复聊天记录时保留 assistant 消息的 Markdown 渲染状态
- [x] 新增清空聊天记录入口，只清本地对话，不清用户偏好画像

### 5. 回归测试

- [x] 新增缓存命中不访问 repository 的单元测试
- [x] 新增缓存缺失只批量查询未命中 POI 并回填缓存的单元测试
- [x] 新增推荐卡片富化使用共享 lookup 的单元测试
- [x] 新增后端大 `size` 请求跨高德页拼接的单元测试
- [x] 更新餐厅查询和控制器测试构造，保持原 API 行为不变
- [x] 新增 AI 推荐 prompt、流式输出 token 预算和前端 Markdown 渲染测试

## 验证命令

```bash
cd ai-service
uv run --with pytest pytest -q

cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw -Dtest=RestaurantMetricSnapshotLookupTest,RestaurantQueryApplicationServiceTest,RestaurantControllerTest,RecommendationCardAssemblerTest test
```

补充完整验证：

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test

cd ../frontend
npm run lint
npm test -- --runInBand
```

验证结果：

- AI 服务：`42 passed`
- 后端：`Tests run: 181, Failures: 0, Errors: 0, Skipped: 0`
- 前端：`5` 个 Jest suites、`32` 个 tests 通过；ESLint 通过

## PR 链接

- PR 待创建：`feat/optimize-ShenZhewei-14`

## 遇到的问题和解决

1. 问题：`develop` 上餐厅列表已经使用 Caffeine 缓存，但 AI 推荐卡片组装仍直接从 repository 读取快照。
   解决：抽出共享 `RestaurantMetricSnapshotLookup`，让列表和推荐都走同一套“缓存命中优先，缺失批量查库”的读取路径。

2. 问题：候选 POI 可能在同一批处理里重复出现。
   解决：lookup 内部保留输入顺序并对未命中 POI 去重，只对真正缺失的 key 发起一次 `findAllById`。

3. 问题：前端附近餐厅池固定 `size=30`，在餐厅密集区域显得候选过少；但直接请求 `size=100` 时，高德单页实际只返回 50 条。
   解决：前端扩大业务请求量，后端按 50 条单页上限自动拼接多页高德结果。

4. 问题：优化不应改变公开 API 契约。
   解决：只调整内部依赖和读取路径，不修改 controller、DTO、OpenAPI 字段和响应语义。

## 心得体会

这次优化对应幻灯片里“缓存 / 慢查询 / P99”的后端性能思路。课程示例强调不要凭直觉乱加索引或缓存，因此我先复用已有的餐厅快照缓存边界，再补齐之前遗漏的 AI 推荐链路。这样优化范围小、行为不变，但能让餐厅列表和推荐问答共享热点快照数据，降低重复数据库读取对高峰响应时间的影响。
