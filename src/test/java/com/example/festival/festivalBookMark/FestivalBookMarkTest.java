package com.example.festival.festivalBookMark;

import com.example.domain.bookmark.repository.FestivalBookmarkRepository;
import com.example.domain.bookmark.service.FestivalBookmarkService;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.Role;
import com.example.domain.member.repository.MemberRepository;
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
public class FestivalBookMarkTest {
    @Autowired
    private FestivalBookmarkService festivalBookmarkService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private FestivalBookmarkRepository festivalBookmarkRepository;

    private Festival savedFestival;
    private final int THREAD_COUNT = 100; // 100명이 동시에 찜 요청
    private List<Member> members = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1. 100명의 서로 다른 유저 생성 (동일 유저의 중복 찜 예외를 방지)
        for (int i = 0; i < THREAD_COUNT; i++) {
            Member member = new Member(
                    "찜유저" + i,
                    "1234",
                    "bookmarkUser" + i,
                    "bm" + i + "@test.com",
                    "닉네임" + i,
                    Role.USER
            );
            members.add(memberRepository.save(member));
        }

        // 2. 찜 대상이 될 축제 하나 생성
        savedFestival = festivalRepository.save(
                Festival.builder()
                        .contentId("FEST-BM-CONCURRENCY")
                        .overview("찜 동시성 테스트용 축제")
                        .mapX(126.9780)
                        .mapY(37.5665)
                        .title("찜 동시성 축제")
                        .address("서울 테스트구")
                        .status(FestivalStatus.ONGOING)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(10))
                        .viewCount(0)
                        .bookMarkCount(0)
                        .averageRate(0.0)
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        // @Transactional이 없으므로 수동으로 데이터를 비워줍니다.
        // 순서 주의: 자식 엔티티인 Bookmark를 먼저 지우고 Festival과 Member를 지워야 외래키 제약조건 위배를 막습니다.
        festivalBookmarkRepository.deleteAllInBatch();
        festivalRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("축제 찜 동시성 테스트 - 100명이 동시에 찜을 누르면 bookMarkCount가 100이 되어야 한다.")
    void bookmarkFestival_Concurrency() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    // 각 스레드가 서로 다른 사용자의 아이디로 찜 요청을 보냄
                    String loginId = members.get(index).getLoginId();
                    festivalBookmarkService.bookmarkFestival(savedFestival.getId(), loginId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 100개의 스레드가 모두 완료될 때까지 대기

        // then
        // 최종적으로 업데이트된 축제 정보를 DB에서 조회
        Festival updatedFestival = festivalRepository.findById(savedFestival.getId()).orElseThrow();

        System.out.println("기대하는 찜 수: " + THREAD_COUNT);
        System.out.println("실제 DB에 저장된 찜 수: " + updatedFestival.getBookMarkCount());

        Assertions.assertThat(updatedFestival.getBookMarkCount()).isEqualTo(THREAD_COUNT);
    }
}
