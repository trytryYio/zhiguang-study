# COMMON MODULE

Shared utilities for API responses, pagination, and common DTOs.

## OVERVIEW

Provides standardized response wrappers, pagination support, and common request DTOs for all API endpoints.

## WHERE TO LOOK

- **BaseResponse.java**: Generic response wrapper with code, data, message fields
- **ResultUtils.java**: Static factory methods for success/error responses
- **PageRequest.java**: Pagination request DTO (extends for list endpoints)
- **DeleteRequest.java**: Standard delete request with id field

## CONVENTIONS

### Response Wrapping
- All API endpoints return `BaseResponse<T>` via `ResultUtils`
- Success: `ResultUtils.success(data)` or `ResultUtils.success()`
- Error: `ResultUtils.error(errorCode)` or `ResultUtils.error(code, message)`
- Never return raw data or custom response classes

### Request DTOs
- Extend `PageRequest` for any paginated list endpoint
- Use `DeleteRequest` for delete operations (single id field)
- All DTOs implement `Serializable` and use `@Data` from Lombok

### Error Code Integration
- `BaseResponse` accepts `ErrorCode` enum from exception package
- Error responses map to `BusinessException` via `GlobalExceptionHandler`

## ANTI-PATTERNS

- Don't create custom response classes - use `BaseResponse<T>`
- Don't return raw objects from controllers - wrap with `ResultUtils`
- Don't hardcode error codes - use `ErrorCode` enum
- Don't add pagination logic manually - extend `PageRequest`
