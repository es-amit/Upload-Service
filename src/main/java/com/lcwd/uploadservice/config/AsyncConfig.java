package com.lcwd.uploadservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "transcodeExecutor")
    public ThreadPoolExecutor transcodeExecutor() {
        return new ThreadPoolExecutor(
                2,                              // corePoolSize
                2,                              // maximumPoolSize
                60L,                            // keepAliveTime
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50), // queueCapacity
                new CustomizableThreadFactory("transcode-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}