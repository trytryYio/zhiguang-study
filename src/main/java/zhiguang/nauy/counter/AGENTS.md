# COUNTER MODULE

## OVERVIEW
Three-layer counting system using Redis bitmaps for fact layer, Redis Hash for aggregation, and SDS for summary.

## STRUCTURE
```
counter/
├── api/           — REST controllers (ActionController, CounterController)
├── config/        — Counter configuration (CounterConfig)
├── event/         — Kafka events and consumers (CounterEvent, CounterAggregationConsumer, CounterRebuildConsumer)
├── schema/        — Key patterns and schema definitions (CounterKeys, CounterSchema, BitmapShard)
└── service/       — Business logic (CounterService, UserCounterService)
```

## WHERE TO LOOK
- **CounterSchema**: Field index mapping (IDX_LIKE=1, IDX_FAV=2) and schema constants
- **BitmapShard**: Sharding logic (CHUNK_SIZE=32K bits, chunkOf/bitOf helpers)
- **CounterKeys**: Redis key naming conventions for fact/aggregation/summary layers
- **CounterServiceImpl**: Three-layer read/write logic, Lua script execution, rebuild mechanism
- **CounterAggregationConsumer**: Kafka consumer for async aggregation from fact to summary
- **CounterRebuildConsumer**: Self-healing rebuild when summary is missing or inconsistent

## CONVENTIONS
- **Fact Layer**: Sharded bitmaps with key pattern `counter:fact:{entityType}:{entityId}:{metric}:{chunkId}`
- **Aggregation Layer**: Redis Hash with key `counter:agg:{entityType}:{entityId}`, fields = metric names
- **Summary Layer**: SDS binary with key `counter:summary:{entityType}:{entityId}`, fixed 4-byte Int32 per field
- **Lua Scripts**: Use `toggleScript` for atomic bitmap operations, returns 1 only on state change
- **Event Publishing**: Emit CounterEvent only when bitmap state changes (idempotent)
- **Rebuild Lock**: Use Redisson distributed lock with rate limiting (3 permits/10s) and exponential backoff

## ANTI-PATTERNS
- NEVER read summary directly without fallback to aggregation and fact layers
- NEVER bypass Lua scripts for bitmap updates (must be atomic)
- NEVER trigger rebuild without rate limiting (prevents thundering herd)
- NEVER use non-sharded bitmaps (always use BitmapShard.chunkOf/bitOf)
- NEVER mix metric indices (use CounterSchema.IDX_* constants)
