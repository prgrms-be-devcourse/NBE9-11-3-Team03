package com.example.domain.festival.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RegionCode {
    SEOUL("11", "서울특별시"),
    BUSAN("26", "부산광역시"),
    DAEGU("27", "대구광역시"),
    INCHEON("28", "인천광역시"),
    GWANGJU("29", "광주광역시"),
    DAEJEON("30", "대전광역시"),
    ULSAN("31", "울산광역시"),
    SEJONG("36", "세종특별자치시"),
    GYEONGGI("41", "경기도"),
    CHUNGBUK("43", "충청북도"),
    CHUNGNAM("44", "충청남도"),
    JEONNAM("46", "전라남도"),
    GYEONGBUK("47", "경상북도"),
    GYEONGNAM("48", "경상남도"),
    JEJU("50", "제주특별자치도"),
    GANGWON("51", "강원도"),
    JEONBUK("52", "전라북도"),

    UNKNOWN("99", "기타/미분류");

    private final String code;
    private final String name;

    //코드->한글 지역명 반환
    public static String getNameByCode(String code) {
        return Arrays.stream(values())
                .filter(region -> region.getCode().equals(code))
                .findFirst()
                .orElse(UNKNOWN)
                .getName();
    }
}
