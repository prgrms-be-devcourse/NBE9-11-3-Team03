package com.example.review.reviewReport;

import com.example.domain.reviewreport.dto.ReviewReportResponse;
import com.example.domain.reviewreport.service.ReviewReportService;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityNotFoundException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewReportService reviewReportService;

    @Test
    @DisplayName("신고 성공")
    @WithMockUser(username = "user1", roles = "USER")
    void reportReview_success() throws Exception {
        Long reviewId = 1L;

        when(reviewReportService.reportReview(eq(reviewId), eq("user1")))
                .thenReturn(new ReviewReportResponse(100L));

        mockMvc.perform(post("/api/reviews/{reviewId}/reports", reviewId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("리뷰 신고가 성공적으로 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(100));
    }

    @Test
    @DisplayName("같은 유저 중복 신고")
    @WithMockUser(username = "user1", roles = "USER")
    void reportReview_duplicate() throws Exception {
        Long reviewId = 1L;

        when(reviewReportService.reportReview(eq(reviewId), eq("user1")))
                .thenThrow(new ConflictException("이미 신고한 리뷰입니다."));

        mockMvc.perform(post("/api/reviews/{reviewId}/reports", reviewId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.message").value("이미 신고한 리뷰입니다."));
    }

    @Test
    @DisplayName("자기 리뷰 신고 시도")
    @WithMockUser(username = "user1", roles = "USER")
    void reportReview_selfReport() throws Exception {
        Long reviewId = 1L;

        when(reviewReportService.reportReview(eq(reviewId), eq("user1")))
                .thenThrow(new BadRequestException("본인 리뷰는 신고할 수 없습니다."));

        mockMvc.perform(post("/api/reviews/{reviewId}/reports", reviewId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.message").value("본인 리뷰는 신고할 수 없습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 신고")
    @WithMockUser(username = "user1", roles = "USER")
    void reportReview_reviewNotFound() throws Exception {
        Long reviewId = 999L;

        when(reviewReportService.reportReview(eq(reviewId), eq("user1")))
                .thenThrow(new EntityNotFoundException("존재하지 않는 리뷰입니다."));

        mockMvc.perform(post("/api/reviews/{reviewId}/reports", reviewId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 리뷰입니다."));
    }

    @Test
    @DisplayName("인증 없이 신고 요청하면 401")
    void reportReview_unauthorized() throws Exception {
        mockMvc.perform(post("/api/reviews/{reviewId}/reports", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }
}