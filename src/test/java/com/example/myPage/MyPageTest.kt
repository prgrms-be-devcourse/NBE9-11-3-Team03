package com.example.myPage

import com.example.domain.bookmark.entity.FestivalBookmark
import com.example.domain.bookmark.repository.FestivalBookmarkRepository
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Member.Companion.create
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
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MyPageTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var festivalBookmarkRepository: FestivalBookmarkRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    @DisplayName("마이페이지 조회 - 회원 정보와 함께 리뷰/북마크 개수가 정확히 조회된다.")
    @WithMockUser(username = "myPageUser")
    fun t1() {
        val member = create("홍길동", "1234", "myPageUser", "mypage@test.com", "길동이t1")
        memberRepository.save(member)

        val festival = Festival(
            "F_006",
            "서울 세계불꽃축제",
            "설명",
            "여의도",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            126.92,
            37.52,
            null, null, null, null, null,
            FestivalStatus.UPCOMING
        )
        festivalRepository.save(festival)

        val review1 = Review(member, festival, "정말 재밌어요!", null, 5)
        val review2 = Review(member, festival, "또 가고 싶네요.", null, 4)
        reviewRepository.saveAll(listOf(review1, review2))

        val bookmark = FestivalBookmark(member, festival)
        festivalBookmarkRepository.save(bookmark)

        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("마이페이지 조회에 성공하였습니다."))
            .andExpect(jsonPath("$.data.nickname").value("길동이t1"))
            .andExpect(jsonPath("$.data.email").value("mypage@test.com"))
            .andExpect(jsonPath("$.data.reviewCount").value(2))
            .andExpect(jsonPath("$.data.bookMarkCount").value(1))
            .andDo(print())
    }

    @Test
    @DisplayName("내가 쓴 리뷰 조회 - 작성한 리뷰 목록이 최신순으로 페이징되어 조회된다.")
    @WithMockUser(username = "myPageUser")
    fun getMyReviewsTest() {
        val member = create("홍길동", "1234", "myPageUser", "mypage@test.com", "길동이t1")
        memberRepository.save(member)

        val festival = Festival(
            "F_TEST",
            "테스트 축제",
            "설명",
            "주소",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
            126.0,
            37.0,
            null, null, null, null, null,
            FestivalStatus.UPCOMING
        )
        festivalRepository.save(festival)

        val review1 = Review(member, festival, "첫 번째 리뷰", null, 5)
        val review2 = Review(member, festival, "두 번째 리뷰", null, 4)
        reviewRepository.saveAll(listOf(review1, review2))

        mockMvc.perform(
            get("/api/users/me/reviews")
                .param("page", "0")
                .param("size", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("200"))
            .andExpect(jsonPath("$.message").value("내가 쓴 리뷰 목록 조회 성공"))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].content").value("두 번째 리뷰"))
            .andExpect(jsonPath("$.data.content[0].festivalTitle").value("테스트 축제"))
            .andExpect(jsonPath("$.data.content[1].content").value("첫 번째 리뷰"))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andDo(print())
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 비밀번호가 일치하면 상태가 WITHDRAWN으로 변경된다.")
    @WithMockUser(username = "withdrawUser")
    fun withdrawSuccessTest() {
        val loginId = "withdrawUser"
        val rawPassword = "password123"

        val member = create(
            "길동",
            passwordEncoder.encode(rawPassword),
            loginId,
            "withdraw@test.com",
            "탈퇴전닉네임"
        )
        memberRepository.save(member)

        val requestBody = """
            {
                "password": "$rawPassword",
                "passwordConfirm": "$rawPassword"
            }
        """.trimIndent()

        mockMvc.perform(
            delete("/api/users/me/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("탈퇴처리가 성공적으로 수행되었습니다."))
            .andDo(print())

        val withdrawnMember = memberRepository.findById(member.id).orElseThrow()
        assertThat(withdrawnMember.status).isEqualTo(MemberStatus.WITHDRAWN)
        assertThat(withdrawnMember.nickname).isEqualTo("탈퇴한회원_${member.id}")
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 비밀번호가 일치하지 않으면 400 에러를 반환한다.")
    @WithMockUser(username = "failUser")
    fun withdrawFailTest() {
        val member = create(
            "길동",
            passwordEncoder.encode("correctPassword"),
            "failUser",
            "fail@test.com",
            "실패테스트"
        )
        memberRepository.save(member)

        val requestBody = """
            {
                "password": "wrongPassword",
                "passwordConfirm": "wrongPassword"
            }
        """.trimIndent()

        mockMvc.perform(
            delete("/api/users/me/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
            .andDo(print())
    }
}