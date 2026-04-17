package zhiguang.nauy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Description: 线程池配置类 -用于异步任务处理// 类说明，在创建类时要填写
 * @ClassName: ThreadPoolConfig    // 类名，会自动填充
 * @Author: oyy         // 创建者
 * @Date: 2026/4/12 16:46   // 时间
 * @Version: 1.0     // 版本
 */
@Configuration
public class ThreadPoolConfig {
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);// 核心线程数
        executor.setMaxPoolSize(50);// 最大线程数
        executor.setQueueCapacity(200);// 队列容量
        executor.setKeepAliveSeconds(30);// 空线程存活时间
        executor.setThreadNamePrefix("NoteExecutor-");// 线程前缀名
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());// 拒绝策略
//        new ThreadPoolExecutor.CallerRunsPolicy() 是一个什么策略 ？
//        机制：当线程池饱和时，不丢弃任务，也不抛出异常，而是由提交任务的线程（即调用 execute 方法的线程）直接执行该任务。
//        作用：提供一种简单的反馈控制机制，降低新任务的提交速度，防止系统过载。
        executor.setWaitForTasksToCompleteOnShutdown(true);// 等待所有任务完成再关闭
        executor.setAwaitTerminationSeconds(60);// 等待时间
        executor.initialize();
        return executor;
    }
}
