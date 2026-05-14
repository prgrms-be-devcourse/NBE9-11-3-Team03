package com.example.domain.admin.dto.response;

import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;

public record AdminMemberWithdrawnResponse(
        Long memberId,
        MemberStatus status
) {
    public static AdminMemberWithdrawnResponse from(Member member){
        return new AdminMemberWithdrawnResponse(
                member.getId(),
                member.getStatus()
        );
    }
}
