package com.example.domain.admin.dto;

import com.example.domain.member.entity.Member;
import org.springframework.data.domain.Page;

import java.util.List;

public record MemberPageResponse(
        List<MemberDetailResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static MemberPageResponse from(Page<Member> memberPage){
        return new MemberPageResponse(
                memberPage.getContent().stream()
                        .map(MemberDetailResponse::from)
                        .toList()
                ,
                memberPage.getNumber(),
                memberPage.getSize(),
                memberPage.getTotalElements(),
                memberPage.getTotalPages()
        );
    }
}
