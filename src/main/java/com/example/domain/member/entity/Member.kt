package com.example.domain.member.entity

import com.example.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.validation.constraints.Email

@Entity
@Table(name = "member")
class Member protected constructor() : BaseEntity() {
    @Column(name = "member_name", nullable = false)
    var memberName: String = ""
        protected set

    @Column(nullable = false)
    var password: String = ""
        protected set

    @Column(name = "login_id", nullable = false, unique = true)
    var loginId: String = ""
        protected set

    @Email
    @Column(nullable = false, unique = true)
    var email: String = ""
        protected set

    @Column(name = "report_count", nullable = false)
    var reportCount: Int = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE
        protected set

    @Column(nullable = false, unique = true)
    var nickname: String = ""
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER
        protected set

    fun withdraw() {
        status = MemberStatus.WITHDRAWN
        loginId = "withdrawn_login_$id"
        email = "withdrawn_$id@deleted.local"
        nickname = "탈퇴한회원_$id"
    }

    fun increaseReportCount() {
        reportCount++
    }

    companion object {
        @JvmOverloads
        @JvmStatic
        fun create(
            userName: String,
            password: String,
            loginId: String,
            email: String,
            nickname: String,
            role: Role = Role.USER,
            reportCount: Int = 0,
        ): Member =
            Member().apply {
                this.memberName = userName
                this.password = password
                this.loginId = loginId
                this.email = email
                this.nickname = nickname
                this.role = role
                this.reportCount = reportCount
            }
    }
}
