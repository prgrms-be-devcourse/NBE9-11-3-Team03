package com.example.domain.member.service;

import com.example.domain.member.dto.request.LoginRequest;
import com.example.domain.member.dto.request.SignupRequest;
import com.example.domain.member.dto.request.TokenReissueRequest;
import com.example.domain.member.dto.response.LoginResponse;
import com.example.domain.member.dto.response.SignupResponse;
import com.example.domain.member.dto.response.TokenReissueResponse;
import com.example.domain.member.dto.response.WithdrawRes;
import com.example.domain.member.entity.AccessTokenBlacklist;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.member.entity.RefreshToken;
import com.example.domain.member.repository.AccessTokenBlacklistRepository;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.member.repository.RefreshTokenRepository;
import com.example.global.exception.*;
import com.example.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 회원가입과 로그인의 비즈니스 흐름을 담당하는 서비스다.
// 컨트롤러는 요청을 받고, 실제 처리 순서는 이 서비스가 조합한다.
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    // 회원가입 때는 비밀번호를 암호화하고, 로그인 때는 입력값과 저장값을 비교한다.
    private final PasswordEncoder passwordEncoder;
    // 로그인 성공 후 access token을 만들기 위해 사용하는 JWT 전용 유틸이다.
    private final JwtUtil jwtUtil;

    @Transactional
    // 1) 회원가입
    // 중복 검사 -> 비밀번호 암호화 -> 엔티티 생성 -> 저장 -> 응답 변환 순서로 진행
    public SignupResponse signup(SignupRequest request) {
        validateDuplicateSignupInfo(request);

        String encodedPassword = encodePassword(request.getPassword());

        Member member = Member.create(
                request.getUserName(),
                encodedPassword,
                request.getLoginId(),
                request.getEmail(),
                request.getNickname()
        );

        Member savedMember = memberRepository.save(member);
        return SignupResponse.from(savedMember);
    }

    // 2) 로그인
    // 회원 조회 -> 탈퇴 여부 확인 -> 비밀번호 검증 -> access/refresh token 발급 순서로 진행합니다.
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = findMemberByLoginId(request.getLoginId());
        validateMemberCanLogin(member);
        validatePassword(request.getPassword(), member.getPassword());

        String accessToken = createAccessToken(member);
        String refreshToken = createAndSaveRefreshToken(member);

        return LoginResponse.of(accessToken, refreshToken, member);
    }

    // 3) 토큰 재발급
    // refresh token이 정상이고 DB에 저장된 값과 같을 때만 새 토큰을 발급함.
    @Transactional
    public TokenReissueResponse reissue(TokenReissueRequest request) {
        return reissue(request.getRefreshToken());
    }

    @Transactional
    public TokenReissueResponse reissue(String refreshTokenValue) {
        validateRefreshToken(refreshTokenValue);

        RefreshToken refreshToken = findRefreshToken(refreshTokenValue);
        // 사용할 수 없는 refresh token이면 재발급하지 않음.
        validateRefreshTokenActive(refreshToken);
        validateRefreshTokenNotExpired(refreshToken);

        Member member = refreshToken.getMember();
        validateMemberCanLogin(member);

        String newAccessToken = createAccessToken(member);
        String newRefreshToken = jwtUtil.createRefreshToken(member);
        refreshToken.update(newRefreshToken, jwtUtil.getExpirationDateTime(newRefreshToken));

        return TokenReissueResponse.of(newAccessToken, newRefreshToken);
    }

    // 4) 로그아웃
    // 로그아웃하면 refresh token row는 남기고 token 값만 비워 재발급을 막습니다.
    @Transactional
    public void logout(String loginId, String accessToken) {
        Member member = findMemberByLoginId(loginId);
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresent(RefreshToken::logout);
        saveAccessTokenBlacklist(accessToken);
    }

    private void saveAccessTokenBlacklist(String accessToken) {
        if (accessToken == null || !jwtUtil.validateToken(accessToken) || !jwtUtil.isAccessToken(accessToken)) {
            return;
        }

        if (accessTokenBlacklistRepository.existsByToken(accessToken)) {
            return;
        }

        // 로그아웃된 access token은 만료 시간까지만 차단 목록에 저장함.
        accessTokenBlacklistRepository.save(
                AccessTokenBlacklist.create(accessToken, jwtUtil.getExpirationDateTime(accessToken))
        );
    }

    // 5) 회원가입 시 아이디, 이메일, 닉네임 중복 여부를 검사
    // 지금은 골격 단계이므로 예외는 IllegalArgumentException으로 두고,
    // 이후 커스텀 예외와 전역 예외 처리 단계에서 세분화하면 된다.
    private void validateDuplicateSignupInfo(SignupRequest request) {
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new DuplicateResourceException("409","이미 사용 중인 아이디입니다.");
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("409","이미 사용 중인 이메일입니다.");
        }

        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new DuplicateResourceException("409","이미 사용 중인 닉네임입니다.");
        }
    }

    // loginId로 회원을 조회한다.
    // 조회 결과가 없으면 서비스 계층 예외로 전환한다.
    private Member findMemberByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomNotFoundException("404","존재하지 않는 계정입니다."));
    }

    // 탈퇴한 회원은 로그인하지 못하도록 상태값을 검사한다.
    private void validateMemberCanLogin(Member member) {
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ForbiddenException("탈퇴된 계정입니다.");
        }
    }

    // 회원가입 시 비밀번호를 암호화해서 저장한다.
    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // 로그인 시 입력한 비밀번호와 저장된 암호화 비밀번호를 비교한다.
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new UnauthorizedException("비밀번호가 일치하지 않습니다.");
        }
    }
    // 토큰을 만드는 세부 로직은 JwtUtil에 맡긴다.
    // 이렇게 분리하면 AuthService는 로그인 흐름에만 집중할 수 있다.
    private String createAccessToken(Member member) {
        return jwtUtil.createAccessToken(member);
    }

    // refresh token은 DB에 저장하고, 이미 있으면 새 값으로 교체함.
    private String createAndSaveRefreshToken(Member member) {
        String refreshTokenValue = jwtUtil.createRefreshToken(member);
        LocalDateTime expiresAt = jwtUtil.getExpirationDateTime(refreshTokenValue);

        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.update(refreshTokenValue, expiresAt),
                        () -> refreshTokenRepository.save(RefreshToken.create(member, refreshTokenValue, expiresAt))
                );

        return refreshTokenValue;
    }

    // refresh token 형식이 맞는지 먼저 확인함.
    private void validateRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken) || !jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 refresh token입니다.");
        }
    }

    // DB에 저장된 refresh token인지 확인함.
    private RefreshToken findRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 refresh token입니다."));
    }

    // 사용할 수 없는 refresh token은 다시 재발급에 사용할 수 없음.
    private void validateRefreshTokenActive(RefreshToken refreshToken) {
        if (!refreshToken.isActive()) {
            throw new UnauthorizedException("사용할 수 없는 refresh token입니다.");
        }
    }

    // 만료된 refresh token은 삭제하고 재발급을 막음.
    private void validateRefreshTokenNotExpired(RefreshToken refreshToken) {
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("만료된 refresh token입니다.");
        }
    }

    //회원 스스로 탈퇴하는 메서드
    @Transactional
    public WithdrawRes selfWithdraw(String loginId,String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomNotFoundException("회원을 찾을 수 없습니다."));
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BadRequestException("이미 탈퇴 처리된 계정입니다.");
        }
        validatePassword(password,member.getPassword());
        member.withdraw();
        // 탈퇴 시에도 남아있는 refresh token을 사용 불가 상태로 바꿈.
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresent(RefreshToken::logout);
        return new WithdrawRes(member.getId(),member.getStatus());
    }
}
