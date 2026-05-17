package com.example.global.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class TokenCookieManager(
    @Value("\${jwt.refresh-token-expiration-ms:1209600000}")
    private val refreshTokenExpirationMs: Long,
    @Value("\${cookie.refresh-token.secure:false}")
    private val refreshTokenCookieSecure: Boolean,
    @Value("\${cookie.refresh-token.same-site:Lax}")
    private val refreshTokenCookieSameSite: String,
    @Value("\${cookie.refresh-token.path:/api/auth}")
    private val refreshTokenCookiePath: String
) {
    fun addRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        // refresh token만 HttpOnly 쿠키로 내려줌.
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, Duration.ofMillis(refreshTokenExpirationMs))
    }

    fun clearRefreshTokenCookie(response: HttpServletResponse) {
        // 로그아웃 시 브라우저에 남은 refresh token 쿠키를 삭제함.
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO)
    }

    // access token은 기존 방식대로 Authorization 헤더에서만 꺼냄.
    fun resolveAccessToken(request: HttpServletRequest): String? =
        extractBearerToken(request)


    fun resolveRefreshToken(request: HttpServletRequest): String? =
        extractCookieValue(request, REFRESH_TOKEN_COOKIE_NAME)


    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Duration) {
        val cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(refreshTokenCookieSecure)
            .sameSite(refreshTokenCookieSameSite)
            .path(refreshTokenCookiePath)
            .maxAge(maxAge)
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun extractBearerToken(request: HttpServletRequest): String? =
        request.getHeader(AUTHORIZATION_HEADER)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)


    private fun extractCookieValue(request: HttpServletRequest, cookieName: String): String? =
        request.cookies?.firstOrNull { it.name == cookieName }?.value


    companion object {
        const val REFRESH_TOKEN_COOKIE_NAME: String = "refreshToken"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
