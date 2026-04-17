package zhiguang.nauy.common;

import lombok.Data;
import zhiguang.nauy.exception.ErrorCode;

import java.io.Serializable;

/**
 * 全局响应封装类
 *
 * @param <T> 对每个接口的返回值进行封装, 统一每个接口的返回结果
 */

@Data
public class BaseResponse<T> implements Serializable {
    private int code;    // 状态码
    private T data;      // 数据
    private String message; // 描述信息

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
