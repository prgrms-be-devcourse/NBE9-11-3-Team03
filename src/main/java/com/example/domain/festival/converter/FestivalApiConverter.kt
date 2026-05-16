package com.example.domain.festival.converter

import com.example.domain.festival.dto.external.FestivalApiItem
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

// 외부 DTO → 내부 엔티티 변환 클래스
@Component
class FestivalApiConverter {
    // 외부 API DTO를 신규 Festival 엔티티로 변환(DB 초기 저장)
    fun toEntityFromListItem(item: FestivalApiItem): Festival {
        val startDate = parseStartDate(requiredText(item.eventstartdate))
        val endDate = parseEndDate(requiredText(item.eventenddate))

        return Festival.builder()
            .contentId(requiredText(item.contentid))
            .title(requiredText(item.title))
            .overview(defaultText(item.overview))
            .contactNumber(normalizeContactNumber(item.tel))
            .firstImageUrl(nullableText(item.firstimage))
            .thumbnailUrl(resolveThumbnail(item.firstimage2, item.firstimage))
            .address(buildAddress(requiredText(item.addr1), item.addr2))
            .homepageUrl(extractHomepageUrl(item.homepage))
            .startDate(startDate)
            .endDate(endDate)
            .mapX(parseDouble(requiredText(item.mapx)))
            .mapY(parseDouble(requiredText(item.mapy)))
            .lDongRegnCd(extractRegionCode(item.lDongRegnCd))
            .status(calculateStatus(startDate, endDate))
            .build()
    }

    // 기존 Festival 엔티티에 외부 API 값을 반영 (contentId 기준으로 이미 존재하는 축제를 Update할 때)
    fun updateFromListItem(festival: Festival, item: FestivalApiItem) {
        val startDate = parseStartDate(requiredText(item.eventstartdate))
        val endDate = parseEndDate(requiredText(item.eventenddate))

        festival.updateFestivalInfo(
            requiredText(item.title),
            festival.overview,
            normalizeContactNumber(item.tel),
            nullableText(item.firstimage),
            resolveThumbnail(item.firstimage2, item.firstimage),
            buildAddress(requiredText(item.addr1), item.addr2),
            festival.homepageUrl,
            startDate,
            endDate,
            parseDouble(requiredText(item.mapx)),
            parseDouble(requiredText(item.mapy)),
            extractRegionCode(item.lDongRegnCd),
            calculateStatus(startDate, endDate)
        )
    }

    // 기존 Festival 엔티티에 외부 API 값을 반영 (변경여부를 기준으로 축제를 Update할 때)
    fun hasListChanges(festival: Festival, item: FestivalApiItem): Boolean {
        val startDate = parseStartDate(requiredText(item.eventstartdate))
        val endDate = parseEndDate(requiredText(item.eventenddate))

        val title = requiredText(item.title)
        val contactNumber = normalizeContactNumber(item.tel)
        val firstImageUrl = nullableText(item.firstimage)
        val thumbnailUrl = resolveThumbnail(item.firstimage2, item.firstimage)
        val address = buildAddress(requiredText(item.addr1), item.addr2)
        val mapX = parseDouble(requiredText(item.mapx))
        val mapY = parseDouble(requiredText(item.mapy))
        val lDongRegnCd = extractRegionCode(item.lDongRegnCd)
        val status = calculateStatus(startDate, endDate)

        return !festival.isSameListInfo(
            title,
            contactNumber,
            firstImageUrl,
            thumbnailUrl,
            address,
            startDate,
            endDate,
            mapX,
            mapY,
            lDongRegnCd,
            status
        )
    }

    // 상세 API 기반 상세 정보 보강
    fun updateDetailFields(festival: Festival, item: FestivalApiItem) {
        festival.updateFestivalDetailInfo(
            defaultText(item.overview),
            extractHomepageUrl(item.homepage)
        )
    }

    // 상세 변경 여부 메서드 추가 (변경여부를 기준으로 축제를 Update할 때)
    fun hasDetailChanges(festival: Festival, item: FestivalApiItem): Boolean {
        val overview = defaultText(item.overview)
        val homepageUrl = extractHomepageUrl(item.homepage)

        return !festival.isSameDetailInfo(
            overview,
            homepageUrl
        )
    }

    // 상세 미완료 여부는 overview 기준으로만 판단한다.
    // homepageUrl은 외부 데이터 특성상 null이 정상값일 수 있으므로, 재보강 대상 판단 기준으로 사용하지 않는다.
    fun isDetailIncomplete(festival: Festival): Boolean =
        "상세 설명 없음" == festival.overview

    // L_DONG_REGN_CD 앞 2자리만 추출
    private fun extractRegionCode(lDongRegnCd: String?): String? {
        if (lDongRegnCd.isNullOrBlank()) {
            return null
        }

        // 2자리 이상이면 앞 2자리만 반환
        return if (lDongRegnCd.length >= 2) {
            lDongRegnCd.substring(0, 2)
        } else {
            lDongRegnCd
        }
    }

