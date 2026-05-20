package com.example.admin

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.repository.ReviewRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminMemberWithdrawnReviewTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var memberRepository: MemberRepository
    @Autowired private lateinit var reviewRepository: ReviewRepository
    @Autowired private lateinit var festivalRepository: FestivalRepository

    @AfterEach
    fun tearDown() {
        reviewRepository.deleteAllInBatch()
        festivalRepository.deleteAllInBatch()
        memberRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("탈퇴한 회원이 작성한 리뷰를 조회하면 닉네임이 '탈퇴된 회원입니다.'로 표시된다.")
    fun t5() {
        val visitor = Member.create("visitor", "1234", "방문자", "visitor@test.com", "방문자닉넴", Role.USER, 0)
        memberRepository.saveAndFlush(visitor)

        val member = Member.create("user5", "1234", "이름5", "user5@test.com", "탈퇴전이름", Role.USER, 0)
        memberRepository.save(member)
        member.withdraw()
        memberRepository.saveAndFlush(member)

        val festival = Festival(
            "F_005", "축제5", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.saveAndFlush(festival)

        val review = Review(member, festival, "탈퇴한 사람이 쓴 리뷰", null, 5)
        reviewRepository.saveAndFlush(review)

        mockMvc.perform(
            get("/api/festivals/${festival.id}/reviews")
                .param("page", "0")
                .param("size", "10")
                .with(user(visitor.loginId).roles("USER"))  // @WithMockUser 대신 실제 loginId 사용
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.data.content[0].nickname").value("탈퇴된 회원입니다."))
            .andDo(print())
    }
}