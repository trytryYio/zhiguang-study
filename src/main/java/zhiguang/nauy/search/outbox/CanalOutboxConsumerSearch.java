package zhiguang.nauy.search.outbox;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import zhiguang.nauy.common.OutboxMessageUtil;
import zhiguang.nauy.search.index.SearchIndexService;

import java.util.List;

/**
 * Canal Outbox 消费者（搜索索引）。
 *
 * <p>监听 canal-outbox topic，处理知文相关的事件，
 * 触发 ES 索引的增量更新。</p>
 */
@Slf4j
@Service

public class CanalOutboxConsumerSearch {

    @Resource
    private SearchIndexService searchIndexService;

    @KafkaListener(topics = "canal-outbox", groupId = "search-index-consumer")
    public void onMessage(String message, Acknowledgment ack) {
        log.info("Consume Canal outbox message: {}", message);
        try {
//        1.监听 Kafka 的 canal-outbox topic，收到消息就触发。
            List<JSONObject> rows = OutboxMessageUtil.extractRows(message);
            if (rows.isEmpty()) {
                ack.acknowledge();
                return;
            }
            //2.从canal 消息提取数据行 -> payload
            for (JSONObject row : rows) {
                //3.取出 payload 是一个json 字符串
                Object payload = row.get("payload");
                // payload 的值: {"entity":"zhiguang/nauy/knowpost","op":"upsert","id":789}

                if (payload == null) continue;

                //4.解析为 json 对象 判断是否为知文事件
                try {
                    JSONObject payloadJson = JSONUtil.parseObj(payload);

                    //5.取entity判断实体类型, 取op判断操作类型
                    String entity = payloadJson.getStr("entity");
                    String op = payloadJson.getStr("op");
                    Long postId = payloadJson.getLong("id");

                    //6.如果是知文事件 -> 触发索引更新
                    if ("zhiguang/nauy/knowpost".equals(entity) && postId != null) {
                        if ("delete".equalsIgnoreCase(op)) {
                            searchIndexService.softDeleteKnowPost(postId);
                        } else {
                            searchIndexService.upsertKnowPost(postId);
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析失败
                }
            }
            ack.acknowledge();
        } catch (Exception e) {
            // 消费失败不 ack
        }


    }
}
