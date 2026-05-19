package com.example.domain.festival.converter

import com.example.domain.festival.dto.external.FestivalApiItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FestivalApiConverterTest {
    private val converter = FestivalApiConverter()

    @Test
    @DisplayName("외부 API 목록 응답 DTO를 Festival 엔티티로 변환한다")
    fun convertToFestivalEntity_test() {
        val item = createApiItem()

        val festival = converter.toEntityFromListItem(item)

        assertThat(festival.contentId).isEqualTo("694576")
        assertThat(festival.title).isEqualTo("가야문화축제")
        assertThat(festival.overview).isEqualTo("설명")
        assertThat(festival.contactNumber).isEqualTo("055-330-6840")
        assertThat(festival.firstImageUrl).isEqualTo("image1.jpg")
        assertThat(festival.thumbnailUrl).isEqualTo("image2.jpg")
        assertThat(festival.address).isEqualTo("경상남도 김해시 대성동")
        assertThat(festival.mapX).isEqualTo(128.87)
        assertThat(festival.mapY).isEqualTo(35.23)
        assertThat(festival.startDate).isEqualTo(LocalDateTime.of(2026, 4, 30, 0, 0))
        assertThat(festival.endDate).isEqualTo(LocalDateTime.of(2026, 5, 30, 23, 59, 59))
        assertThat(festival.status).isNotNull()
    }

    @Test
    @DisplayName("firstimage2가 없으면 firstimage를 썸네일로 사용한다")
    fun resolveThumbnail_fallback_test() {
        val item = createApiItem(firstimage2 = null)

        val festival = converter.toEntityFromListItem(item)

        assertThat(festival.thumbnailUrl).isEqualTo("image1.jpg")
    }

    @Test
    @DisplayName("overview가 비어 있으면 기본 설명으로 대체한다")
    fun defaultOverview_test() {
        val item = createApiItem(overview = " ")

        val festival = converter.toEntityFromListItem(item)

        assertThat(festival.overview).isEqualTo("상세 설명 없음")
    }

    private fun createApiItem(
        overview: String? = "설명",
        firstimage2: String? = "image2.jpg"
    ): FestivalApiItem =
        FestivalApiItem(
            contentid = "694576",
            title = "가야문화축제",
            tel = "055-330-6840",
            addr1 = "경상남도 김해시",
            addr2 = "대성동",
            overview = overview,
            firstimage = "image1.jpg",
            firstimage2 = firstimage2,
            mapx = "128.87",
            mapy = "35.23",
            eventstartdate = "20260430",
            eventenddate = "20260530"
        )
}
