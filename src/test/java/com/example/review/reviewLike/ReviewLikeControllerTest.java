package com.example.review.reviewLike;

import com.example.domain.review.controller.ReviewLikeController;
import com.example.domain.review.service.ReviewLikeService;
import com.example.domain.reviewlike.dto.ReviewLikeResponseDto;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ConflictException;
import com.example.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewLikeService reviewLikeService;

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("좋아요 성공")
    void likeReview_success() throws Exception {
        Long reviewId = 1L;
        ReviewLikeResponseDto response = new ReviewLikeResponseDto(reviewId, 100L, true, 1);

        when(reviewLikeService.likeReview(eq(reviewId), eq("user1")))
                .thenReturn(response);

        mockMvc.perform(post("/api/reviews/{reviewId}/like", reviewId)
                        .principal(() -> "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("좋아요 상태가 변경되었습니다."))
                .andExpect(jsonPath("$.data.reviewId").value(1))
                .andExpect(jsonPath("$.data.memberId").value(100))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("같은 유저 중복 좋아요")
    void likeReview_duplicate() throws Exception {
        Long reviewId = 1L;

        when(reviewLikeService.likeReview(eq(reviewId), eq("user1")))
                .thenThrow(new ConflictException("이미 좋아요를 누른 리뷰입니다."));

        mockMvc.perform(post("/api/reviews/{reviewId}/like", reviewId)
                        .principal(() -> "user1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.message").value("이미 좋아요를 누른 리뷰입니다."));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("좋아요 취소 성공")
    void cancelLikeReview_success() throws Exception {
        Long reviewId = 1L;
        ReviewLikeResponseDto response = new ReviewLikeResponseDto(reviewId, 100L, false, 0);

        when(reviewLikeService.cancelLikeReview(eq(reviewId), eq("user1")))
                .thenReturn(response);

        mockMvc.perform(delete("/api/reviews/{reviewId}/like", reviewId)
                        .principal(() -> "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리뷰 좋아요가 취소되었습니다."))
                .andExpect(jsonPath("$.data.reviewId").value(1))
                .andExpect(jsonPath("$.data.memberId").value(100))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("좋아요 누르지 않은 상태에서 취소")
    void cancelLikeReview_withoutLike() throws Exception {
        Long reviewId = 1L;

        when(reviewLikeService.cancelLikeReview(eq(reviewId), eq("user1")))
                .thenThrow(new BadRequestException("좋아요를 누르지 않은 리뷰입니다."));

        mockMvc.perform(delete("/api/reviews/{reviewId}/like", reviewId)
                        .principal(() -> "user1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.message").value("좋아요를 누르지 않은 리뷰입니다."));
    }
}