package com.example.domain.member.entity;

// refresh token이 현재 사용할 수 있는 상태인지 구분함.
public enum RefreshTokenStatus {
    // 재발급에 사용할 수 있는 상태
    ACTIVE,
    // 다시 사용할 수 없는 상태
    UNACTIVATED
}
