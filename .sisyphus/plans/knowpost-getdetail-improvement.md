# 知识帖文系统 - getDetail() 方法完善计划

## TL;DR

> **快速总结**：完善 `KnowPostServiceImpl.getDetail()` 方法，添加 L2 Redis 缓存、单飞锁、缓存穿透和雪崩防护，将第四、五阶段的内容注释掉，留足空间。
>
> **交付物**：
> - 完善的 `getDetail()` 方法（支持三级缓存）
> - 添加 `singleFlight` 字段和 `DETAIL_LAYOUT_VER` 常量
> - 添加 `tryProcessCacheHit()` 辅助方法
> - 注释掉第四、五阶段的内容（计数、用户状态、AI、搜索、Outbox）
>
> **预估工作量**：Medium
> **并行执行**：NO - 顺序执行
> **关键路径**：Task 1 → Task 2 → Task 3 → Task 4

---

## Context

### Original Request
用户要求按照开发阶段来完善 `getDetail()` 方法，当前在**第三阶段：核心业务功能**的**6. 知识帖文系统**，还没到第四阶段（用户关系管理和计数系统），涉及到第四、五阶段的内容要注释掉，先省略，留足空间。

### Interview Summary
**Key Discussions**:
- 当前阶段：第三阶段 - 知识帖文系统
- 已实现：L1 Caffeine 缓存、L3 数据库查询、权限校验、热点检测
- 需要实现：L2 Redis 缓存、单飞锁、缓存穿透防护、缓存雪崩防护
- 需要注释：第四阶段（计数、用户状态）、第五阶段（AI、搜索、Outbox）

**Research Findings**:
- 参考项目：`源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java`
- 参考项目实现了完整的三级缓存、单飞锁、缓存穿透和雪崩防护
- 参考项目使用了 `singleFlight`（ConcurrentHashMap）来实现单飞锁
- 参考项目使用了 `tryProcessCacheHit()` 辅助方法来处理缓存命中逻辑

### Metis Review
**Identified Gaps** (addressed):
- Gap 1: 缺少 `singleFlight` 字段 - 已在计划中添加
- Gap 2: 缺少 `DETAIL_LAYOUT_VER` 常量 - 已在计划中添加
- Gap 3: 缺少 `tryProcessCacheHit()` 辅助方法 - 已在计划中添加
- Gap 4: 需要明确注释掉第四、五阶段的内容 - 已在计划中明确

---

## Work Objectives

### Core Objective
完善 `KnowPostServiceImpl.getDetail()` 方法，实现完整的三级缓存架构（L1 Caffeine + L2 Redis + L3 Database），添加单飞锁、缓存穿透和雪崩防护，将第四、五阶段的内容注释掉。

### Concrete Deliverables
- 完善的 `getDetail()` 方法（支持三级缓存）
- 添加 `singleFlight` 字段（ConcurrentHashMap<String, Object>）
- 添加 `DETAIL_LAYOUT_VER` 常量（int）
- 添加 `tryProcessCacheHit()` 辅助方法
- 注释掉第四、五阶段的内容（计数、用户状态、AI、搜索、Outbox）

### Definition of Done
- [ ] `getDetail()` 方法支持三级缓存（L1 Caffeine + L2 Redis + L3 Database）
- [ ] 添加单飞锁机制（防止缓存击穿）
- [ ] 添加缓存穿透防护（NULL 值缓存）
- [ ] 添加缓存雪崩防护（随机过期时间）
- [ ] 第四、五阶段的内容已注释掉，留足空间
- [ ] 代码编译通过，无语法错误

### Must Have
- 三级缓存架构（L1 Caffeine + L2 Redis + L3 Database）
- 单飞锁机制（防止缓存击穿）
- 缓存穿透防护（NULL 值缓存）
- 缓存雪崩防护（随机过期时间）
- 第四、五阶段的内容已注释掉

### Must NOT Have (Guardrails)
- 不要实现第四阶段的计数查询（CounterService）
- 不要实现第四阶段的用户状态查询（liked/faved）
- 不要实现第五阶段的 AI 摘要生成
- 不要实现第五阶段的搜索索引更新
- 不要实现第五阶段的 Outbox 事件写入
- 不要修改其他方法，只修改 `getDetail()` 方法

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** - ALL verification is agent-executed. No exceptions.
> Acceptance criteria requiring "user manually tests/confirms" are FORBIDDEN.

