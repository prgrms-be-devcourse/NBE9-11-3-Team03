package com.example.domain.admin.dto.response

import com.example.domain.member.entity.Member
import java.time.LocalDateTime

data class MemberDetailResponse(
    val memberId: Long,
    val loginId: String,
    val email: String,
    val nickname: String,
    val reportCount: Int,
    val status: String,
    val role: String,
    val createdAt: LocalDateTime?
) {
    companion object {
        @JvmStatic
        fun from(member: Member): MemberDetailResponse {
            return MemberDetailResponse(
                member.getId(),
                member.getLoginId(),
                member.getEmail(),
                member.getNickname(),
                member.getReportCount(),
                member.getStatus().name,
                member.getRole().name,
                member.getCreatedAt()
            )
        }
    }
}
