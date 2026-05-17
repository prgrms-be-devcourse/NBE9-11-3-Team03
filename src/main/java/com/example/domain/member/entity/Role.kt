package com.example.domain.member.entity;

// 회원이 어떤 권한 범위를 가지는지 표현하는 enum이다.
// Spring Security 인가 처리에서 관리자/일반 회원을 구분할 때 사용하게 된다.
public enum Role {
    // 일반 회원 권한
    USER,
    // 관리자 권한
    ADMIN
}
