package com.example.global;

import com.example.global.exceptionHandler.GlobalExceptionHandler;
import com.example.global.rsData.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("429 예외는 429 응답과 전용 메시지로 변환된다")
    void handleTooManyRequests() {
        HttpClientErrorException.TooManyRequests exception =
                (HttpClientErrorException.TooManyRequests) HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                );

        ResponseEntity<RsData<Void>> response = handler.handleTooManyRequests(exception);

        assertEquals(429, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("429", response.getBody().status());
        assertTrue(response.getBody().message().contains("동기화가 중단"));
    }

    @Test
    @DisplayName("502 예외는 502 응답과 불안정 메시지로 변환된다")
    void handleBadGateway() {
        ResponseEntity<RsData<Void>> response =
                handler.handleHttpServerErrorException(
                        new HttpServerErrorException(HttpStatus.BAD_GATEWAY)
                );

        assertEquals(502, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("502", response.getBody().status());
        assertTrue(response.getBody().message().contains("응답이 불안정"));
    }
}