### Test Decision
- **Infrastructure exists**: YES (JUnit 5 + Spring Boot Test)
- **Automated tests**: Tests-after (先实现，后添加测试)
- **Framework**: JUnit 5 + Spring Boot Test
- **If TDD**: 每个任务先实现，后添加测试

### QA Policy
Every task MUST include agent-executed QA scenarios (see TODO template below).
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Backend/Service**: Use Bash (curl) - Send requests, assert status + response fields
- **Database**: Use Bash (mysql) - Query database, assert data integrity

---

## Execution Strategy

### Parallel Execution Waves

> Maximize throughput by grouping independent tasks into parallel waves.
> Each wave completes before the next begins.
> Target: 5-8 tasks per wave. Fewer than 3 per wave (except final) = under-splitting.

```
Wave 1 (Start Immediately - 字段和常量添加):
├── Task 1: 添加 singleFlight 字段 [quick]
├── Task 2: 添加 DETAIL_LAYOUT_VER 常量 [quick]
└── Task 3: 添加 tryProcessCacheHit() 辅助方法 [quick]

Wave 2 (After Wave 1 - 核心逻辑实现):
├── Task 4: 完善 getDetail() 方法 - L2 Redis 缓存 [deep]
├── Task 5: 完善 getDetail() 方法 - 单飞锁机制 [deep]
├── Task 6: 完善 getDetail() 方法 - 缓存穿透防护 [deep]
└── Task 7: 完善 getDetail() 方法 - 缓存雪崩防护 [deep]

Wave 3 (After Wave 2 - 注释和清理):
├── Task 8: 注释掉第四、五阶段的内容 [quick]
└── Task 9: 代码编译验证 [quick]

Wave FINAL (After ALL tasks — 验证):
├── Task F1: 代码编译验证 [quick]
└── Task F2: 功能验证（可选）[quick]
-> Present results -> Get explicit user okay

Critical Path: Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Task 8 → Task 9 → F1-F2 → user okay
Parallel Speedup: ~40% faster than sequential
Max Concurrent: 3 (Wave 1), 4 (Wave 2), 2 (Wave 3)
```

### Dependency Matrix (abbreviated - show ALL tasks in your generated plan)

- **1-3**: - - 4-7, 1
- **4**: 1, 2, 3 - 8, 2
- **5**: 1, 2, 3 - 8, 2
- **6**: 1, 2, 3 - 8, 2
- **7**: 1, 2, 3 - 8, 2
- **8**: 4, 5, 6, 7 - 9, 3
- **9**: 8 - F1, F2, 4

> This is abbreviated for reference. YOUR generated plan must include the FULL matrix for ALL tasks.

### Agent Dispatch Summary

- **1**: **3** - T1 → `quick`, T2 → `quick`, T3 → `quick`
- **2**: **4** - T4 → `deep`, T5 → `deep`, T6 → `deep`, T7 → `deep`
- **3**: **2** - T8 → `quick`, T9 → `quick`
- **4**: **2** - F1 → `quick`, F2 → `quick`

---

## TODOs

> Implementation + Test = ONE Task. Never separate.
> EVERY task MUST have: Recommended Agent Profile + Parallelization info + QA Scenarios.
> **A task WITHOUT QA Scenarios is INCOMPLETE. No exceptions.**

