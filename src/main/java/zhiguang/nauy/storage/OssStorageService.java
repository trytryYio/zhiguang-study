package zhiguang.nauy.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;

import lombok.RequiredArgsConstructor;
import zhiguang.nauy.exception.BusinessException;
import zhiguang.nauy.exception.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zhiguang.nauy.storage.OssProperties;

import java.io.IOException;
import java.time.Instant;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云OSS存储服务
 */
@Service
@RequiredArgsConstructor
public class OssStorageService {

    private final OssProperties props;

    /**
     * 上传头像
     * 
     * @param userId 用户ID
     * @param file   文件
     * @return 头像URL
     */

    public String uploadAvatar(long userId, MultipartFile file) {
        ensureConfigured();// 确保配置 属性不为空

        String original = file.getOriginalFilename();// 获取原始文件名
        String ext = "";// 文件扩展名
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));// 获取文件扩展名substring 效果: .jpg
        }
//        String objectKey = "avatars/" + userId + "-" + Instant.now().toEpochMilli() + ext;
//        也就是 avatars/userId-时间戳.jpg
        String objectKey = props.getFolder() + "/" + userId + "-" + Instant.now().toEpochMilli() + ext;
//        创建OSSClient实例
        OSS client = new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKeyId(),
                props.getAccessKeySecret());

        try {
            // 上传文件
            PutObjectRequest request = new PutObjectRequest(props.getBucket(), objectKey, file.getInputStream());
            client.putObject(request);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像文件读取失败");
        } finally {
            client.shutdown();
        }
        // 返回外链
        return publicUrl(objectKey);
    }

    /**
     * 获取文件外链
     *
     * @param objectKey
     * @return
     */

    public String publicUrl(String objectKey) {
        if (props.getPublicDomain() != null && !props.getPublicDomain().isBlank()) {
            // 如果配置了公共域名，则返回该域名
//            `replaceAll("/$", "")` 是去掉域名末尾的斜杠，避免拼接出双斜杠
            return props.getPublicDomain().replaceAll("/$", "") + "/" + objectKey;
        }
        // 如果没有配置公共域名，则返回 OSS 域名
        return "https://" + props.getBucket() + "." + props.getEndpoint() + "/" + objectKey;
    }

    /**
     * 生成用于直传的 PUT 预签名 URL。
     * 客户端必须在上传时设置与签名一致的 Content-Type。
     *
     * @param objectKey        目标对象键
     * @param contentType      上传内容类型（如 text/markdown, image/png）
     * @param expiresInSeconds 有效期秒数（建议 300-900）
     * @return 可直接用于 PUT 上传的预签名 URL
     */
    public String generatePresignedPutUrl(String objectKey, String contentType, int expiresInSeconds) {
        ensureConfigured();
        OSS client = new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKeyId(),
                props.getAccessKeySecret());
        try {
            Date expiration = new Date(System.currentTimeMillis() + expiresInSeconds * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(props.getBucket(), objectKey,
                    HttpMethod.PUT);
            request.setExpiration(expiration);
            if (contentType != null && !contentType.isBlank()) {
                request.setContentType(contentType);
            }
            URL url = client.generatePresignedUrl(request);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    /**
     * 确保配置已正确设置
     */
    private void ensureConfigured() {
        if (props.getEndpoint() == null || props.getAccessKeyId() == null || props.getAccessKeySecret() == null
                || props.getBucket() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "对象存储未配置");
        }
    }
}
