package com.example.domain.admin.dto.response

import com.example.domain.member.entity.Member
import org.springframework.data.domain.Page

data class MemberPageResponse(
    val content: List<MemberDetailResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        @JvmStatic
        fun from(memberPage: Page<Member>): MemberPageResponse {
            return MemberPageResponse(
                content = memberPage.content
                    .map { MemberDetailResponse.from(it) },
                page = memberPage.number,
                size = memberPage.size,
                totalElements = memberPage.totalElements,
                totalPages = memberPage.totalPages
            )
        }
    }
}