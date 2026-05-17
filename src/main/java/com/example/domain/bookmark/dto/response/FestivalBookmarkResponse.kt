package com.example.domain.bookmark.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FestivalBookmarkResponse {

    private Long festivalId;
    private Long memberId;
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
    private Integer bookmarkCount;


}
