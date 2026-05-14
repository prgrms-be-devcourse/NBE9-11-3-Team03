package com.example.admin;

import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
// ⚠️ 멀티 스레드 동시성 테스트에서는 @Transactional을 제외해야 정확한 조회가 가능합니다.
public class AdminReviewConcurrencyTest {

    @Autowired private ReviewService reviewService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private FestivalRepository festivalRepository;

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAllInBatch();
        festivalRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("관리자 2명이 동시에 한 유저의 '서로 다른 2개의 리뷰'를 BLIND 처리 시, 작성자의 신고횟수 갱신 손실발생 확인")
    void adminBlindConcurrencyTest() throws InterruptedException {
        // given
        // 1. 악플러 회원 1명 생성 (초기 신고 횟수 0)
        Member author = new Member("baduser", "1234", "악플러", "bad@test.com", "악성유저", 0);
        memberRepository.save(author);

        // 2. 축제 생성
        Festival festival = new Festival("F_001", "동시성축제", "설명", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0);
        festivalRepository.save(festival);

        // 3. 해당 회원이 작성한 악성 리뷰 2개 생성
        Review review1 = new Review(author, festival, "첫 번째 악성 리뷰", null, 1);
        Review review2 = new Review(author, festival, "두 번째 악성 리뷰", null, 1);
        reviewRepository.save(review1);
        reviewRepository.save(review2);

        // 4. 스레드 환경 세팅 (스레드 2개)
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // when
        // 스레드 1: 관리자 A가 첫 번째 리뷰를 블라인드 처리
        executorService.submit(() -> {
            try {
                startLatch.await(); // 시작 신호 대기
                reviewService.processReviewAction(review1.getId(), "BLIND");
            } catch (Exception e) {
                log.error("스레드 1 에러", e);
            } finally {
                endLatch.countDown();
            }
        });

        // 스레드 2: 관리자 B가 두 번째 리뷰를 블라인드 처리
        executorService.submit(() -> {
            try {
                startLatch.await(); // 시작 신호 대기
                reviewService.processReviewAction(review2.getId(), "BLIND");
            } catch (Exception e) {
                log.error("스레드 2 에러", e);
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown(); // 스래드 동시출발
        endLatch.await();       // 두 스레드의 작업이 모두 끝날 때까지 메인 스레드 대기

        // then
        // DB에서 최신 회원 정보 조회
        Member findMember = memberRepository.findById(author.getId()).orElseThrow();

        System.out.println("=========================================");
        System.out.println("최종 신고 횟수(reportCount): " + findMember.getReportCount());
        System.out.println("=========================================");

        assertThat(findMember.getReportCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("한 리뷰에 대해 한 명은 BLIND, 한 명은 DISMISS를 동시에 요청할 때 방어 로직 검증")
    void blindAndDismissConcurrencyTest() throws InterruptedException {
        // given
        Member author = new Member("baduser2", "1234", "악플러2", "bad2@test.com", "악성유저2", 0);
        memberRepository.save(author);

        Festival festival = new Festival("F_002", "동시성축제2", "설명", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0);
        festivalRepository.save(festival);

        Review targetReview = new Review(author, festival, "어그로 끄는 리뷰 내용", null, 1);
        for (int i = 0; i < 5; i++) {
            targetReview.increaseReportCount();
        }
        reviewRepository.save(targetReview);
        Long targetReviewId = targetReview.getId();

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // when
        executorService.submit(() -> {
            try {
                startLatch.await();
                reviewService.processReviewAction(targetReviewId, "BLIND");
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                startLatch.await();
                reviewService.processReviewAction(targetReviewId, "DISMISS");
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(exceptionCount.get()).isEqualTo(1);
    }
}