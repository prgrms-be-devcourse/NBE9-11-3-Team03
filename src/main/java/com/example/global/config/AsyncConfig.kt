package com.example.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["festivalDetailTaskExecutor"])
    fun festivalDetailTaskExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2 // 기본 실행 스레드 수
            maxPoolSize = 4 // 최대 실행 스레드 수
            queueCapacity = 100 // 대기 큐 크기
            setThreadNamePrefix("festival-detail-") // 스레드 이름 prefix
            initialize()
        }
}