package com.example.review.reviewLike

import com.example.domain.review.service.ReviewLikeService
import com.example.domain.reviewlike.dto.response.ReviewLikeResponse
import com.example.global.exception.BadRequestException
import com.example.global.exception.ConflictException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ReviewLikeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var reviewLikeService: ReviewLikeService

    @Test
    @WithMockUser(username = "user1", roles = ["USER"])
    @DisplayName("좋아요 성공")
    fun likeReview_success() {
        val reviewId = 1L
        val response = ReviewLikeResponse(
            reviewId = reviewId,
            memberId = 100L,
            isLiked = true,
            likeCount = 1
        )

        given(reviewLikeService.likeReview(reviewId, "user1"))
            .willReturn(response)

        mockMvc.perform(
            post("/api/reviews/{reviewId}/like", reviewId)
                .principal { "user1" }
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("좋아요 상태가 변경되었습니다."))
            .andExpect(jsonPath("$.data.reviewId").value(1))
            .andExpect(jsonPath("$.data.memberId").value(100))
            .andExpect(jsonPath("$.data.isLiked").value(true))
            .andExpect(jsonPath("$.data.likeCount").value(1))
    }

    @Test
    @WithMockUser(username = "user1", roles = ["USER"])
    @DisplayName("같은 유저 중복 좋아요")
    fun likeReview_duplicate() {
        val reviewId = 1L

        given(reviewLikeService.likeReview(reviewId, "user1"))
            .willThrow(ConflictException("이미 좋아요를 누른 리뷰입니다."))

        mockMvc.perform(
            post("/api/reviews/{reviewId}/like", reviewId)
                .principal { "user1" }
        )
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("409"))
            .andExpect(jsonPath("$.message").value("이미 좋아요를 누른 리뷰입니다."))
    }

    @Test
    @WithMockUser(username = "user1", roles = ["USER"])
    @DisplayName("좋아요 취소 성공")
    fun cancelLikeReview_success() {
        val reviewId = 1L
        val response = ReviewLikeResponse(
            reviewId = reviewId,
            memberId = 100L,
            isLiked = false,
            likeCount = 0
        )

        given(reviewLikeService.cancelLikeReview(reviewId, "user1"))
            .willReturn(response)

        mockMvc.perform(
            delete("/api/reviews/{reviewId}/like", reviewId)
                .principal { "user1" }
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("리뷰 좋아요가 취소되었습니다."))
            .andExpect(jsonPath("$.data.reviewId").value(1))
            .andExpect(jsonPath("$.data.memberId").value(100))
            .andExpect(jsonPath("$.data.isLiked").value(false))
            .andExpect(jsonPath("$.data.likeCount").value(0))
    }

    @Test
    @WithMockUser(username = "user1", roles = ["USER"])
    @DisplayName("좋아요 누르지 않은 상태에서 취소")
    fun cancelLikeReview_withoutLike() {
        val reviewId = 1L

        given(reviewLikeService.cancelLikeReview(reviewId, "user1"))
            .willThrow(BadRequestException("좋아요를 누르지 않은 리뷰입니다."))

        mockMvc.perform(
            delete("/api/reviews/{reviewId}/like", reviewId)
                .principal { "user1" }
        )
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("400"))
            .andExpect(jsonPath("$.message").value("좋아요를 누르지 않은 리뷰입니다."))
    }
}