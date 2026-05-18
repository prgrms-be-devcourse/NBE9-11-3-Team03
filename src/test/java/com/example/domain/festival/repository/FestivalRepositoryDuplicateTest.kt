package com.example.domain.festival.repository

import com.example.domain.festival.converter.FestivalApiConverter
import com.example.domain.festival.dto.external.FestivalApiItem
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class FestivalRepositoryDuplicateTest(
    @Autowired private val festivalRepository: FestivalRepository,
    @Autowired private val converter: FestivalApiConverter,
    @Autowired private val entityManager: EntityManager
) {
    // 목적. 같은 contentId를 가진 축제를 두 번 저장 시, UNIQUE 제약조건에 의해 예외가 발생하는지 검증
    @Test
    @DisplayName("같은 contentId 두 번 저장 시 UNIQUE 제약으로 예외 발생")
    fun duplicate_contentId_test() {
        val duplicatedContentId = "duplicate_test_content_id"
        val firstItem = createFestivalApiItem(
            contentId = duplicatedContentId,
            title = "첫 번째 축제"
        )
        val secondItem = createFestivalApiItem(
            contentId = duplicatedContentId,
            title = "두 번째 축제"
        )

        val firstFestival = converter.toEntityFromListItem(firstItem)
        festivalRepository.save(firstFestival)

        entityManager.flush()
        entityManager.clear()

        val secondFestival = converter.toEntityFromListItem(secondItem)

        assertThatThrownBy {
            festivalRepository.save(secondFestival)
            entityManager.flush()
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun createFestivalApiItem(
        contentId: String,
        title: String
    ): FestivalApiItem =
        FestivalApiItem(
            contentid = contentId,
            title = title,
            overview = "테스트용 설명",
            tel = "055-330-6840",
            firstimage = "image1.jpg",
            firstimage2 = "image2.jpg",
            addr1 = "경상남도 김해시",
            addr2 = "대성동",
            mapx = "128.87",
            mapy = "35.23",
            eventstartdate = "20260430",
            eventenddate = "20260503"
        )
}
