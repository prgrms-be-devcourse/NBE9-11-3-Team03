package com.example.global

import com.example.global.config.AsyncConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class AsyncConfigTest {
    @Test
    @DisplayName("AsyncConfig가 정상적으로 로딩된다")
    fun `AsyncConfig가 정상적으로 로딩된다`() {
        AnnotationConfigApplicationContext(AsyncConfig::class.java).use { context ->
            assertThat(context).isNotNull()
        }
    }
}