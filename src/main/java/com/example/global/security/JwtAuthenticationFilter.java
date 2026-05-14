package com.example.global.security;

import com.example.domain.member.repository.AccessTokenBlacklistRepository;
import com.example.global.jwt.JwtUtil;
import com.example.global.rsData.RsData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtUtil jwtUtil;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final TokenCookieManager tokenCookieManager;
    private final ObjectMapper objectMapper;

    // 개발 중 Postman 테스트를 위해 정식 JWT와 별도로 허용할 고정 토큰 사용 여부다.
    @Value("${security.dev-token.enabled:false}")
    private boolean devTokenEnabled;

    // 개발용 고정 토큰 값이다. 예: Authorization: Bearer dev-temp-token
    @Value("${security.dev-token.value:}")
    private String devTokenValue;

    // 개발용 토큰으로 인증할 때 SecurityContext에 넣을 임시 사용자 식별값이다.
    @Value("${security.dev-token.login-id:dev-user}")
    private String devTokenLoginId;

    // 개발용 토큰으로 인증할 때 부여할 임시 권한이다.
    @Value("${security.dev-token.role:USER}")
    private String devTokenRole;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = tokenCookieManager.resolveAccessToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (isLoggedOutAccessToken(token)) {
                writeUnauthorizedResponse(response, "이미 로그아웃 처리된 토큰입니다.");
                return;
            }

            authenticateByToken(token, request);
        }

        filterChain.doFilter(request, response);
    }

    // 개발용 고정 토큰이면 임시 인증을 만들고, 아니면 정식 JWT인지 검증한다.
    private void authenticateByToken(String token, HttpServletRequest request) {
        if (isDevToken(token)) {
            setAuthentication(devTokenLoginId, devTokenRole, request);
            return;
        }

        if (jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token) && !isBlacklisted(token)) {
            setAuthentication(jwtUtil.getLoginId(token), jwtUtil.getRole(token), request);
        }
    }

    private boolean isBlacklisted(String token) {
        // 로그아웃된 access token이면 다시 인증하지 않음.
        return accessTokenBlacklistRepository.existsByToken(token);
    }

    private boolean isLoggedOutAccessToken(String token) {
        return jwtUtil.validateToken(token) && jwtUtil.isAccessToken(token) && isBlacklisted(token);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                new RsData<>("401", message, null)
        );
    }


    // 개발용 토큰은 실제 JWT가 아니므로 설정값과 정확히 일치할 때만 통과시킨다.
    private boolean isDevToken(String token) {
        return devTokenEnabled
                && StringUtils.hasText(devTokenValue)
                && devTokenValue.equals(token);
    }

    // Spring Security가 "이 요청은 인증된 사용자 요청"이라고 알 수 있도록 인증 정보를 저장한다.
    private void setAuthentication(String loginId, String role, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginId,
                null,
                List.of(new SimpleGrantedAuthority(toAuthority(role)))
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Spring Security 권한 이름은 ROLE_USER, ROLE_ADMIN처럼 ROLE_ 접두사를 붙여 사용한다.
    private String toAuthority(String role) {
        if (!StringUtils.hasText(role)) {
            return ROLE_PREFIX + "USER";
        }

        if (role.startsWith(ROLE_PREFIX)) {
            return role;
        }

        return ROLE_PREFIX + role;
    }
}
