package com.example.domain.festival.entity;

import com.example.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "festival", indexes = {
        // 1. 지도 뷰: 상태와 좌표를 묶은 복합 인덱스
        @Index(name = "idx_festival_status_location", columnList = "status, mapx, mapy"),

        // 2. 리스트 뷰 (지역별): 지역코드와 시작일을 묶은 복합 인덱스
        @Index(name = "idx_festival_region_date", columnList = "L_DONG_REGN_CD, startDate"),

        // 3. 리스트 뷰 (상태별): 상태와 시작일을 묶은 복합 인덱스
        @Index(name = "idx_festival_status_date", columnList = "status, start_date")
})
public class Festival extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String contentId; // API 축제 고유 ID

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String overview;

    private String contactNumber;

    private String firstImageUrl;

    private String thumbnailUrl;

    @Column(nullable = false)
    private String address;

    @Column(length = 500)
    private String homepageUrl;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Double mapX; // 경도

    @Column(nullable = false)
    private Double mapY; // 위도

    private String lDongRegnCd; // 법정동 코드

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FestivalStatus status = FestivalStatus.UPCOMING;

    @Builder.Default
    @Column(nullable = false)
    private Integer viewCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer bookMarkCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Double averageRate = 0.0;


    //초기 데이터용 삭제
    public Festival(
            String contentId,
            String title,
            String overview,
            String address,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double mapX,
            Double mapY
    ) {
        this.contentId = contentId;
        this.title = title;
        this.overview = overview;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.mapX = mapX;
        this.mapY = mapY;

        // 기본값 세팅
        this.status = FestivalStatus.UPCOMING;
        this.viewCount = 0;
        this.bookMarkCount = 0;
        this.averageRate = 0.0;
    }
    public void updateAverageRating(Double averageRating) {
        this.averageRate = averageRating;
    }

    //기존 축제 데이터 갱신용 메서드 (목록동기화)
    //contentId는 외부 식별자이므로 보통 수정하지 않는다.
    public void updateFestivalInfo(
            String title,
            String overview,
            String contactNumber,
            String firstImageUrl,
            String thumbnailUrl,
            String address,
            String homepageUrl,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double mapX,
            Double mapY,
            String lDongRegnCd,
            FestivalStatus status
    ) {
        this.title = title;
        this.overview = overview;
        this.contactNumber = contactNumber;
        this.firstImageUrl = firstImageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.address = address;
        this.homepageUrl = homepageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.mapX = mapX;
        this.mapY = mapY;
        this.lDongRegnCd = lDongRegnCd;
        this.status = status;
    }


    // 기존 축제 데이터 갱신용 메서드 (상세 동기화)
    public void updateFestivalDetailInfo(
            String overview,
            String homepageUrl
    ) {
        this.overview = overview;
        this.homepageUrl = homepageUrl;
    }

    //축제 목록 변경 여부 판단 메서드
    public boolean isSameListInfo(
            String title,
            String contactNumber,
            String firstImageUrl,
            String thumbnailUrl,
            String address,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double mapX,
            Double mapY,
            String lDongRegnCd,
            FestivalStatus status
    ) {
        return Objects.equals(this.title, title)
                && Objects.equals(this.contactNumber, contactNumber)
                && Objects.equals(this.firstImageUrl, firstImageUrl)
                && Objects.equals(this.thumbnailUrl, thumbnailUrl)
                && Objects.equals(this.address, address)
                && Objects.equals(this.startDate, startDate)
                && Objects.equals(this.endDate, endDate)
                && Objects.equals(this.mapX, mapX)
                && Objects.equals(this.mapY, mapY)
                && Objects.equals(this.lDongRegnCd, lDongRegnCd)
                && this.status == status;
    }

    //축제 상세 장보 변경 여부 판단 메서드
    public boolean isSameDetailInfo(
            String overview,
            String homepageUrl
    ) {
        return Objects.equals(this.overview, overview)
                && Objects.equals(this.homepageUrl, homepageUrl);
    }

    //축제 찜 수 1 증가
    public void increaseBookmarkCount() {
        this.bookMarkCount++;
    }

    // 축제 찜 수 1 감소
    public void decreaseBookmarkCount() {
        if (this.bookMarkCount > 0) {
            this.bookMarkCount--;
        }
    }
}