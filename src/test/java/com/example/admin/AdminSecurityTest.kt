package com.example.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    // 테스트용으로 사용할 임의의 관리자 API 엔드포인트
    private static final String ADMIN_TEST_URL = "/api/admin/members";

    @Test
    @DisplayName("비로그인 사용자가 관리자 API에 접근하면 401(Unauthorized) 에러가 발생한다")
    public void unauthenticatedUserAccessAdminApi() throws Exception {
        // 인증 정보(Token이나 @WithMockUser) 없이 요청
        mockMvc.perform(get(ADMIN_TEST_URL))
                .andExpect(status().isUnauthorized()) // 401 상태 코드 기대
                .andDo(print());
    }

    @Test
    @DisplayName("일반 유저(ROLE_USER)가 관리자 API에 접근하면 403(Forbidden) 에러가 발생한다")
    @WithMockUser(username = "normalUser", roles = "USER") // 일반 유저 권한 주입
    public void normalUserAccessAdminApi() throws Exception {
        // 일반 유저 권한으로 관리자 엔드포인트 요청
        mockMvc.perform(get(ADMIN_TEST_URL))
                .andExpect(status().isForbidden()) // 403 상태 코드 기대
                .andDo(print());
    }
}
