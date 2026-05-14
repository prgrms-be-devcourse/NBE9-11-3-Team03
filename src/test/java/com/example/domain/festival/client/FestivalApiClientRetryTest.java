package com.example.domain.festival.client;

import com.example.domain.festival.dto.external.FestivalApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FestivalApiClientRetryTest {

    private RestTemplate restTemplate;
    private FestivalApiClient festivalApiClient;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        festivalApiClient = new FestivalApiClient(restTemplate);

        ReflectionTestUtils.setField(festivalApiClient, "serviceKey", "test-key");
        ReflectionTestUtils.setField(festivalApiClient, "baseUrl", "http://test.com");
    }

    @Test
    @DisplayName("429 발생 시 재시도 없이 즉시 예외를 던진다")
    void fetchFestivalList_TooManyRequests_ThrowsImmediately() {
        when(restTemplate.getForObject(any(URI.class), eq(FestivalApiResponse.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        assertThrows(HttpClientErrorException.TooManyRequests.class,
                () -> festivalApiClient.fetchFestivalList(1, 10, "20260101"));

        verify(restTemplate, times(1))
                .getForObject(any(URI.class), eq(FestivalApiResponse.class));
    }

    @Test
    @DisplayName("502 발생 시 최대 2번까지 재시도 후 실패한다")
    void fetchFestivalList_BadGateway_RetriesThenThrows() {
        when(restTemplate.getForObject(any(URI.class), eq(FestivalApiResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        assertThrows(HttpServerErrorException.class,
                () -> festivalApiClient.fetchFestivalList(1, 10, "20260101"));

        verify(restTemplate, times(2))
                .getForObject(any(URI.class), eq(FestivalApiResponse.class));
    }

    @Test
    @DisplayName("502 후 두 번째 시도에서 성공하면 응답을 반환한다")
    void fetchFestivalList_BadGateway_ThenSuccess() {
        FestivalApiResponse response = new FestivalApiResponse();

        when(restTemplate.getForObject(any(URI.class), eq(FestivalApiResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))
                .thenReturn(response);

        FestivalApiResponse result = festivalApiClient.fetchFestivalList(1, 10, "20260101");

        assertNotNull(result);
        assertSame(response, result);

        verify(restTemplate, times(2))
                .getForObject(any(URI.class), eq(FestivalApiResponse.class));
    }

    @Test
    @DisplayName("500은 재시도 대상이 아니므로 즉시 실패한다")
    void fetchFestivalList_InternalServerError_NoRetry() {
        when(restTemplate.getForObject(any(URI.class), eq(FestivalApiResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(HttpServerErrorException.class,
                () -> festivalApiClient.fetchFestivalList(1, 10, "20260101"));

        verify(restTemplate, times(1))
                .getForObject(any(URI.class), eq(FestivalApiResponse.class));
    }

    @Test
    @DisplayName("상세 조회도 502 발생 시 최대 2번까지 재시도한다")
    void fetchFestivalDetail_BadGateway_RetriesThenThrows() {
        when(restTemplate.getForObject(any(URI.class), eq(FestivalApiResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        assertThrows(HttpServerErrorException.class,
                () -> festivalApiClient.fetchFestivalDetail("content-1"));

        verify(restTemplate, times(2))
                .getForObject(any(URI.class), eq(FestivalApiResponse.class));
    }
}