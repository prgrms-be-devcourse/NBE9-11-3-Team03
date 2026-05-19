package com.example.global

import com.example.global.exceptionHandler.GlobalExceptionHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpClientErrorException.TooManyRequests
import org.springframework.web.client.HttpServerErrorException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    @DisplayName("429 예외는 429 응답과 전용 메시지로 변환된다")
    fun handleTooManyRequests() {
        // given
        val exception = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too Many Requests",
            HttpHeaders.EMPTY,
            ByteArray(0),
            null
        ) as TooManyRequests

        // when
        val response = handler.handleTooManyRequests(exception)

        // then
        assertThat(response.statusCode.value()).isEqualTo(429)

        // !! 대신 requireNotNull을 사용하면 안전하게 Non-null 타입으로 스마트 캐스팅됩니다.
        val body = requireNotNull(response.body) { "응답 Body는 null일 수 없습니다." }
        assertThat(body.status).isEqualTo("429")
        assertThat(body.message).contains("동기화가 중단")
    }

    @Test
    @DisplayName("502 예외는 502 응답과 불안정 메시지로 변환된다")
    fun handleBadGateway() {
        // given
        val exception = HttpServerErrorException(HttpStatus.BAD_GATEWAY)

        // when
        val response = handler.handleHttpServerErrorException(exception)

        // then
        assertThat(response.statusCode.value()).isEqualTo(502)

        val body = requireNotNull(response.body) { "응답 Body는 null일 수 없습니다." }
        assertThat(body.status).isEqualTo("502")
        assertThat(body.message).contains("응답이 불안정")
    }
}