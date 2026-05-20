package com.example.review

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.repository.ReviewRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    private lateinit var savedMember: Member
    private lateinit var savedFestival: Festival
    private lateinit var savedReview: Review
    private lateinit var otherMember: Member

    @BeforeEach
    fun setUp() {
        savedMember = memberRepository.save(
            Member.create(
                "유저2",
                "1234",
                "user2",
                "user2@test.com",
                "닉네임2",
                Role.USER
            )
        )

        otherMember = memberRepository.save(
            Member.create(
                "유저3",
                "1234",
                "user3",
                "user3@test.com",
                "닉네임3",
                Role.USER
            )
        )

        savedFestival = festivalRepository.save(
            Festival(
                "FEST-001",
                "리뷰 테스트 축제",
                "리뷰 테스트용 축제",
                "서울 테스트구",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10),
                126.9780,
                37.5665,
                null,
                null,
                null,
                null,
                null,
                FestivalStatus.ONGOING
            )
        )

        savedReview = reviewRepository.save(
            Review(
                savedMember,
                savedFestival,
                "삭제 전 리뷰",
                "old_review_image.jpg",
                5
            )
        )
    }

    @Test
    @DisplayName("리뷰 작성 성공 - 로그인한 사용자가 축제 리뷰를 작성한다.")
    fun createReview_success() {
        val newFestival = festivalRepository.save(
            Festival(
                "FEST-002",
                "리뷰 작성 성공 축제",
                "리뷰 작성 성공 테스트용 축제",
                "서울 테스트동",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                127.0000,
                37.5000,
                null,
                null,
                null,
                null,
                null,
                FestivalStatus.ONGOING
            )
        )

        val requestBody = """
            {
              "content": "리뷰 작성 테스트 내용",
              "rating": 4
            }
        """.trimIndent()

        val requestDtoPart = MockMultipartFile(
            "requestDto",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestBody.toByteArray(StandardCharsets.UTF_8)
        )

        val imagePart = MockMultipartFile(
            "image",
            "create.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0xFF.toByte(),
                0xE0.toByte(),
                0x00,
                0x10,
                0x4A,
                0x46,
                0x49,
                0x46,
                0x00,
                0x01,
                0xFF.toByte(),
                0xD9.toByte()
            )
        )

        mockMvc.perform(
            multipart("/api/festivals/{festivalId}/reviews", newFestival.id)
                .file(requestDtoPart)
                .file(imagePart)
                .with(user("user2").roles("USER"))
                .with(csrf())
        )
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.message").value("리뷰 작성이 완료 되었습니다."))
            .andExpect(jsonPath("$.data.festivalId").value(newFestival.id))
            .andExpect(jsonPath("$.data.memberId").value(savedMember.id))
            .andExpect(jsonPath("$.data.content").value("리뷰 작성 테스트 내용"))
            .andExpect(jsonPath("$.data.rating").value(4))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.image").isNotEmpty())
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - 특정 축제의 리뷰 목록을 조회한다.")
    fun getReviewList_success() {
        reviewRepository.save(
            Review(
                savedMember,
                savedFestival,
                "두번째 리뷰",
                "https://example.com/review2.jpg",
                4
            )
        )

        mockMvc.perform(
            get("/api/festivals/{festivalId}/reviews", savedFestival.id)
                .with(user("user2").roles("USER"))
                .param("page", "0")
                .param("size", "10")
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("축제 리뷰 목록 조회 성공"))
            .andExpect(jsonPath("$.data.festivalId").value(savedFestival.id))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10))
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.content.length()").value(2))
    }

    @Test
    @DisplayName("리뷰 수정 성공 - 본인 리뷰를 수정한다.")
    fun updateReview_success() {
        val requestBody = """
            {
              "content": "수정된 리뷰 내용입니다.",
              "rating": 3
            }
        """.trimIndent()

        val requestDtoPart = MockMultipartFile(
            "requestDto",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestBody.toByteArray(StandardCharsets.UTF_8)
        )

        val imagePart = MockMultipartFile(
            "image",
            "updated.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0xFF.toByte(),
                0xE0.toByte(),
                0x00,
                0x10,
                0x4A,
                0x46,
                0x49,
                0x46,
                0x00,
                0x01,
                0xFF.toByte(),
                0xD9.toByte()
            )
        )

        mockMvc.perform(
            multipart("/api/reviews/{reviewId}", savedReview.id)
                .file(requestDtoPart)
                .file(imagePart)
                .with(user("user2").roles("USER"))
                .with(csrf())
                .with { request ->
                    request.setMethod(HttpMethod.PATCH.name())
                    request
                }
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("리뷰 수정 완료"))
            .andExpect(jsonPath("$.data.content").value("수정된 리뷰 내용입니다."))
            .andExpect(jsonPath("$.data.rating").value(3))
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 본인 리뷰를 논리 삭제한다.")
    fun deleteReview_success() {
        mockMvc.perform(
            delete("/api/reviews/{reviewId}", savedReview.id)
                .with(user("user2").roles("USER"))
                .with(csrf())
        )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("리뷰 삭제가 완료되었습니다."))
            .andExpect(jsonPath("$.data.reviewId").value(savedReview.id))
            .andExpect(jsonPath("$.data.status").value("DELETED"))
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 작성자가 아닌 회원은 수정할 수 없다.")
    fun updateReview_fail_notAuthor() {
        val requestBody = """
            {
              "content": "남의 리뷰 수정 시도",
              "rating": 3,
              "deleteImage": false
            }
        """.trimIndent()

        val requestDtoPart = MockMultipartFile(
            "requestDto",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestBody.toByteArray(StandardCharsets.UTF_8)
        )

        mockMvc.perform(
            multipart("/api/reviews/{reviewId}", savedReview.id)
                .file(requestDtoPart)
                .with(user("user3").roles("USER"))
                .with(csrf())
                .with { request ->
                    request.setMethod(HttpMethod.PATCH.name())
                    request
                }
        )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("본인이 작성한 리뷰만 수정할 수 있습니다."))
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 작성자가 아닌 회원은 삭제할 수 없다.")
    fun deleteReview_fail_notAuthor() {
        mockMvc.perform(
            delete("/api/reviews/{reviewId}", savedReview.id)
                .with(user("user3").roles("USER"))
                .with(csrf())
        )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("본인이 작성한 리뷰만 삭제할 수 있습니다."))
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 같은 회원은 같은 축제에 중복 리뷰를 작성할 수 없다.")
    fun createReview_fail_duplicateReview() {
        val requestBody = """
            {
              "content": "중복 리뷰 작성 시도",
              "rating": 4
            }
        """.trimIndent()

        val requestDtoPart = MockMultipartFile(
            "requestDto",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestBody.toByteArray(StandardCharsets.UTF_8)
        )

        mockMvc.perform(
            multipart("/api/festivals/{festivalId}/reviews", savedFestival.id)
                .file(requestDtoPart)
                .with(user("user2").roles("USER"))
                .with(csrf())
        )
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("이미 해당 축제에 리뷰를 작성했습니다."))
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 비로그인 사용자는 리뷰를 작성할 수 없다.")
    fun createReview_fail_unauthorized() {
        val newFestival = festivalRepository.save(
            Festival(
                "FEST-003",
                "비로그인 테스트 축제",
                "비로그인 리뷰 작성 테스트용 축제",
                "서울 테스트구",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
                127.1000,
                37.6000,
                null,
                null,
                null,
                null,
                null,
                FestivalStatus.ONGOING
            )
        )

        val requestBody = """
            {
              "content": "비로그인 리뷰 작성 시도",
              "rating": 4
            }
        """.trimIndent()

        val requestDtoPart = MockMultipartFile(
            "requestDto",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            requestBody.toByteArray(StandardCharsets.UTF_8)
        )

        mockMvc.perform(
            multipart("/api/festivals/{festivalId}/reviews", newFestival.id)
                .file(requestDtoPart)
                .with(csrf())
        )
            .andDo(print())
            .andExpect(status().isUnauthorized())
    }
}