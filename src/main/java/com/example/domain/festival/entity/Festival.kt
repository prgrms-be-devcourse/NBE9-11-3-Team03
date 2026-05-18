package com.example.domain.festival.entity

import com.example.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "festival",
    indexes = [
        Index(
            name = "idx_festival_status_location",
            columnList = "status, mapx, mapy"
        ), Index(
            name = "idx_festival_region_date",
            columnList = "L_DONG_REGN_CD, startDate"
        ), Index(name = "idx_festival_status_date", columnList = "status, start_date")]
)
class Festival(
// API 축제 고유 ID
    @Column(nullable = false, unique = true)
    val contentId: String,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var overview: String,
    @Column(nullable = false)
    var address: String,
    @Column(nullable = false)
    var startDate: LocalDateTime,
    @Column(nullable = false)
    var endDate: LocalDateTime,
    // 경도
    @Column(nullable = false)
    var mapX: Double,
    // 위도
    @Column(nullable = false)
    var mapY: Double,

    var contactNumber: String? = null,
    var firstImageUrl: String? = null,
    var thumbnailUrl: String? = null,

    @Column(length = 500)
    var homepageUrl: String? = null,

    // 법정동 코드
    var lDongRegnCd: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FestivalStatus = FestivalStatus.UPCOMING,

    @Column(nullable = false)
    var viewCount: Int = 0,

    @Column(nullable = false)
    var bookMarkCount: Int = 0,

    @Column(nullable = false)
    var averageRate: Double = 0.0,
) : BaseEntity() {

    fun updateAverageRating(averageRating: Double) {
        this.averageRate = averageRating
    }

    //기존 축제 데이터 갱신용 메서드 (목록동기화)
    //contentId는 외부 식별자이므로 보통 수정하지 않는다.
    fun updateFestivalInfo(
        title: String,
        overview: String,
        contactNumber: String?,
        firstImageUrl: String?,
        thumbnailUrl: String?,
        address: String,
        homepageUrl: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        mapX: Double,
        mapY: Double,
        lDongRegnCd: String?,
        status: FestivalStatus
    ) {
        this.title = title
        this.overview = overview
        this.contactNumber = contactNumber
        this.firstImageUrl = firstImageUrl
        this.thumbnailUrl = thumbnailUrl
        this.address = address
        this.homepageUrl = homepageUrl
        this.startDate = startDate
        this.endDate = endDate
        this.mapX = mapX
        this.mapY = mapY
        this.lDongRegnCd = lDongRegnCd
        this.status = status
    }


    // 기존 축제 데이터 갱신용 메서드 (상세 동기화)
    fun updateFestivalDetailInfo(
        overview: String,
        homepageUrl: String?
    ) {
        this.overview = overview
        this.homepageUrl = homepageUrl
    }

    //축제 목록 변경 여부 판단 메서드
    fun isSameListInfo(
        title: String,
        contactNumber: String?,
        firstImageUrl: String?,
        thumbnailUrl: String?,
        address: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        mapX: Double,
        mapY: Double,
        lDongRegnCd: String?,
        status: FestivalStatus
    ): Boolean {
        return this.title == title
                && this.contactNumber == contactNumber
                && this.firstImageUrl == firstImageUrl
                && this.thumbnailUrl == thumbnailUrl
                && this.address == address
                && this.startDate == startDate
                && this.endDate == endDate
                && this.mapX == mapX
                && this.mapY == mapY
                && this.lDongRegnCd == lDongRegnCd
                && this.status == status
    }

    //축제 상세 장보 변경 여부 판단 메서드
    fun isSameDetailInfo(
        overview: String,
        homepageUrl: String?
    ): Boolean =
        this.overview == overview && this.homepageUrl == homepageUrl


    //축제 찜 수 1 증가
    fun increaseBookmarkCount() {
        this.bookMarkCount++
    }

    // 축제 찜 수 1 감소
    fun decreaseBookmarkCount() {
        if (this.bookMarkCount > 0) this.bookMarkCount--

    }
}