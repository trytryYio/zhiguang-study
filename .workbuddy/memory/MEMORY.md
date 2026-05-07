# 工作记忆

- 2026-04-28：zhiguang_new 头像上传失败的核心根因不是后端 ProfileController 或 OpenAPI 文档本身，而是前端自定义 request.ts 默认强制设置 Content-Type: application/json，且未兼容 FormData / requestType="form"。当前 openapi 生成的 profileController.uploadAvatar 已能生成 FormData，后续应优先检查请求封装层是否兼容生成代码约定。
