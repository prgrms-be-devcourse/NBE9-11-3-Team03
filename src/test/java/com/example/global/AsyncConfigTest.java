package com.example.global;

import com.example.global.config.AsyncConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    @DisplayName("AsyncConfig가 정상적으로 로딩된다")
    void asyncConfig_load_test() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AsyncConfig.class);

        assertThat(context).isNotNull();

        context.close();
    }
}