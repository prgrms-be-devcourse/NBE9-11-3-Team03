package com.example.domain.admin.dto;

import com.example.domain.member.entity.Member;

import java.time.LocalDateTime;

public record MemberDetailResponse(
        Long memberId,
        String loginId,
        String email,
        String nickname,
        int reportCount,
        String status,
        String role,
        LocalDateTime createdAt
) {
    public static MemberDetailResponse from(Member member) {
        return new MemberDetailResponse(
                member.getId(),
                member.getLoginId(),
                member.getEmail(),
                member.getNickname(),
                member.getReportCount(),
                member.getStatus().name(),
                member.getRole().name(),
                member.getCreatedAt()
        );
    }
}
