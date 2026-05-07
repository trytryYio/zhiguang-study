# AUTH MODULE

Authentication and authorization layer with JWT-based stateless auth, email verification, and session management.

## STRUCTURE

```
auth/
├── api/           — REST controllers and DTOs (login, register, token refresh, password reset)
├── audit/         — Login logging and audit trail (LoginLogs domain + service)
├── config/        — Security config, JWT encoder/decoder, auth properties, PEM key utilities
├── model/         — Value objects (ClientInfo, IdentifierType)
├── service/       — Core auth business logic (AuthService)
├── token/         — JWT issuance, validation, refresh token store (Redis whitelist)
└── verdication/   — Email verification code generation, storage, rate limiting
```

## WHERE TO LOOK

- **JWT flow**: `JwtService.issueTokenPair()` → `AuthService.register()/login()` → token storage
- **Refresh token whitelist**: `RedisRefreshTokenStoreImpl` (key: `auth:rt:{userId}:{jti}`)
- **Verification rate limits**: `VerificationService.enforceSendInterval()` (60s) and `enforceDailyLimit()` (10/day)
- **Security rules**: `SecurityConfig.securityFilterChain()` — public endpoints vs authenticated
- **Password reset**: `AuthService.resetPassword()` — revokes ALL refresh tokens for user

## CONVENTIONS

- **JWT claims**: `uid` (user ID), `token_type` (access/refresh), `jti` (refresh token ID for whitelist)
- **Token rotation**: Refresh always revokes old token, issues new pair, updates whitelist
- **Verification storage**: Redis Hash `auth:code:{scene}:{identifier}` with fields: code, maxAttempts, attempts
- **Client info extraction**: Three-layer fallback: X-Forwarded-For → X-Real-IP → getRemoteAddr()
- **Password policy**: Must contain letters + numbers, min length from `authProperties.password.minLength`

## ANTI-PATTERNS

- **Never** store refresh tokens in database — use Redis whitelist only
- **Never** use access tokens for refresh — check `token_type` claim first
- **Never** skip verification code validation in register/login/reset flows
- **Never** allow password reset without revoking all existing refresh tokens
- **Never** expose raw password hashes — always use BCrypt encoder
