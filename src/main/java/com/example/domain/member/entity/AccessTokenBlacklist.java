package com.example.domain.member.entity;

import com.example.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_token_blacklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessTokenBlacklist extends BaseEntity {

    // 로그아웃된 access token을 저장함.
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    // 이 시간이 지나면 blacklist 기록을 지워도 됨.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private AccessTokenBlacklist(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static AccessTokenBlacklist create(String token, LocalDateTime expiresAt) {
        return new AccessTokenBlacklist(token, expiresAt);
    }
}
