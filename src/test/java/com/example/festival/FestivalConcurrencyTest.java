package com.example.festival;

import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.festival.service.FestivalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class FestivalConcurrencyTest {
    @Autowired
    private FestivalService festivalService;

    @Autowired
    private FestivalRepository festivalRepository;

    private Festival savedFestival;

    @BeforeEach
    void setUp() {
        // 테스트 전: 조회수가 0인 축제 생성
        Festival festival = Festival.builder()
                .contentId("FEST-CONCURRENCY")
                .title("동시성 테스트 축제")
                .overview("동시성 테스트용 축제입니다.")
                .mapX(126.9780)
                .mapY(37.5665)
                .address("서울 테스트구")
                .status(FestivalStatus.ONGOING)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(10))
                .viewCount(0) // 초기 조회수 0
                .bookMarkCount(0)
                .averageRate(0.0)
                .build();

        savedFestival = festivalRepository.saveAndFlush(festival);
    }

    @AfterEach
    void tearDown() {
        // 멀티 스레드 테스트 롤백X -> 수동으로 DB delete
        festivalRepository.deleteAll();
    }

    @Test
    @DisplayName("조회수 동시성 문제 - 100명이 동시에 조회하면 조회수가 100이 되지 않는다 (Lost Update 발생)")
    void viewCount_concurrency_issue_test() throws InterruptedException {
        // given
        int threadCount = 100;
        // 1. 32개 스레드 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // 2. 100개의 요청 끝날떄 까지 대기
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 100번의 상세 조회 요청 호출
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    festivalService.getFestival(savedFestival.getId());
                } finally {
                    latch.countDown(); // 작업 하나 종료마다 latch -1
                }
            });
        }

        latch.await(); // 100개의 작업이 다 끝날 때까지 메인 스레드 대기

        // then: DB에 최종 반영된 조회수 확인
        Festival updatedFestival = festivalRepository.findById(savedFestival.getId()).orElseThrow();
        int finalViewCount = updatedFestival.getViewCount();

        System.out.println("====== 테스트 결과 ======");
        System.out.println("요청 횟수: " + threadCount);
        System.out.println("실제 반영된 조회수: " + finalViewCount);
        System.out.println("누락된 조회수: " + (threadCount - finalViewCount));
        System.out.println("=========================");

        // 동시성 문제 해결로 100과 일치해야함
        assertThat(finalViewCount).isEqualTo(threadCount);
    }
}
