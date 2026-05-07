package zhiguang.nauy.knowpost.domain.id;

import org.springframework.stereotype.Component;

/**
 * 线程安全的雪花算法 ID 生成器
 * <p>
 * 生成的64位ID结构：
 * <ul>
 *   <li>1位符号位（固定为0）</li>
 *   <li>41位时间戳（毫秒级，可使用约69年）</li>
 *   <li>5位数据中心ID（支持32个数据中心）</li>
 *   <li>5位工作节点ID（每个数据中心支持32个节点）</li>
 *   <li>12位序列号（每毫秒支持4096个ID）</li>
 * </ul>
 * </p>
 */
@Component
public class SnowflakeIdGenerator {
    /**
     * 起始时间戳（2024-01-01 00:00:00 UTC）
     * 用于减少时间戳位数，延长ID可用年限
     */
    private static final long EPOCH = 1704067200000L;

    /**
     * 工作节点ID所占位数 5个工作节点
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据中心ID所占位数 5个数据中心
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 序列号所占位数 12 位序列号 4096
     * 同一台机器在同一毫秒内最多生成4096个ID
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 最大工作节点ID值（31） 位运算
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 最大数据中心ID值（31） 位运算
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 工作节点ID左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID左移位数
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 序列号掩码（4095）
     * 用于限制序列号在0-4095 范围内
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 数据中心ID
     */
    private final long datacenterId;

    /**
     * 工作节点ID
     */
    private final long workerId;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 当前毫秒内的序列号
     */
    private long sequence = 0L;

    /**
     * 默认构造函数
     * 使用数据中心ID=1, 工作节点ID=1
     */
//    可以改成 @Value("${id.datacenterId-id }") 这种方法就可以在yaml文件中配置了
    public SnowflakeIdGenerator() {
        this(1, 1);
    }

    /**
     * 构造函数
     *
     * @param datacenterId 数据中心ID，范围0-31
     * @param workerId     工作节点ID，范围0-31
     * @throws IllegalArgumentException 当ID超出有效范围时抛出
     */
    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 生成下一个唯一ID
     * <p>
     * 该方法使用synchronized保证线程安全，处理以下情况：
     * <ul>
     *   <li>时钟回拨：小幅度回拨（≤5ms）等待追回，大幅度回拨直接拒绝</li>
     *   <li>同一毫秒并发：通过序列号递增处理，序列号用完后等待下一毫秒</li>
     * </ul>
     * </p>
     * synchronized 线程锁保证了同一时间内只能有一个线程执行，避免多个线程生成重复的ID。
     *
     * @return 生成的64位唯一ID
     * @throws IllegalStateException 当时钟回拨过大或线程被中断时抛出
     */
    public long nextId() {
        long timestamp = currentTime();

        // 处理时钟回拨情况
//        正常情况下，系统时间是不断向前走的
//        10:00:00 → 10:00:01 → 10:00:02
//        但有时会出现这种情况：
//        10:00:05 → 10:00:03 ← 时间倒退了！

//        如果发生时钟回拨，则需要处理
        if (timestamp < lastTimestamp) {
//            0.计算波动值
            long offset = lastTimestamp - timestamp;

            // 1. 小幅度回拨（比如 NTP 校时导致的 1~5ms 间抖动）：等待一会儿再试
            if (offset <= 5) {
                try {
                    // 睡 offset 毫秒，给系统时钟一点时间"追上来"
                    Thread.sleep(offset);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrupted while waiting for clock to catch up", e);
                }

                timestamp = currentTime();
                if (timestamp < lastTimestamp) {
                    // 等完还是没追上，说明问题较严重，直接拒绝
                    throw new IllegalStateException(
                            "Clock is still behind after waiting. last=" + lastTimestamp + ", now=" + timestamp);
                }
            } else {
                // 2. 回拨幅度太大，直接拒绝，避免线程长时间阻塞
                throw new IllegalStateException(
                        "Clock moved backwards too much. Refusing to generate id. offset=" + offset + "ms");
            }
        }

        // 处理同一毫秒内的并发请求：序列号逻辑
        if (lastTimestamp == timestamp) {
//            同一毫秒内 序列号递增   按位 与运算：确保 sequence 始终在 0-4095 范围内循环
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 这一毫秒的 4096 个名额用完了
//                等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);

            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;// 更新 lastTimestamp

        // 组装 64 位 ID
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一毫秒
     * 当同一毫秒内序列号用尽时，阻塞等待直到下一毫秒
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 新的时间戳（大于lastTimestamp）
     */
    private long waitNextMillis(long lastTimestamp) {
//        循环等待下一毫秒
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    /**
     * 获取当前系统时间戳（毫秒）
     *
     * @return 当前时间戳
     */
    private long currentTime() {
        return System.currentTimeMillis();
    }
}
