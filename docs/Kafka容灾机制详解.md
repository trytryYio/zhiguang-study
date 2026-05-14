# Apache Kafka 容灾机制详解

## 一、项目中的 Kafka 概览

本项目使用了两个 Kafka Topic，实现两个不同的业务场景：

| Topic | 生产者 | 消费者（组） | 用途 |
|---|---|---|---|
| `canal-outbox` | CanalKafkaBridge | CanalOutboxConsumer（关注关系）, CanalOutboxConsumerSearch（ES索引） | Canal binlog → 事件驱动 |
| `counter-events` | CounterEventProducer | CounterAggregationConsumer（聚合）, CounterRebuildConsumer（重建） | 点赞/收藏计数器 |

---

## 二、Kafka 容灾是什么？

Kafka 容灾是指当系统出现故障（网络抖动、进程崩溃、消息处理失败等）时，Kafka 能够**保证消息不丢失、不重复**，并能**自动恢复**的能力。

在本项目中，容灾体现在以下层面：

### 2.1 生产者端容灾

| 机制 | 配置/代码 | 作用 |
|---|---|---|
| **acks=all** | `application.yaml: acks: all` | 生产者发送消息后，等待**所有 ISR 副本**都确认后才认为发送成功。防止单节点宕机导致消息丢失 |
| **幂等生产者** | `application.yaml: enable.idempotence: true` | 启用幂等性，保证消息在重试时不会产生重复。每个生产者分配唯一 PID，消息带序列号，broker 去重 |
| **严格有序** | `application.yaml: max.in.flight.requests.per.connection: 1` | 同一连接最多1个未确认请求，保证消息严格有序（配合幂等性使用） |
| **自动重试** | `application.yaml: retries: 3` | 发送失败时自动重试3次 |

### 2.2 消费者端容灾

| 机制 | 配置/代码 | 作用 |
|---|---|---|
| **手动提交 offset** | `application.yaml: enable-auto-commit: false, ack-mode: manual` | 消费者处理完业务逻辑后才提交 offset。如果处理失败，不提交，Kafka 会重新投递 |
| **处理失败不 ack** | 所有 Consumer 的 catch 块中不调用 `ack.acknowledge()` | 消息处理异常时，Kafka 认为消费未完成，会重新投递该消息 |
| **幂等消费** | `RelationEventProcessor` 使用 Redis SETNX 去重，10分钟 TTL | 即使 Kafka 重复投递，业务层也能去重，防止重复处理 |
| **Bitmap 幂等** | `CounterServiceImpl` Lua 脚本返回状态变化标志 | 只有状态真正改变时才发送 Kafka 事件，从源头保证幂等 |

### 2.3 Kafka Broker 端容灾（基础设施层）

| 机制 | 说明 |
|---|---|
| **副本机制** | Topic 可配置多个副本（replication factor），分布在不同 broker 上 |
| **ISR 集合** | 与 leader 保持同步的副本集合，`acks=all` 要求所有 ISR 确认 |
| **Controller 选举** | broker 宕机时，Kafka 自动选举新 controller，重新分配分区 leader |
| **日志段压缩** | 某些 topic 可启用 compaction，保留每个 key 的最新值 |

---

## 三、本项目的 Kafka 容灾实现详解

### 3.1 Canal Outbox Topic 的容灾

这是最关键的容灾场景——保证知文发布后一定能写入 ES 索引。

```
                     生产者容灾                           消费者容灾
                  ┌─────────────┐                    ┌─────────────────┐
                  │ acks=all    │                    │ 手动 ack        │
                  │ retries=3   │                    │ 失败不 ack      │
                  │ idempotence │                    │ Kafka 自动重投  │
                  └──────┬──────┘                    └────────┬────────┘
                         │                                    │
  MySQL binlog → Canal → CanalKafkaBridge → Kafka → CanalOutboxConsumerSearch → ES
                         │                                    │
                         │              ┌─────────────────────┤
                         │              │                     │
                         │        处理成功:               处理失败:
                         │        ack.acknowledge()      不调用 ack
                         │        → 提交 offset          → Kafka 重新投递
                         │        → 消息消费完成          → 消息回到队列
                         │                                → 等待下次消费
```

**故障场景1：ES 临时不可用**
1. Consumer 调用 `searchIndexService.upsertKnowPost()` 抛异常
2. catch 块中不调用 `ack.acknowledge()`
3. Kafka 认为消息未消费，稍后重新投递
4. ES 恢复后，消息被成功处理

**故障场景2：Consumer 进程崩溃**
1. 进程崩溃时，offset 没有提交
2. Consumer 重启后，Kafka 从上次提交的 offset 重新投递
3. 因为 upsert 是幂等操作（用 postId 作为文档 ID），重复执行不会产生脏数据

**故障场景3：Canal 临时断开**
1. Canal 支持断点续传（通过 batchId + ack 机制）
2. CanalKafkaBridge 在处理完一批消息后才 ack Canal
3. 如果 CanalKafkaBridge 崩溃，Canal 会重新投递未 ack 的批次

### 3.2 Counter Events Topic 的容灾

计数器使用三层存储架构，容灾机制更为复杂：

```
Layer 1: Redis Bitmaps (实时读取，性能最高)
    ↑ flush(1s)
Layer 2: Redis Hash 聚合桶 (临时缓冲)
    ↑ Kafka 消费
Layer 3: Kafka Events (持久化存储，可回放)
```

