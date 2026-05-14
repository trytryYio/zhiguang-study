# PROJECT KNOWLEDGE BASE

**Generated:** 2026-04-25
**Commit:** c5d7870
**Branch:** master

---

This file provides guidance for AI agents working on this repository.

## Project Overview

**知光 (ZhiGuang)** — A knowledge community platform with AI-powered features. This is the **user's working project** (Java 17, Spring Boot 3.2.4), distinct from the reference project in `源代码/zhiguang_be/` (Java 21).

**Critical**: This project uses **MyBatis-Plus**, not plain MyBatis like the reference project.

## Quick Start

```bash
# Build
mvn clean package

# Run (development)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassNameTest

# Run a single test method
mvn test -Dtest=ClassNameTest#methodName
```

## Local Graphify Usage

- In this repo, use the supported graphify subcommands: `graphify query "..."`, `graphify explain "..."`, `graphify path "A" "B"`, and `graphify update "."`.
- Do **not** use bare `graphify .` here. The reliable local workflow is subcommand-based.
- In OpenCode, prefer the local graphify tools exposed by `.opencode/plugins/graphify.js` instead of relying on `/graphify` slash syntax.
- This environment uses Windows PowerShell 5.1, so command generation must avoid `&&`.

## Architecture

### Package Structure
```
zhiguang.nauy/
├── auth/          — Authentication (JWT, login/register, email verification, refresh tokens)
├── cache/         — Cache layer config + custom hotkey detection
├── common/        — Shared utilities, exception handling, outbox message helpers
├── config/        — Global configs (Caffeine, CORS, MyBatis-Plus, Redisson, thread pools)
├── counter/       — Counting system (likes, favorites, follows) using Redis bitmaps + Lua scripts
├── exception/     — Custom exceptions and error handling
├── knowpost/      — Post/content system (CRUD, feed, RAG integration, AI summary generation)
├── profile/       — User profile management
├── storage/       — OSS file storage (presigned URLs, upload handling)
└── user/          — User domain
```

### Tech Stack
- **Framework**: Spring Boot 3.2.4 + Java 17 + Maven
- **Database**: MySQL 8.0 + MyBatis-Plus 3.5.15
- **Cache**: Redis (Spring Data Redis) + Redisson 3.52.0 (distributed locks) + Caffeine 3.1.8 (local cache)
- **Messaging**: Kafka (async writes, aggregation, outbox pattern)
- **Search**: Elasticsearch 9.2 + Spring AI Vector Store
- **AI/LLM**: Spring AI 1.0.3 with OpenAI + DeepSeek models, RAG pipeline
- **Storage**: Alibaba Cloud OSS (presigned URL + direct upload)
- **Security**: Spring Security + OAuth2 Resource Server + JWT (RS256)
- **Sync**: Canal 1.1.8 (MySQL binlog subscription for outbox → Kafka)
- **API Docs**: Knife4j 4.5.0 (OpenAPI 3)

## Key Design Patterns

### MyBatis-Plus Specifics
- **Logical Delete**: Use `@TableLogic` annotation on `isDelete` field (Boolean, tinyint(1) in MySQL with 0/1)
- **Pagination**: Configured in `MybatisPlusConfig.java` with `PaginationInnerInterceptor(DbType.MYSQL)`
- **Mapper Locations**: `classpath*:mapper/*.xml` (configured in application.yaml)
- **Auto-Generated Methods**: `removeById()`, `updateById()`, `selectById()`, etc. are provided by MyBatis-Plus

### Outbox Pattern
- Follow/like events write to `outbox` table in the same DB transaction
- Canal subscribes to binlog and publishes to Kafka for async propagation
- Filter: `zhiguang\.outbox` (configured in application.yaml)

### Bitmap Counting
- Like/favorite counters use sharded Redis bitmaps with Lua script atomic updates
- Sampling consistency checks and self-healing rebuild
- Three-layer architecture: Fact (bitmap) → Aggregation (Redis Hash) → Summary (SDS fixed structure)

### Feed 3-Level Cache
- L1: Caffeine local cache (80% traffic)
- L2: Redis fragment cache (ids/item/count separately)
- L3: Database (MySQL)
- Hotkey detection extends TTL dynamically
- Single-flight lock prevents cache stampede
- Hour-based sharding reduces cross-hour invalidation risk

