package zhiguang.nauy.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Description: // 类说明，在创建类时要填写
 * @ClassName: OssStorageService    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/20 17:00   // 时间
 * @Version: 1.0     // 版本
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;
    private String publicDomain; // 可以选 如自定义cdn 域名,图片访问域名
    private String folder="avatars"; //默认上传目录
}