**正常写入流程：**
```
用户点赞 → CounterServiceImpl.toggle()
  → Lua 脚本原子操作 Bitmap (SETBIT/CLEARBIT)
  → Lua 返回 1（状态改变）→ 发送 Kafka 事件
  → CounterAggregationConsumer 消费 → Redis Hash HINCRBY
  → 每1秒 flush → Lua 脚本将 delta 折叠到 SDS 二进制计数器
```

**容灾重建流程（灾后恢复）：**
```
Redis 数据丢失 → 开启重建: counter.rebuild.enabled=true
  → CounterRebuildConsumer 从 earliest offset 回放所有历史事件
  → 逐条执行 Lua 脚本，重新折叠到 SDS
  → 重建完成 → 关闭重建: counter.rebuild.enabled=false
```

**重建的保护机制：**

| 机制 | 代码位置 | 作用 |
|---|---|---|
| 分布式锁 | `CounterServiceImpl:158-173` | 防止多个实例同时重建同一个 entity |
| 读前检查 | `CounterServiceImpl:187-190` | 获取锁后重新读取 SDS，如果已恢复则跳过重建 |
| 限流器 | `CounterServiceImpl:370-381` | Redisson RateLimiter: 每10秒3个请求/entity，防止惊群效应 |
| 指数退避 | `CounterServiceImpl:317-346` | 重建失败后，等待时间翻倍（500ms → 1s → 2s → ... → 30s），避免频繁重试 |
| 条件开关 | `counter.rebuild.enabled: false` | 默认关闭，仅在灾后手动开启 |

---

## 四、故障场景模拟与恢复

### 场景1：Kafka Broker 宕机重启

| 阶段 | 行为 |
|---|---|
| 宕机时 | Producer: acks=all 会阻塞，发送超时后报错。Consumer: 处理中的消息不会 ack，连接断开 |
| 重启后 | Producer: 自动重连，重试3次。Consumer: 重新分配 partition，从上次提交的 offset 继续消费 |
| 数据影响 | 零丢失（acks=all + 手动ack）。可能有少量重复（幂等机制去重） |

### 场景2：Consumer 消息处理失败

| 阶段 | 行为 |
|---|---|
| 失败时 | Consumer 不调用 ack，Kafka 保持消息在队列中 |
| 重试 | Kafka 自动重投递（取决于 consumer 配置的重试次数和间隔） |
| 数据影响 | 消息最终被成功处理，零丢失 |

### 场景3：Canal Kafka Bridge 进程崩溃

| 阶段 | 行为 |
|---|---|
| 崩溃时 | Canal 连接断开，未 ack 的 batch 回到队列 |
| 重启后 | CanalKafkaBridge 重新连接 Canal，从上次 batchId 继续消费 |
| 数据影响 | Canal 的 batchId 机制保证 at-least-once，重复消息由下游幂等消费去重 |

### 场景4：Redis 全部数据丢失

| 阶段 | 行为 |
|---|---|
| 丢失时 | SDS 计数器、Bitmap、ZSet 缓存全部清零 |
| 恢复步骤 | 1. 设置 `counter.rebuild.enabled: true`<br>2. 重启应用<br>3. CounterRebuildConsumer 从 earliest 回放所有 Kafka 事件<br>4. 逐条重建 SDS 计数器<br>5. 重建完成后设置 `counter.rebuild.enabled: false` |
| 数据影响 | 计数器数据可完整恢复（Kafka 保留了所有历史事件） |

---

## 五、Kafka 容灾与业务模块的关系

```
┌─────────────────────────────────────────────────────────────┐
│                    Kafka 容灾体系                             │
├──────────────────────┬──────────────────────────────────────┤
│    canal-outbox      │         counter-events               │
│   (事件驱动容灾)      │       (计数器容灾)                    │
├──────────────────────┼──────────────────────────────────────┤
│ 生产者: CanalKafka   │ 生产者: CounterEvent                 │
│   acks=all           │   acks=all                           │
│   retries=3          │   retries=3                          │
│   idempotence=true   │   idempotence=true                   │
├──────────────────────┼──────────────────────────────────────┤
│ 消费者1: 关注关系     │ 消费者1: CounterAggregation           │
│   Redis SETNX 去重   │   Redis Hash 聚合                    │
│   失败不 ack         │   1s 定时 flush + 失败重试            │
├──────────────────────┼──────────────────────────────────────┤
│ 消费者2: ES 索引      │ 消费者2: CounterRebuild (容灾)        │
│   upsert 幂等        │   从 earliest 回放重建                │
│   失败不 ack         │   分布式锁 + 限流 + 指数退避           │
├──────────────────────┼──────────────────────────────────────┤
│ 下游: ES + MySQL     │ 下游: Redis SDS + Bitmap             │
│   (Canal断点续传)     │   (可完整重建)                        │
└──────────────────────┴──────────────────────────────────────┘
```

---

## 六、总结

本项目的 Kafka 容灾设计遵循了以下核心原则：

1. **At-Least-Once 投递**：生产者 acks=all + 消费者手动 ack，保证消息不丢失
2. **幂等消费**：Redis SETNX 去重、Bitmap Lua 原子操作、ES upsert，保证重复消息不产生副作用
3. **可回放重建**：Kafka 保留所有历史事件，Redis 数据丢失时可从 earliest 回放完整重建
4. **渐进式恢复**：分布式锁 + 限流 + 指数退避，避免重建时的惊群效应和资源争用
5. **无死信队列**：本项目未实现 DLQ（Dead Letter Queue），失败消息依赖 Kafka 自身重试机制

这些机制共同保证了系统在各种故障场景下的数据一致性和可用性。
