package com.example.festival.festivalBookMark;

import com.example.domain.bookmark.entity.FestivalBookmark;
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
public class FestivalBookMarkCancelTest {
    @Autowired
    private FestivalBookmarkService festivalBookmarkService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private FestivalRepository festivalRepository;
    @Autowired
    private FestivalBookmarkRepository festivalBookmarkRepository;

    private Festival savedFestival;
    private final int THREAD_COUNT = 100;
    private List<Member> members = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 1. 100명의 유저 생성
        for (int i = 0; i < THREAD_COUNT; i++) {
            Member member = new Member("찜유저" + i, "1234", "bmUser" + i, "bm" + i + "@test.com", "닉네임" + i, Role.USER);
            members.add(memberRepository.save(member));
        }

        Festival initialFestival = Festival.builder()
                .contentId("FEST-BM-CANCEL").overview("테스트").mapX(126.9).mapY(37.5).title("축제").address("주소")
                .status(FestivalStatus.ONGOING).startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(10)).build();

        savedFestival = festivalRepository.save(initialFestival);

        // 3. 이미 DB에 저장된 savedFestival을 참조하여 100개의 찜을 정상적으로 저장합니다.
        for (Member member : members) {
            festivalBookmarkRepository.save(new FestivalBookmark(member, savedFestival));
            savedFestival.increaseBookmarkCount(); // 메모리 객체의 찜 카운트를 1씩 올림 (최종 100)
        }

        // 4. 찜 카운트가 100이 된 상태를 다시 DB에 반영(UPDATE) 해줍니다.
        savedFestival = festivalRepository.save(savedFestival);
    }

    @AfterEach
    void tearDown() {
        festivalBookmarkRepository.deleteAllInBatch();
        festivalRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("축제 찜 취소 동시성 테스트 - 찜 수가 100인 상태에서 100명이 동시에 취소하면 0이 되어야 한다.")
    void cancelBookmark_Concurrency() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    String loginId = members.get(index).getLoginId();
                    festivalBookmarkService.cancelBookmark(savedFestival.getId(), loginId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then: DB에서 최신 축제를 다시 조회하여 찜 수가 0인지 확인
        Festival updatedFestival = festivalRepository.findById(savedFestival.getId()).orElseThrow();
        System.out.println("취소 후 실제 찜 수: " + updatedFestival.getBookMarkCount());
        Assertions.assertThat(updatedFestival.getBookMarkCount()).isEqualTo(0);
    }
}
