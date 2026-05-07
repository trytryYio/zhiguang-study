# CONFIG MODULE

## OVERVIEW
Global infrastructure configuration beans for caching, database, async execution, and distributed locks.

## STRUCTURE
- MybatisPlusConfig: MyBatis-Plus pagination interceptor
- ThreadPoolConfig: Async task executor customization
- RedissionConfig: Redisson distributed lock client
- CaffeineConfig: Local cache (deprecated, incomplete)
- CorsConfig: CORS filter (deprecated, handled by Security)

## WHERE TO LOOK
- **Pagination**: MybatisPlusConfig.java - MySQL pagination interceptor
- **Async execution**: ThreadPoolConfig.java - custom thread pool for @Async
- **Distributed locks**: RedissionConfig.java - Redisson client with watchdog timeout
- **Local cache**: CaffeineConfig.java - basic localCache bean (incomplete, missing feed-specific caches)

## CONVENTIONS
- Use @Configuration for all config classes
- Bean methods return infrastructure components (Cache, RedissonClient, etc.)
- Read timeouts and pool sizes from application.yaml
- Redisson uses single-server mode with lock watchdog timeout

## ANTI-PATTERNS
- Don't add new beans to CaffeineConfig - it's deprecated and incomplete
- Don't use CorsConfig - CORS is handled by Security configuration
- Don't hardcode Redis connection details - use RedisProperties
