package com.example.admin

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.entity.Review
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.review.service.ReviewService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
// ⚠️ 멀티 스레드 동시성 테스트에서는 @Transactional을 제외해야 정확한 조회가 가능합니다.
class AdminReviewConcurrencyTest {

    // 코틀린 환경에 맞는 안전한 로거 선언
    private val log = LoggerFactory.getLogger(this::class.java)

    @Autowired private lateinit var reviewService: ReviewService
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
    @DisplayName("관리자 2명이 동시에 한 유저의 '서로 다른 2개의 리뷰'를 BLIND 처리 시, 작성자의 신고횟수 갱신 손실발생 확인")
    fun adminBlindConcurrencyTest() {
        // given
        val author = Member("baduser", "1234", "악플러", "bad@test.com", "악성유저", 0)
        memberRepository.save(author)

        val festival = Festival(
            "F_001", "동시성축제", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.save(festival)

        val review1 = Review(author, festival, "첫 번째 악성 리뷰", null, 1)
        val review2 = Review(author, festival, "두 번째 악성 리뷰", null, 1)
        reviewRepository.saveAll(listOf(review1, review2))

        val threadCount = 2
        val executorService = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(threadCount)

        // when
        executorService.submit {
            try {
                startLatch.await() // 시작 신호 대기
                reviewService.processReviewAction(review1.id, "BLIND")
            } catch (e: Exception) {
                log.error("스레드 1 에러", e)
            } finally {
                endLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await() // 시작 신호 대기
                reviewService.processReviewAction(review2.id, "BLIND")
            } catch (e: Exception) {
                log.error("스레드 2 에러", e)
            } finally {
                endLatch.countDown()
            }
        }

        startLatch.countDown() // 스레드 동시 출발
        endLatch.await()       // 두 스레드의 작업이 모두 끝날 때까지 메인 스레드 대기

        // then
        val findMember = memberRepository.findById(author.id).orElseThrow()

        println("=========================================")
        println("최종 신고 횟수(reportCount): ${findMember.reportCount}")
        println("=========================================")

        assertThat(findMember.reportCount).isEqualTo(2)
    }

    @Test
    @DisplayName("한 리뷰에 대해 한 명은 BLIND, 한 명은 DISMISS를 동시에 요청할 때 방어 로직 검증")
    fun blindAndDismissConcurrencyTest() {
        // given
        val author = Member("baduser2", "1234", "악플러2", "bad2@test.com", "악성유저2", 0)
        memberRepository.save(author)

        val festival = Festival(
            "F_002", "동시성축제2", "설명", "주소",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0
        )
        festivalRepository.save(festival)

        val targetReview = Review(author, festival, "어그로 끄는 리뷰 내용", null, 1)

        // 코틀린의 repeat 함수를 사용하여 5번 반복
        repeat(5) {
            targetReview.increaseReportCount()
        }
        reviewRepository.save(targetReview)

        val targetReviewId = targetReview.id

        val threadCount = 2
        val executorService = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(threadCount)

        val successCount = AtomicInteger(0)
        val exceptionCount = AtomicInteger(0)

        // when
        executorService.submit {
            try {
                startLatch.await()
                reviewService.processReviewAction(targetReviewId, "BLIND")
                successCount.incrementAndGet()
            } catch (e: Exception) {
                exceptionCount.incrementAndGet()
            } finally {
                endLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                reviewService.processReviewAction(targetReviewId, "DISMISS")
                successCount.incrementAndGet()
            } catch (e: Exception) {
                exceptionCount.incrementAndGet()
            } finally {
                endLatch.countDown()
            }
        }

        startLatch.countDown()
        endLatch.await()

        // then
        assertThat(successCount.get()).isEqualTo(1)
        println(successCount.get())
        println(exceptionCount.get())
        assertThat(exceptionCount.get()).isEqualTo(1)
    }
}