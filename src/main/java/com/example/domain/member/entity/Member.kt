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
open class Member protected constructor() : BaseEntity() {
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

    constructor(
        loginId: String,
        password: String,
        memberName: String,
        email: String,
        nickname: String,
        reportCount: Int
    ) : this() {
        this.loginId = loginId
        this.password = password
        this.memberName = memberName
        this.email = email
        this.nickname = nickname
        this.reportCount = reportCount
        this.status = MemberStatus.ACTIVE
        this.role = Role.USER
    }

    constructor(
        memberName: String,
        password: String,
        loginId: String,
        email: String,
        nickname: String,
        role: Role
    ) : this() {
        this.memberName = memberName
        this.password = password
        this.loginId = loginId
        this.email = email
        this.nickname = nickname
        this.role = role
        this.reportCount = 0
        this.status = MemberStatus.ACTIVE
    }

    private constructor(
        memberName: String,
        password: String,
        loginId: String,
        email: String,
        nickname: String,
        reportCount: Int,
        status: MemberStatus,
        role: Role
    ) : this() {
        this.memberName = memberName
        this.password = password
        this.loginId = loginId
        this.email = email
        this.nickname = nickname
        this.reportCount = reportCount
        this.status = status
        this.role = role
    }

    // 탈퇴는 물리 삭제 대신 상태값을 바꾸고 개인정보를 비식별화함.
    fun withdraw() {
        status = MemberStatus.WITHDRAWN
        loginId = "withdrawn_login_$id"
        email = "withdrawn_$id@deleted.local"
        nickname = "탈퇴한회원_$id"
    }

    // 신고횟수 증가
    fun increaseReportCount() {
        reportCount++
    }

    class Builder {
        private var memberName: String = ""
        private var password: String = ""
        private var loginId: String = ""
        private var email: String = ""
        private var nickname: String = ""
        private var reportCount: Int = 0
        private var status: MemberStatus = MemberStatus.ACTIVE
        private var role: Role = Role.USER

        fun memberName(memberName: String) = apply { this.memberName = memberName }
        fun password(password: String) = apply { this.password = password }
        fun loginId(loginId: String) = apply { this.loginId = loginId }
        fun email(email: String) = apply { this.email = email }
        fun nickname(nickname: String) = apply { this.nickname = nickname }
        fun reportCount(reportCount: Int) = apply { this.reportCount = reportCount }
        fun status(status: MemberStatus) = apply { this.status = status }
        fun role(role: Role) = apply { this.role = role }

        fun build(): Member =
            Member(memberName, password, loginId, email, nickname, reportCount, status, role)
    }

    companion object {
        @JvmStatic
        fun create(
            userName: String,
            password: String,
            loginId: String,
            email: String,
            nickname: String
        ): Member =
            Member(userName, password, loginId, email, nickname, 0, MemberStatus.ACTIVE, Role.USER)

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
