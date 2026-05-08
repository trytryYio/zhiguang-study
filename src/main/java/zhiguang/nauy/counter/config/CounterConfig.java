package zhiguang.nauy.counter.config;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * Counter模块配置。
 *
 * <p>配置内容：</p>
 * <ul>
 *   <li>启用Kafka（@EnableKafka）</li>
 *   <li>启用调度（@EnableScheduling）</li>
 *   <li>配置Kafka Template用于事件发布</li>
 * </ul>
 */

@Configuration
@EnableScheduling
@EnableKafka
public class CounterConfig {
    /**
     * 配置Kafka Producer。
     *
     * <p>Kafka Producer配置：</p>
     * <ul>
     *   <li>使用KafkaProperties配置项</li>
     *   <li>统一字符串序列化</li>
     * </ul>
     *
     * @param properties KafkaProperties
     * @return ProducerFactory
     */
    @Bean
    public ProducerFactory<String, String> stringProducerFactory(KafkaProperties properties) {
        // 1. 从 Spring Boot 配置中获取 Kafka 生产者的所有配置属性
        Map<String, Object> props = new HashMap<>(properties.buildProducerProperties(null));

        // 2. 使用这些配置创建一个生产者工厂
        //    Key 和 Value 都使用 StringSerializer 进行字符串序列化
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()); // 统一字符串序列化
    }

    /**
     * 配置Kafka Template。
     *
     * <p>Kafka Template配置：</p>
     * <ul>
     *   <li>使用stringProducerFactory配置项</li>
     * </ul>
     *
     * @param pf ProducerFactory
     * @return KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }
}
