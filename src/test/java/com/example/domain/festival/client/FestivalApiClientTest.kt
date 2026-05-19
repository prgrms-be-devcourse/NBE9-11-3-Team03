package com.example.domain.festival.client;

import com.example.domain.festival.dto.external.FestivalApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FestivalApiClientTest {
    //목적. FestivalApiClient 실제 호출 테스트(공공 API가 정상 호출되는지, 응답이 DTO로 잘 매핑되는지)
    @Autowired
    private FestivalApiClient festivalApiClient;

    @Test
    @DisplayName("공공 API 축제 목록 조회 테스트")
    void fetch_festival_list_test() {
        FestivalApiResponse response = festivalApiClient.fetchFestivalList(1, 10, "20260101");

        assertThat(response).isNotNull();
        assertThat(response.getResponse()).isNotNull();
        assertThat(response.getResponse().getHeader()).isNotNull();
        assertThat(response.getResponse().getHeader().getResultCode()).isEqualTo("0000");
        assertThat(response.getResponse().getBody()).isNotNull();
        assertThat(response.getResponse().getBody().getItems()).isNotNull();
        assertThat(response.getResponse().getBody().getItems().getItem()).isNotEmpty();
    }

    @Test
    @DisplayName("공공 API 축제 상세 조회 테스트")
    void fetch_festival_detail_test() {
        // 1. 먼저 목록을 조회하여 실제 존재하는 contentId를 하나 가져옵니다.
        FestivalApiResponse listResponse = festivalApiClient.fetchFestivalList(1, 10, "20260101");

        // 목록 데이터가 있는지 먼저 검증
        assertThat(listResponse.getResponse().getBody().getItems()).isNotNull();
        var firstItem = listResponse.getResponse().getBody().getItems().getItem().get(0);
        String validContentId = firstItem.getContentid(); // 실제 존재하는 ID 추출

        // 2. 추출한 실제 ID로 상세 조회를 시도합니다.
        FestivalApiResponse response = festivalApiClient.fetchFestivalDetail(validContentId);

        // 3. 단계별 검증 (Null 체크 포함)
        assertThat(response).isNotNull();
        assertThat(response.getResponse().getHeader().getResultCode()).isEqualTo("0000");

        assertThat(response.getResponse().getBody())
                .withFailMessage("API 응답의 Body가 null입니다.")
                .isNotNull();

        assertThat(response.getResponse().getBody().getItems())
                .withFailMessage("해당 ID(%s)에 대한 상세 정보(items)가 없습니다.", validContentId)
                .isNotNull();

        // 4. 핵심 필드 확인
        var items = response.getResponse().getBody().getItems().getItem();
        assertThat(items).isNotEmpty();

        var item = items.get(0);
        // 상세 정보 데이터가 존재할 때만 필드 검증
        assertThat(item.getOverview()).isNotNull();
        assertThat(item.getTitle()).isEqualTo(firstItem.getTitle());
    }
}
