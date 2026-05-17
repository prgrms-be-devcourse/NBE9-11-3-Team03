package com.example.domain.festival.dto.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FestivalApiItems {
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)  //단일 객체도 배열처럼 받게 설정
    private List<FestivalApiItem> item;
}
