# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Project Overview

**知光 (ZhiGuang)** — A knowledge community platform (similar to a content-sharing social app) with AI-powered features. Users can publish posts, follow others, like/favorite content, and use RAG-based Q&A.

This is a **monorepo-style** working directory containing three sub-projects:

| Path | Description |
|---|---|
| `源代码/zhiguang_be/` | Main backend (Spring Boot 3.2, Java 21) — the primary codebase |
| `源代码/zhiguang_fe/` | Frontend (React 18 + Vite + TypeScript) |
| `pom.xml` (root) | A smaller backend variant (`nauy-zhiguang`, Java 17) — likely a learning/experiment copy |

## Backend (`源代码/zhiguang_be/`)

### Tech Stack
- **Framework**: Spring Boot 3.2 + Java 21 + Maven
- **Database**: MySQL 8.0 + MyBatis
- **Cache**: Redis (Spring Data Redis) + Redisson (distributed locks) + Caffeine (local cache)
- **Messaging**: Kafka (async writes, aggregation, outbox pattern)
- **Search**: Elasticsearch 9.2 + Spring AI Vector Store
- **AI/LLM**: Spring AI with OpenAI + DeepSeek models, RAG pipeline
- **Storage**: Alibaba Cloud OSS (presigned URL + direct upload)
- **Security**: Spring Security + OAuth2 Resource Server + JWT (RS256)
- **Sync**: Canal (MySQL binlog subscription for outbox → Kafka)

### Package Architecture
```
com.tongji/
├── auth/          — Authentication (JWT, login/register, email verification, refresh tokens)
├── cache/         — Cache layer config + custom hotkey detection
├── common/        — Shared utilities, exception handling, outbox message helpers
├── config/        — Global configs (ES, Redisson, thread pools)
├── counter/       — Counting system (likes, favorites, follows) using Redis bitmaps + Lua scripts
├── knowpost/      — Post/content system (CRUD, feed, RAG integration, AI summary generation)
├── llm/           — LLM integration (RAG index/query, AI description generation)
├── profile/       — User profile management
├── relation/      — Follow/unfollow system (outbox + Canal → Kafka async propagation)
├── search/        — Elasticsearch search + autocomplete
├── storage/       — OSS file storage (presigned URLs, upload handling)
└── user/          — User domain
```

### Key Design Patterns
- **Outbox Pattern**: Follow/like events write to `outbox` table in the same DB transaction. Canal subscribes to binlog and publishes to Kafka for async propagation.
- **Bitmap Counting**: Like/favorite counters use sharded Redis bitmaps with Lua script atomic updates, sampling consistency checks, and self-healing rebuild.
- **Feed 3-Level Cache**: Caffeine (L1) → Redis page cache (L2) → Redis fragment cache (L3) with hotkey detection and single-flight deduplication.
- **Progressive Publishing**: Posts use OSS presigned URLs for direct frontend upload, then confirm with backend.

### Common Commands
```bash
cd 源代码/zhiguang_be

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

### Configuration
- Main config: `src/main/resources/application.yml`
- Database schema: `db/schema.sql`
- RSA keys for JWT: `src/main/resources/keys/`

## Frontend (`源代码/zhiguang_fe/`)

### Tech Stack
- React 18 + TypeScript + Vite 5
- React Router 6 (routes: `/`, `/search`, `/create`, `/learn`, `/profile`, `/post/:id`, `/login`, `/register`)
- CSS Modules for styling
- react-markdown + remark-gfm for Markdown rendering
- @coze/api for AI chat integration

### Component Structure
```
src/
├── App.tsx                 — Router definition
├── context/AuthContext.tsx — Auth state provider
├── components/
│   ├── cards/CourseCard    — Post/content card
│   ├── common/             — Shared UI (FollowButton, LikeFavBar, SearchBar, Tags, etc.)
│   ├── icons/Icon          — Icon component
│   └── layout/             — AppLayout, MainHeader, Sidebar
├── features/auth/          — Auth status display
└── pages/                  — Page components (Home, Search, Create, Profile, Login, etc.)
```

### Common Commands
```bash
cd 源代码/zhiguang_fe

# Install dependencies
npm install

# Dev server
npm run dev

# Build
npm run build

# Type check
npm run lint

# Preview production build
npm run preview
```

## Database

Run `db/new.sql` for the schema. Key tables: `users`, `know_posts`, `outbox`, `following`, `follower`, `login_logs`.

## External Services Required

- MySQL 8.0
- Redis
- Kafka
- Elasticsearch
- Alibaba Cloud OSS bucket
- OpenAI / DeepSeek API keys

## Coding Guidelines

Derived from Andrej Karpathy's observations on LLM coding pitfalls.

### 1. Think Before Coding

- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If code could be much shorter, rewrite it.

### 3. Surgical Changes

- Touch only what's necessary. Clean up only your own mess.
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

### 4. Goal-Driven Execution

- Transform tasks into verifiable criteria before implementing.
- For multi-step tasks, state a brief plan with verification checkpoints.
- Loop independently until criteria are met.
