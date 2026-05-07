# EXCEPTION MODULE

Centralized exception handling with custom business exceptions and global error response mapping.

## WHERE TO LOOK

- **BusinessException.java**: Custom runtime exception with error code field
- **ErrorCode.java**: Enum of standardized error codes (4xx/5xx patterns)
- **GlobalExceptionHandler.java**: @RestControllerAdvice for exception-to-response mapping
- **ThrowUtils.java**: Utility for conditional exception throwing

## CONVENTIONS

### Exception Throwing
- Use `ThrowUtils.throwIf(condition, errorCode)` for validation checks
- Prefer `BusinessException` over generic `RuntimeException` for business logic errors
- Always include error code via `ErrorCode` enum, not raw integers

### Error Code Design
- 4xxxx: Client errors (params, auth, not found)
- 5xxxx: Server errors (system, operation failures)
- Add domain-specific codes to `ErrorCode` enum, don't invent new ones inline

### Global Handler
- `BusinessException` returns actual error code and message
- `RuntimeException` returns generic `SYSTEM_ERROR` to avoid exposing internals
- All exceptions logged with `@Slf4j` before response

### Response Format
- All errors return `BaseResponse<?>` via `ResultUtils.error()`
- Consistent structure: `{ code, message, data }`

## ANTI-PATTERNS

- Don't throw raw `RuntimeException` with custom messages in business logic
- Don't create new error codes outside `ErrorCode` enum
- Don't expose stack traces or internal system details in error responses
- Don't use `throw new Exception()` - always use typed exceptions
