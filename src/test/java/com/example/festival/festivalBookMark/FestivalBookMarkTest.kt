package com.example.festival.festivalBookMark

import com.example.domain.bookmark.repository.FestivalBookmarkRepository
import com.example.domain.bookmark.service.FestivalBookmarkService
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
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
import kotlin.jvm.optionals.getOrNull

@SpringBootTest
@ActiveProfiles("test")
class FestivalBookMarkTest {

    @Autowired
    private lateinit var festivalBookmarkService: FestivalBookmarkService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @Autowired
    private lateinit var festivalBookmarkRepository: FestivalBookmarkRepository

    private lateinit var savedFestival: Festival
    private val members = mutableListOf<Member>()

    companion object {
        private const val THREAD_COUNT = 100 // 100명이 동시에 찜 요청
    }

    @BeforeEach
    fun setUp() {
        // 1. 100명의 서로 다른 유저 생성 (동일 유저의 중복 찜 예외를 방지)
        repeat(THREAD_COUNT) { i ->
            val member = Member.create("찜유저$i", "1234", "bookmarkUser$i", "bm$i@test.com", "닉네임$i", Role.USER)
            members.add(memberRepository.save(member))
        }

        // 2. 찜 대상이 될 축제 하나 생성
        savedFestival = festivalRepository.save(
            Festival(
                "FEST-BM-CONCURRENCY",
                "찜 동시성 축제",
                "찜 동시성 테스트용 축제",
                "서울 테스트구",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(10),
                126.9780,
                37.5665,
                null, null, null, null, null,
                FestivalStatus.ONGOING
            )
        )
    }

    @AfterEach
    fun tearDown() {
        // @Transactional이 없으므로 수동으로 데이터를 비워줍니다.
        // 순서 주의: 자식 엔티티인 Bookmark를 먼저 지우고 Festival과 Member를 지워야 외래키 제약조건 위배를 막습니다.
        festivalBookmarkRepository.deleteAllInBatch()
        festivalRepository.deleteAllInBatch()
        memberRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("축제 찜 동시성 테스트 - 100명이 동시에 찜을 누르면 bookMarkCount가 100이 되어야 한다.")
    fun bookmarkFestival_Concurrency() {
        // given
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(THREAD_COUNT)

        // when
        members.forEach { member ->
            executorService.submit {
                try {
                    // 각 스레드가 서로 다른 사용자의 아이디로 찜 요청을 보냄
                    festivalBookmarkService.bookmarkFestival(savedFestival.id, member.loginId)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await() // 100개의 스레드가 모두 완료될 때까지 대기

        // then
        // 최종적으로 업데이트된 축제 정보를 DB에서 조회
        val updatedFestival = festivalRepository.findById(savedFestival.id).getOrNull()
            ?: error("축제를 찾을 수 없습니다.")

        println("기대하는 찜 수: $THREAD_COUNT")
        println("실제 DB에 저장된 찜 수: ${updatedFestival.bookMarkCount}")

        assertThat(updatedFestival.bookMarkCount).isEqualTo(THREAD_COUNT)
    }
}