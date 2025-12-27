package com.smartLive.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池
 */
@Configuration
public class ThreadPoolConfig {
    /**
     * 创建线程池
     * @return
     */
    @Bean
    public ExecutorService executorService(){
        AtomicInteger atomicInteger = new AtomicInteger();
        //任务队列最多存100个任务
        LinkedBlockingDeque<Runnable>queue = new LinkedBlockingDeque<>(100);
        return new ThreadPoolExecutor(7, 10, 0L, TimeUnit.MILLISECONDS, queue,r -> new Thread(r, "threadPool-" + atomicInteger.getAndIncrement()), new ThreadPoolExecutor.DiscardPolicy());
    }
    /**
     * 创建任务线程池
     * @return
     */
    @Bean
    public ScheduledExecutorService scheduledExecutorService(){
        AtomicInteger atomicInteger = new AtomicInteger();
        return  Executors.newScheduledThreadPool(5, r -> new Thread(r, "scheduledThreadPool-" + atomicInteger.getAndIncrement()));
    }
}
