package com.example.domain.member.dto.response;

import com.example.domain.bookmark.entity.FestivalBookmark;

import java.time.LocalDateTime;

public record MyBookMarkItemRes(
        Long bookmarkId,
        Long festivalId,
        String title,
        String address,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime bookmarkedAt

) {
    public static MyBookMarkItemRes from(FestivalBookmark festivalBookmark){
        return new MyBookMarkItemRes(
                festivalBookmark.getId(),
                festivalBookmark.getFestival().getId(),
                festivalBookmark.getFestival().getTitle(),
                festivalBookmark.getFestival().getAddress(),
                festivalBookmark.getFestival().getStartDate(),
                festivalBookmark.getFestival().getEndDate(),
                festivalBookmark.getCreatedAt()
        );
    }
}
