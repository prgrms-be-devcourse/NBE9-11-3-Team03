package com.example.domain.festival.repository

import com.example.domain.festival.converter.FestivalApiConverter
import com.example.domain.festival.dto.external.FestivalApiItem
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class FestivalRepositoryUpdateTest(
    @Autowired private val festivalRepository: FestivalRepository,
    @Autowired private val converter: FestivalApiConverter,
    @Autowired private val entityManager: EntityManager
) {
    // 목적. 같은 contentId를 가진 기존 축제가 있을 때, 목록 동기화용 updateFromListItem 로직이 정상적으로 기존 데이터를 수정하는지 검증
    @Test
    @DisplayName("같은 contentId 기존 데이터가 있으면 updateFromListItem로 수정된다")
    fun update_existing_festival_test() {
        val contentId = "update_test_content_id"
        val originalItem = createFestivalApiItem(
            contentId = contentId,
            title = "기존 축제 제목",
            overview = "기존 설명",
            addr1 = "경상남도 김해시",
            addr2 = "대성동"
        )

        val originalFestival = converter.toEntityFromListItem(originalItem)
        festivalRepository.save(originalFestival)
        entityManager.flush()
        entityManager.clear()

        val updatedItem = createFestivalApiItem(
            contentId = contentId,
            title = "수정된 축제 제목",
            overview = "수정된 설명",
            addr1 = "경상남도 김해시",
            addr2 = "봉황동"
        )

        val existingFestival = festivalRepository.findByContentId(contentId)
            .orElseThrow()

        converter.updateFromListItem(existingFestival, updatedItem)

        entityManager.flush()
        entityManager.clear()

        val updatedFestival = festivalRepository.findByContentId(contentId)
            .orElseThrow()

        assertThat(updatedFestival.contentId).isEqualTo(contentId)
        assertThat(updatedFestival.title).isEqualTo("수정된 축제 제목")
        assertThat(updatedFestival.overview).isEqualTo("기존 설명")
        assertThat(updatedFestival.address).isEqualTo("경상남도 김해시 봉황동")
    }

    private fun createFestivalApiItem(
        contentId: String,
        title: String,
        overview: String,
        addr1: String,
        addr2: String
    ): FestivalApiItem =
        FestivalApiItem(
            contentid = contentId,
            title = title,
            overview = overview,
            tel = "055-330-6840",
            firstimage = "image1.jpg",
            firstimage2 = "image2.jpg",
            addr1 = addr1,
            addr2 = addr2,
            mapx = "128.87",
            mapy = "35.23",
            eventstartdate = "20260430",
            eventenddate = "20260503"
        )
}
