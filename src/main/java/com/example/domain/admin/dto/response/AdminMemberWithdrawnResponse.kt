package com.example.domain.admin.dto.response

import com.example.domain.member.entity.Member
import com.example.domain.member.entity.MemberStatus

data class AdminMemberWithdrawnResponse(
    val memberId: Long,
    val status: MemberStatus
) {
    companion object {
        @JvmStatic
        fun from(member: Member): AdminMemberWithdrawnResponse {
            return AdminMemberWithdrawnResponse(
                member.getId(),
                member.status
            )
        }
    }
}
