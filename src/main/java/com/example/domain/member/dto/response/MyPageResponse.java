package com.example.domain.member.dto.response;

import com.example.domain.member.entity.Role;

public record MyPageResponse(
        Long memberId,
        String email,
        String nickname,
        long reviewCount,
        long bookMarkCount,
        Role role
) {
}
