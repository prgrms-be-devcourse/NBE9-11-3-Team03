package com.example.global.jwt;

import com.example.domain.member.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    // JWT를 만들 때 사용하는 비밀키다.
    // 같은 비밀키로 만든 토큰만 서버가 정상 토큰으로 인정할 수 있다.
    @Value("${jwt.secret:festival-auth-service-jwt-secret-key-for-local-development-20260414}")
    private String secret;

    // access token이 몇 ms 동안 유효한지 설정
    // 기본값 1800000ms = 30분
    @Value("${jwt.access-token-expiration-ms:1800000}")
    private long accessTokenExpirationMs;

    // refresh token이 몇 ms 동안 유효한지 설정함.
    // 기본값 1209600000ms = 14일
    @Value("${jwt.refresh-token-expiration-ms:1209600000}")
    private long refreshTokenExpirationMs;

    private SecretKey secretKey;

    // @Value로 설정값이 주입된 뒤, 문자열 secret을 JWT 서명용 SecretKey 객체로 바꾼다.
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 로그인에 성공한 회원 정보를 이용해 access token을 발급받음
    // subject에는 loginId를 넣고, claim에는 추가로 필요한 회원 정보를 넣는다.
    public String createAccessToken(Member member) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(member.getLoginId())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim("memberId", member.getId())
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // 로그인 성공 시 함께 내려줄 refresh token을 발급함.
    public String createRefreshToken(Member member) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(member.getLoginId())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .claim("memberId", member.getId())
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // 토큰 안에 저장해둔 memberId를 꺼냄
    // JWT 파싱 결과가 Integer나 Long처럼 Number 타입으로 올 수 있어서 longValue()로 변환했습니다.
    public Long getMemberId(String token) {
        Object memberId = parseClaims(token).get("memberId");

        if (memberId instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(memberId.toString());
    }

    // 토큰의 subject에 저장해둔 loginId를 꺼냄
    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    // 토큰의 claim에 저장해둔 권한 정보를 꺼냄
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 토큰 만료 시간을 DB에 저장하기 위해 LocalDateTime으로 바꿈.
    public LocalDateTime getExpirationDateTime(String token) {
        return LocalDateTime.ofInstant(
                parseClaims(token).getExpiration().toInstant(),
                ZoneId.systemDefault()
        );
    }

    // 일반 API 인증에는 access token만 사용할 수 있게 구분함.
    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    // 토큰 재발급에는 refresh token만 사용할 수 있게 구분함.
    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    // 토큰이 서버에서 만든 정상 토큰인지, 만료되지는 않았는지 확인해야 함
    // 파싱 중 예외가 발생하면 잘못된 토큰으로 판단해 false를 반환하게 했습니다.
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // JWT 문자열을 해석해서 payload 안의 Claims 정보를 꺼냄
    // verifyWith(secretKey)를 사용 ==> 서명이 맞지 않는 토큰은 여기서 예외가 발생
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
