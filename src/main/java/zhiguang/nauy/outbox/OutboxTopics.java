package zhiguang.nauy.outbox;

import lombok.NoArgsConstructor;

/**
 * Outbox 相关 Kafka 主题常量。
 * 约定：Canal 将 outbox 表的行变更转发至主题 `canal-outbox`，下游消费者据此处理。
 */
@NoArgsConstructor
public final class OutboxTopics {

 public   final static String PREFIX = "canal-outbox";
}
