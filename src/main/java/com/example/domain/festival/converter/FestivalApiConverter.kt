package com.example.domain.festival.converter;

import com.example.domain.festival.dto.external.FestivalApiItem;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//외부 DTO → 내부 엔티티 변환 클래스
@Component
public class FestivalApiConverter {
    //공공 API 날짜 문자열 포맷 ex. 20260430
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 외부 API DTO를 신규 Festival 엔티티로 변환(DB 초기 저장)
    public Festival toEntityFromListItem(FestivalApiItem item) {
        LocalDateTime startDate = parseStartDate(item.getEventstartdate());
        LocalDateTime endDate = parseEndDate(item.getEventenddate());

        return Festival.builder()
                .contentId(safeTrim(item.getContentid()))
                .title(safeTrim(item.getTitle()))
                .overview(defaultText(item.getOverview()))
                .contactNumber(normalizeContactNumber(item.getTel()))
                .firstImageUrl(nullableText(item.getFirstimage()))
                .thumbnailUrl(resolveThumbnail(item.getFirstimage2(), item.getFirstimage()))
                .address(buildAddress(item.getAddr1(), item.getAddr2()))
                .homepageUrl(extractHomepageUrl(item.getHomepage()))
                .startDate(startDate)
                .endDate(endDate)
                .mapX(parseDouble(item.getMapx()))
                .mapY(parseDouble(item.getMapy()))
                .lDongRegnCd(extractRegionCode(item.getLDongRegnCd()))
                .status(calculateStatus(startDate, endDate))
                .build();
    }

    //기존 Festival 엔티티에 외부 API 값을 반영 (contendId 기준으로 이미 존재하는 축제를 Update할 때)
    public void updateFromListItem(Festival festival, FestivalApiItem item) {
        LocalDateTime startDate = parseStartDate(item.getEventstartdate());
        LocalDateTime endDate = parseEndDate(item.getEventenddate());

        festival.updateFestivalInfo(
                safeTrim(item.getTitle()),
                festival.getOverview(),
                normalizeContactNumber(item.getTel()),
                nullableText(item.getFirstimage()),
                resolveThumbnail(item.getFirstimage2(), item.getFirstimage()),
                buildAddress(item.getAddr1(), item.getAddr2()),
                festival.getHomepageUrl(),
                startDate,
                endDate,
                parseDouble(item.getMapx()),
                parseDouble(item.getMapy()),
                extractRegionCode(item.getLDongRegnCd()),
                calculateStatus(startDate, endDate)
        );
    }

