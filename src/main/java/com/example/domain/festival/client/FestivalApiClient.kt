package com.example.domain.festival.client;


import com.example.domain.festival.dto.external.FestivalApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// 공공 축제 API 호출 전용 클래스
@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalApiClient {

    // 재시도 정책 설정값
    private static final int MAX_RETRY_COUNT = 2;
    private static final long RETRY_DELAY_MS = 500L;

    // HTTP 요청 전송 객체
    private final RestTemplate restTemplate;

    @Value("${api.public-data.key}")
    private String serviceKey;

    @Value("${api.public-data.base-url}")
    private String baseUrl;

    // 축제 목록 조회 API 호출
    public FestivalApiResponse fetchFestivalList(
            int pageNo,
            int numOfRows,
            String eventStartDate
    ) {

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
                .build(true)
                .toUri();

        return executeWithRetry(uri);
    }

    // 축제 상세 조회 API 호출
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

    // 외부 API 호출 재시도 로직
    private FestivalApiResponse executeWithRetry(URI uri) {

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {

            try {

                log.debug(
                        "[FestivalApi] 외부 API 호출 시도 - attempt={}, uri={}",
                        attempt,
                        uri
                );

                return restTemplate.getForObject(
                        uri,
                        FestivalApiResponse.class
                );

            } catch (HttpClientErrorException.TooManyRequests e) {

                // 429는 quota 보호를 위해 즉시 중단
                log.warn(
                        "[FestivalApi] 외부 API 호출 한도 초과 - uri={}",
                        uri
                );

                throw e;

            } catch (HttpServerErrorException e) {

                // 5xx 서버 오류 로그
                log.warn(
                        "[FestivalApi] 외부 API 서버 오류 - attempt={}, status={}, uri={}",
                        attempt,
                        e.getStatusCode(),
                        uri
                );

                // 502 / 503 / 504만 재시도
                if (!isRetryableServerError(e)
                        || attempt == MAX_RETRY_COUNT) {
                    throw e;
                }

                sleepRetryDelay();
            }
        }

        throw new IllegalStateException(
                "외부 API 재시도 로직이 비정상 종료되었습니다."
        );
    }

    // 재시도 대상 서버 오류 정의
    private boolean isRetryableServerError(HttpServerErrorException e) {

        HttpStatus status =
                HttpStatus.valueOf(e.getStatusCode().value());

        return status == HttpStatus.BAD_GATEWAY
                || status == HttpStatus.SERVICE_UNAVAILABLE
                || status == HttpStatus.GATEWAY_TIMEOUT;
    }

    // 재시도 대기
    private void sleepRetryDelay() {

        try {

            Thread.sleep(RETRY_DELAY_MS);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.error(
                    "[FestivalApi] 외부 API 재시도 대기 중 인터럽트 발생 - message={}",
                    e.getMessage(),
                    e
            );

            throw new IllegalStateException(
                    "외부 API 재시도 대기 중 인터럽트가 발생했습니다.",
                    e
            );
        }
    }
}