package com.example.domain.member.controller

import com.example.domain.member.dto.request.LoginRequest
import com.example.domain.member.dto.request.SignupRequest
import com.example.domain.member.dto.response.LoginResponse
import com.example.domain.member.dto.response.SignupResponse
import com.example.domain.member.dto.response.TokenReissueResponse
import com.example.domain.member.service.AuthService
import com.example.global.response.ApiRes
import com.example.global.security.TokenCookieManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원 인증 API")
class AuthController(
    private val authService: AuthService,
    private val tokenCookieManager: TokenCookieManager
) {

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이름, 아이디, 비밀번호, 이메일, 닉네임을 입력받아 회원가입을 처리합니다.")
    fun signup(
        @Valid @RequestBody request: SignupRequest
    ): ResponseEntity<ApiRes<SignupResponse>> {
        val response = authService.signup(request)

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiRes(201, "회원가입 성공", response))
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "loginId와 비밀번호를 검증한 뒤 로그인 결과를 반환합니다.")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<ApiRes<LoginResponse>> {
        val response = authService.login(request)
        tokenCookieManager.addRefreshTokenCookie(httpResponse, response.refreshToken)

        return ResponseEntity.ok(ApiRes(200, "로그인 성공", response))
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "refresh token을 검증한 뒤 새 토큰을 발급합니다.")
    fun reissue(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<ApiRes<TokenReissueResponse>> {
        val refreshToken = tokenCookieManager.resolveRefreshToken(httpRequest)

        val response = authService.reissue(refreshToken)
        tokenCookieManager.addRefreshTokenCookie(httpResponse, response.refreshToken)

        return ResponseEntity.ok(ApiRes(200, "토큰 재발급 성공", response))
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인한 회원의 refresh token을 사용 불가 상태로 변경합니다.")
    fun logout(
        authentication: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiRes<Void>> {
        authService.logout(authentication.name, tokenCookieManager.resolveAccessToken(request))
        tokenCookieManager.clearRefreshTokenCookie(response)

        return ResponseEntity.ok(ApiRes(200, "로그아웃 성공", null))
    }
}
