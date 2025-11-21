package com.smartLive.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.LinkedHashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService executorService(){
        AtomicInteger atomicInteger = new AtomicInteger();
        //任务队列最多存100个任务
        LinkedBlockingDeque<Runnable>queue = new LinkedBlockingDeque<>(100);
        return new ThreadPoolExecutor(7, 10, 0L, TimeUnit.MILLISECONDS, queue,r -> new Thread(r, "threadPool-" + atomicInteger.getAndIncrement()), new ThreadPoolExecutor.DiscardPolicy());
    }
}
