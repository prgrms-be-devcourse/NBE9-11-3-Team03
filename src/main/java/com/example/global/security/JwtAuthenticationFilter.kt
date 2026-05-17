package com.example.global.security

import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.repository.AccessTokenBlacklistRepository
import com.example.domain.member.repository.MemberRepository
import com.example.global.jwt.JwtUtil
import com.example.global.rsData.RsData
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.nio.charset.StandardCharsets

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val accessTokenBlacklistRepository: AccessTokenBlacklistRepository,
    private val memberRepository: MemberRepository,
    private val tokenCookieManager: TokenCookieManager,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    // 개발 중 Postman 테스트를 위해 정식 JWT와 별도로 허용할 고정 토큰 사용 여부다.
    @Value("\${security.dev-token.enabled:false}")
    private var devTokenEnabled: Boolean = false

    // 개발용 고정 토큰 값이다. 예: Authorization: Bearer dev-temp-token
    @Value("\${security.dev-token.value:}")
    private lateinit var devTokenValue: String

    // 개발용 토큰으로 인증할 때 SecurityContext에 넣을 임시 사용자 식별값이다.
    @Value("\${security.dev-token.login-id:dev-user}")
    private lateinit var devTokenLoginId: String

    // 개발용 토큰으로 인증할 때 부여할 임시 권한이다.
    @Value("\${security.dev-token.role:USER}")
    private lateinit var devTokenRole: String

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = tokenCookieManager.resolveAccessToken(request)

        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            if (isLoggedOutAccessToken(token)) {
                log.warn("[Auth] 블랙리스트 토큰 접근 시도 - uri={}", request.requestURI)
                writeUnauthorizedResponse(response, "이미 로그아웃 처리된 토큰입니다.")
                return
            }

            authenticateByToken(token, request)
        }

        filterChain.doFilter(request, response)
    }

    // 개발용 고정 토큰이면 임시 인증을 만들고, 아니면 정식 JWT인지 검증한다.
    private fun authenticateByToken(token: String, request: HttpServletRequest) {
        if (isDevToken(token)) {
            setAuthentication(devTokenLoginId, devTokenRole, request)
            return
        }

        if (!jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token)) {
            log.warn("[Auth] 유효하지 않은 토큰 접근 - uri={}", request.requestURI)
            return
        }

        if (!isBlacklisted(token)) {
            val loginId = jwtUtil.getLoginId(token)
            // 탈퇴한 회원의 남아있는 access token은 유효해도 인증 처리하지 않음.
            if (isActiveMember(loginId)) {
                setAuthentication(loginId, jwtUtil.getRole(token), request)
            }
        }
    }

    // DB에 남아있는 회원 상태가 ACTIVE인 경우에만 인증을 허용.
    private fun isActiveMember(loginId: String): Boolean =
        memberRepository.findByLoginId(loginId)
            .map { member -> member.status == MemberStatus.ACTIVE }
            .orElse(false)

    private fun isBlacklisted(token: String): Boolean {
        // 로그아웃된 access token이면 다시 인증하지 않음.
        return accessTokenBlacklistRepository.existsByToken(token)
    }

    private fun isLoggedOutAccessToken(token: String): Boolean =
        jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token) && isBlacklisted(token)

    @Throws(IOException::class)
    private fun writeUnauthorizedResponse(response: HttpServletResponse, message: String) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()

        objectMapper.writeValue(
            response.writer,
            RsData<String?>("401", message, null)
        )
    }

    // 개발용 토큰은 실제 JWT가 아니므로 설정값과 정확히 일치할 때만 통과시킨다.
    private fun isDevToken(token: String): Boolean =
        devTokenEnabled && StringUtils.hasText(devTokenValue) && devTokenValue == token

    // Spring Security가 "이 요청은 인증된 사용자 요청"이라고 알 수 있도록 인증 정보를 저장한다.
    private fun setAuthentication(loginId: String, role: String, request: HttpServletRequest) {
        val authentication = UsernamePasswordAuthenticationToken(
            loginId,
            null,
            listOf(SimpleGrantedAuthority(toAuthority(role)))
        )

        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authentication
    }

    // Spring Security 권한 이름은 ROLE_USER, ROLE_ADMIN처럼 ROLE_ 접두사를 붙여 사용한다.
    private fun toAuthority(role: String?): String {
        if (!StringUtils.hasText(role)) {
            return ROLE_PREFIX + "USER"
        }

        if (role!!.startsWith(ROLE_PREFIX)) {
            return role
        }

        return ROLE_PREFIX + role
    }

    companion object {
        private const val ROLE_PREFIX = "ROLE_"
        private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }
}