### Progressive Publishing
- Posts use OSS presigned URLs for direct frontend upload
- Frontend uploads directly to OSS, then confirms with backend

## Configuration

### Main Config
- **File**: `src/main/resources/application.yaml` (note: `.yaml`, not `.yml`)
- **Database**: MySQL on localhost:3306, database `zg_auth`
- **Redis**: localhost:6379
- **Kafka**: localhost:9092, topic `counter-events`
- **Elasticsearch**: localhost:9200
- **Canal**: localhost:11111, destination `example`

### JWT Keys
- **Location**: `src/main/resources/keys/`
- **Files**: `private.pem`, `public.pem`
- **Config**: `auth.jwt.private-key` and `auth.jwt.public-key` in application.yaml

### MyBatis-Plus Config
- **File**: `src/main/java/zhiguang/nauy/config/MybatisPlusConfig.java`
- **Features**: Pagination interceptor for MySQL

### Caffeine Config
- **File**: `src/main/java/zhiguang/nauy/config/CaffeineConfig.java`
- **Current State**: Has `localCache()` bean but incomplete (missing feed-specific caches)
- **Reference**: See `源代码/zhiguang_be/src/main/java/com/tongji/cache/config/CacheConfig.java` for complete implementation

## Testing

### Test Setup
- **Framework**: JUnit 5 + Spring Boot Test
- **Location**: `src/test/java/zhiguang/nauy/`
- **Existing Test**: `NauyApplicationTests.java` (basic context load test)

### Running Tests
```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=ClassNameTest

# Single test method
mvn test -Dtest=ClassNameTest#methodName
```

## External Services Required

- MySQL 8.0
- Redis
- Kafka
- Elasticsearch 9.2
- Alibaba Cloud OSS bucket
- OpenAI / DeepSeek API keys

## Important Constraints

### MyBatis-Plus vs MyBatis
- **This project uses MyBatis-Plus**, not plain MyBatis
- Use `@TableLogic` for logical delete
- Use auto-generated methods from `BaseMapper<T>`
- Mapper XML files are in `src/main/resources/mapper/`

### Package Structure
- **This project**: `zhiguang.nauy.*`
- **Reference project**: `com.tongji.*`
- Do not copy package names from reference project

### Configuration File
- **This project**: `application.yaml`
- **Reference project**: `application.yml`
- Note the `.yaml` extension

### Java Version
- **This project**: Java 17
- **Reference project**: Java 21

## Common Gotchas

### CaffeineConfig Incomplete
- Current `CaffeineConfig.java` only has `localCache()` bean
- Missing: `feedPublicCache`, `feedMineCache`, `knowPostDetailCache` beans
- Reference: `源代码/zhiguang_be/src/main/java/com/tongji/cache/config/CacheConfig.java`

### KnowPostFeedServiceImpl Incomplete
- `getPublicFeed()` returns null after L1 cache hit
- Missing: L2 Redis fallback, L3 DB fallback, single-flight lock
- Reference: `源代码/zhiguang_be/src/main/java/com/tongji/knowpost/service/impl/KnowPostFeedServiceImpl.java`

### Counter System
- Three-layer architecture: Fact → Aggregation → Summary
- Uses Redis bitmaps for fact layer
- Uses Redis Hash for aggregation layer
- Uses SDS fixed structure for summary layer
- Reference: `源代码/zhiguang_be/src/main/java/com/tongji/counter/`

### HotKeyDetector
- Sliding time window: 60s window / 10s segments = 6 segments
- `record(key)` increments current segment
- `rotate()` runs every 10s via `@Scheduled`
- `heat(key)` = sum of all segments
- `level(key)` maps to NONE/LOW/MEDIUM/HIGH
- `ttlForPublic/ttlForMine(baseTtl, key)` = baseTtl + extendSeconds(level)

## Reference Project

The reference project is in `源代码/zhiguang_be/` and uses:
- Java 21
- Plain MyBatis
- Package structure: `com.tongji.*`
- Configuration: `application.yml`

**Use the reference project for patterns and implementation details, but adapt to this project's stack.**
