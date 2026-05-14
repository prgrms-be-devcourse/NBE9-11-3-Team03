package com.example.domain.admin.dto;

import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;

public record AdminMemberWithdrawnRes(
        Long memberId,
        MemberStatus status
) {
    public static AdminMemberWithdrawnRes from(Member member){
        return new AdminMemberWithdrawnRes(
                member.getId(),
                member.getStatus()
        );
    }
}
