package com.vlstream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 录制任务配置
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class RecordingTaskConfig {

    @Value("${recording.thread.core-pool-size:5}")
    private int corePoolSize;

    @Value("${recording.thread.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${recording.thread.keep-alive-time:60}")
    private long keepAliveTime;

    @Value("${recording.thread.queue-capacity:100}")
    private int queueCapacity;

    /**
     * 录制任务线程池
     */
    @Bean
    public ThreadPoolExecutor recordingThreadPool() {
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new RecordingThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 录制线程工厂
     */
    private static class RecordingThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "recording-task-";

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
} 