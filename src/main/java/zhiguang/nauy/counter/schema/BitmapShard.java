package zhiguang.nauy.counter.schema;

/**
 * 位图分片配置与帮助函数。
 *
 * 为什么需要分片？
 * - 如果所有用户都在一个位图键里，键会非常大（用户ID可能到百万级）
 * - 分片后每个键最多32K位（4KB），避免单个Redis键过大
 *
 * 分片逻辑：
 * - 每个分片32K位（32768位 = 4KB）
 * - userId 123456 → chunk=3, bit=24992
 * - 存储到键 bm:like:knowpost:123:3 的第24992位
 */
public class BitmapShard {
    // 每个分片的位数（32K位 = 4KB/分片）
    public static final int CHUNK_SIZE = 32_768;

    /**
     * 计算userId属于哪个分片
     * @param userId 用户ID
     * @return 分片编号
     */
    public static long chunkOf(long userId) {
        return userId / CHUNK_SIZE;
    }

    /**
     * 计算userId在分片内的偏移量
     * @param userId 用户ID
     * @return 在分片内的位偏移（0 ~ CHUNK_SIZE-1）
     */
    public static long bitOf(long userId) {
        return userId % CHUNK_SIZE;
    }

    // 私有构造函数，防止实例化
    private BitmapShard() {}
}
