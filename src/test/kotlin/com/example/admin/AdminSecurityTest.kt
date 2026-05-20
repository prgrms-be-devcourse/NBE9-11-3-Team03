package com.example.admin

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityTest {

    // lateinit var를 적용하여 불필요한 null 처리와 !! 연산자를 완전히 제거합니다.
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("비로그인 사용자가 관리자 API에 접근하면 401(Unauthorized) 에러가 발생한다")
    fun unauthenticatedUserAccessAdminApi() {
        // Given & When & Then (정적 임포트를 활용한 간결한 체이닝 구조)
        mockMvc.perform(get(ADMIN_TEST_URL))
            .andExpect(status().isUnauthorized) // .isUnauthorized() 메서드 호출을 프로퍼티 접근 형태로 변경
            .andDo(print())
    }

    @Test
    @DisplayName("일반 유저(ROLE_USER)가 관리자 API에 접근하면 403(Forbidden) 에러가 발생한다")
    @WithMockUser(username = "normalUser", roles = ["USER"]) // 일반 유저 권한 가짜 주입
    fun normalUserAccessAdminApi() {
        // Given & When & Then
        mockMvc.perform(get(ADMIN_TEST_URL))
            .andExpect(status().isForbidden) // .isForbidden()에서 괄호를 생략한 코틀린 스타일 프로퍼티 접근
            .andDo(print())
    }

    companion object {
        // 테스트용으로 사용할 임의의 관리자 API 엔드포인트
        private const val ADMIN_TEST_URL = "/api/admin/members"
    }
}