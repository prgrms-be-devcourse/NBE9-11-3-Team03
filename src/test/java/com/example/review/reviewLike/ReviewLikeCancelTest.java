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
import com.example.domain.reviewlike.entity.ReviewLike;
import com.example.domain.reviewlike.repository.ReviewLikeRepository;
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
public class ReviewLikeCancelTest {
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
    private final int THREAD_COUNT = 100;
    private List<Member> members = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1. 100명의 유저 생성
        for (int i = 0; i < THREAD_COUNT; i++) {
            Member member = new Member("유저" + i, "1234", "user" + i, "user" + i + "@test.com", "닉네임" + i, Role.USER);
            members.add(memberRepository.save(member));
        }

        // 2. 축제 생성 및 DB 저장
        savedFestival = festivalRepository.save(
                new Festival(
                        "FEST-CANCEL",
                        "축제",
                        "테스트",
                        "주소",
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(10),
                        126.9,
                        37.5,
                        null, null, null, null, null,
                        FestivalStatus.ONGOING
                )
        );
        // 3. 리뷰를 먼저 DB에 저장하여 ID를 부여받습니다.
        Review initialReview = new Review(members.get(0), savedFestival, "내용", "이미지", 5);
        savedReview = reviewRepository.save(initialReview);

        // 4. 이미 DB에 저장된 savedReview를 참조하여 100개의 좋아요를 저장합니다.
        for (Member member : members) {
            reviewLikeRepository.save(new ReviewLike(member, savedReview));
            savedReview.increaseLikeCount(); // 메모리 객체 카운트 +1
        }

        // 5. 카운트가 100으로 올라간 상태를 다시 DB에 반영해줍니다.
        savedReview = reviewRepository.save(savedReview);
    }

    @AfterEach
    void tearDown() {
        reviewLikeRepository.deleteAllInBatch();
        reviewRepository.deleteAllInBatch();
        festivalRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("리뷰 좋아요 취소 동시성 테스트 - 좋아요가 100인 상태에서 100명이 동시에 취소하면 0이 되어야 한다.")
    void cancelLikeReview_Concurrency() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    String loginId = members.get(index).getLoginId();
                    reviewLikeService.cancelLikeReview(savedReview.getId(), loginId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then: DB에서 최신 리뷰를 다시 조회하여 좋아요 수가 0인지 확인
        Review updatedReview = reviewRepository.findById(savedReview.getId()).orElseThrow();
        System.out.println("취소 후 실제 좋아요 수: " + updatedReview.getLikeCount());
        Assertions.assertThat(updatedReview.getLikeCount()).isEqualTo(0);
    }
}
