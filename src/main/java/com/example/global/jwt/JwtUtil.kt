package com.example.global.jwt

import com.example.domain.member.entity.Member
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil(
    // JWT를 만들 때 사용하는 비밀키다.
    // 같은 비밀키로 만든 토큰만 서버가 정상 토큰으로 인정할 수 있다.
    @Value("\${jwt.secret:festival-auth-service-jwt-secret-key-for-local-development-20260414}")
    private val secret: String,

    // access token이 몇 ms 동안 유효한지 설정
    // 기본값 1800000ms = 30분
    @Value("\${jwt.access-token-expiration-ms:1800000}")
    private val accessTokenExpirationMs: Long,

    // refresh token이 몇 ms 동안 유효한지 설정함.
    // 기본값 1209600000ms = 14일
    @Value("\${jwt.refresh-token-expiration-ms:1209600000}")
    private val refreshTokenExpirationMs: Long,
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    // 로그인에 성공한 회원 정보를 이용해 access token을 발급받음
    // subject에는 loginId를 넣고, claim에는 추가로 필요한 회원 정보를 넣는다.
    fun createAccessToken(member: Member): String =
        buildToken(member, ACCESS_TOKEN_TYPE, accessTokenExpirationMs)

    // 로그인 성공 시 함께 내려줄 refresh token을 발급함.
    fun createRefreshToken(member: Member): String =
        buildToken(member, REFRESH_TOKEN_TYPE, refreshTokenExpirationMs)


    // 토큰 안에 저장해둔 memberId를 꺼냄
    // JWT 파싱 결과가 Integer나 Long처럼 Number 타입으로 올 수 있어서 longValue()로 변환했습니다.
    fun getMemberId(token: String): Long {
        val memberId = parseClaims(token)["memberId"]
        return when (memberId) {
            is Number -> memberId.toLong()
            else -> memberId.toString().toLong()
        }
    }

    // 토큰의 subject에 저장해둔 loginId를 꺼냄
    fun getLoginId(token: String): String = parseClaims(token).subject

    // 토큰의 claim에 저장해둔 권한 정보를 꺼냄
    fun getRole(token: String): String? =
        parseClaims(token).get("role", String::class.java)

    // 토큰 만료 시간을 DB에 저장하기 위해 LocalDateTime으로 바꿈.
    fun getExpirationDateTime(token: String): LocalDateTime =
        LocalDateTime.ofInstant(parseClaims(token).expiration.toInstant(), ZoneId.systemDefault())

    // 일반 API 인증에는 access token만 사용할 수 있게 구분함.
    fun isAccessToken(token: String): Boolean =
        ACCESS_TOKEN_TYPE == parseClaims(token).get(TOKEN_TYPE_CLAIM, String::class.java)

    // 토큰 재발급에는 refresh token만 사용할 수 있게 구분함.
    fun isRefreshToken(token: String): Boolean =
        REFRESH_TOKEN_TYPE == parseClaims(token).get(TOKEN_TYPE_CLAIM, String::class.java)

    // 토큰이 서버에서 만든 정상 토큰인지, 만료되지는 않았는지 확인해야 함
    // 파싱 중 예외가 발생하면 잘못된 토큰으로 판단해 false를 반환하게 했습니다.
    fun validateToken(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (e: Exception) {
            when (e) {
                is JwtException, is IllegalArgumentException -> false
                else -> throw e
            }
        }


    // access/refresh 토큰 생성 공통 로직
    private fun buildToken(member: Member, tokenType: String, expirationMs: Long): String {
        val now = Date()
        val expiration = Date(now.time + expirationMs)
        return Jwts.builder()
            .subject(member.loginId)
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .claim("memberId", member.id)
            .claim("role", member.role.name)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    // JWT 파싱. 서명이 안 맞거나 만료되면 예외 발생
    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    companion object {
        private const val TOKEN_TYPE_CLAIM = "type"
        private const val ACCESS_TOKEN_TYPE = "access"
        private const val REFRESH_TOKEN_TYPE = "refresh"
    }
}
