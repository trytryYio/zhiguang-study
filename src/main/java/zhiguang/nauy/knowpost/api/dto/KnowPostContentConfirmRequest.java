package zhiguang.nauy.knowpost.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 内容上传确认请求DTO
 *
 * @param objectKey OSS对象键
 * @param etag      文件ETag标识
 * @param size      文件大小（字节）
 * @param sha256    文件SHA256哈希值
 */
@Data
public class KnowPostContentConfirmRequest {
    @NotBlank String objectKey;
    @NotBlank String etag;
    @NotNull Long size;
    @NotBlank String sha256;
}