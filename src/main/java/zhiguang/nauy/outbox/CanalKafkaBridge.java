package zhiguang.nauy.outbox;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
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
public class CanalKafkaBridge  implements SmartLifecycle {

    /** Kafka 模板，用于将解析后的 Outbox 消息转发至指定主题 */
    private final KafkaTemplate<String, String> kafka;

    /** Canal 桥接功能开关，由配置文件 canal.enabled 决定 */
    private final boolean enabled;

    /** Canal Server 的主机地址 */
    private final String host;

    /** Canal Server 的通信端口 */
    private final int port;

    /** Canal 订阅的目标实例名称（Destination） */
    private final String destination;

    /** 连接 Canal Server 所需的用户名 */
    private final String username;

    /** 连接 Canal Server 所需的密码 */
    private final String password;

    /** 订阅过滤规则，例如：zhiguang\.outbox */
    private final String filter;

    /** 每次从 Canal Server 拉取消息的最大批次大小 */
    private final int batchSize;

    /** 轮询 Canal Server 的时间间隔（毫秒） */
    private final long intervalMs;

    /** 桥接器运行状态标识，使用 volatile 保证多线程可见性 */
    private volatile boolean running;

    /** 任务执行器，用于在后台线程中异步处理 Canal 消息拉取任务 */
    private final TaskExecutor taskExecutor;

    /** Canal 客户端连接器实例，负责与 Canal Server 建立 TCP 连接并订阅 Binlog */
    private CanalConnector connector;

    public CanalKafkaBridge(
        KafkaTemplate<String, String> kafka,
        @Qualifier("threadPoolTaskExecutor") TaskExecutor taskExecutor,
        @Value("${canal.enabled}") boolean enabled,
        @Value("${canal.host}") String host,
        @Value("${canal.port}") int port,
        @Value("${canal.destination}") String destination,
        @Value("${canal.username:}") String username,
        @Value("${canal.password:}") String password,
        @Value("${canal.filter}") String filter,
        @Value("${canal.batchSize:1000}") int batchSize,
        @Value("${canal.intervalMs:1000}") long intervalMs) {
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
        running=true;
        //开启异步模式 ,因为canal 是监听 mysql 是一个一直while的循环 如果不开启异步 程序会卡死在这
        taskExecutor.execute(() -> {
            //1.连接上canal
            CanalConnector conn = null;
            try{
                conn = CanalConnectors.newSingleConnector(
                    // localhost:11111
                    new InetSocketAddress(host, port),
                    //destination: 实例名称（通常是 example，对应 Canal 配置里的一个文件夹）。
                    destination,
                    username,
                    password);
                conn.connect();
                //订阅目标 (Subscribe)
                conn.subscribe(filter); // 告诉 Canal：我只要 outbox 表的数据
                //设置回滚点
                conn.rollback();
                connector = conn;
                log.info("Canal connected: host={} port={} dest={} filter={}",
                    host, port, destination, filter);
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
