package zhiguang.nauy.storage.file;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor

public class StoragePresignResponse {
    /**
     * 对象键名，表示要上传或访问的对象在存储系统中的唯一标识
     */
    private String objectKey;

    /**
     * 预签名的PUT请求URL，客户端可使用此URL直接上传文件到存储服务
     */
    private String putUrl;

    /**
     * 请求头信息，包含预签名操作所需的各种HTTP头字段
     */
    private Map<String, String> headers;

    /**
     * 过期时间，表示预签名URL的有效期限（单位：秒）
     */
    private int expire;
}
