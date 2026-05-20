package com.example.domain.bookmark.entity

import com.example.domain.festival.entity.Festival
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.QMember.member
import com.example.global.entity.BaseCreatedEntity
import jakarta.persistence.*

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(
        name = "uk_festival_bookmark_member_festival",
        columnNames = ["member_id", "festival_id"]
    )]
)
class FestivalBookmark(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    val festival: Festival
) : BaseCreatedEntity() {}