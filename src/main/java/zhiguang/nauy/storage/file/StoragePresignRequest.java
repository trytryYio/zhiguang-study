package zhiguang.nauy.storage.file;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 存储预签名请求实体类
 * 用于存储上传预签名相关的请求参数
 */
@Data
@AllArgsConstructor
public class StoragePresignRequest {
    /**
     * 场景标识
     * 可选值：knowpost_content | knowpost_image
     */
    @NotBlank
    private String scene;// knowpost_content |knowpost_image

    /**
     * 帖子ID
     * 使用字符串类型避免前端精度丢失问题
     */
    @NotBlank
    private String postId;// 字符串避免前端精度丢失

    /**
     * 内容类型
     * 指定上传文件的内容类型
     */
    @NotBlank
    private String contentType;

    /**
     * 文件扩展名
     * 可选字段，用于指定文件扩展名
     */
    private String ext;

}