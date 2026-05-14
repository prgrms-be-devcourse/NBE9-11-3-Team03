package com.example.review.reviewLike;


import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.Role;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.review.service.ReviewLikeService;
import com.example.domain.reviewlike.repository.ReviewLikeRepository;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ReviewLikeTest {
    @Autowired
    private ReviewLikeService reviewLikeService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    private Festival savedFestival;
    private Review savedReview;
    private final int THREAD_COUNT = 100; // 100명이 동시에 좋아요 요청
    private List<Member> members = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1. 100명의 서로 다른 유저 생성 (한 유저는 한 리뷰에 1번만 좋아요 가능하므로)
        for (int i = 0; i < THREAD_COUNT; i++) {
            Member member = new Member(
                    "유저" + i,
                    "1234",
                    "user" + i,
                    "user" + i + "@test.com",
                    "닉네임" + i,
                    Role.USER
            );
            members.add(memberRepository.save(member));
        }

        // 2. 축제 생성
        savedFestival = festivalRepository.save(
                Festival.builder()
                        .contentId("FEST-CONCURRENCY")
                        .overview("동시성 테스트용 축제")
                        .mapX(126.9780)
                        .mapY(37.5665)
                        .title("동시성 축제")
                        .address("서울 테스트구")
                        .status(FestivalStatus.ONGOING)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(10))
                        .viewCount(0)
                        .bookMarkCount(0)
                        .averageRate(0.0)
                        .build()
        );

        // 3. 리뷰 생성 (작성자는 첫 번째 유저로 지정)
        savedReview = reviewRepository.save(
                new Review(
                        members.get(0),
                        savedFestival,
                        "동시성 테스트 리뷰",
                        "https://example.com/review.jpg",
                        5
                )
        );
    }

    @AfterEach
    void tearDown() {
        // @Transactional이 없으므로 테스트 종료 후 데이터를 직접 지워주어 다른 테스트에 영향을 주지 않게 합니다.
        reviewLikeRepository.deleteAllInBatch();
        reviewRepository.deleteAllInBatch();
        festivalRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("리뷰 좋아요 동시성 테스트 - 100명이 동시에 좋아요를 누르면 likeCount가 100이 되어야 한다.")
    void likeReview_Concurrency() throws InterruptedException {
        // given
        // 고정된 크기(32개)의 스레드 풀 생성 (실제 서버가 요청을 처리하는 환경과 유사하게 구성)
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 100개의 작업이 모두 끝날 때까지 기다리기 위한 장치
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i; // 람다식 내부에서 사용하기 위해 final 변수로 할당
            executorService.submit(() -> {
                try {
                    // 각기 다른 로그인 아이디로 좋아요 요청
                    String loginId = members.get(index).getLoginId();
                    reviewLikeService.likeReview(savedReview.getId(), loginId);
                } finally {
                    // 성공하든 예외가 발생하든 CountDownLatch를 1 감소시킴
                    latch.countDown();
                }
            });
        }
        // 모든 스레드가 작업을 완료할 때까지 메인 스레드 대기 (latch가 0이 될 때까지)
        latch.await();
        // then
        // DB에서 최신 리뷰 정보를 다시 조회하여 좋아요 수 확인
        Review updatedReview = reviewRepository.findById(savedReview.getId()).orElseThrow();

        System.out.println("기대하는 좋아요 수: " + THREAD_COUNT);
        System.out.println("실제 DB에 저장된 좋아요 수: " + updatedReview.getLikeCount());

        // 기존 엔티티 메서드(++하는 방식)였다면 이 테스트가 실패하고 100보다 작은 숫자가 나옵니다.
        // DB 업데이트 쿼리를 작성하셨다면 테스트가 성공(100)할 것입니다!
        assertThat(updatedReview.getLikeCount()).isEqualTo(THREAD_COUNT);
    }
}
