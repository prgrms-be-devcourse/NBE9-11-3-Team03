package com.example.admin

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.MemberStatus
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.repository.ReviewRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
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
class AdminMemberControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @Test
    @DisplayName("관리자 전체회원 목록조회")
    fun t1() {
        mockMvc.perform(
            get("/api/admin/members")
                .param("page", "0")
                .header("Authorization", "Bearer dev-temp-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("회원 목록 조회 성공"))
            .andDo(print())
    }

    @Test
    @DisplayName("신고누적순")
    fun t2() {
        val lowReport = Member("user1", "1234", "이름1", "user1@test.com", "저신고자", 1)
        val midReport = Member("user2", "1234", "이름2", "user2@test.com", "중신고자", 5)
        val highReport = Member("user3", "1234", "이름3", "user3@test.com", "고신고자", 10)

        memberRepository.saveAll(listOf(lowReport, midReport, highReport))

        mockMvc.perform(
            get("/api/admin/members/reported")
                .param("page", "0")
                .header("Authorization", "Bearer dev-temp-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("신고 누적회원 조회 성공"))
            .andExpect(jsonPath("$.data.content[0].nickname").value("고신고자"))
            .andExpect(jsonPath("$.data.content[0].reportCount").value(10))
            .andExpect(jsonPath("$.data.content[1].nickname").value("중신고자"))
            .andDo(print())
    }

    @Test
    @DisplayName("관리자가 회원을 강제 탈퇴시키면 상태가 WITHDRAWN으로 변경되고 닉네임이 마스킹된다.")
    fun t4() {
        val member = Member("user4", "1234", "이름4", "user4@test.com", "활동중인회원", 0)
        memberRepository.save(member)

        val memberId = member.id

        mockMvc.perform(
            patch("/api/admin/members/$memberId/withdraw")
                .header("Authorization", "Bearer dev-temp-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("회원이 강제 탈퇴 처리되었습니다."))
            .andDo(print())

        // Then:
        val withdrawnMember = memberRepository.findById(memberId!!).get()

        // 1. 상태가 WITHDRAWN인지 확인
        assertThat(withdrawnMember.status).isEqualTo(MemberStatus.WITHDRAWN)

        // 2. DB의 닉네임이 '탈퇴회원_ID' 형식으로 변경되었는지 확인
        assertThat(withdrawnMember.nickname).isEqualTo("탈퇴한회원_$memberId")
    }

    @Test
    @DisplayName("탈퇴한 회원이 작성한 리뷰를 조회하면 닉네임이 '탈퇴된 회원입니다.'로 표시된다.")
    @WithMockUser(username = "visitor", roles = ["USER"]) // 1. API를 호출할 유저 지정
    fun t5() {
        val visitor = Member("visitor", "1234", "방문자", "visitor@test.com", "방문자닉넴", 0)
        memberRepository.save(visitor)

        val member = Member("user5", "1234", "이름5", "user5@test.com", "탈퇴전이름", 0)
        memberRepository.save(member)
        member.withdraw() // 상태: WITHDRAWN 변경
        memberRepository.save(member)

        // 4. 축제 생성
        val festival = Festival(
            "F_005", "축제5", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.save(festival)

        // 5. 리뷰 생성
        val review = Review(member, festival, "탈퇴한 사람이 쓴 리뷰", null, 5)
        reviewRepository.save(review)

        // 6. When: 축제 리뷰 목록 조회 API 호출
        mockMvc.perform(
            get("/api/festivals/${festival.id}/reviews")
                .param("page", "0")
                .param("size", "10")
                .param("memberId", member.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.data.content[0].nickname").value("탈퇴된 회원입니다."))
            .andDo(print())
    }
}