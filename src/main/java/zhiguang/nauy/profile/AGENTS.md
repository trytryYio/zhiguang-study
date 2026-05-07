# PROFILE MODULE

## OVERVIEW
User profile management with partial updates, avatar upload, and uniqueness validation.

## STRUCTURE
```
profile/
├── controller/     — REST endpoints (profile update, avatar upload)
├── dto/            — Request/Response records (ProfilePatchRequest, ProfileResponse)
└── service/        — Business logic (ProfileService, ProfileServiceImpl)
```

## WHERE TO LOOK
- **ProfileController**: REST endpoints using `@AuthenticationPrincipal Jwt`
- **ProfileServiceImpl**: Partial update logic with `LambdaUpdateWrapper`
- **ProfilePatchRequest/ProfileResponse**: Record-based DTOs

## CONVENTIONS
- Use `@AuthenticationPrincipal Jwt` + `JwtService.extractUserId()` for user ID extraction
- Partial updates: only non-null/non-blank fields are updated via `LambdaUpdateWrapper`
- zgId (知光号) requires uniqueness validation excluding current user
- Avatar upload uses OSS presigned URLs via `OssStorageService`
- Use `@Transactional` on service methods that modify data
- Return updated snapshot after write (read-after-write pattern)

## ANTI-PATTERNS
- Don't use `BeanUtils.copyProperties()` for partial updates (overwrites nulls)
- Don't skip uniqueness validation for zgId
- Don't update all fields when only some are provided
- Don't return stale data after updates (always re-read)
