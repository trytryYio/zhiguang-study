package zhiguang.nauy.outbox;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import zhiguang.nauy.common.OutboxMessageUtil;
import zhiguang.nauy.relation.event.RelationEvent;
import zhiguang.nauy.relation.processor.RelationEventProcessor;

import java.util.List;

/**
 * Canal Outbox 消费者。
 *
 * <p>职责：消费 Canal 桥接写入的 outbox 主题消息，提取 payload 并反序列化为 RelationEvent，
 * 交由处理器落库与更新缓存/计数；使用手动位点确保处理成功语义。</p>
 */

@Service
@Slf4j
public class CanalOutboxConsumer {
    @Resource
    private RelationEventProcessor processor;

    /**
     * 消费 Canal outbox 消息并转为关系事件处理。
     *
     * @param message Kafka 消息内容
     * @param ack     位点确认对象
     */
    @KafkaListener(topics = OutboxTopics.PREFIX, groupId = "relation-outbox-consumer")
    public void consume(String message, Acknowledgment ack) {
        //{
        //  "table": "outbox",
        //  "type": "INSERT",
        //  "data": [
        //    {
        //      "payload": "{\"type\":\"FollowCreated\",\"toUserId\":123,\"fromUserId\":456,\"id\":789}"
        //    }
        //  ]
        //}
        log.info("Consume Canal outbox message: {}", message);
        //  1. 反序列化消息
        // 1. row 是 JSONObject，包含: {"payload": "{...}"}
        try {
            // 提取数据行
            List<JSONObject> rows = OutboxMessageUtil.extractRows(message);
            if (rows.isEmpty()) {
                ack.acknowledge();// 确认消费成功
                return;
            }
            for (JSONObject row : rows) {
                // 反序列化
                // 2. 获取 payload（已经是普通字符串，没有转义符）
                Object payload = row.getObj("payload");
                // payload 的值: {"type":"FollowCreated","toUserId":123,"fromUserId":456,"id":789}
                if (payload == null) {
                    continue;
                }
            // 3. 转为字符串
                String payloadJson = payload.toString();

                // 4. 反序列化为 RelationEvent 对象
                RelationEvent event = JSONUtil.toBean(payloadJson, RelationEvent.class);
                // 5. 处理
                processor.process(event);
                //消费成功
                ack.acknowledge();
            }
        } catch (Exception e) {
            // 消费失败不 ack，等待 Kafka 重试
        }

    }


}
