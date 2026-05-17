package com.example.domain.festival.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter //Test용도 (FestivalApiConverterTest)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) //역직렬화시, 안쓰는 필드 무시
public class FestivalApiItem {
    private String contentid;
    private String title;
    private String tel;
    private String addr1;
    private String addr2;
    private String homepage;
    private String overview;
    private String firstimage;
    private String firstimage2;
    private String mapx;
    private String mapy;
    @JsonProperty("lDongRegnCd") //Jackson 기본 전략: snake_case ↔ camelCase 변환, Naming 정책이 애매해 DTO 매핑이 안되는 것을 방지
    private String lDongRegnCd;
    private String eventstartdate;
    private String eventenddate;
    private String modifiedtime; //수정시간, INCREMENTAL 동기화 고려 시 참고 가능
}
