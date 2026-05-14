package zhiguang.nauy.outbox;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;

/**
 * Canal→Kafka 桥接器。
 *
 * <p>职责：订阅 outbox 表的行级变更（ROWDATA），仅转发 INSERT/UPDATE 的 payload 字段到 Kafka 主题；
 * 批次确认位点确保至少一次语义。</p>
 *
 * <p>可靠性：解析失败或非关心类型不提交位点；停止时断开 Canal 连接并清理资源。</p>
 */
@Service
@Slf4j
public class CanalKafkaBridge implements SmartLifecycle {

    /**
     * Kafka 模板，用于将解析后的 Outbox 消息转发至指定主题
     */
    private final KafkaTemplate<String, String> kafka;

    /**
     * Canal 桥接功能开关，由配置文件 canal.enabled 决定
     */
    private final boolean enabled;

    /**
     * Canal Server 的主机地址
     */
    private final String host;

    /**
     * Canal Server 的通信端口
     */
    private final int port;

    /**
     * Canal 订阅的目标实例名称（Destination）
     */
    private final String destination;

    /**
     * 连接 Canal Server 所需的用户名
     */
    private final String username;

    /**
     * 连接 Canal Server 所需的密码
     */
    private final String password;

    /**
     * 订阅过滤规则，例如：zhiguang\.outbox
     */
    private final String filter;

    /**
     * 每次从 Canal Server 拉取消息的最大批次大小
     */
    private final int batchSize;

    /**
     * 轮询 Canal Server 的时间间隔（毫秒）
     */
    private final long intervalMs;

    /**
     * 桥接器运行状态标识，使用 volatile 保证多线程可见性
     */
    private volatile boolean running;

    /**
     * 任务执行器，用于在后台线程中异步处理 Canal 消息拉取任务
     */
    private final TaskExecutor taskExecutor;

    /**
     * Canal 客户端连接器实例，负责与 Canal Server 建立 TCP 连接并订阅 Binlog
     */
    private CanalConnector connector;

    public CanalKafkaBridge(KafkaTemplate<String, String> kafka, @Qualifier("threadPoolTaskExecutor") TaskExecutor taskExecutor, @Value("${canal.enabled}") boolean enabled, @Value("${canal.host}") String host, @Value("${canal.port}") int port, @Value("${canal.destination}") String destination, @Value("${canal.username:}") String username, @Value("${canal.password:}") String password, @Value("${canal.filter}") String filter, @Value("${canal.batchSize:1000}") int batchSize, @Value("${canal.intervalMs:1000}") long intervalMs) {
        this.kafka = kafka;
        this.taskExecutor = taskExecutor;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.destination = destination;
        this.username = username;
        this.password = password;
        this.filter = filter;
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
    }


