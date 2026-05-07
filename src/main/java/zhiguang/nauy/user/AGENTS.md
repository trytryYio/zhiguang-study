# USER MODULE

## OVERVIEW
User domain with CRUD operations, phone/email lookup, and profile management.

## STRUCTURE
```
user/
├── domain/       — User entity with Lombok + MyBatis-Plus annotations
├── mapper/       — UserMapper extends BaseMapper<User> with custom queries
└── service/      — UserService interface + UserServiceImpl implementation
```

## WHERE TO LOOK
- **UserServiceImpl** — Business logic patterns, validation, transaction handling
- **User** entity — Field mappings, convertToProfileResponse() method
- **UserMapper** — Custom queries (e.g., zgId uniqueness check)

## CONVENTIONS
- Use **LambdaQueryWrapper** for type-safe queries (preferred over QueryWrapper)
- Use **ThrowUtils.throwIf()** for validation and error handling
- Use **@Transactional(readOnly = true)** for read operations
- Use **lambdaUpdate()** for conditional updates with null checks
- Entity methods include **convertToProfileResponse()** for DTO conversion
- Use **Date** (not LocalDateTime) for timestamps with **Instant.now()**
- Set **updatedAt** on all update operations

## ANTI-PATTERNS
- Don't use QueryWrapper when LambdaQueryWrapper is available
- Don't forget to set updatedAt on updates
- Don't skip validation with ThrowUtils.throwIf()
- Don't use updateById() when lambdaUpdate() provides conditional updates
