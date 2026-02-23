package com.gb28181.sipserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 
 * 配置应用程序使用的线程池，包括：
 * - 异步任务执行器
 * - SIP消息处理线程池
 * - 媒体数据处理线程池
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolConfig.class);

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    /**
     * 异步任务执行器
     * 
     * @return 异步任务执行器
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolProperties.ExecutorProperties props = threadPoolProperties.getAsync();
        ThreadPoolTaskExecutor executor = buildExecutor(
                props,
                "AsyncTask-",
                new ThreadPoolExecutor.CallerRunsPolicy(),
                60);
        logger.info("异步任务执行器初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), props.getQueueCapacity());
        return executor;
    }

    /**
     * SIP消息处理线程池
     * 
     * @return SIP消息处理线程池
     */
    @Bean(name = "sipMessageExecutor")
    public Executor sipMessageExecutor() {
        ThreadPoolProperties.ExecutorProperties props = threadPoolProperties.getSipMessage();
        ThreadPoolTaskExecutor executor = buildExecutor(
                props,
                "SipMessage-",
                new ThreadPoolExecutor.DiscardOldestPolicy(),
                30);
        logger.info("SIP消息处理线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), props.getQueueCapacity());
        return executor;
    }

    /**
     * 媒体数据处理线程池
     * 
     * @return 媒体数据处理线程池
     */
    @Bean(name = "mediaDataExecutor")
    public Executor mediaDataExecutor() {
        ThreadPoolProperties.ExecutorProperties props = threadPoolProperties.getMedia();
        ThreadPoolTaskExecutor executor = buildExecutor(
                props,
                "MediaData-",
                new ThreadPoolExecutor.DiscardOldestPolicy(),
                30);
        logger.info("媒体数据处理线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), props.getQueueCapacity());
        return executor;
    }

    /**
     * 推流任务线程池
     * 
     * @return 推流任务线程池
     */
    @Bean(name = "pushTaskExecutor")
    public Executor pushTaskExecutor() {
        ThreadPoolProperties.ExecutorProperties props = threadPoolProperties.getPush();
        ThreadPoolTaskExecutor executor = buildExecutor(
                props,
                "PushTask-",
                new ThreadPoolExecutor.CallerRunsPolicy(),
                60);
        logger.info("推流任务线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), props.getQueueCapacity());
        return executor;
    }

    private ThreadPoolTaskExecutor buildExecutor(ThreadPoolProperties.ExecutorProperties properties,
                                                  String threadNamePrefix,
                                                  RejectedExecutionHandler handler,
                                                  int awaitTerminationSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCoreSize());
        executor.setMaxPoolSize(properties.getMaxSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(handler);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }
}
