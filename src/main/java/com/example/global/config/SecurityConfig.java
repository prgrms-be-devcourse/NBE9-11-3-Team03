package com.example.global.config;

import com.example.global.rsData.RsData;
import com.example.global.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // JWT 방식은 서버 세션을 사용하지 않으므로 CSRF 보호끄기
                .csrf(AbstractHttpConfigurer::disable)
                // 브라우저 기본 로그인창과 Basic Auth를 끄고, 우리가 만든 JWT 필터로 인증
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // JWT는 요청마다 토큰을 확인하므로 서버에 로그인 세션을 저장하지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인증이 필요한 API에 토큰 없이 접근하거나, 잘못된 토큰으로 접근했을 때 실행
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                            objectMapper.writeValue(
                                    response.getWriter(),
                                    new RsData<>("401", "로그인이 필요합니다.", null)
                            );
                        })
                        // 로그인은 성공했지만 필요한 권한이 없는 API에 접근했을 때 실행된다.
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                            objectMapper.writeValue(
                                    response.getWriter(),
                                    new RsData<>("403", "접근 권한이 없습니다.", null)
                            );
                        })
                )
                // H2 콘솔을 iframe으로 열 수 있도록 개발 중에만 같은 출처 frame을 허용한다.
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인, 토큰 재발급은 access token 없이 접근할 수 있어야 함.
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/reissue").permitAll()
                        // Swagger와 H2 콘솔은 개발 중 API 확인을 위해 열어둔다.
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**",
                                "/uploads/**"
                        ).permitAll()
                        // 관리자 API는 ADMIN 권한이 있는 사용자나 개발용 ADMIN 토큰만 접근할 수 있다.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 비회원은 축제목록조회와 축제상세조회만 가능하도록 설정 했다.
                        .requestMatchers(HttpMethod.GET, "/api/festivals", "/api/festivals/*").permitAll()
                        // 위에서 허용한 API를 제외한 나머지 API는 JWT 또는 개발용 토큰 인증이 필요
                        .anyRequest().authenticated()
                )
                // Controller에 도달하기 전에 Authorization 헤더의 JWT 또는 개발용 토큰을 검사
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
