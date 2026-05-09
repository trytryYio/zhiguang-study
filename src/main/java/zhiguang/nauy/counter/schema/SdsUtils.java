package zhiguang.nauy.counter.schema;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;

/**
 * SDS（Simple Dynamic String）二进制读写工具类。
 *
 * <p>提供大端序整数的读写功能，支持任意字节数，以及 Redis SDS 数据的存取。</p>
 */
public final class SdsUtils {

    private SdsUtils() {}

    /**
     * 从字节数组中按大端序读取一个指定字节数的整数。
     *
     * @param buf        包含二进制数据的字节数组
     * @param offset     起始偏移量
     * @param byteCount  字节数（如 4 表示 32 位，8 表示 64 位）
     * @return 解析后的整数值
     */
    public static long readIntBE(byte[] buf, int offset, int byteCount) {
        if (buf == null || offset < 0 || byteCount <= 0 || offset + byteCount > buf.length) {
            throw new IllegalArgumentException("Invalid parameters for readIntBE");
        }

        long n = 0;
        for (int i = 0; i < byteCount; i++) {
            n = (n << 8) | (buf[offset + i] & 0xFFL);
        }
        return n;
    }

    /**
     * 从字节数组中按大端序读取一个 32 位整数（兼容旧接口）。
     *
     * @param buf    包含二进制数据的字节数组
     * @param offset 起始偏移量
     * @return 解析后的 32 位整数值
     */
    public static long readInt32BE(byte[] buf, int offset) {
        return readIntBE(buf, offset, 4);
    }

    /**
     * 从字节数组中按大端序读取一个 64 位整数。
     *
     * @param buf    包含二进制数据的字节数组
     * @param offset 起始偏移量
     * @return 解析后的 64 位整数值
     */
    public static long readInt64BE(byte[] buf, int offset) {
        return readIntBE(buf, offset, 8);
    }

    /**
     * 将整数以大端序写入字节数组（支持任意字节数）。
     *
     * @param buf        目标字节数组
     * @param offset     写入位置的偏移量
     * @param value      要写入的整数值
     * @param byteCount  字节数（如 4 表示 32 位，8 表示 64 位）
     */
    public static void writeIntBE(byte[] buf, int offset, long value, int byteCount) {
        if (buf == null || offset < 0 || byteCount <= 0 || offset + byteCount > buf.length) {
            throw new IllegalArgumentException("Invalid parameters for writeIntBE");
        }

        // 边界检查：确保值在范围内
        long maxValue = (1L << (byteCount * 8)) - 1;
        long n = Math.max(0, Math.min(value, maxValue));

        // 从高位到低位写入
        for (int i = byteCount - 1; i >= 0; i--) {
            buf[offset + i] = (byte) (n & 0xFF);
            n >>>= 8;
        }
    }

    /**
     * 将 32 位整数以大端序写入字节数组（兼容旧接口）。
     *
     * @param buf    目标字节数组
     * @param offset 写入位置的偏移量
     * @param value  要写入的整数值
     */
    public static void writeInt32BE(byte[] buf, int offset, long value) {
        writeIntBE(buf, offset, value, 4);
    }

    /**
     * 将 64 位整数以大端序写入字节数组。
     *
     * @param buf    目标字节数组
     * @param offset 写入位置的偏移量
     * @param value  要写入的整数值
     */
    public static void writeInt64BE(byte[] buf, int offset, long value) {
        writeIntBE(buf, offset, value, 8);
    }

    /**
     * 从 Redis 读取 SDS 原始二进制数据。
     *
     * @param redis  StringRedisTemplate 实例
     * @param sdsKey SDS 计数器的 Redis Key
     * @return 原始字节数组，若 Key 不存在则返回 null
     */
    public static byte[] getRaw(StringRedisTemplate redis, String sdsKey) {
        String value = redis.opsForValue().get(sdsKey);
        return value != null ? value.getBytes(StandardCharsets.UTF_8) : null;
    }

    /**
     * 将 SDS 二进制数据写入 Redis。
     *
     * @param redis  StringRedisTemplate 实例
     * @param sdsKey SDS 计数器的 Redis Key
     * @param data   待存储的二进制字节数组
     */
    public static void setRaw(StringRedisTemplate redis, String sdsKey, byte[] data) {
        if (data == null || data.length == 0) {
            redis.delete(sdsKey);
            return;
        }
        String value = new String(data, StandardCharsets.UTF_8);
        redis.opsForValue().set(sdsKey, value);
    }
}
