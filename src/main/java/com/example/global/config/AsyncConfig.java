package com.example.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "festivalDetailTaskExecutor")
    public Executor festivalDetailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 실행 스레드 수
        executor.setCorePoolSize(2);

        // 최대 실행 스레드 수
        executor.setMaxPoolSize(4);

        // 대기 큐 크기
        executor.setQueueCapacity(100);

        // 스레드 이름 prefix
        executor.setThreadNamePrefix("festival-detail-");

        executor.initialize();
        return executor;
    }
}