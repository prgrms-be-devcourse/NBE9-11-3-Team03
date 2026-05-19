package com.example.domain.festival.repository;

import com.example.domain.festival.converter.FestivalApiConverter;
import com.example.domain.festival.dto.external.FestivalApiItem;
import com.example.domain.festival.entity.Festival;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class FestivalRepositoryUpdateTest {
    // 목적. 같은 contentId를 가진 기존 축제가 있을 때, 목록 동기화용 updateFromListItem 로직이 정상적으로 기존 데이터를 수정하는지 검증
    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private FestivalApiConverter converter;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("같은 contentId 기존 데이터가 있으면 updateFromListItem로 수정된다")
    void update_existing_festival_test() {
        // given
        String contentId = "update_test_content_id";

        // 최초 저장용 데이터
        FestivalApiItem originalItem = createFestivalApiItem(
                contentId,
                "기존 축제 제목",
                "기존 설명",
                "경상남도 김해시",
                "대성동"
        );

        Festival originalFestival = converter.toEntityFromListItem(originalItem);
        festivalRepository.save(originalFestival);
        em.flush();
        em.clear();

        // 수정용 새 데이터
        FestivalApiItem updatedItem = createFestivalApiItem(
                contentId,
                "수정된 축제 제목",
                "수정된 설명",
                "경상남도 김해시",
                "봉황동"
        );

        // when
        Festival existingFestival = festivalRepository.findByContentId(contentId)
                .orElseThrow();

        converter.updateFromListItem(existingFestival, updatedItem);

        em.flush();
        em.clear();

        // then
        Festival updatedFestival = festivalRepository.findByContentId(contentId)
                .orElseThrow();

        assertThat(updatedFestival.getContentId()).isEqualTo(contentId);
        assertThat(updatedFestival.getTitle()).isEqualTo("수정된 축제 제목");
        assertThat(updatedFestival.getOverview()).isEqualTo("기존 설명");
        assertThat(updatedFestival.getAddress()).isEqualTo("경상남도 김해시 봉황동");
    }

    // 테스트용 외부 API DTO 생성 메서드
    private FestivalApiItem createFestivalApiItem(
            String contentId,
            String title,
            String overview,
            String addr1,
            String addr2
    ) {
        FestivalApiItem item = new FestivalApiItem();
        item.setContentid(contentId);
        item.setTitle(title);
        item.setOverview(overview);
        item.setTel("055-330-6840");
        item.setFirstimage("image1.jpg");
        item.setFirstimage2("image2.jpg");
        item.setAddr1(addr1);
        item.setAddr2(addr2);
        item.setMapx("128.87");
        item.setMapy("35.23");
        item.setEventstartdate("20260430");
        item.setEventenddate("20260503");
        return item;
    }
}
