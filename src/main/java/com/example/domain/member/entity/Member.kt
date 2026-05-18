package com.example.domain.member.entity;

import com.example.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.*;

@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Member extends BaseEntity {
    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String password;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private Integer reportCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    public Member(
            String loginId,
            String password,
            String memberName,
            String email,
            String nickname,
            int reportCount
    ) {
        this.loginId = loginId;
        this.password = password;
        this.memberName = memberName;
        this.email = email;
        this.nickname = nickname;
        this.reportCount = reportCount;
        this.status = MemberStatus.ACTIVE;
        this.role = Role.USER;
    }

    public Member(
            String memberName,
            String password,
            String loginId,
            String email,
            String nickname,
            Role role
    ) {
        this.memberName = memberName;
        this.password = password;
        this.loginId = loginId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.reportCount = 0;
        this.status = MemberStatus.ACTIVE;
    }

    private Member(
            String memberName,
            String password,
            String loginId,
            String email,
            String nickname,
            Integer reportCount,
            MemberStatus status,
            Role role
    ) {
        this.memberName = memberName;
        this.password = password;
        this.loginId = loginId;
        this.email = email;
        this.nickname = nickname;
        this.reportCount = reportCount;
        this.status = status;
        this.role = role;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getPassword() {
        return password;
    }

    public String getLoginId() {
        return loginId;
    }

    public Integer getReportCount() {
        return reportCount;
    }

    public String getEmail() {
        return email;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public String getNickname() {
        return nickname;
    }

    public Role getRole() {
        return role;
    }

    public static Member create(
            String userName,
            String password,
            String loginId,
            String email,
            String nickname
    ) {
        return new Member(
                userName,
                password,
                loginId,
                email,
                nickname,
                0,
                MemberStatus.ACTIVE,
                Role.USER
        );
    }

    // 탈퇴는 물리 삭제 대신 상태값을 바꾸고 개인정보를 비식별화함.
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.loginId = "withdrawn_login_" + this.getId();
        this.email = "withdrawn_" + this.getId() + "@deleted.local";
        this.nickname = "탈퇴한회원_" + this.getId();
    }
    //신고횟수 증가
    public void increaseReportCount() {
        this.reportCount++;
    }}