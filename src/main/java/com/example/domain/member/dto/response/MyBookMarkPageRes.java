package com.example.domain.member.dto.response;

import com.example.domain.bookmark.entity.FestivalBookmark;
import com.example.domain.review.entity.Review;
import org.springframework.data.domain.Page;

import java.util.List;

public record MyBookMarkPageRes(
        List<MyBookMarkItemRes>  content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static MyBookMarkPageRes from(Page<FestivalBookmark> bookmarks){
        return new MyBookMarkPageRes(
                bookmarks.getContent().stream()
                        .map(MyBookMarkItemRes::from)
                        .toList(),
                bookmarks.getNumber(),
                bookmarks.getSize(),
                bookmarks.getTotalElements(),
                bookmarks.getTotalPages(),
                bookmarks.hasNext()
        );
    }
}
