package com.example.domain.festival.client;


import com.example.domain.festival.dto.external.FestivalApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

//공공 축제 API 호출 전용 클래스
@Component
@RequiredArgsConstructor
public class FestivalApiClient {

    //재시도 정책 설정값 추가
    private static final int MAX_RETRY_COUNT = 2;
    private static final long RETRY_DELAY_MS = 500L;

    //HTTP 요청을 보내기 위한 스프링 제공 객체
    private final RestTemplate restTemplate;

    //application.yaml 파일에 공공 API 인증키 및 base URL을 저장해야함
    @Value("${api.public-data.key}")
    private String serviceKey;

    @Value("${api.public-data.base-url}")
    private String baseUrl;


    //축제 목록 조회 API 호출
    public FestivalApiResponse fetchFestivalList(int pageNo, int numOfRows, String eventStartDate) {

        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/searchFestival2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "TestApp")
                .queryParam("_type", "json")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("eventStartDate", eventStartDate)
                .build(true) // 인코딩 유지
                .toUri();

        return executeWithRetry(uri);
    }


    //축제 상세 조회 API 호출
    public FestivalApiResponse fetchFestivalDetail(String contentId) {

        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/detailCommon2")
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "TestApp")
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("serviceKey", serviceKey)
                .build(true)
                .toUri();

        return executeWithRetry(uri);
    }


    //  외부 API 호출 재시도 로직 추가 (429 즉시 실패, 5xx 일부만 재시도)
    private FestivalApiResponse executeWithRetry(URI uri) {
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                // 재시도 횟수 확인용 로그
                System.out.println("외부 API 호출 시도: " + attempt);

                return restTemplate.getForObject(uri, FestivalApiResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                // 429는 quota 보호를 위해 재시도 하지 않고, 즉시 throw
                throw e;
            } catch (HttpServerErrorException e) {
                //실패 로그 추가 (몇 번째 실패인지 확인)
                System.out.println("API 호출 실패 (시도 " + attempt + "), status=" + e.getStatusCode());

                // 502/503/504만 재시도, 그 외 즉시 throw
                if (!isRetryableServerError(e) || attempt == MAX_RETRY_COUNT) {
                    throw e;
                }

                sleepRetryDelay();
            }
        }

        throw new IllegalStateException("외부 API 재시도 로직이 비정상 종료되었습니다.");
    }


    // 재시도 대상 서버 오류 정의
    private boolean isRetryableServerError(HttpServerErrorException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        return status == HttpStatus.BAD_GATEWAY
                || status == HttpStatus.SERVICE_UNAVAILABLE
                || status == HttpStatus.GATEWAY_TIMEOUT;
    }

    // 재시도 간격 (짧게 유지 - quota 보호)
    private void sleepRetryDelay() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("외부 API 재시도 대기 중 인터럽트가 발생했습니다.", e);
        }
    }
}