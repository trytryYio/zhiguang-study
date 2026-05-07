# KNOWPOST MODULE

Post/content system with RAG integration, AI summary generation, and 3-level feed caching.

## STRUCTURE

```
knowpost/
├── api/           — REST controllers + DTOs (requests/responses)
├── domain/        — Entities (KnowPosts), row mappers (FeedRow, DetailRow), ID generator
├── mapper/        — MyBatis-Plus mappers (extends BaseMapper)
└── service/       — Service interfaces + implementations
```

## WHERE TO LOOK

- **Feed logic**: `KnowPostFeedServiceImpl` — 3-level cache, hotkey detection, hour-based sharding
- **CRUD operations**: `KnowPostServiceImpl` — draft creation, content confirmation, publishing
- **Entity model**: `KnowPosts` — main table with OSS metadata, visibility, status fields
- **API contracts**: `api/dto/` — request/response DTOs for all endpoints

## CONVENTIONS

- **Entity**: Use `@TableName("know_posts")`, `@TableId(type = IdType.INPUT)` for snowflake IDs, `@TableLogic` on `isDelete`
- **Mapper**: Extend `BaseMapper<KnowPosts>`, annotate with `@Mapper`
- **Service**: Extend `ServiceImpl<Mapper, Entity>`, inject `Cache<String, FeedPageResponse>` for L1 cache
- **Feed cache keys**: `feed:public:{size}:{page}:v{version}` (public), `feed:mine:{userId}:{size}:{page}` (personal)
- **Hour-based sharding**: `feed:public:ids:{size}:{hourSlot}:{page}` to reduce cross-hour invalidation risk
- **Hotkey TTL**: Use `HotKeyDetector.record(key)` and `ttlForPublic/ttlForMine(baseTtl, key)` for dynamic extension

## ANTI-PATTERNS

- Don't return null after L1 cache hit — implement L2 Redis fallback and L3 DB fallback
- Don't skip hotkey recording — call `recordItemHotKey(item.id())` for each feed item
- Don't mix user-specific state in shared cache — use `enrich()` to overlay liked/faved per user
- Don't ignore pagination limits — clamp size to 1-50, page to >=1
- Don't forget OSS metadata — store objectKey, etag, size, sha256 after content confirmation
