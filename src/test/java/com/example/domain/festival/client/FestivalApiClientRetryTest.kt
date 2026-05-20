package com.example.domain.festival.client

import com.example.domain.festival.dto.external.FestivalApiResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpClientErrorException.TooManyRequests
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

internal class FestivalApiClientRetryTest {
    private val restTemplate = mock(RestTemplate::class.java)
    private val festivalApiClient = FestivalApiClient(
        restTemplate = restTemplate,
        serviceKey = "test-key",
        baseUrl = "https://test.com"
    )

    @Test
    @DisplayName("429 발생 시 재시도 없이 즉시 예외를 던진다")
    fun fetchFestivalList_TooManyRequests_ThrowsImmediately() {
        val exception = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too Many Requests",
            HttpHeaders.EMPTY,
            ByteArray(0),
            null
        )

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willThrow(exception)

        assertThatThrownBy {
            festivalApiClient.fetchFestivalList(1, 10, "20260101")
        }.isInstanceOf(TooManyRequests::class.java)

        verify(restTemplate, times(1))
            .getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java))
    }

    @Test
    @DisplayName("502 발생 시 최대 2번까지 재시도 후 실패한다")
    fun fetchFestivalList_BadGateway_RetriesThenThrows() {
        val exception = HttpServerErrorException(HttpStatus.BAD_GATEWAY)

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willThrow(exception)

        assertThatThrownBy {
            festivalApiClient.fetchFestivalList(1, 10, "20260101")
        }.isSameAs(exception)

        verify(restTemplate, times(2))
            .getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java))
    }

    @Test
    @DisplayName("502 후 두 번째 시도에서 성공하면 응답을 반환한다")
    fun fetchFestivalList_BadGateway_ThenSuccess() {
        val response = FestivalApiResponse()

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willThrow(HttpServerErrorException(HttpStatus.BAD_GATEWAY))
            .willReturn(response)

        val result = festivalApiClient.fetchFestivalList(1, 10, "20260101")

        assertThat(result).isSameAs(response)
        verify(restTemplate, times(2))
            .getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java))
    }

    @Test
    @DisplayName("500은 재시도 대상이 아니므로 즉시 실패한다")
    fun fetchFestivalList_InternalServerError_NoRetry() {
        val exception = HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willThrow(exception)

        assertThatThrownBy {
            festivalApiClient.fetchFestivalList(1, 10, "20260101")
        }.isSameAs(exception)

        verify(restTemplate, times(1))
            .getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java))
    }

    @Test
    @DisplayName("상세 조회도 502 발생 시 최대 2번까지 재시도한다")
    fun fetchFestivalDetail_BadGateway_RetriesThenThrows() {
        val exception = HttpServerErrorException(HttpStatus.BAD_GATEWAY)

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willThrow(exception)

        assertThatThrownBy {
            festivalApiClient.fetchFestivalDetail("content-1")
        }.isSameAs(exception)

        verify(restTemplate, times(2))
            .getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java))
    }
}
