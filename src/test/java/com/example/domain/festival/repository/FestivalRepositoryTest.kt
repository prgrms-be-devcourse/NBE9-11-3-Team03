package com.example.domain.festival.repository

import com.example.domain.festival.converter.FestivalApiConverter
import com.example.domain.festival.dto.external.FestivalApiItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class FestivalRepositoryTest(
    @Autowired private val festivalRepository: FestivalRepository,
    @Autowired private val converter: FestivalApiConverter
) {
    // 목적. DB 저장 테스트
    @Test
    @DisplayName("Festival 저장 및 조회 테스트")
    fun festival_save_and_find_test() {
        val testContentId = "694576_${System.currentTimeMillis()}"
        val item = createFestivalApiItem(testContentId)

        val festival = converter.toEntityFromListItem(item)
        festivalRepository.saveAndFlush(festival)

        val saved = festivalRepository.findByContentId(testContentId)
            .orElseThrow()

        assertThat(saved.contentId).isEqualTo(testContentId)
        assertThat(saved.title).isEqualTo("가야문화축제")
        assertThat(saved.address).isEqualTo("경상남도 김해시 대성동")
    }

    private fun createFestivalApiItem(contentId: String): FestivalApiItem =
        FestivalApiItem(
            contentid = contentId,
            title = "가야문화축제",
            overview = "설명",
            tel = "055-330-6840",
            firstimage = "image1.jpg",
            firstimage2 = "image2.jpg",
            addr1 = "경상남도 김해시",
            addr2 = "대성동",
            mapx = "128.87",
            mapy = "35.23",
            eventstartdate = "20260430",
            eventenddate = "20260530"
        )
}
