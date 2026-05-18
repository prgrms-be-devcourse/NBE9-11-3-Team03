package com.example.domain.festival.repository;

import com.example.domain.festival.converter.FestivalApiConverter;
import com.example.domain.festival.dto.external.FestivalApiItem;
import com.example.domain.festival.entity.Festival;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
//@Transactional
@Rollback(false)
public class FestivalRepositoryTest {
    // 목적. DB 저장 테스트
    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private FestivalApiConverter converter;

    @Test
    @DisplayName("Festival 저장 및 조회 테스트")
    void festival_save_and_find_test() {
        String testContentId = "694576_" + System.currentTimeMillis();

        FestivalApiItem item = new FestivalApiItem();
        item.setContentid(testContentId);
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

        Festival festival = converter.toEntityFromListItem(item);
        festivalRepository.saveAndFlush(festival);

        Festival saved = festivalRepository.findByContentId(testContentId)
                .orElseThrow();

        System.out.println("saved id = " + saved.getId());
        System.out.println("saved contentId = " + saved.getContentId());

        assertThat(saved.getContentId()).isEqualTo(testContentId);
        assertThat(saved.getTitle()).isEqualTo("가야문화축제");
        assertThat(saved.getAddress()).isEqualTo("경상남도 김해시 대성동");
    }
}