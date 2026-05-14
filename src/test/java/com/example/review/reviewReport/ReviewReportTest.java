package com.example.review.reviewReport;

import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.Role;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.reviewreport.repository.ReviewReportRepository;
import com.example.domain.reviewreport.service.ReviewReportService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@ActiveProfiles("test")
public class ReviewReportTest {

    @Autowired
    private ReviewReportService reviewReportService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    private Festival savedFestival;
    private Review savedReview;
    private Member writer;

    private final int THREAD_COUNT = 100; // 100명이 동시에 신고 요청
    private List<Member> reporters = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1. 리뷰 작성자를 먼저 따로 생성합니다.
        //    본인 리뷰 신고 방지 로직이 추가되었기 때문에,
        //    신고자 목록과 리뷰 작성자를 분리해야 동시성 테스트가 정확해집니다.
        writer = memberRepository.save(
                new Member(
                        "리뷰작성자",
                        "1234",
                        "writer",
                        "writer@test.com",
                        "작성자닉네임",
                        Role.USER
                )
        );

        // 2. 100명의 서로 다른 신고자를 생성합니다.
        //    중복 신고 방지 로직을 우회하기 위해 모든 신고자는 서로 다른 계정이어야 합니다.
        for (int i = 0; i < THREAD_COUNT; i++) {
            Member member = new Member(
                    "신고자" + i,
                    "1234",
                    "reporter" + i,
                    "reporter" + i + "@test.com",
                    "닉네임" + i,
                    Role.USER
            );
            reporters.add(memberRepository.save(member));
        }

        // 3. 축제 생성
        savedFestival = festivalRepository.save(
                Festival.builder()
                        .contentId("FEST-REPORT-CONCURRENCY")
                        .overview("신고 동시성 테스트용 축제")
                        .mapX(126.9780)
                        .mapY(37.5665)
                        .title("신고 동시성 축제")
                        .address("서울 테스트구")
                        .status(FestivalStatus.ONGOING)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(10))
                        .viewCount(0)
                        .bookMarkCount(0)
                        .averageRate(0.0)
                        .build()
        );

        // 4. 리뷰 생성
        //    리뷰는 신고자 중 한 명이 아니라, 별도로 만든 writer가 작성한 것으로 설정합니다.
        savedReview = reviewRepository.save(
                new Review(
                        writer,
                        savedFestival,
                        "어그로성 불쾌한 리뷰 내용입니다.",
                        "https://example.com/bad-review.jpg",
                        1
                )
        );
    }

    @AfterEach
    void tearDown() {
        // @Transactional이 없으므로 다음 테스트에 영향을 주지 않도록 수동으로 데이터를 삭제합니다.
        reviewReportRepository.deleteAllInBatch();
        reviewRepository.deleteAllInBatch();
        festivalRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("리뷰 신고 동시성 테스트 - 100명이 동시에 신고하면 reportCount가 100이 되어야 한다.")
    void reportReview_Concurrency() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    // 각기 다른 아이디로 리뷰 신고 요청
                    String loginId = reporters.get(index).getLoginId();
                    reviewReportService.reportReview(savedReview.getId(), loginId);
                } finally {
                    latch.countDown(); // 스레드 작업 완료 시 카운트 감소
                }
            });
        }

        latch.await(); // 100개의 요청이 모두 처리될 때까지 메인 스레드 대기
        executorService.shutdown();

        // then
        // 최신 리뷰 정보를 DB에서 다시 조회
        Review updatedReview = reviewRepository.findById(savedReview.getId()).orElseThrow();

        System.out.println("기대하는 누적 신고 수: " + THREAD_COUNT);
        System.out.println("실제 DB에 저장된 누적 신고 수: " + updatedReview.getReportCount());

        // 동시성 제어가 성공적으로 이루어졌다면 100과 일치해야 함
        Assertions.assertThat(updatedReview.getReportCount()).isEqualTo(THREAD_COUNT);
    }
}