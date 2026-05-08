package zhiguang.nauy.counter.event;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import zhiguang.nauy.counter.schema.CounterKeys;
import zhiguang.nauy.counter.schema.CounterSchema;

import java.util.List;

/**
 * 灾难场景下的计数重建消费者：基于 earliest 回放历史事件，直接折叠到 SDS。
 * 默认关闭，仅当 counter.rebuild.enabled=true 时启用。
 *
 * <p>与正常聚合消费者不同，此消费者跳过聚合层，直接将每个事件的 delta 折叠到 SDS。
 * 适用于 Redis 数据完全丢失后，从 Kafka 历史事件重建计数的场景。</p>
 *
 * <p>使用方式：启动时设置 counter.rebuild.enabled=true，待重建完成后重启应用恢复正常模式。</p>
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "counter.rebuild.enabled", havingValue = "true")
public class CounterRebuildConsumer {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> incrScript;

    public CounterRebuildConsumer(StringRedisTemplate redis) {
        this.redis = redis;

        // 创建增量折叠到SDS的Lua脚本（与CounterAggregationConsumer相同）
        this.incrScript = new DefaultRedisScript<>();
        this.incrScript.setResultType(Long.class);
        this.incrScript.setScriptText("""
            local cntKey = KEYS[1]
            local schemaLen = tonumber(ARGV[1])
            local fieldSize = tonumber(ARGV[2])
            local idx = tonumber(ARGV[3])
            local delta = tonumber(ARGV[4])

            local function read32be(s, off)
              local b = {string.byte(s, off+1, off+4)}
              local n = 0
              for i=1,4 do n = n * 256 + b[i] end
              return n
            end

            local function write32be(n)
              local t = {}
              for i=4,1,-1 do t[i] = n % 256; n = math.floor(n/256) end
              return string.char(unpack(t))
            end

            local cnt = redis.call('GET', cntKey)
            if not cnt then cnt = string.rep(string.char(0), schemaLen * fieldSize) end
            local off = idx * fieldSize
            local v = read32be(cnt, off) + delta
            if v < 0 then v = 0 end
            local seg = write32be(v)
            cnt = string.sub(cnt, 1, off) .. seg .. string.sub(cnt, off+fieldSize+1)
            redis.call('SET', cntKey, cnt)
            return 1
            """);
    }

    /**
     * Kafka 重建消费者。
     *
     * <p>从 Kafka 最早的消息开始消费，直接将每个事件的 delta 折叠到 SDS，跳过聚合层。</p>
     *
     * @param message Kafka 消息
     * @param ack Kafka 消费者确认接口
     */
    @KafkaListener(topics = CounterTopics.EVENTS, groupId = "counter-rebuild",
        properties = {"auto.offset.reset=earliest"})
    public void onEvent(String message, Acknowledgment ack) {
        try {
            // 1. 解析事件
            CounterEvent event = JSONUtil.toBean(message, CounterEvent.class);

            // 2. 生成 SDS Key
            String sdsKey = CounterKeys.sdsKey(event.getEntityType(), event.getEntityId());

            // 3. 执行 Lua 脚本，将 delta 原子性地折叠到 SDS
            redis.execute(incrScript, List.of(sdsKey),
                String.valueOf(CounterSchema.SCHEMA_LEN),
                String.valueOf(CounterSchema.BYTES_PER_METRIC),
                String.valueOf(event.getIdx()),
                String.valueOf(event.getDelta())
            );

            // 4. 确认消费
            ack.acknowledge();

            log.debug("重建计数: entityType={}, entityId={}, metric={}, delta={}",
                event.getEntityType(), event.getEntityId(), event.getMetric(), event.getDelta());

        } catch (Exception e) {
            log.error("重建计数失败，消息: {}", message, e);
            // 不确认，让 Kafka 重试
        }
    }
}
