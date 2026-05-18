package com.example.admin

import com.example.domain.admin.dto.request.ReviewProcessRequest
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminReviewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("신고 누적 5회 이상인 리뷰 조회")
    fun t1() {
        // Given
        val author = Member.create("user1", "1234", "이름1", "user1@test.com", "작성자", Role.USER,0)
        memberRepository.save(author)

        // 이전 단계에서 정의한 코틀린 스타일 생성자 호출 적용 (JvmOverloads 활용)
        val festival = Festival(
            "F_001", "축제", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1),
            127.0, 37.0
        )
        festivalRepository.save(festival)

        val lowReport = Review(author, festival, "정상리뷰", null, 5)
        val midReport = Review(author, festival, "중간신고", null, 3)
        val highReport = Review(author, festival, "고신고리뷰", null, 1)

        // 리플렉션을 통한 내부 reportCount 필드 제어
        ReflectionTestUtils.setField(lowReport, "reportCount", 0)
        ReflectionTestUtils.setField(midReport, "reportCount", 8)
        ReflectionTestUtils.setField(highReport, "reportCount", 15)

        reviewRepository.saveAll(listOf(lowReport, midReport, highReport))

        // When & Then
        mockMvc.perform(
            get("/api/admin/reviews/reported")
                .param("page", "0")
                .header("Authorization", "Bearer dev-temp-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("신고된 리뷰 목록 조회가 완료되었습니다."))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.content[0].reportCount").value(15))
            .andExpect(jsonPath("$.data.content[0].content").value("고신고리뷰"))
            .andExpect(jsonPath("$.data.content[1].reportCount").value(8))
            .andDo(print())
    }

    @Test
    @DisplayName("리뷰 블라인드 처리 - 리뷰 상태 변경 및 작성자 신고 횟수 증가 확인")
    fun t2() {
        // Given
        val author = Member.create("user2", "1234", "이름2", "user2@test.com", "작성자2")
        memberRepository.save(author)

        val festival = Festival(
            "F_002", "축제2", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.save(festival)

        val targetReview = Review(author, festival, "신고 대상 리뷰", null, 5)
        reviewRepository.save(targetReview)

        val request = ReviewProcessRequest("BLIND")

        // When & Then
        mockMvc.perform(
            patch("/api/admin/reviews/${targetReview.id}/status")
                .header("Authorization", "Bearer dev-temp-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("리뷰가 블라인드 처리되었습니다."))
            .andDo(print())

        // DB 검증 단계 (프로퍼티 직접 접근으로 전환)
        val updatedReview = reviewRepository.findById(targetReview.id).get()
        val updatedAuthor = memberRepository.findById(author.id).get()

        assertThat(updatedReview.status).isEqualTo(ReviewStatus.BLIND)
        assertThat(updatedAuthor.reportCount).isEqualTo(1)
    }

    @Test
    @DisplayName("신고 초기화(무혐의) - 리뷰 신고수 리셋 확인")
    fun t3() {
        // Given
        val author = Member.create("user3", "1234", "이름3", "user3@test.com", "작성자3")
        memberRepository.save(author)

        val festival = Festival(
            "F_003", "축제3", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.save(festival)

        val targetReview = Review(author, festival, "억울한 리뷰", null, 5)
        ReflectionTestUtils.setField(targetReview, "reportCount", 10)
        reviewRepository.save(targetReview)

        val request = ReviewProcessRequest("DISMISS")

        // When & Then
        mockMvc.perform(
            patch("/api/admin/reviews/${targetReview.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer dev-temp-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("리뷰 신고횟수가 초기화 되었습니다."))
            .andDo(print())

        val updatedReview = reviewRepository.findById(targetReview.id).get()
        assertThat(updatedReview.reportCount).isEqualTo(0)
    }

    @Test
    @DisplayName("전체 리뷰 목록 조회 시 블라인드된 리뷰는 노출되지 않아야 한다")
    @WithMockUser(username = "user4", roles = ["USER"])
    fun t4() {
        // Given
        val author = Member.create("user4", "1234", "이름4", "user4@test.com", "작성자4")
        memberRepository.save(author)

        val festival = Festival(
            "F_004", "축제4", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.save(festival)

        val normalReview = Review(author, festival, "정상 리뷰", null, 5)
        val blindReview = Review(author, festival, "블라인드 리뷰", null, 5)

        ReflectionTestUtils.setField(blindReview, "status", ReviewStatus.BLIND)
        reviewRepository.saveAll(listOf(normalReview, blindReview))

        // When & Then (문자열 보간법 및 정적 임포트 적용)
        mockMvc.perform(
            get("/api/festivals/${festival.id}/reviews")
                .param("festivalId", festival.contentId)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].content").value("정상 리뷰"))
            .andDo(print())
    }
}