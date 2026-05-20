package com.example.domain.festival.client

import com.example.domain.festival.dto.external.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

class FestivalApiClientTest {
    private val restTemplate = mock(RestTemplate::class.java)
    private val festivalApiClient = FestivalApiClient(
        restTemplate = restTemplate,
        serviceKey = "test-service-key",
        baseUrl = "https://apis.data.go.kr/B551011/KorService2"
    )

    @Test
    @DisplayName("축제 목록 조회 API 호출 URI를 생성하고 응답을 반환한다")
    fun fetchFestivalList_test() {
        val apiResponse = createResponse()

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willReturn(apiResponse)

        val response = festivalApiClient.fetchFestivalList(1, 10, "20260101")

        val uriCaptor = ArgumentCaptor.forClass(URI::class.java)
        verify(restTemplate).getForObject(uriCaptor.capture(), eq(FestivalApiResponse::class.java))

        assertThat(response).isEqualTo(apiResponse)
        assertThat(uriCaptor.value.toString()).contains("/searchFestival2")
        assertThat(uriCaptor.value.toString()).contains("pageNo=1")
        assertThat(uriCaptor.value.toString()).contains("numOfRows=10")
        assertThat(uriCaptor.value.toString()).contains("eventStartDate=20260101")
    }

    @Test
    @DisplayName("축제 상세 조회 API 호출 URI를 생성하고 응답을 반환한다")
    fun fetchFestivalDetail_test() {
        val apiResponse = createResponse()

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willReturn(apiResponse)

        val response = festivalApiClient.fetchFestivalDetail("1001")

        val uriCaptor = ArgumentCaptor.forClass(URI::class.java)
        verify(restTemplate).getForObject(uriCaptor.capture(), eq(FestivalApiResponse::class.java))

        assertThat(response).isEqualTo(apiResponse)
        assertThat(uriCaptor.value.toString()).contains("/detailCommon2")
        assertThat(uriCaptor.value.toString()).contains("contentId=1001")
    }

    @Test
    @DisplayName("429 응답은 재시도하지 않고 즉시 예외를 전파한다")
    fun fetchFestivalList_tooManyRequests_test() {
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
        }.isSameAs(exception)

        verify(restTemplate, times(1))
            .getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java))
    }

    @Test
    @DisplayName("재시도 대상 5xx 응답은 최대 재시도 횟수만큼 호출 후 예외를 전파한다")
    fun fetchFestivalList_retryableServerError_test() {
        val exception = HttpServerErrorException(HttpStatus.BAD_GATEWAY)

        given(restTemplate.getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java)))
            .willThrow(exception)

        assertThatThrownBy {
            festivalApiClient.fetchFestivalList(1, 10, "20260101")
        }.isSameAs(exception)

        verify(restTemplate, times(2))
            .getForObject(any(URI::class.java), eq(FestivalApiResponse::class.java))
    }

    private fun createResponse(): FestivalApiResponse {
        val body = FestivalApiBody().apply {
            setPrivateItems(
                FestivalApiItems(
                    item = listOf(
                        FestivalApiItem(
                            contentid = "1001",
                            title = "가야문화축제"
                        )
                    )
                )
            )
            numOfRows = 1
            pageNo = 1
            totalCount = 1
        }

        return FestivalApiResponse(
            response = FestivalApiResponse.Response(
                header = FestivalApiHeader(
                    resultCode = "0000",
                    resultMsg = "OK"
                ),
                body = body
            )
        )
    }

    private fun FestivalApiBody.setPrivateItems(items: FestivalApiItems) {
        val field = javaClass.getDeclaredField("items")
        field.isAccessible = true
        field.set(this, items)
    }
}
