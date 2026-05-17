package com.example.domain.member.service

import com.example.domain.member.dto.request.LoginRequest
import com.example.domain.member.dto.request.SignupRequest
import com.example.domain.member.dto.request.TokenReissueRequest
import com.example.domain.member.dto.response.LoginResponse
import com.example.domain.member.dto.response.SignupResponse
import com.example.domain.member.dto.response.TokenReissueResponse
import com.example.domain.member.dto.response.WithdrawResponse
import com.example.domain.member.entity.AccessTokenBlacklist
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.entity.RefreshToken
import com.example.domain.member.repository.AccessTokenBlacklistRepository
import com.example.domain.member.repository.MemberRepository
import com.example.domain.member.repository.RefreshTokenRepository
import com.example.global.exception.BadRequestException
import com.example.global.exception.CustomNotFoundException
import com.example.global.exception.DuplicateResourceException
import com.example.global.exception.ForbiddenException
import com.example.global.exception.UnauthorizedException
import com.example.global.jwt.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
@Transactional(readOnly = true)
class AuthService(
    private val memberRepository: MemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val accessTokenBlacklistRepository: AccessTokenBlacklistRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {

    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        validateDuplicateSignupInfo(request)

        val encodedPassword = encodePassword(request.password)
        val member = Member.create(
            request.userName,
            encodedPassword,
            request.loginId,
            request.email,
            request.nickname
        )

        val savedMember = memberRepository.save(member)
        log.info("[Member] 회원가입 완료 - loginId=${savedMember.loginId}")

        return SignupResponse.from(savedMember)
    }

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val member = findMemberByLoginId(request.loginId)
        validateMemberCanLogin(member)
        validateLoginPassword(request.loginId, request.password, member.password)

        val accessToken = createAccessToken(member)
        val refreshToken = createAndSaveRefreshToken(member)

        log.info("[Member] 로그인 성공 - loginId=${member.loginId}")
        return LoginResponse.of(accessToken, refreshToken, member)
    }

    @Transactional
    fun reissue(request: TokenReissueRequest): TokenReissueResponse {
        return reissue(request.refreshToken)
    }

    @Transactional
    fun reissue(refreshTokenValue: String?): TokenReissueResponse {
        validateRefreshToken(refreshTokenValue)

        val refreshToken = findRefreshToken(refreshTokenValue)
        validateRefreshTokenActive(refreshToken)
        validateRefreshTokenNotExpired(refreshToken)

        val member = refreshToken.member
        validateMemberCanLogin(member)

        val newAccessToken = createAccessToken(member)
        val newRefreshToken = jwtUtil.createRefreshToken(member)
        refreshToken.update(newRefreshToken, jwtUtil.getExpirationDateTime(newRefreshToken))

        log.info("[Member] 토큰 재발급 완료 - loginId=${member.loginId}")
        return TokenReissueResponse.of(newAccessToken, newRefreshToken)
    }

    @Transactional
    fun logout(loginId: String, accessToken: String?) {
        val member = findMemberByLoginId(loginId)
        refreshTokenRepository.findByMemberId(member.id)
            .ifPresent(RefreshToken::logout)

        saveAccessTokenBlacklist(accessToken)
        log.info("[Member] 로그아웃 처리 - loginId=${member.loginId}")
    }

    @Transactional
    fun selfWithdraw(loginId: String, password: String?, accessToken: String?): WithdrawResponse {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { CustomNotFoundException("회원을 찾을 수 없습니다.") }

        if (member.status == MemberStatus.WITHDRAWN) {
            throw BadRequestException("이미 탈퇴 처리된 계정입니다.")
        }

        validatePassword(password, member.password)
        member.withdraw()
        refreshTokenRepository.findByMemberId(member.id)
            .ifPresent(RefreshToken::logout)

        saveAccessTokenBlacklist(accessToken)
        log.info("[Member] 회원 탈퇴 처리 - loginId=$loginId")

        return WithdrawResponse(member.id, member.status)
    }

    private fun saveAccessTokenBlacklist(accessToken: String?) {
        if (accessToken == null || !jwtUtil.validateToken(accessToken) || !jwtUtil.isAccessToken(accessToken)) {
            return
        }

        if (accessTokenBlacklistRepository.existsByToken(accessToken)) {
            return
        }

        accessTokenBlacklistRepository.save(
            AccessTokenBlacklist.create(accessToken, jwtUtil.getExpirationDateTime(accessToken))
        )
    }

    private fun validateDuplicateSignupInfo(request: SignupRequest) {
        if (memberRepository.existsByLoginId(request.loginId)) {
            throw DuplicateResourceException("409", "이미 사용 중인 아이디입니다.")
        }

        if (memberRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("409", "이미 사용 중인 이메일입니다.")
        }

        if (memberRepository.existsByNickname(request.nickname)) {
            throw DuplicateResourceException("409", "이미 사용 중인 닉네임입니다.")
        }
    }

    private fun findMemberByLoginId(loginId: String?): Member {
        return memberRepository.findByLoginId(loginId)
            .orElseThrow { CustomNotFoundException("404", "존재하지 않는 계정입니다.") }
    }

    private fun validateMemberCanLogin(member: Member) {
        if (member.status == MemberStatus.WITHDRAWN) {
            throw ForbiddenException("탈퇴된 계정입니다.")
        }
    }

    private fun encodePassword(rawPassword: String?): String {
        return passwordEncoder.encode(rawPassword)
    }

    private fun validatePassword(rawPassword: String?, encodedPassword: String) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw UnauthorizedException("비밀번호가 일치하지 않습니다.")
        }
    }

    private fun validateLoginPassword(loginId: String?, rawPassword: String?, encodedPassword: String) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("[Member] 로그인 실패 - loginId=$loginId")
            throw UnauthorizedException("비밀번호가 일치하지 않습니다.")
        }
    }

    private fun createAccessToken(member: Member): String {
        return jwtUtil.createAccessToken(member)
    }

    private fun createAndSaveRefreshToken(member: Member): String {
        val refreshTokenValue = jwtUtil.createRefreshToken(member)
        val expiresAt = jwtUtil.getExpirationDateTime(refreshTokenValue)

        val refreshToken = refreshTokenRepository.findByMemberId(member.id).orElse(null)
        if (refreshToken != null) {
            refreshToken.update(refreshTokenValue, expiresAt)
        } else {
            refreshTokenRepository.save(RefreshToken.create(member, refreshTokenValue, expiresAt))
        }

        return refreshTokenValue
    }

    private fun validateRefreshToken(refreshToken: String?) {
        if (!StringUtils.hasText(refreshToken) || !jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw UnauthorizedException("유효하지 않은 refresh token입니다.")
        }
    }

    private fun findRefreshToken(refreshToken: String?): RefreshToken {
        return refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow { UnauthorizedException("유효하지 않은 refresh token입니다.") }
    }

    private fun validateRefreshTokenActive(refreshToken: RefreshToken) {
        if (!refreshToken.isActive) {
            throw UnauthorizedException("사용할 수 없는 refresh token입니다.")
        }
    }

    private fun validateRefreshTokenNotExpired(refreshToken: RefreshToken) {
        if (refreshToken.isExpired) {
            log.warn("[Member] 만료된 리프레시 토큰 - memberId=${refreshToken.member.id}")
            refreshTokenRepository.delete(refreshToken)
            throw UnauthorizedException("만료된 refresh token입니다.")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuthService::class.java)
    }
}
