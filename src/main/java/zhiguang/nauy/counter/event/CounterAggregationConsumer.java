package zhiguang.nauy.counter.event;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import zhiguang.nauy.counter.schema.CounterKeys;
import zhiguang.nauy.counter.schema.CounterSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 计数事件聚合与刷写消费者。
 *
 * <p>职责：</p>
 * - 消费点赞/收藏等增量事件，写入 Redis 聚合桶（Hash）；
 * - 以固定延迟定时任务将聚合增量折叠到 SDS 固定结构计数；
 * - 刷写成功后删除聚合字段，避免重复加算。
 */
@Service
@Slf4j
public class CounterAggregationConsumer {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> incrScript; // 折叠到 SDS的Redis Lua 脚本
    private final DefaultRedisScript<Long> decrScript;// 扣减Hash的Redis Lua 脚本

    public CounterAggregationConsumer(StringRedisTemplate redis) {
        this.redis = redis;
        // 创建增量折叠到SDS的Lua脚本
        this.incrScript = new DefaultRedisScript<>();
        this.incrScript.setResultType(Long.class);
        this.incrScript.setScriptText(INCR_FIELD_LUA);

        // 创建扣减Hash字段的Lua脚本
        this.decrScript = new DefaultRedisScript<>();
        this.decrScript.setResultType(Long.class);
        this.decrScript.setScriptText(DECR_FIELD_LUA);
    }
    /**
     * Kafka 消费者。
     *
     * <p>消费点赞/收藏等事件，写入 Redis 聚合桶（Hash）。</p>
     *
     * @param message Kafka 消息
     * @param ack Kafka 消费者确认接口
     */
    @KafkaListener(topics = CounterTopics.EVENTS,groupId = "counter-egg")
    public void onEvent(String message,  Acknowledgment ack) throws Exception{
        try{
//            1.解析 事件
            CounterEvent event = JSONUtil.toBean(message, CounterEvent.class);

            //2.处理业务逻辑
            String aggKey = CounterKeys.aggKey(event.getEntityType(), event.getEntityId());

            //aggKey：聚合桶的键（如 agg:knowpost:123）
            //event.getMetric()：Hash 字段名（指标类型，如 "like"）
            //event.getDelta()：增量值（+1 或 -1）
            redis.opsForHash().increment(aggKey, event.getMetric(), event.getDelta());


            //3.确认消费(手动提交offset)
            ack.acknowledge();
        }catch (Exception e){
            log.error("Kafka 消费失败", e);
            //不提交，Kafka 会自动重试
        }

    }

    @Scheduled(fixedDelay = 1000L)//5秒执行一次
    public void flush(){
        //第一步扫描所有的聚合桶key
        Set<String>keys = redis.keys("agg:"+ CounterSchema.SCHEMA_ID+"*");
        if (keys == null || keys.isEmpty()){
            //没有需要处理的了
            return;
        }
        //第二步 遍历所有的桶
        for (String key : keys) {
            //2.1获取hash 的所有field-value
            Map<Object, Object> fields = redis.opsForHash().entries(key);
            if (fields.isEmpty())continue;
            //Key: agg:V1:knowpost:123
            //├─ Field: "like" → Value: "5"    (5个用户点赞)
            //├─ Field: "fav"  → Value: "2"    (2个用户收藏)

            //2.2解析 key 获取 entityType 和 entityId
            // agg:V1:knowpost:123 → parts[0]="agg", parts[1]="V1", parts[2]="knowpost", parts[3]="123"
            //
            String[] split = key.split(":", 4);
            if (split.length <4) continue;
            //2.3生成SDS 的key
            String sdsKey = CounterKeys.sdsKey(split[2], split[3]);
            //2.4 遍历每一个 field 和 value
            for (Map.Entry<Object, Object> entry : fields.entrySet()){
                String field = String.valueOf(entry.getKey());
                long value = Long.parseLong((String.valueOf(entry.getValue())));

                if (value == 0) continue; //无增量 跳过
                int idx = Integer.parseInt( field);
                 try {
                     //2.5执行lua 脚本 将 delta 折叠到sds
                     redis.execute(incrScript, List.of(sdsKey),
                         String.valueOf(CounterSchema.SCHEMA_LEN),
                         String.valueOf(CounterSchema.BYTES_PER_METRIC),
                         String.valueOf(idx),
                         String.valueOf(value)
                         );
                     //2.6 扣减hash 中的 value
                     redis.execute(decrScript, List.of(key), field, String.valueOf(value));
                 }catch (Exception e){
                     //留存字段下一轮重试
                 }

            }

        }

    }


    private static final String INCR_FIELD_LUA = """

            local cntKey = KEYS[1]
            local schemaLen = tonumber(ARGV[1])
            local fieldSize = tonumber(ARGV[2]) -- 固定为4
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
            """;

    private static final String DECR_FIELD_LUA = """
            local key = KEYS[1]
            local field = ARGV[1]
            local delta = tonumber(ARGV[2])
            local v = redis.call('HINCRBY', key, field, -delta)
            if v == 0 then
                redis.call('HDEL', key, field)
            end
            return v
            """;


}