    @Override
    public void start() {
        //0.判断canal 是否已启动
        if (running) {
            log.info("Canal bridge start skipped: running={} enabled={}", running, enabled);
            return;
        }
        running = true;
        //开启异步模式 ,因为canal 是监听 mysql 是一个一直while的循环 如果不开启异步 程序会卡死在这
        taskExecutor.execute(() -> {
            //1.连接上canal
            CanalConnector conn = null;
            try {
                conn = CanalConnectors.newSingleConnector(
                    // localhost:11111
                    new InetSocketAddress(host, port),
                    //destination: 实例名称（通常是 example，对应 Canal 配置里的一个文件夹）。
                    destination, username, password);
                conn.connect();
                //订阅目标 (Subscribe)
                conn.subscribe(filter); // 告诉 Canal：我只要 outbox 表的数据
                //设置回滚点
                conn.rollback();
                connector = conn;
                log.info("Canal connected: host={} port={} dest={} filter={}", host, port, destination, filter);
                //2.循环拉取（蹲点）
                while (running) {
//                    蹲守mysql outbox表格变化的点
                    //getWithoutAck 是 Canal Client API 中最核心的数据拉取方法。它的名字直译过来就是“获取但不确认”。
                    Message message = conn.getWithoutAck(batchSize);
                    //Message (快递箱)
                    //├── id: 10086 (批次号/签收单号)
                    //└── entries: [ (货物列表)
                    //      ├── Entry 1 (小包裹):
                    //      │     ├── Header: 表=outbox, 类型=INSERT
                    //      │     └── StoreValue: [二进制数据: payload="..."]
                    //      │
                    //      ├── Entry 2 (小包裹):
                    //      │     ├── Header: 表=outbox, 类型=UPDATE
                    //      │     └── StoreValue: [二进制数据: payload="..."]
                    //      └── ...
                    //]
                    long batchId = message.getId();
                    //
                    // 2.1 判断是否获取到有效数据：batchId为-1表示无新数据，或消息条目为空
                    if (batchId == -1 || message.getEntries() == null || message.getEntries().isEmpty()) {
                        try {
                            // 无数据时休眠指定间隔，避免空轮询占用CPU资源
                            Thread.sleep(intervalMs);
                        } catch (InterruptedException ignored) {
                            // 捕获中断异常并恢复中断状态，确保线程能正确响应停止信号
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        //2.2 遍历message里面所有的entry
                        for (CanalEntry.Entry entry : message.getEntries()) {
                            //2.2.1 无关信息筛选
                            //只看行数据,跳过事务开启 /结束这些无关信息
                            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                                //entry.getEntryType() 大致有

                                //ROWDATA
                                //包含 INSERT/UPDATE/DELETE 的具体数据

                                //TRANSACTIONBEGIN
                                //事务标记

                                //TRANSACTIONEND
                                //事务标记

                                //HEARTBEAT
                                //保活信号
                                continue;
                            }
                            //2.2.2 深度解析 仅关注 insert 和update 的数据
                            CanalEntry.RowChange rowChange;
                            try {

                                //筛选出来后 解析成为具体的 行变更对象
                                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                            } catch (Exception e) {
                                // 解析失败时记录警告日志并跳过当前条目，不提交位点以保证至少一次语义
                                log.warn("Failed to parse RowChange: {}", e.getMessage());
                                continue;
                            }
                            //从行数据中 提取eventType 仅处理 INSERT 和 UPDATE 事件
                            CanalEntry.EventType eventType = rowChange.getEventType();
                            // 仅处理 INSERT 和 UPDATE 事件，忽略 DELETE 等其他类型
                            if (eventType != CanalEntry.EventType.INSERT && eventType != CanalEntry.EventType.UPDATE) {
                                continue;
                            }

                            // 2.3 提取 payload 并构建 Kafka 消息

                            // 使用 Hutool 创建数组节点
                            JSONArray dataArray = JSONUtil.createArray();

                            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                                JSONObject rowNode = JSONUtil.createObj();
                                // 遍历变更后的列，仅提取 payload 字段
                                for (CanalEntry.Column col : rowData.getAfterColumnsList()) {
                                    if ("payload".equalsIgnoreCase(col.getName())) {
                                        rowNode.set("payload", col.getValue());
                                        break; // 找到 payload 后即可跳出内层循环
                                    }
                                }
                                // 只有当 rowNode 不为空（即找到了 payload）时才添加到数组
                                if (!rowNode.isEmpty()) {
                                    dataArray.add(rowNode);
                                }
                            }

                            // 如果 dataArray 为空，说明没有提取到有效的 payload，跳过发送
                            if (dataArray.isEmpty()) {
                                continue;
                            }

                            // 使用 Hutool 创建根对象
                            JSONObject msgNode = JSONUtil.createObj();
                            msgNode.set("table", entry.getHeader().getTableName());
                            msgNode.set("type", eventType == CanalEntry.EventType.INSERT ? "INSERT" : "UPDATE");
                            msgNode.set("data", dataArray);

                            try {
                                // 直接转换为 JSON 字符串
                                String json = msgNode.toString();
                                kafka.send(OutboxTopics.PREFIX, json);
                                log.debug("Sent outbox message to Kafka: table={}, type={}", entry.getHeader().getTableName(), eventType);
                            } catch (Exception e) {
                                // 发送失败记录警告，但不中断后续处理
                                log.warn("Failed to send to Kafka: {}", e.getMessage());
                            }
                        }

                        conn.ack(batchId);
                    }



                }
            } catch (Exception e) {
                //链接失败
                log.error("Canal bridge error", e);
            } finally {
                if (conn != null) {
                    //不管如何都要关闭conn
                    try {
                        conn.disconnect();
                        log.info("Canal disconnected: dest={}", destination);
                    } catch (Exception ex) {
                        log.warn("Canal disconnect failed: {}", ex.getMessage());
                    }
                }
            }

        });
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
