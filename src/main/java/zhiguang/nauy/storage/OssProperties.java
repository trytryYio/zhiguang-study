package zhiguang.nauy.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * OSS对象存储配置属性类
 * 用于从application.yaml中读取OSS相关配置信息
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    /**
     * OSS服务端点地址
     * 例如：oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * 访问密钥ID
     * 用于身份验证的AccessKey ID
     */
    private String accessKeyId;

    /**
     * 访问密钥密码
     * 用于身份验证的AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * OSS存储桶名称
     * 文件存储的目标Bucket
     */
    private String bucket;

    /**
     * 自定义公开访问域名（可选）
     * 如CDN域名或图片专用访问域名，配置后优先使用此域名生成访问URL
     */
    private String publicDomain;

    /**
     * 默认上传目录
     * 文件上传时的默认文件夹路径，默认为avatars
     */
    private String folder = "avatars";

}
