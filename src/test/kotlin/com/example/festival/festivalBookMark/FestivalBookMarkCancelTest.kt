package com.example.festival.festivalBookMark

import com.example.domain.bookmark.entity.FestivalBookmark
import com.example.domain.bookmark.repository.FestivalBookmarkRepository
import com.example.domain.bookmark.service.FestivalBookmarkService
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Member.Companion.create
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import org.assertj.core.api.Assertions
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
class FestivalBookMarkCancelTest {

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
        private const val THREAD_COUNT = 100
    }

    @BeforeEach
    fun setUp() {
        repeat(THREAD_COUNT) { i ->
            val member = Member.create("찜유저$i", "1234", "bmUser$i", "bm$i@test.com", "닉네임$i", Role.USER)
            members.add(memberRepository.save(member))
        }

        savedFestival = festivalRepository.save(
            Festival(
                "FEST-BM-CANCEL", "축제", "테스트", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(10),
                126.9, 37.5,
                null, null, null, null, null,
                FestivalStatus.ONGOING
            )
        )

        members.forEach { member ->
            festivalBookmarkRepository.save(FestivalBookmark(member, savedFestival))
            savedFestival.increaseBookmarkCount()
        }

        savedFestival = festivalRepository.save(savedFestival)
    }

    @AfterEach
    fun tearDown() {
        festivalBookmarkRepository.deleteAllInBatch()
        festivalRepository.deleteAllInBatch()
        memberRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("축제 찜 취소 동시성 테스트 - 찜 수가 100인 상태에서 100명이 동시에 취소하면 0이 되어야 한다.")
    fun cancelBookmark_Concurrency() {
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(THREAD_COUNT)

        members.forEachIndexed { index, member ->
            executorService.submit {
                try {
                    festivalBookmarkService.cancelBookmark(savedFestival.id, member.loginId)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        val updatedFestival = festivalRepository.findById(savedFestival.id).getOrNull()
            ?: error("축제를 찾을 수 없습니다.")

        println("취소 후 실제 찜 수: ${updatedFestival.bookMarkCount}")
        assertThat(updatedFestival.bookMarkCount).isEqualTo(0)
    }
}