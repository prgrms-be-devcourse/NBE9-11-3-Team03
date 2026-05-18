package com.example.domain.member.entity;

import com.example.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    // 한 회원당 refresh token 하나만 저장함.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = true, unique = true, length = 500)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // refresh token이 재발급에 사용 가능한 상태인지 저장함.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private RefreshTokenStatus status = RefreshTokenStatus.ACTIVE;

    // refresh token이 로그아웃 처리된 시간을 저장함.
    @Column(name = "logged_out_at")
    private LocalDateTime loggedOutAt;

    private RefreshToken(Member member, String token, LocalDateTime expiresAt) {
        this.member = member;
        this.token = token;
        this.expiresAt = expiresAt;
        this.status = RefreshTokenStatus.ACTIVE;
    }

    public static RefreshToken create(Member member, String token, LocalDateTime expiresAt) {
        return new RefreshToken(member, token, expiresAt);
    }

    // 재로그인이나 토큰 재발급 시 기존 refresh token 값을 새 값으로 교체합니다.
    public void update(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
        // 새 토큰을 저장하면 다시 사용할 수 있는 상태로 바꿈.
        this.status = RefreshTokenStatus.ACTIVE;
        this.loggedOutAt = null;
    }

    // 로그아웃하면 기록은 남기고 token 값과 상태만 바꿈.
    public void logout() {
        this.token = null;
        this.status = RefreshTokenStatus.UNACTIVATED;
        this.loggedOutAt = LocalDateTime.now();
    }

    // 재발급 전에 사용할 수 있는 refresh token인지 확인함.
    public boolean isActive() {
        return status == RefreshTokenStatus.ACTIVE;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
