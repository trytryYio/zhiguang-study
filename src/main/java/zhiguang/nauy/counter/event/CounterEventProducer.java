package zhiguang.nauy.counter.event;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 事件生产者
 * @author nauy
 *
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CounterEventProducer {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    /**
     * 发送kafka事件
     * @param event
     */
    public void publish(CounterEvent event) {
        try {
            //第一步 把事件对象转化为json字符串
            String json = JSONUtil.toJsonStr(event);
            //第二步 发送kafka事件
            kafka.send(CounterTopics.EVENTS, json);

        }catch (Exception e){

            //处理异常
            log.error("Kafka 发送失败，降级存储", e);        }
    }
}