    //기존 Festival 엔티티에 외부 API 값을 반영 (변경여부를 기준으로 축제를 Update할 때)
    public boolean hasListChanges(Festival festival, FestivalApiItem item) {
        LocalDateTime startDate = parseStartDate(item.getEventstartdate());
        LocalDateTime endDate = parseEndDate(item.getEventenddate());

        String title = safeTrim(item.getTitle());
        String contactNumber = normalizeContactNumber(item.getTel());
        String firstImageUrl = nullableText(item.getFirstimage());
        String thumbnailUrl = resolveThumbnail(item.getFirstimage2(), item.getFirstimage());
        String address = buildAddress(item.getAddr1(), item.getAddr2());
        Double mapX = parseDouble(item.getMapx());
        Double mapY = parseDouble(item.getMapy());
        String lDongRegnCd = extractRegionCode(item.getLDongRegnCd());
        FestivalStatus status = calculateStatus(startDate, endDate);

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
        );
    }

    //상세 API 기반 상세 정보 보강
    public void updateDetailFields(Festival festival, FestivalApiItem item) {
        festival.updateFestivalDetailInfo(
                defaultText(item.getOverview()),
                extractHomepageUrl(item.getHomepage())
        );
    }

    //상세 변경 여부 메서드 추가 (변경여부를 기준으로 축제를 Update할 때)
    public boolean hasDetailChanges(Festival festival, FestivalApiItem item) {
        String overview = defaultText(item.getOverview());
        String homepageUrl = extractHomepageUrl(item.getHomepage());

        return !festival.isSameDetailInfo(
                overview,
                homepageUrl
        );
    }

    // 상세 미완료 여부는 overview 기준으로만 판단한다.
    // homepageUrl은 외부 데이터 특성상 null이 정상값일 수 있으므로, 재보강 대상 판단 기준으로 사용하지 않는다.
    public boolean isDetailIncomplete(Festival festival) {
        return "상세 설명 없음".equals(festival.getOverview());
    }

    // L_DONG_REGN_CD 앞 2자리만 추출
    private String extractRegionCode(String lDongRegnCd) {
        if (lDongRegnCd == null || lDongRegnCd.isBlank()) {
            return null;
        }

        // 2자리 이상이면 앞 2자리만 반환
        return lDongRegnCd.length() >= 2
                ? lDongRegnCd.substring(0, 2)
                : lDongRegnCd;
    }

    // Homepage 필드 데이터 정제 코드
    private String extractHomepageUrl(String homepage) {
        if (homepage == null || homepage.isBlank()) {
            return null;
        }

        // HTML 파싱
        var doc = Jsoup.parse(homepage);

        // <a href="..."> 태그가 있으면 전부 추출
        var links = doc.select("a[href]");
        if (!links.isEmpty()) {
            List<String> urls = new ArrayList<>();

            links.forEach(link -> {
                String href = link.attr("href").trim();
                if (!href.isEmpty()) {
                    urls.add(href);
                }
            });

            return String.join(", ", urls);
        }

        // HTML이 없거나 href가 없으면 → 문자열에서 URL만 추출
        String text = doc.text();

        Pattern pattern = Pattern.compile("https?://[^\\s]+");
        Matcher matcher = pattern.matcher(text);

        List<String> urls = new ArrayList<>();

        while (matcher.find()) {
            urls.add(matcher.group());
        }

        if (urls.isEmpty()) {
            return null;
        }

        return String.join(", ", urls);
    }

    // CONTACT_NUMBER  필드 데이터 정제 코드(
    private String normalizeContactNumber(String rawTel) {
        String text = nullableText(rawTel);
        if (text == null) {
            return null;
        }

        // 줄바꿈/탭 제거 후 공백 정리
        text = text.replaceAll("[\\r\\n\\t]+", " ");
        text = text.replaceAll("\\s+", " ").trim();

        //전 화번호 + 전화번호가 바로 붙어 있는 경우 분리   ex: 043-532-3325043-539-3605 -> 043-532-3325, 043-539-3605
        text = text.replaceAll(
                "(\\d{2,4}-\\d{3,4}-\\d{4})(?=\\d{2,4}-\\d{3,4}-\\d{4})",
                "$1, "
        );

        // 전화번호 뒤에 한글/영문이 바로 붙으면 구분자 분리 ex. 02-319-1220운영사 -> 02-319-1220 / 운영사
        text = text.replaceAll(
                "(\\d{2,4}-\\d{3,4}-\\d{4})(?=[가-힣A-Za-z])",
                "$1 , "
        );

        // 한글/영문 뒤에 전화번호가 바로 붙으면 공백으로 분리 ex. 행사장02-319-1220 -> 행사장 02-319-1220
        text = text.replaceAll(
                "([가-힣A-Za-z])(?=\\d{2,4}-\\d{3,4}-\\d{4})",
                "$1 "
        );

        // 구분자 주변 공백 정리
        text = text.replaceAll("\\s*/\\s*", " / ");
        text = text.replaceAll("\\s*,\\s*", ", ");
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }



    //시작일 문자열(yyyyMMdd)을 LocalDateTime 시작 시각으로 변환 ex. 20260430 -> 2026-04-30T00:00:00
    private LocalDateTime parseStartDate(String rawDate) {
        LocalDate date = LocalDate.parse(rawDate, DATE_FORMATTER);
        return date.atStartOfDay();
    }


    //종료일 문자열(yyyyMMdd)을 LocalDateTime 종료 시각으로 변환 ex. 20260503 -> 2026-05-03T23:59:59
    private LocalDateTime parseEndDate(String rawDate) {
        LocalDate date = LocalDate.parse(rawDate, DATE_FORMATTER);
        return LocalDateTime.of(date, LocalTime.of(23, 59, 59));
    }

    //문자열 좌표를 Double로 변환 ex. "128.872858180758" -> 128.872858180758
    private Double parseDouble(String value) {
        return Double.parseDouble(value);
    }

    //축제 상태 계산 (UPCOMING, ONGOING, ENDED)
    private FestivalStatus calculateStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startDate)) {
            return FestivalStatus.UPCOMING;
        }

        if (now.isAfter(endDate)) {
            return FestivalStatus.ENDED;
        }
        return FestivalStatus.ONGOING;
    }

     //주소 합치기 (addr1 + addr2)
    private String buildAddress(String addr1, String addr2) {
        String base = safeTrim(addr1);
        String detail = nullableText(addr2);

        if (detail == null) {
            return base;
        }
        return base + " " + detail;
    }

    //이미지 URL 걸정 (firstimage2가 있으면 썸네일로 사용, 없으면 firstimage를 대체 사용)
    private String resolveThumbnail(String firstimage2, String firstimage) {
        String second = nullableText(firstimage2);
        if (second != null) {
            return second;
        }
        return nullableText(firstimage);
    }

    //overview 같은 필드가 비어 있으면 기본 텍스트로 대체, Festival 엔티티에서 overview는 not null이기 때문
    private String defaultText(String value) {
        String text = nullableText(value);
        return text != null ? text : "상세 설명 없음";
    }


    //nullableText 및 safeTrim 메서드는 API 데이터를 안정적으로 저장하기 위함

    //nullableText(): null => null, 공백문자열(" ") => null 처리 역할
    private String nullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    //safeTrim(): null 및 공백이면 예외, 아니면 trim해서 반환하는 역할
    private String safeTrim(String value) {
        if (value == null) {
            throw new IllegalArgumentException("필수 값이 null 입니다.");
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("필수 값이 비어 있습니다.");
        }
        return trimmed;
    }
}
