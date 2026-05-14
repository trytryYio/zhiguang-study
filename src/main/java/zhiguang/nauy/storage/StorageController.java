package zhiguang.nauy.storage;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zhiguang.nauy.auth.token.JwtService;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.exception.BusinessException;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import zhiguang.nauy.knowpost.mapper.KnowPostsMapper;
import zhiguang.nauy.storage.dto.StoragePresignRequest;
import zhiguang.nauy.storage.file.StoragePresignResponse;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@Validated
@RequiredArgsConstructor
public class StorageController {

    private final OssStorageService ossStorageService;
    private final JwtService jwtService;
    private final KnowPostsMapper KnowPostsMapper;

    /**
     * 获取用于直传的 PUT 预签名 URL。
     */
    @PostMapping("/presign")
    public BaseResponse<StoragePresignResponse> presign(@Valid @RequestBody StoragePresignRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);

        long postId;
        try {
            postId = Long.parseLong(request.postId());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "postId 非法");
        }

        // 权限校验：postId 必须属于当前用户
        KnowPosts post = KnowPostsMapper.findById(postId);
        if (post == null || post.getCreatorId() == null || post.getCreatorId() != userId) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿不存在或无权限");
        }

        String scene = request.scene();
        String objectKey;
        BaseResponse<String> response = normalizeExt(request.ext(), request.contentType(), scene);
        String ext =response.getData().toString();

        if ("KnowPosts_content".equals(scene)) {
            objectKey = "posts/" + postId + "/content" + ext;
        } else if ("KnowPosts_image".equals(scene)) {
            String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC")).format(Instant.now());
            String rand = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
            objectKey = "posts/" + postId + "/images/" + date + "/" + rand + ext;
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的上传场景");
        }

        int expiresIn = 600; // 10 分钟
        String putUrl = ossStorageService.generatePresignedPutUrl(objectKey, request.contentType(), expiresIn);
        Map<String, String> headers = Map.of("Content-Type", request.contentType());
        return ResultUtils.success( new StoragePresignResponse(objectKey, putUrl, headers, expiresIn));
    }

    private BaseResponse<String>  normalizeExt(String ext, String contentType, String scene) {
        if (ext != null && !ext.isBlank()) {
            return ResultUtils.success(ext.startsWith(".") ? ext : "." + ext);
        }
        if ("KnowPosts_content".equals(scene)) {
            return ResultUtils.success( switch (contentType) {
                case "text/markdown" -> ".md";
                case "text/html" -> ".html";
                case "text/plain" -> ".txt";
                case "application/json" -> ".json";
                default -> ".bin";
            });
        } else {
            return ResultUtils.success(switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".img";
            });
        }
    }
}
