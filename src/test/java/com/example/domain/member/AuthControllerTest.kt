package com.example.domain.member

import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import com.example.domain.member.repository.RefreshTokenRepository
import com.jayway.jsonpath.JsonPath
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(
    properties = ["api.public-data.key=test-key", "api.public-data.base-url=https://example.com", "file.upload-dir=uploads", "security.dev-token.enabled=false"
    ]
)
class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    @DisplayName("회원가입 성공")
    fun signup_success() {
        // 회원가입 요청에 필요한 값을 준비합니다.
        val requestBody = signupRequest("authUser1", "auth1@test.com", "인증유저1")

        // 회원가입 API를 호출하고 저장된 회원 정보가 응답되는지 확인합니다.
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.loginId").value("authUser1"))
            .andExpect(jsonPath("$.data.nickname").value("인증유저1"))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
    }

    @Test
    @DisplayName("회원가입 실패 - loginId 중복")
    fun signup_fail_duplicateLoginId() {
        // 같은 loginId를 가진 회원을 미리 저장합니다.
        saveMember("authUser2", "auth2@test.com", "인증유저2", "1234")

        val requestBody = signupRequest("authUser2", "auth2-new@test.com", "인증유저2새닉네임")

        // 이미 사용 중인 loginId로 가입하면 409 응답이 나와야 합니다.
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value("409"))
    }

    @Test
    @DisplayName("회원가입 실패 - email 중복")
    fun signup_fail_duplicateEmail() {
        // 같은 email을 가진 회원을 미리 저장합니다.
        saveMember("authUser3", "auth3@test.com", "인증유저3", "1234")

        val requestBody = signupRequest("authUser3New", "auth3@test.com", "인증유저3새닉네임")

        // 이미 사용 중인 email로 가입하면 409 응답이 나와야 합니다.
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value("409"))
    }

    @Test
    @DisplayName("로그인 성공")
    fun login_success() {
        // 로그인할 회원을 미리 DB에 저장합니다.
        saveMember("authUser4", "auth4@test.com", "인증유저4", "1234")

        val requestBody = loginRequest("authUser4", "1234")

        // 로그인 성공 시 access token은 응답 body로, refresh token은 쿠키로 내려옵니다.
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.loginId").value("authUser4"))
            .andExpect(cookie().exists("refreshToken"))
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    fun login_fail_wrongPassword() {
        // 비밀번호 검증을 위해 암호화된 비밀번호를 가진 회원을 저장합니다.
        saveMember("authUser5", "auth5@test.com", "인증유저5", "1234")

        val requestBody = loginRequest("authUser5", "wrong-password")

        // 저장된 비밀번호와 입력 비밀번호가 다르면 401 응답이 나와야 합니다.
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("401"))
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 아이디")
    fun login_fail_notFoundLoginId() {
        val requestBody = loginRequest("missingUser", "1234")

        // 존재하지 않는 loginId로 로그인하면 404 응답이 나와야 합니다.
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value("404"))
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    fun reissue_success() {
        saveMember("authUser6", "auth6@test.com", "인증유저6", "1234")

        // 먼저 로그인해서 refresh token 쿠키를 발급받습니다.
        val refreshTokenCookie = loginAndGetRefreshTokenCookie("authUser6", "1234")

        // refresh token 쿠키로 새 access token을 재발급받습니다.
        mockMvc.perform(
            post("/api/auth/reissue")
                .cookie(refreshTokenCookie)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(cookie().exists("refreshToken"))
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 리프레쉬 토큰")
    fun reissue_fail_expiredRefreshToken() {
        saveMember("authUser7", "auth7@test.com", "인증유저7", "1234")

        val refreshTokenCookie = loginAndGetRefreshTokenCookie("authUser7", "1234")
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenCookie.value)
            ?: error("refresh token should exist")

        // DB 기준 만료 시간을 과거로 바꿔서 만료된 refresh token 상황을 만듭니다.
        ReflectionTestUtils.setField(refreshToken, "expiresAt", LocalDateTime.now().minusSeconds(1))

        mockMvc.perform(
            post("/api/auth/reissue")
                .cookie(refreshTokenCookie)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("401"))
    }

    @Test
    @DisplayName("인증 없이 보호된 엔드포인트 접근 실패")
    fun protectedEndpoint_fail_withoutAuthentication() {
        // 토큰 없이 마이페이지 API에 접근하면 Security 설정에 의해 401 응답이 나와야 합니다.
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("401"))
    }

    @Test
    @DisplayName("회원 탈퇴 성공 후 기존 access token 인증 실패")
    fun selfWithdraw_success_thenAccessTokenCannotAuthenticate() {
        saveMember("withdrawUser1", "withdraw1@test.com", "탈퇴테스트1", "1234")
        // 탈퇴 전에 발급받은 access token을 준비.
        val accessToken = loginAndGetAccessToken("withdrawUser1", "1234")

        // 현재 access token으로 회원 탈퇴를 요청.
        mockMvc.perform(
            delete("/api/users/me/withdraw")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(withdrawRequest("1234", "1234"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("WITHDRAWN"))

        // 탈퇴 후 같은 access token으로 인증 요청을 하면 실패해야 함.
        mockMvc.perform(
            get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        )
            .andExpect(status().isUnauthorized)
    }

    private fun saveMember(
        loginId: String,
        email: String,
        nickname: String,
        password: String
    ): Member =
        memberRepository.save(
            Member.create(
                userName = "테스트회원",
                password = passwordEncoder.encode(password),
                loginId = loginId,
                email = email,
                nickname = nickname,
                role = Role.USER
            )
        )

    private fun loginAndGetRefreshTokenCookie(loginId: String, password: String): Cookie {
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest(loginId, password))
        )
            .andExpect(status().isOk)
            .andReturn()

        return loginResult.response.getCookie("refreshToken")
            ?: error("refreshToken cookie should exist")
    }

    // 로그인 응답 body에서 access token만 꺼내 테스트에 사용.
    private fun loginAndGetAccessToken(loginId: String, password: String): String {
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest(loginId, password))
        )
            .andExpect(status().isOk)
            .andReturn()

        return JsonPath.read(loginResult.response.contentAsString, "$.data.accessToken")
            ?: error("access token should exist")
    }

    private fun signupRequest(loginId: String, email: String, nickname: String): String {
        return """
                {
                  "userName": "테스트회원",
                  "loginId": "$loginId",
                  "password": "1234",
                  "email": "$email",
                  "nickname": "$nickname"
                }
                """.trimIndent()
    }

    private fun loginRequest(loginId: String, password: String): String {
        return """
                {
                  "loginId": "$loginId",
                  "password": "$password"
                }
                """.trimIndent()
    }

    // 회원 탈퇴 요청 body를 생성.
    private fun withdrawRequest(password: String, passwordConfirm: String): String {
        return """
                {
                  "password": "$password",
                  "passwordConfirm": "$passwordConfirm"
                }
                """.trimIndent()
    }
}