    // Homepage 필드 데이터 정제 코드
    private fun extractHomepageUrl(homepage: String?): String? {
        if (homepage.isNullOrBlank()) {
            return null
        }

        // HTML 파싱
        val doc = Jsoup.parse(homepage)

        // <a href="..."> 태그가 있으면 전부 추출
        val links = doc.select("a[href]")
        if (links.isNotEmpty()) {
            val urls = links
                .map { it.attr("href").trim() }
                .filter { it.isNotEmpty() }

            return urls.takeIf { it.isNotEmpty() }?.joinToString(", ")
        }

        // HTML이 없거나 href가 없으면 → 문자열에서 URL만 추출
        val text = doc.text()

        val pattern = Pattern.compile("https?://[^\\s]+")
        val matcher = pattern.matcher(text)

        val urls = mutableListOf<String>()

        while (matcher.find()) {
            urls.add(matcher.group())
        }

        return urls.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    // CONTACT_NUMBER 필드 데이터 정제 코드
    private fun normalizeContactNumber(rawTel: String?): String? {
        var text = nullableText(rawTel) ?: return null

        // 줄바꿈/탭 제거 후 공백 정리
        text = text.replace("[\\r\\n\\t]+".toRegex(), " ")
        text = text.replace("\\s+".toRegex(), " ").trim()

        // 전화번호 + 전화번호가 바로 붙어 있는 경우 분리 ex: 043-532-3325043-539-3605 -> 043-532-3325, 043-539-3605
        text = text.replace(
            "(\\d{2,4}-\\d{3,4}-\\d{4})(?=\\d{2,4}-\\d{3,4}-\\d{4})".toRegex(),
            "$1, "
        )

        // 전화번호 뒤에 한글/영문이 바로 붙으면 구분자 분리 ex. 02-319-1220운영사 -> 02-319-1220 / 운영사
        text = text.replace(
            "(\\d{2,4}-\\d{3,4}-\\d{4})(?=[가-힣A-Za-z])".toRegex(),
            "$1 , "
        )

        // 한글/영문 뒤에 전화번호가 바로 붙으면 공백으로 분리 ex. 행사장02-319-1220 -> 행사장 02-319-1220
        text = text.replace(
            "([가-힣A-Za-z])(?=\\d{2,4}-\\d{3,4}-\\d{4})".toRegex(),
            "$1 "
        )

        // 구분자 주변 공백 정리
        text = text.replace("\\s*/\\s*".toRegex(), " / ")
        text = text.replace("\\s*,\\s*".toRegex(), ", ")
        text = text.replace("\\s+".toRegex(), " ").trim()

        return text
    }

    // 시작일 문자열(yyyyMMdd)을 LocalDateTime 시작 시각으로 변환 ex. 20260430 -> 2026-04-30T00:00:00
    private fun parseStartDate(rawDate: String): LocalDateTime {
        val date = LocalDate.parse(rawDate, DATE_FORMATTER)
        return date.atStartOfDay()
    }

    // 종료일 문자열(yyyyMMdd)을 LocalDateTime 종료 시각으로 변환 ex. 20260503 -> 2026-05-03T23:59:59
    private fun parseEndDate(rawDate: String): LocalDateTime {
        val date = LocalDate.parse(rawDate, DATE_FORMATTER)
        return LocalDateTime.of(date, LocalTime.of(23, 59, 59))
    }

    // 문자열 좌표를 Double로 변환 ex. "128.872858180758" -> 128.872858180758
    private fun parseDouble(value: String): Double =
        value.toDouble()

    // 축제 상태 계산 (UPCOMING, ONGOING, ENDED)
    private fun calculateStatus(startDate: LocalDateTime, endDate: LocalDateTime): FestivalStatus {
        val now = LocalDateTime.now()

        if (now.isBefore(startDate)) {
            return FestivalStatus.UPCOMING
        }

        if (now.isAfter(endDate)) {
            return FestivalStatus.ENDED
        }

        return FestivalStatus.ONGOING
    }

    // 주소 합치기 (addr1 + addr2)
    private fun buildAddress(addr1: String, addr2: String?): String {
        val base = requiredText(addr1)
        val detail = nullableText(addr2)

        return if (detail == null) {
            base
        } else {
            "$base $detail"
        }
    }

    // 이미지 URL 결정 (firstimage2가 있으면 썸네일로 사용, 없으면 firstimage를 대체 사용)
    private fun resolveThumbnail(firstimage2: String?, firstimage: String?): String? {
        val second = nullableText(firstimage2)

        return second ?: nullableText(firstimage)
    }

    // overview 같은 필드가 비어 있으면 기본 텍스트로 대체, Festival 엔티티에서 overview는 not null이기 때문
    private fun defaultText(value: String?): String =
        nullableText(value) ?: "상세 설명 없음"

    // nullableText 및 requiredText 메서드는 API 데이터를 안정적으로 저장하기 위함
    // nullableText(): null => null, 공백문자열(" ") => null 처리 역할
    private fun nullableText(value: String?): String? {
        val trimmed = value?.trim()

        return if (trimmed.isNullOrEmpty()) {
            null
        } else {
            trimmed
        }
    }

    // requiredText(): null 및 공백이면 예외, 아니면 trim해서 반환하는 역할
    private fun requiredText(value: String?): String {
        val trimmed = value?.trim()

        require(!trimmed.isNullOrEmpty()) { "필수 값이 비어 있습니다." }

        return trimmed
    }

    companion object {
        // 공공 API 날짜 문자열 포맷 ex. 20260430
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}