- [ ] 1. 添加 singleFlight 字段

  **What to do**:
  - 在 `KnowPostServiceImpl` 类中添加 `singleFlight` 字段
  - 字段类型：`ConcurrentHashMap<String, Object>`
  - 字段初始化：`new ConcurrentHashMap<>()`
  - 字段用途：用于单飞锁机制，防止缓存击穿

  **Must NOT do**:
  - 不要修改其他字段
  - 不要添加其他不必要的字段

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: 简单的字段添加，不需要复杂的逻辑
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3)
  - **Blocks**: Tasks 4, 5, 6, 7
  - **Blocked By**: None (can start immediately)

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:40` - singleFlight 字段定义

  **API/Type References** (contracts to implement against):
  - `java.util.concurrent.ConcurrentHashMap` - ConcurrentHashMap 类型

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - 无

  **WHY Each Reference Matters** (explain the relevance):
  - 参考项目的 singleFlight 字段定义，确保实现一致

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 字段添加成功
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 singleFlight 字段定义
      2. 验证字段类型为 ConcurrentHashMap<String, Object>
      3. 验证字段已初始化
    Expected Result: 找到 singleFlight 字段定义，类型和初始化正确
    Failure Indicators: 未找到字段定义，或类型/初始化不正确
    Evidence: .sisyphus/evidence/task-1-singleflight-field.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `feat(knowpost): add singleFlight field for cache lock`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 2. 添加 DETAIL_LAYOUT_VER 常量

  **What to do**:
  - 在 `KnowPostServiceImpl` 类中添加 `DETAIL_LAYOUT_VER` 常量
  - 常量类型：`int`
  - 常量值：`1`
  - 常量用途：用于缓存 Key 的版本控制，当详情页布局变化时可以递增此版本号

  **Must NOT do**:
  - 不要修改其他常量
  - 不要添加其他不必要的常量

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: 简单的常量添加，不需要复杂的逻辑
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3)
  - **Blocks**: Tasks 4, 5, 6, 7
  - **Blocked By**: None (can start immediately)

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:39` - DETAIL_LAYOUT_VER 常量定义

  **API/Type References** (contracts to implement against):
  - 无

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - 无

  **WHY Each Reference Matters** (explain the relevance):
  - 参考项目的 DETAIL_LAYOUT_VER 常量定义，确保实现一致

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 常量添加成功
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 DETAIL_LAYOUT_VER 常量定义
      2. 验证常量类型为 int
      3. 验证常量值为 1
    Expected Result: 找到 DETAIL_LAYOUT_VER 常量定义，类型和值正确
    Failure Indicators: 未找到常量定义，或类型/值不正确
    Evidence: .sisyphus/evidence/task-2-detail-layout-ver.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `feat(knowpost): add DETAIL_LAYOUT_VER constant`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 3. 添加 tryProcessCacheHit() 辅助方法

  **What to do**:
  - 在 `KnowPostServiceImpl` 类中添加 `tryProcessCacheHit()` 辅助方法
  - 方法签名：`private KnowPostDetailResponse tryProcessCacheHit(String cached, long id, String pageKey, Long uid, String sourceLog)`
  - 方法功能：处理缓存命中逻辑，包括：
    - 缓存为空，返回 null
    - 命中空值缓存（"NULL"），抛出异常
    - 反序列化缓存数据，填充 L1 缓存，记录热度并续期，返回响应
  - 方法返回值：`KnowPostDetailResponse`（缓存命中时返回响应对象，未命中时返回 null）

  **Must NOT do**:
  - 不要修改其他方法
  - 不要添加其他不必要的方法

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: 简单的辅助方法添加，逻辑清晰
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2)
  - **Blocks**: Tasks 4, 5, 6, 7
  - **Blocked By**: None (can start immediately)

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:446-475` - tryProcessCacheHit() 方法实现

  **API/Type References** (contracts to implement against):
  - `zhiguang.nauy.knowpost.api.dto.KnowPostDetailResponse` - 响应对象类型

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - `com.fasterxml.jackson.databind.ObjectMapper` - JSON 序列化/反序列化

  **WHY Each Reference Matters** (explain the relevance):
  - 参考项目的 tryProcessCacheHit() 方法实现，确保逻辑一致

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 方法添加成功
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 tryProcessCacheHit 方法定义
      2. 验证方法签名正确
      3. 验证方法包含缓存命中逻辑
    Expected Result: 找到 tryProcessCacheHit 方法定义，签名和逻辑正确
    Failure Indicators: 未找到方法定义，或签名/逻辑不正确
    Evidence: .sisyphus/evidence/task-3-tryprocesscachehit-method.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `feat(knowpost): add tryProcessCacheHit() helper method`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 4. 完善 getDetail() 方法 - L2 Redis 缓存

  **What to do**:
  - 在 `getDetail()` 方法中添加 L2 Redis 缓存查询逻辑
  - 缓存 Key 格式：`"knowpost:detail:" + id + ":v" + DETAIL_LAYOUT_VER`
  - 缓存查询逻辑：
    - 从 Redis 中读取缓存数据
    - 调用 `tryProcessCacheHit()` 方法处理缓存命中
    - 如果缓存命中，直接返回响应
  - 缓存写入逻辑（在数据库查询后）：
    - 将响应对象序列化为 JSON
    - 写入 Redis，设置过期时间（60秒 + 随机抖动）
    - 填充 L1 Caffeine 缓存

  **Must NOT do**:
  - 不要修改 L1 Caffeine 缓存逻辑
  - 不要修改 L3 数据库查询逻辑
  - 不要实现第四、五阶段的功能

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `deep`
    - Reason: 需要深入理解缓存逻辑，确保实现正确
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 (with Tasks 5, 6, 7)
  - **Blocks**: Task 8
  - **Blocked By**: Tasks 1, 2, 3

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:332-339` - L2 Redis 缓存查询逻辑
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:413-427` - L2 Redis 缓存写入逻辑

  **API/Type References** (contracts to implement against):
  - `org.springframework.data.redis.core.StringRedisTemplate` - Redis 客户端
  - `com.fasterxml.jackson.databind.ObjectMapper` - JSON 序列化/反序列化

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - Spring Data Redis - Redis 操作
  - Jackson - JSON 序列化/反序列化

  **WHY Each Reference Matters** (explain the relevance):
  - 参考项目的 L2 Redis 缓存逻辑，确保实现一致

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: L2 Redis 缓存查询成功
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 getDetail 方法中的 Redis 缓存查询逻辑
      2. 验证缓存 Key 格式正确
      3. 验证调用了 tryProcessCacheHit() 方法
    Expected Result: 找到 L2 Redis 缓存查询逻辑，Key 格式和调用正确
    Failure Indicators: 未找到缓存查询逻辑，或 Key 格式/调用不正确
    Evidence: .sisyphus/evidence/task-4-l2-redis-cache.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `feat(knowpost): add L2 Redis cache to getDetail()`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 5. 完善 getDetail() 方法 - 单飞锁机制

  **What to do**:
  - 在 `getDetail()` 方法中添加单飞锁机制
  - 单飞锁逻辑：
    - 使用 `singleFlight.computeIfAbsent(pageKey, k -> new Object())` 获取锁对象
    - 使用 `synchronized(lock)` 加锁
    - 锁内再次检查缓存（双重检查）
    - 如果缓存已由其他线程填充，直接返回
    - 数据库查询后，释放锁：`singleFlight.remove(pageKey)`
  - 单飞锁用途：防止高并发下大量请求同时打到数据库（缓存击穿/惊群效应）

  **Must NOT do**:
  - 不要修改其他逻辑
  - 不要实现第四、五阶段的功能

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `deep`
    - Reason: 需要深入理解单飞锁机制，确保实现正确
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 (with Tasks 4, 6, 7)
  - **Blocks**: Task 8
  - **Blocked By**: Tasks 1, 2, 3

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:343-359` - 单飞锁机制实现

  **API/Type References** (contracts to implement against):
  - `java.util.concurrent.ConcurrentHashMap` - ConcurrentHashMap 类型

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - Java 并发编程 - synchronized 关键字

  **WHY Each Reference Matters** (explain the relevance):
  - 参考项目的单飞锁机制实现，确保逻辑一致

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 单飞锁机制添加成功
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 getDetail 方法中的单飞锁逻辑
      2. 验证使用了 singleFlight.computeIfAbsent()
      3. 验证使用了 synchronized(lock)
      4. 验证锁内再次检查缓存（双重检查）
      5. 验证释放锁：singleFlight.remove(pageKey)
    Expected Result: 找到单飞锁机制，所有逻辑正确
    Failure Indicators: 未找到单飞锁机制，或逻辑不正确
    Evidence: .sisyphus/evidence/task-5-singleflight-lock.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `feat(knowpost): add single-flight lock to getDetail()`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 6. 完善 getDetail() 方法 - 缓存穿透防护

  **What to do**:
  - 在 `getDetail()` 方法中添加缓存穿透防护
  - 缓存穿透防护逻辑：
    - 数据库查询后，如果内容不存在或已删除，写入 "NULL" 空值缓存
    - 空值缓存过期时间：30秒 + 随机抖动（31秒）
    - 空值缓存用途：防止查询不存在的数据导致一直打数据库
  - 在 `tryProcessCacheHit()` 方法中处理空值缓存：
    - 如果缓存值为 "NULL"，抛出异常

  **Must NOT do**:
  - 不要修改其他逻辑
  - 不要实现第四、五阶段的功能

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `deep`
    - Reason: 需要深入理解缓存穿透防护机制，确保实现正确
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 (with Tasks 4, 5, 7)
  - **Blocks**: Task 8
  - **Blocked By**: Tasks 1, 2, 3

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:365-370` - 缓存穿透防护实现
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:452-455` - tryProcessCacheHit() 中的空值缓存处理

  **API/Type References** (contracts to implement against):
  - `org.springframework.data.redis.core.StringRedisTemplate` - Redis 客户端

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - Spring Data Redis - Redis 操作

  **WHY Each Reference Matters** (explain the relevance):
  - 参考项目的缓存穿透防护实现，确保逻辑一致

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 缓存穿透防护添加成功
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 getDetail 方法中的缓存穿透防护逻辑
      2. 验证写入 "NULL" 空值缓存
      3. 验证空值缓存过期时间为 30秒 + 随机抖动
      4. 搜索 tryProcessCacheHit() 方法中的空值缓存处理
      5. 验证如果缓存值为 "NULL"，抛出异常
    Expected Result: 找到缓存穿透防护逻辑，所有逻辑正确
    Failure Indicators: 未找到缓存穿透防护逻辑，或逻辑不正确
    Evidence: .sisyphus/evidence/task-6-cache-penetration-protection.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `feat(knowpost): add cache penetration protection to getDetail()`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 7. 完善 getDetail() 方法 - 缓存雪崩防护

  **What to do**:
  - 在 `getDetail()` 方法中添加缓存雪崩防护
  - 缓存雪崩防护逻辑：
    - 写入 Redis 缓存时，添加随机抖动（Jitter）
    - 基础过期时间：60秒
    - 随机抖动：0-30秒
    - 最终过期时间：60-90秒
    - 根据热度检测结果动态调整 TTL（热点内容缓存时间更长）
  - 缓存雪崩防护用途：防止大量缓存同时过期（雪崩）

  **Must NOT do**:
  - 不要修改其他逻辑
  - 不要实现第四、五阶段的功能

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `deep`
    - Reason: 需要深入理解缓存雪崩防护机制，确保实现正确
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 (with Tasks 4, 5, 6)
  - **Blocks**: Task 8
  - **Blocked By**: Tasks 1, 2, 3

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostServiceImpl.java:416-421` - 缓存雪崩防护实现

  **API/Type References** (contracts to implement against):
  - `org.springframework.data.redis.core.StringRedisTemplate` - Redis 客户端
  - `zhiguang.nauy.cache.config.HotKeyDetector` - 热点检测器

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - Spring Data Redis - Redis 操作
  - Java 并发编程 - ThreadLocalRandom

  **WHY Each Reference Matters** (explain the relevance):
  - 参考项目的缓存雪崩防护实现，确保逻辑一致

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 缓存雪崩防护添加成功
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 getDetail 方法中的缓存雪崩防护逻辑
      2. 验证添加了随机抖动（Jitter）
      3. 验证基础过期时间为 60秒
      4. 验证随机抖动为 0-30秒
      5. 验证根据热度检测结果动态调整 TTL
    Expected Result: 找到缓存雪崩防护逻辑，所有逻辑正确
    Failure Indicators: 未找到缓存雪崩防护逻辑，或逻辑不正确
    Evidence: .sisyphus/evidence/task-7-cache-avalanche-protection.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `feat(knowpost): add cache avalanche protection to getDetail()`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 8. 注释掉第四、五阶段的内容

  **What to do**:
  - 在 `getDetail()` 方法中注释掉第四、五阶段的内容
  - 第四阶段内容（需要注释）：
    - 计数查询（CounterService）
    - 用户状态查询（liked/faved）
  - 第五阶段内容（需要注释）：
    - AI 摘要生成
    - 搜索索引更新
    - Outbox 事件写入
  - 注释格式：
    ```java
    // TODO: 第四阶段 - 计数查询（CounterService）
    // TODO: 第四阶段 - 用户状态查询（liked/faved）
    // TODO: 第五阶段 - AI 摘要生成
    // TODO: 第五阶段 - 搜索索引更新
    // TODO: 第五阶段 - Outbox 事件写入
    ```
  - 留足空间：为后续实现预留足够的空间和注释

  **Must NOT do**:
  - 不要删除代码，只注释
  - 不要修改其他逻辑

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: 简单的注释操作，不需要复杂的逻辑
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 3 (with Task 9)
  - **Blocks**: Task 9
  - **Blocked By**: Tasks 4, 5, 6, 7

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - 无（根据用户需求注释）

  **API/Type References** (contracts to implement against):
  - 无

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - 无

  **WHY Each Reference Matters** (explain the relevance):
  - 根据用户需求注释掉第四、五阶段的内容

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 第四、五阶段的内容已注释
    Tool: Bash (grep)
    Preconditions: 代码已修改
    Steps:
      1. 搜索 getDetail 方法中的第四阶段注释
      2. 验证计数查询已注释
      3. 验证用户状态查询已注释
      4. 搜索 getDetail 方法中的第五阶段注释
      5. 验证 AI 摘要生成已注释
      6. 验证搜索索引更新已注释
      7. 验证 Outbox 事件写入已注释
    Expected Result: 找到所有第四、五阶段的注释，内容已注释
    Failure Indicators: 未找到注释，或内容未注释
    Evidence: .sisyphus/evidence/task-8-comment-out-phase4-5.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] grep 输出结果

  **Commit**: NO (groups with Task 9)
  - Message: `chore(knowpost): comment out phase 4 and 5 features`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

- [ ] 9. 代码编译验证

  **What to do**:
  - 运行 `mvn compile` 验证代码编译通过
  - 检查是否有编译错误
  - 如果有编译错误，修复后重新编译

  **Must NOT do**:
  - 不要修改代码逻辑，只修复编译错误

  **Recommended Agent Profile**:
  > Select category + skills based on task domain. Justify each choice.
  - **Category**: `quick`
    - Reason: 简单的编译验证，不需要复杂的逻辑
  - **Skills**: []
    - 不需要特殊技能
  - **Skills Evaluated but Omitted**:
    - 无

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 3 (with Task 8)
  - **Blocks**: F1, F2
  - **Blocked By**: Task 8

  **References** (CRITICAL - Be Exhaustive):

  > The executor has NO context from your interview. References are their ONLY guide.
  > Each reference must answer: "What should I look at and WHY?"

  **Pattern References** (existing code to follow):
  - 无

  **API/Type References** (contracts to implement against):
  - 无

  **Test References** (testing patterns to follow):
  - 无

  **External References** (libraries and frameworks):
  - Maven - 编译工具

  **WHY Each Reference Matters** (explain the relevance):
  - 验证代码编译通过，确保没有语法错误

  **Acceptance Criteria**:

  > **AGENT-EXECUTABLE VERIFICATION ONLY** - No human action permitted.
  > Every criterion MUST be verifiable by running a command or using a tool.

  **If TDD (tests enabled):**
  - [ ] 代码编译通过：`mvn compile`

  **QA Scenarios (MANDATORY - task is INCOMPLETE without these):**

  > **This is NOT optional. A task without QA scenarios WILL BE REJECTED.**
  >
  > Write scenario tests that verify the ACTUAL BEHAVIOR of what you built.
  > Minimum: 1 happy path + 1 failure/edge case per task.
  > Each scenario = exact tool + exact steps + exact assertions + evidence path.
  >
  > **The executing agent MUST run these scenarios after implementation.**
  > **The orchestrator WILL verify evidence files exist before marking task complete.**

  ```
  Scenario: 代码编译通过
    Tool: Bash (mvn compile)
    Preconditions: 代码已修改
    Steps:
      1. 运行 mvn compile
      2. 检查编译输出
      3. 验证没有编译错误
    Expected Result: 编译成功，没有错误
    Failure Indicators: 编译失败，有错误
    Evidence: .sisyphus/evidence/task-9-compile-verification.txt
  ```

  **Evidence to Capture**:
  - [ ] 每个证据文件命名：task-{N}-{scenario-slug}.{ext}
  - [ ] mvn compile 输出结果

  **Commit**: YES
  - Message: `feat(knowpost): complete getDetail() method with 3-tier cache`
  - Files: `src/main/java/zhiguang/nauy/knowpost/service/impl/KnowPostServiceImpl.java`
  - Pre-commit: `mvn compile`

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
>
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.

- [ ] F1. **Plan Compliance Audit** — `quick`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, grep for keywords). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in .sisyphus/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `quick`
  Run `mvn compile` + linter. Review all changed files for: `as any`/`@ts-ignore`, empty catches, console.log in prod, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names (data/result/item/temp).
  Output: `Build [PASS/FAIL] | Lint [PASS/FAIL] | Files [N clean/N issues] | VERDICT`

---

## Commit Strategy

- **9**: `feat(knowpost): complete getDetail() method with 3-tier cache` - KnowPostServiceImpl.java, mvn compile

---

## Success Criteria

### Verification Commands
```bash
mvn compile  # Expected: BUILD SUCCESS
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] Code compiles successfully
