package zhiguang.nauy.counter.event;

/**
 * 灾难场景下的计数重建消费者：基于 earliest 回放历史事件，直接折叠到 SDS。
 * 默认关闭，仅当 counter.rebuild.enabled=true 时启用。
 */
public class CounterRebuildConsumer {
    
    // TODO: 实现灾难恢复重建逻辑
}
