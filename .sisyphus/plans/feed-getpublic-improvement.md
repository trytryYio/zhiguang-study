# 完善 getPublicFeed() 计划

## 目标
完善 `KnowPostFeedServiceImpl.getPublicFeed()` 方法，实现完整的 3 级缓存架构

## 当前问题
1. 缺少 L2 Redis 片段缓存查询
2. 缺少单飞锁机制
3. 缺少 Redis 缓存写入
4. 缺少用户维度状态叠加（enrich）
5. 缺少多个辅助方法

---

## 待办事项

- [ ] 1. 添加缺失的字段和常量
  - LAYOUT_VER 常量
  - singleFlight 字段
  - ObjectMapper 依赖
  - CounterService 依赖（ Phase 4 功能，先注释）
  - feedMineCache 依赖

- [ ] 2. 添加缺失的辅助方法
  - assembleFromCache() - 从 Redis 片段缓存组装页面
  - writeCaches() - 写入 Redis 片段缓存
  - enrich() - 叠加用户维度状态（Phase 4 功能，先注释）

- [ ] 3. 完善 getPublicFeed() 方法
  - 添加 L2 Redis 缓存查询
  - 添加单飞锁机制
  - 添加 Redis 缓存写入
  - 添加缓存雪崩防护（随机 TTL + jitter）
  - 调用 enrich() 方法（Phase 4 功能，先注释）

- [ ] 4. 完善 getMyPublished() 方法
  - 完善 L2 Redis 缓存逻辑
  - 完善缓存写入逻辑

- [ ] 5. 验证编译
  - mvn compile