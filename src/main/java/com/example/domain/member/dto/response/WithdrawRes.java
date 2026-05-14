package com.example.domain.member.dto.response;

import com.example.domain.member.entity.MemberStatus;

public record WithdrawRes(
        Long memberId,
        MemberStatus status
) {
}
