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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class FestivalRepositoryDuplicateTest {
    // 목적. 같은 contentId를 가진 축제를 두 번 저장 시, UNIQUE 제약조건에 의해 예외가 발생하는지 검증
    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private FestivalApiConverter converter;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("같은 contentId 두 번 저장 시 UNIQUE 제약으로 예외 발생")
    void duplicate_contentId_test() {
        // given
        String duplicatedContentId = "duplicate_test_content_id";

        FestivalApiItem firstItem = createFestivalApiItem(
                duplicatedContentId,
                "첫 번째 축제"
        );

        FestivalApiItem secondItem = createFestivalApiItem(
                duplicatedContentId,
                "두 번째 축제"
        );

        // 첫 번째 저장
        Festival firstFestival = converter.toEntityFromListItem(firstItem);
        festivalRepository.save(firstFestival);

        // DB에 실제 반영되도록 flush
        em.flush();
        em.clear();

        // when & then
        Festival secondFestival = converter.toEntityFromListItem(secondItem);

        // save 호출 시점에 바로 예외가 터질 수 있으므로, save와 flush를 모두 감싸줍니다.
        assertThrows(DataIntegrityViolationException.class, () -> {
            festivalRepository.save(secondFestival);
            em.flush();
        });
    }

    // 테스트용 외부 API DTO 생성 메서드
    private FestivalApiItem createFestivalApiItem(String contentId, String title) {
        FestivalApiItem item = new FestivalApiItem();
        item.setContentid(contentId);
        item.setTitle(title);
        item.setOverview("테스트용 설명");
        item.setTel("055-330-6840");
        item.setFirstimage("image1.jpg");
        item.setFirstimage2("image2.jpg");
        item.setAddr1("경상남도 김해시");
        item.setAddr2("대성동");
        item.setMapx("128.87");
        item.setMapy("35.23");
        item.setEventstartdate("20260430");
        item.setEventenddate("20260503");
        return item;
    }
}