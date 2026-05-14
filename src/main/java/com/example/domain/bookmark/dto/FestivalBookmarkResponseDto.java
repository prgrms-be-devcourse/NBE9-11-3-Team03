package com.example.domain.bookmark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FestivalBookmarkResponseDto {

    private Long festivalId;
    private Long memberId;
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
    private Integer bookmarkCount;


}
