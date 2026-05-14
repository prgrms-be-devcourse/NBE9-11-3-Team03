package com.example.domain.member.dto.response;

import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.member.entity.Role;

public record MyPageRes(
        Long memberId,
        String email,
        String nickname,
        long reviewCount,
        long bookMarkCount,
        Role role
) {
}
