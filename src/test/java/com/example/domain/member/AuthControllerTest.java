package com.example.domain.member;

import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.RefreshToken;
import com.example.domain.member.entity.Role;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.member.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "api.public-data.key=test-key",
        "api.public-data.base-url=https://example.com",
        "file.upload-dir=uploads",
        "security.dev-token.enabled=false"
})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // 회원가입 요청에 필요한 값을 준비합니다.
        String requestBody = signupRequest("authUser1", "auth1@test.com", "인증유저1");

        // 회원가입 API를 호출하고 저장된 회원 정보가 응답되는지 확인합니다.
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.loginId").value("authUser1"))
                .andExpect(jsonPath("$.data.nickname").value("인증유저1"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("회원가입 실패 - loginId 중복")
    void signup_fail_duplicateLoginId() throws Exception {
        // 같은 loginId를 가진 회원을 미리 저장합니다.
        saveMember("authUser2", "auth2@test.com", "인증유저2", "1234");

        String requestBody = signupRequest("authUser2", "auth2-new@test.com", "인증유저2새닉네임");

        // 이미 사용 중인 loginId로 가입하면 409 응답이 나와야 합니다.
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"));
    }

    @Test
    @DisplayName("회원가입 실패 - email 중복")
    void signup_fail_duplicateEmail() throws Exception {
        // 같은 email을 가진 회원을 미리 저장합니다.
        saveMember("authUser3", "auth3@test.com", "인증유저3", "1234");

        String requestBody = signupRequest("authUser3New", "auth3@test.com", "인증유저3새닉네임");

        // 이미 사용 중인 email로 가입하면 409 응답이 나와야 합니다.
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // 로그인할 회원을 미리 DB에 저장합니다.
        saveMember("authUser4", "auth4@test.com", "인증유저4", "1234");

        String requestBody = loginRequest("authUser4", "1234");

        // 로그인 성공 시 access token은 응답 body로, refresh token은 쿠키로 내려옵니다.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.loginId").value("authUser4"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_fail_wrongPassword() throws Exception {
        // 비밀번호 검증을 위해 암호화된 비밀번호를 가진 회원을 저장합니다.
        saveMember("authUser5", "auth5@test.com", "인증유저5", "1234");

        String requestBody = loginRequest("authUser5", "wrong-password");

        // 저장된 비밀번호와 입력 비밀번호가 다르면 401 응답이 나와야 합니다.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401"));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 아이디")
    void login_fail_notFoundLoginId() throws Exception {
        String requestBody = loginRequest("missingUser", "1234");

        // 존재하지 않는 loginId로 로그인하면 404 응답이 나와야 합니다.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() throws Exception {
        saveMember("authUser6", "auth6@test.com", "인증유저6", "1234");

        // 먼저 로그인해서 refresh token 쿠키를 발급받습니다.
        Cookie refreshTokenCookie = loginAndGetRefreshTokenCookie("authUser6", "1234");

        // refresh token 쿠키로 새 access token을 재발급받습니다.
        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 리프레쉬 토큰")
    void reissue_fail_expiredRefreshToken() throws Exception {
        saveMember("authUser7", "auth7@test.com", "인증유저7", "1234");

        Cookie refreshTokenCookie = loginAndGetRefreshTokenCookie("authUser7", "1234");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenCookie.getValue()).orElseThrow();

        // DB 기준 만료 시간을 과거로 바꿔서 만료된 refresh token 상황을 만듭니다.
        ReflectionTestUtils.setField(refreshToken, "expiresAt", LocalDateTime.now().minusSeconds(1));

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401"));
    }

    @Test
    @DisplayName("인증 없이 보호된 엔드포인트 접근 실패")
    void protectedEndpoint_fail_withoutAuthentication() throws Exception {
        // 토큰 없이 마이페이지 API에 접근하면 Security 설정에 의해 401 응답이 나와야 합니다.
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401"));
    }

    private Member saveMember(String loginId, String email, String nickname, String password) {
        return memberRepository.save(new Member(
                "테스트회원",
                passwordEncoder.encode(password),
                loginId,
                email,
                nickname,
                Role.USER
        ));
    }

    private Cookie loginAndGetRefreshTokenCookie(String loginId, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(loginId, password)))
                .andExpect(status().isOk())
                .andReturn();

        return loginResult.getResponse().getCookie("refreshToken");
    }

    private String signupRequest(String loginId, String email, String nickname) {
        return """
                {
                  "userName": "테스트회원",
                  "loginId": "%s",
                  "password": "1234",
                  "email": "%s",
                  "nickname": "%s"
                }
                """.formatted(loginId, email, nickname);
    }

    private String loginRequest(String loginId, String password) {
        return """
                {
                  "loginId": "%s",
                  "password": "%s"
                }
                """.formatted(loginId, password);
    }
}
