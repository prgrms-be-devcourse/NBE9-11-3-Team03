package com.example.review.reviewLike

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.review.service.ReviewLikeService
import com.example.domain.reviewlike.entity.ReviewLike
import com.example.domain.reviewlike.repository.ReviewLikeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
@ActiveProfiles("test")
class ReviewLikeCancelTest {

    @Autowired
    private lateinit var reviewLikeService: ReviewLikeService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var reviewLikeRepository: ReviewLikeRepository

    private lateinit var savedFestival: Festival
    private lateinit var savedReview: Review
    private val members = mutableListOf<Member>()

    @BeforeEach
    fun setUp() {
        repeat(THREAD_COUNT) { index ->
            val member = Member.create(
                "유저$index",
                "1234",
                "user$index",
                "user$index@test.com",
                "닉네임$index",
                Role.USER
            )

            members.add(memberRepository.save(member))
        }

        savedFestival = festivalRepository.save(
            Festival(
                "FEST-CANCEL",
                "축제",
                "테스트",
                "주소",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10),
                126.9,
                37.5,
                null,
                null,
                null,
                null,
                null,
                FestivalStatus.ONGOING
            )
        )

        val initialReview = Review(
            members[0],
            savedFestival,
            "내용",
            "이미지",
            5
        )

        savedReview = reviewRepository.save(initialReview)

        members.forEach { member ->
            reviewLikeRepository.save(ReviewLike(member, savedReview))
            savedReview.increaseLikeCount()
        }

        savedReview = reviewRepository.save(savedReview)
    }

    @AfterEach
    fun tearDown() {
        reviewLikeRepository.deleteAllInBatch()
        reviewRepository.deleteAllInBatch()
        festivalRepository.deleteAllInBatch()
        memberRepository.deleteAllInBatch()
        members.clear()
    }

    @Test
    @DisplayName("리뷰 좋아요 취소 동시성 테스트 - 좋아요가 100인 상태에서 100명이 동시에 취소하면 0이 되어야 한다.")
    fun cancelLikeReview_Concurrency() {
        val executorService = Executors.newFixedThreadPool(32)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(THREAD_COUNT)

        repeat(THREAD_COUNT) { index ->
            executorService.submit {
                try {
                    startLatch.await()

                    val loginId = members[index].loginId
                    reviewLikeService.cancelLikeReview(savedReview.id, loginId)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executorService.shutdown()

        val updatedReview = reviewRepository.findById(savedReview.id).orElseThrow()

        println("취소 후 실제 좋아요 수: ${updatedReview.likeCount}")

        assertThat(updatedReview.likeCount).isEqualTo(0)
    }

    companion object {
        private const val THREAD_COUNT = 100
    }
}
