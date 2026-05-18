package com.example.festival

import com.example.domain.festival.dto.request.FestivalSearchRequest
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.repository.FestivalRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FestivalRepositoryImplTest {
    @Autowired
    private lateinit var festivalRepository: FestivalRepository

    @BeforeEach
    fun setUp() {
        // 테스트용 더미 데이터 세팅
        festivalRepository.save(
            Festival.forTest(
                contentId = "FEST-001",
                title = "진행중인 서울 벚꽃축제",
                overview = "서울 벚꽃축제 개요입니다.",
                address = "서울 영등포구",
                startDate = LocalDateTime.now().minusDays(2),
                endDate = LocalDateTime.now().plusDays(5),
                mapX = 126.9780,
                mapY = 37.5665,
                lDongRegnCd = "11",
                status = FestivalStatus.ONGOING,
                viewCount = 100,
                bookMarkCount = 10,
                averageRate = 4.5,

                )
        )

        festivalRepository.save(
            Festival.forTest(
                contentId = "FEST-002",
                title = "예정된 경기 불꽃축제",
                overview = "경기 불꽃축제 개요입니다.",
                address = "경기 가평군",
                startDate = LocalDateTime.now().plusDays(10),
                endDate = LocalDateTime.now().plusDays(12),
                mapX = 127.0,
                mapY = 37.0,
                lDongRegnCd = "41",
                status = FestivalStatus.UPCOMING,
                viewCount = 500,
                bookMarkCount = 50,
            )
        )

        festivalRepository.save(
            Festival.forTest(
                contentId = "FEST-003",
                title = "종료된 부산 바다축제",
                overview = "부산 바다축제 개요입니다.",
                address = "부산 해운대구",
                startDate = LocalDateTime.now().minusDays(20),
                endDate = LocalDateTime.now().minusDays(15),
                mapX = 129.0,
                mapY = 35.0,
                lDongRegnCd = "26",
                status = FestivalStatus.ENDED,
                viewCount = 1000,
                bookMarkCount = 100,
                averageRate = 4.8,
            )
        )

        festivalRepository.save(
            Festival.forTest(
                contentId = "FEST-004",
                title = "진행중인 제주 감귤축제",
                overview = "제주 감귤축제 개요입니다.",
                address = "제주 서귀포시",
                startDate = LocalDateTime.now().minusDays(1),
                endDate = LocalDateTime.now().plusDays(10),
                mapX = 126.5,
                mapY = 33.4,
                lDongRegnCd = "50",
                status = FestivalStatus.ONGOING,
                viewCount = 50,
                bookMarkCount = 5,
                averageRate = 3.5,
            )
        )

        festivalRepository.save(
            Festival.forTest(
                contentId = "FEST-005",
                title = "종료된 강릉 커피축제",
                overview = "강릉 커피축제 개요입니다.",
                address = "강원도 강릉시",
                startDate = LocalDateTime.now().minusDays(10),
                endDate = LocalDateTime.now().minusDays(5),
                mapX = 128.876,
                mapY = 37.751,
                lDongRegnCd = "51",
                status = FestivalStatus.ENDED,
                viewCount = 2000,
                bookMarkCount = 300,
                averageRate = 4.9,
            )
        )
    }

    @Test
    @DisplayName("1. 글로벌 기본 정렬 룰 테스트 (조건 없을 때)")
    fun defaultSortTest() {
        val condition = FestivalSearchRequest()
        val pageRequest = PageRequest.of(0, 10)

        val result = festivalRepository.searchFestivals(condition, pageRequest)

        assertThat<Festival?>(result.getContent()).hasSize(5)

        // 1순위 룰: ONGOING -> UPCOMING -> ENDED 확인
        assertThat(result.content[0].status).isEqualTo(FestivalStatus.ONGOING)
        assertThat(result.content[1].status).isEqualTo(FestivalStatus.ONGOING)
        assertThat(result.content[2].status).isEqualTo(FestivalStatus.UPCOMING)
        assertThat(result.content[3].status).isEqualTo(FestivalStatus.ENDED)

        // 2순위 룰: ONGOING 끼리는 시작일(startDate) 임박순(오름차순)인지 확인
        // (서울 벚꽃이 -2일, 제주 감귤이 -1일이므로 제주 감귤축제가 더 나중에 시작했으니 뒤에 와야 함)
        assertThat(result.content[0].title).isEqualTo("진행중인 서울 벚꽃축제")
        assertThat(result.content[1].title).isEqualTo("진행중인 제주 감귤축제")
    }

    @Test
    @DisplayName("2. 지역 및 키워드 필터링 테스트")
    fun searchFilterTest() {
        // given
        val condition = FestivalSearchRequest(regionCode = "11", keyword = "벚꽃")
        val pageRequest = PageRequest.of(0, 10)

        // when
        val result = festivalRepository.searchFestivals(condition, pageRequest)

        // then
        assertThat(result.getContent()).hasSize(1)
        assertThat(result.content[0].title).isEqualTo("진행중인 서울 벚꽃축제")
    }

    @Test
    @DisplayName("3. 다중 조건 정렬 테스트 (조회순 내림차순)")
    fun customSortTest() {
        val condition = FestivalSearchRequest()
        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"))

        val result = festivalRepository.searchFestivals(condition, pageRequest)

        assertThat(result.content[0].title).isEqualTo("진행중인 서울 벚꽃축제")
        assertThat(result.content[1].title).isEqualTo("진행중인 제주 감귤축제")
        assertThat(result.content[2].title).isEqualTo("예정된 경기 불꽃축제")
    }

    @Test
    @DisplayName("4. 월(Month) 겹침 로직 테스트")
    fun monthOverlapTest() {
        val currentMonth = LocalDateTime.now().monthValue
        val condition = FestivalSearchRequest(month = currentMonth)
        val pageRequest = PageRequest.of(0, 10)

        val result = festivalRepository.searchFestivals(condition, pageRequest)

        assertThat(result.content).isNotEmpty
    }


    @Test
    @DisplayName("5. 상태(Status) 단일 필터링 및 ENDED 기본 정렬 테스트")
    fun statusFilterAndEndedSortTest() {
        val condition = FestivalSearchRequest(status = FestivalStatus.ENDED)
        val pageRequest = PageRequest.of(0, 10)

        val result = festivalRepository.searchFestivals(condition, pageRequest)

        assertThat(result.content).hasSize(2)// 부산, 강릉 2개가 나와야 함
        // 핵심 검증: ENDED 상태인 경우 종료일(endDate)이 최근인 것(내림차순)이 먼저 오는지 확인
        // 강릉(-5일)이 부산(-15일)보다 최근에 끝났으므로 강릉이 1등이어야 함!
        assertThat(result.content[0].title).isEqualTo("종료된 강릉 커피축제")
        assertThat(result.content[1].title).isEqualTo("종료된 부산 바다축제")
    }


    @Test
    @DisplayName("6. 찜순(bookMarkCount) 다중 조건 정렬 테스트")
    fun bookMarkSortTest() {
        // given: 조건 없이 찜순 내림차순 정렬 요청
        val condition = FestivalSearchRequest()
        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "bookMarkCount"))

        // when
        val result = festivalRepository.searchFestivals(condition, pageRequest)

        // then
        // 1순위(상태) ONGOING 내에서 찜순 정렬: 서울(10) > 제주(5)
        assertThat(result.content[0].title).isEqualTo("진행중인 서울 벚꽃축제")
        assertThat(result.content[1].title).isEqualTo("진행중인 제주 감귤축제")
        assertThat(result.content[2].title).isEqualTo("예정된 경기 불꽃축제")
        assertThat(result.content[3].title).isEqualTo("종료된 강릉 커피축제")
        assertThat(result.content[4].title).isEqualTo("종료된 부산 바다축제")
    }

    @Test
    @DisplayName("7. 검색 결과가 전혀 없는 경우 (Empty) 테스트")
    fun emptyResultTest() {
        // given: 절대 있을 수 없는 요상한 조건으로 검색
        val condition = FestivalSearchRequest(regionCode = "하와이", keyword = "외계인")
        val pageRequest = PageRequest.of(0, 10)

        // when
        val result = festivalRepository.searchFestivals(condition, pageRequest)

        // then
        // 에러가 터지지 않고 0건짜리 빈 페이지를 정상적으로 리턴해야 함
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @Test
    @DisplayName("8. 내 주변 축제 검색 (마커용) - 반경 10km 좁은 검색")
    fun nearbySearch_10km_Test() {
        // given: 내 위치를 서울 한복판(경도 126.9780, 위도 37.5665)으로 설정
        // 기존 4개 조건은 null로 두고, 방금 추가한 좌표 3개만 세팅
        val condition = FestivalSearchRequest(mapX = 126.9780, mapY = 37.5665, radiusKm = 10.0)

        // when: 마커 전용 메서드(findNearbyFestivals) 호출
        val result = festivalRepository.findNearbyFestivals(condition)

        // then
        // 서울 벚꽃축제만 10km 안에 들어오고, 나머지는 컷(Cut)
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("진행중인 서울 벚꽃축제")

    }

    @Test
    @DisplayName("9. 내 주변 축제 검색 (마커용) - 반경 100km 넓은 검색")
    fun nearbySearch_100km_Test() {
        // given: 똑같이 서울 한복판에서, 이번엔 반경을 100km로 확 넓힘
        val condition = FestivalSearchRequest(mapX = 126.9780, mapY = 37.5665, radiusKm = 100.0)

        // when
        val result = festivalRepository.findNearbyFestivals(condition)

        // then
        // 100km로 넓히면 서울(동일 위치)과 경기(약 60km 거리) 2개가 잡혀야 함!
        // 강릉, 부산, 제주는 여전히 100km 밖이므로 안 나와야 함
        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("진행중인 서울 벚꽃축제")
        assertThat(result[1].title).isEqualTo("예정된 경기 불꽃축제")

    }
}
