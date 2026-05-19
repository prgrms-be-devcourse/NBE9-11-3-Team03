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
class ReviewLikeTest {

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
        // 1. 100명의 서로 다른 유저 생성
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

        // 2. 축제 생성
        savedFestival = festivalRepository.save(
            Festival(
                "FEST-CONCURRENCY",
                "동시성 축제",
                "동시성 테스트용 축제",
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

        // 3. 리뷰 생성
        savedReview = reviewRepository.save(
            Review(
                members[0],
                savedFestival,
                "동시성 테스트 리뷰",
                "https://example.com/review.jpg",
                5
            )
        )
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
    @DisplayName("리뷰 좋아요 동시성 테스트 - 100명이 동시에 좋아요를 누르면 likeCount가 100이 되어야 한다.")
    fun likeReview_Concurrency() {
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(THREAD_COUNT)

        repeat(THREAD_COUNT) { index ->
            executorService.submit {
                try {
                    val loginId = members[index].loginId
                    reviewLikeService.likeReview(savedReview.id, loginId)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executorService.shutdown()

        val updatedReview = reviewRepository.findById(savedReview.id).orElseThrow()

        println("기대하는 좋아요 수: $THREAD_COUNT")
        println("실제 DB에 저장된 좋아요 수: ${updatedReview.likeCount}")

        assertThat(updatedReview.likeCount).isEqualTo(THREAD_COUNT)
    }

    companion object {
        private const val THREAD_COUNT = 100
    }
}