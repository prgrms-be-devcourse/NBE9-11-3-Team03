package com.example.review.reviewReport

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.reviewreport.repository.ReviewReportRepository
import com.example.domain.reviewreport.service.ReviewReportService
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
class ReviewReportTest {

    @Autowired
    private lateinit var reviewReportService: ReviewReportService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @Autowired
    private lateinit var reviewRepository: ReviewRepository

    @Autowired
    private lateinit var reviewReportRepository: ReviewReportRepository

    private lateinit var savedFestival: Festival
    private lateinit var savedReview: Review
    private lateinit var writer: Member

    private val reporters = mutableListOf<Member>()

    @BeforeEach
    fun setUp() {
        // 1. 리뷰 작성자를 먼저 따로 생성합니다.
        writer = memberRepository.save(
            Member.create(
                "리뷰작성자",
                "1234",
                "writer",
                "writer@test.com",
                "작성자닉네임",
                Role.USER
            )
        )

        // 2. 100명의 서로 다른 신고자를 생성합니다.
        repeat(THREAD_COUNT) { index ->
            val member = Member.create(
                "신고자$index",
                "1234",
                "reporter$index",
                "reporter$index@test.com",
                "닉네임$index",
                Role.USER
            )

            reporters.add(memberRepository.save(member))
        }

        // 3. 축제 생성
        savedFestival = festivalRepository.save(
            Festival(
                "FEST-REPORT-CONCURRENCY",
                "신고 동시성 축제",
                "신고 동시성 테스트용 축제",
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

        // 4. 리뷰 생성
        savedReview = reviewRepository.save(
            Review(
                writer,
                savedFestival,
                "어그로성 불쾌한 리뷰 내용입니다.",
                "https://example.com/bad-review.jpg",
                1
            )
        )
    }

    @AfterEach
    fun tearDown() {
        reviewReportRepository.deleteAllInBatch()
        reviewRepository.deleteAllInBatch()
        festivalRepository.deleteAllInBatch()
        memberRepository.deleteAllInBatch()
        reporters.clear()
    }

    @Test
    @DisplayName("리뷰 신고 동시성 테스트 - 100명이 동시에 신고하면 reportCount가 100이 되어야 한다.")
    fun reportReview_Concurrency() {
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(THREAD_COUNT)

        repeat(THREAD_COUNT) { index ->
            executorService.submit {
                try {
                    val loginId = reporters[index].loginId
                    reviewReportService.reportReview(savedReview.id, loginId)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executorService.shutdown()

        val updatedReview = reviewRepository.findById(savedReview.id).orElseThrow()

        println("기대하는 누적 신고 수: $THREAD_COUNT")
        println("실제 DB에 저장된 누적 신고 수: ${updatedReview.reportCount}")

        assertThat(updatedReview.reportCount).isEqualTo(THREAD_COUNT)
    }

    companion object {
        private const val THREAD_COUNT = 100
    }
}