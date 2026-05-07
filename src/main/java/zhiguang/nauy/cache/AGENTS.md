# CACHE MODULE

**OVERVIEW**: Cache layer configuration with Caffeine local cache, hotkey detection, and Elasticsearch client setup.

## STRUCTURE

```
cache/
└── config/
    ├── CacheConfig.java          — Caffeine L2 cache beans (feedPublic, feedMine, knowPostDetail)
    ├── CacheProperties.java      — Configuration properties binding (cache.* prefix)
    ├── HotKeyDetector.java       — Sliding window hotkey detection
    ├── ElasticsearchConfig.java  — Elasticsearch client bean
    └── EsProperties.java         — Elasticsearch properties (spring.elasticsearch.*)
```

## WHERE TO LOOK

- **Cache beans**: `CacheConfig.java` — Three Caffeine caches with configurable TTL and max size
- **Hotkey detection**: `HotKeyDetector.java` — Sliding window algorithm for dynamic TTL extension
- **Configuration**: `CacheProperties.java` — Nested static classes for L2 cache and hotkey settings
- **Elasticsearch**: `ElasticsearchConfig.java` — Client setup with optional authentication

## CONVENTIONS

- **Caffeine beans**: Use `@Bean("beanName")` with descriptive names (feedPublicCache, feedMineCache, knowPostDetailCache)
- **Properties binding**: Use `@ConfigurationProperties(prefix = "...")` with nested static inner classes
- **Hotkey levels**: NONE/LOW/MEDIUM/HIGH mapped from segment sum thresholds
- **TTL extension**: `ttlForPublic/ttlForMine(baseTtl, key)` adds extendSeconds based on hotkey level

## ANTI-PATTERNS

- **Don't hardcode TTL**: Use `CacheProperties` for all cache configuration values
- **Don't skip hotkey detection**: Always call `record(key)` before cache operations
- **Don't ignore segment rotation**: `@Scheduled` method must run every segmentSeconds
- **Don't create cache beans without properties**: Inject `CacheProperties` for configuration
