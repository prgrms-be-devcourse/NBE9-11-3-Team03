package com.example.admin

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminReviewBlindVisibilityTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var reviewRepository: ReviewRepository
    @Autowired private lateinit var memberRepository: MemberRepository
    @Autowired private lateinit var festivalRepository: FestivalRepository

    @AfterEach
    fun tearDown() {
        reviewRepository.deleteAllInBatch()
        festivalRepository.deleteAllInBatch()
        memberRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("전체 리뷰 목록 조회 시 블라인드된 리뷰는 노출되지 않아야 한다")
    fun t4() {
        val author = Member.create("user4", "1234", "이름4", "user4@test.com", "작성자4")
        memberRepository.saveAndFlush(author)

        val festival = Festival(
            "F_004", "축제4", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.saveAndFlush(festival)

        val normalReview = Review(author, festival, "정상 리뷰", null, 5)
        val blindReview = Review(author, festival, "블라인드 리뷰", null, 5)
        ReflectionTestUtils.setField(blindReview, "status", ReviewStatus.BLIND)
        reviewRepository.saveAllAndFlush(listOf(normalReview, blindReview))

        mockMvc.perform(
            get("/api/festivals/${festival.id}/reviews")
                .param("page", "0")
                .param("size", "10")
                .with(user(author.loginId).roles("USER"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].content").value("정상 리뷰"))
            .andDo(print())
    }
}