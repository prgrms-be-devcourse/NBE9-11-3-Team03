package com.example.domain.festival.converter;

import com.example.domain.festival.dto.external.FestivalApiItem;
import com.example.domain.festival.entity.Festival;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FestivalApiConverterTest {
    private final FestivalApiConverter converter = new FestivalApiConverter();

    @Test
    // 목적. Converter → Entity 변환
    void ConvertToFestivalEntity_test() {
        // given
        FestivalApiItem item = new FestivalApiItem();

        // 수동으로 값 세팅 (테스트용)
        item.setContentid("694576");
        item.setTitle("가야문화축제");
        item.setOverview("설명");
        item.setTel("055-330-6840");
        item.setFirstimage("image1.jpg");
        item.setFirstimage2("image2.jpg");
        item.setAddr1("경상남도 김해시");
        item.setAddr2("대성동");
        item.setMapx("128.87");
        item.setMapy("35.23");
        item.setEventstartdate("20260430");
        item.setEventenddate("20260530");

        // when
        Festival festival = converter.toEntityFromListItem(item);

        // then
        assertThat(festival.getContentId()).isEqualTo("694576");
        assertThat(festival.getTitle()).isEqualTo("가야문화축제");
        assertThat(festival.getAddress()).isEqualTo("경상남도 김해시 대성동");
        assertThat(festival.getMapX()).isEqualTo(128.87);
        assertThat(festival.getMapY()).isEqualTo(35.23);
        assertThat(festival.getStartDate()).isNotNull();
        assertThat(festival.getEndDate()).isNotNull();
        assertThat(festival.getStatus()).isNotNull();
    }
}