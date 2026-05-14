package com.example.domain.member.dto.response;

import com.example.domain.bookmark.entity.FestivalBookmark;
import org.springframework.data.domain.Page;

import java.util.List;

public record MyBookMarkPageResponse(
        List<MyBookMarkItemResponse>  content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static MyBookMarkPageResponse from(Page<FestivalBookmark> bookmarks){
        return new MyBookMarkPageResponse(
                bookmarks.getContent().stream()
                        .map(MyBookMarkItemResponse::from)
                        .toList(),
                bookmarks.getNumber(),
                bookmarks.getSize(),
                bookmarks.getTotalElements(),
                bookmarks.getTotalPages(),
                bookmarks.hasNext()
        );
    }
}
