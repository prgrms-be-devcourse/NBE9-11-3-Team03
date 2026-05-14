package com.example.festival;

import com.example.domain.festival.dto.FestivalSearchRequestDto;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class FestivalRepositoryImplTest {

    @Autowired
    private FestivalRepository festivalRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 더미 데이터 세팅
        festivalRepository.save(Festival.builder()
                .contentId("FEST-001")
                .overview("서울 벚꽃축제 개요입니다.")
                .mapX(126.9780)
                .mapY(37.5665)
                .lDongRegnCd("11")
                .title("진행중인 서울 벚꽃축제")
                .address("서울 영등포구")
                .status(FestivalStatus.ONGOING)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(5))
                .viewCount(100)
                .bookMarkCount(10)
                // 만약 averageRate 도 NOT NULL 이라면 .averageRate(0.0) 추가!
                .averageRate(4.5)
                .build());

        festivalRepository.save(Festival.builder()
                .contentId("FEST-002")
                .overview("경기 불꽃축제 개요입니다.")
                .mapX(127.0)
                .mapY(37.0)
                .lDongRegnCd("41")
                .title("예정된 경기 불꽃축제")
                .address("경기 가평군")
                .status(FestivalStatus.UPCOMING)
                .startDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(12))
                .viewCount(500)
                .bookMarkCount(50)
                .averageRate(0.0)
                .build());

        festivalRepository.save(Festival.builder()
                .contentId("FEST-003")
                .overview("부산 바다축제 개요입니다.")
                .mapX(129.0)
                .mapY(35.0)
                .lDongRegnCd("26")
                .title("종료된 부산 바다축제")
                .address("부산 해운대구")
                .status(FestivalStatus.ENDED)
                .startDate(LocalDateTime.now().minusDays(20))
                .endDate(LocalDateTime.now().minusDays(15))
                .viewCount(1000)
                .bookMarkCount(100)
                .averageRate(4.8)
                .build());

        festivalRepository.save(Festival.builder()
                .contentId("FEST-004")
                .overview("제주 감귤축제 개요입니다.")
                .mapX(126.5)
                .mapY(33.4)
                .lDongRegnCd("50")
                .title("진행중인 제주 감귤축제")
                .address("제주 서귀포시")
                .status(FestivalStatus.ONGOING)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .viewCount(50)
                .bookMarkCount(5)
                .averageRate(3.5)
                .build());

        festivalRepository.save(Festival.builder()
                .contentId("FEST-005")
                .overview("강릉 커피축제 개요입니다.")
                .mapX(128.876)
                .mapY(37.751)
                .lDongRegnCd("51")
                .title("종료된 강릉 커피축제")
                .address("강원도 강릉시")
                .status(FestivalStatus.ENDED)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(5)) // 부산(-15일)보다 최근에 끝남!
                .viewCount(2000)
                .bookMarkCount(300)
                .averageRate(4.9)
                .build());
    }

    @Test
    @DisplayName("1. 글로벌 기본 정렬 룰 테스트 (조건 없을 때)")
    void defaultSortTest() {
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto(null, null, null, null, null, null, null);
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<Festival> result = festivalRepository.searchFestivals(condition, pageRequest);

        assertThat(result.getContent()).hasSize(5);

        // 1순위 룰: ONGOING -> UPCOMING -> ENDED 확인
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(FestivalStatus.ONGOING);
        assertThat(result.getContent().get(1).getStatus()).isEqualTo(FestivalStatus.ONGOING);
        assertThat(result.getContent().get(2).getStatus()).isEqualTo(FestivalStatus.UPCOMING);
        assertThat(result.getContent().get(3).getStatus()).isEqualTo(FestivalStatus.ENDED);

        // 2순위 룰: ONGOING 끼리는 시작일(startDate) 임박순(오름차순)인지 확인
        // (서울 벚꽃이 -2일, 제주 감귤이 -1일이므로 제주 감귤축제가 더 나중에 시작했으니 뒤에 와야 함)
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("진행중인 서울 벚꽃축제");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("진행중인 제주 감귤축제");
    }

    @Test
    @DisplayName("2. 지역 및 키워드 필터링 테스트")
    void searchFilterTest() {
        // given
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto("11", null, null, "벚꽃", null, null,null);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Festival> result = festivalRepository.searchFestivals(condition, pageRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("진행중인 서울 벚꽃축제");
    }

    @Test
    @DisplayName("3. 다중 조건 정렬 테스트 (조회순 내림차순)")
    void customSortTest() {
        // given
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto(null, null, null, null, null, null,null);
        // 조회수(viewCount) 내림차순 정렬 추가
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"));

        // when
        Page<Festival> result = festivalRepository.searchFestivals(condition, pageRequest);

        // then
        // 글로벌 1순위(상태값)는 무조건 유지되어야 하므로 ONGOING인 서울(100)과 제주(50)가 먼저 나와야 함
        // 그 안에서 조회수 정렬이 먹혀서 서울(100) -> 제주(50) 순이 되어야 함
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("진행중인 서울 벚꽃축제"); // 100
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("진행중인 제주 감귤축제"); // 50

        // 그 다음 상태인 UPCOMING 부산(500)이 나와야 함 (조회수가 500이라도 상태가 2순위이므로)
        assertThat(result.getContent().get(2).getTitle()).isEqualTo("예정된 경기 불꽃축제");
    }

    @Test
    @DisplayName("4. 월(Month) 겹침 로직 테스트")
    void monthOverlapTest() {
        // given
        // 현재 달을 기준으로 세팅 (setUp 데이터들이 현재 날짜 기준이므로)
        int currentMonth = LocalDateTime.now().getMonthValue();
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto(null, null, currentMonth, null, null, null,null);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Festival> result = festivalRepository.searchFestivals(condition, pageRequest);

        // then
        // 현재 달에 걸쳐있는 축제들이 정상적으로 나오는지 확인
        // (보통 setUp 데이터 4개가 다 걸리거나, 날짜 경계에 따라 다를 수 있으므로 데이터가 존재하는지만 검증)
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("5. 상태(Status) 단일 필터링 및 ENDED 기본 정렬 테스트")
    void statusFilterAndEndedSortTest() {
        // given: 상태를 ENDED(종료)로만 검색
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto(null, FestivalStatus.ENDED, null, null, null, null,null);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Festival> result = festivalRepository.searchFestivals(condition, pageRequest);

        // then
        assertThat(result.getContent()).hasSize(2); // 부산, 강릉 2개가 나와야 함

        // ⭐️ 핵심 검증: ENDED 상태인 경우 종료일(endDate)이 최근인 것(내림차순)이 먼저 오는지 확인
        // 강릉(-5일)이 부산(-15일)보다 최근에 끝났으므로 강릉이 1등이어야 함!
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("종료된 강릉 커피축제");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("종료된 부산 바다축제");
    }

    @Test
    @DisplayName("6. 찜순(bookMarkCount) 다중 조건 정렬 테스트")
    void bookMarkSortTest() {
        // given: 조건 없이 찜순 내림차순 정렬 요청
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto(null, null, null, null, null, null,null);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "bookMarkCount"));

        // when
        Page<Festival> result = festivalRepository.searchFestivals(condition, pageRequest);

        // then
        // 1순위(상태) ONGOING 내에서 찜순 정렬: 서울(10) > 제주(5)
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("진행중인 서울 벚꽃축제");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("진행중인 제주 감귤축제");

        // UPCOMING 상태: 경기(50)
        assertThat(result.getContent().get(2).getTitle()).isEqualTo("예정된 경기 불꽃축제");

        // 1순위(상태) ENDED 내에서 찜순 정렬: 강릉(300) > 부산(100)
        assertThat(result.getContent().get(3).getTitle()).isEqualTo("종료된 강릉 커피축제");
        assertThat(result.getContent().get(4).getTitle()).isEqualTo("종료된 부산 바다축제");
    }

    @Test
    @DisplayName("7. 검색 결과가 전혀 없는 경우 (Empty) 테스트")
    void emptyResultTest() {
        // given: 절대 있을 수 없는 요상한 조건으로 검색
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto("하와이", null, null, "외계인", null, null,null);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Festival> result = festivalRepository.searchFestivals(condition, pageRequest);

        // then
        // 에러가 터지지 않고 0건짜리 빈 페이지를 정상적으로 리턴해야 함
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("8. 내 주변 축제 검색 (마커용) - 반경 10km 좁은 검색")
    void nearbySearch_10km_Test() {
        // given: 내 위치를 서울 한복판(경도 126.9780, 위도 37.5665)으로 설정
        Double myMapX = 126.9780;
        Double myMapY = 37.5665;
        Double radiusKm = 10.0; // 반경 10km

        // 기존 4개 조건은 null로 두고, 방금 추가한 좌표 3개만 세팅
        FestivalSearchRequestDto condition = new FestivalSearchRequestDto(null, null, null, null, myMapX, myMapY, radiusKm);

        // when: 마커 전용 메서드(findNearbyFestivals) 호출
        List<Festival> result = festivalRepository.findNearbyFestivals(condition);

        // then
        // 서울 벚꽃축제만 10km 안에 들어오고, 나머지는 컷(Cut)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("진행중인 서울 벚꽃축제");
    }

    @Test
    @DisplayName("9. 내 주변 축제 검색 (마커용) - 반경 100km 넓은 검색")
    void nearbySearch_100km_Test() {
        // given: 똑같이 서울 한복판에서, 이번엔 반경을 100km로 확 넓힘
        Double myMapX = 126.9780;
        Double myMapY = 37.5665;
        Double radiusKm = 100.0; // 반경 100km

        FestivalSearchRequestDto condition = new FestivalSearchRequestDto(null, null, null, null, myMapX, myMapY, radiusKm);

        // when
        List<Festival> result = festivalRepository.findNearbyFestivals(condition);

        // then
        // 100km로 넓히면 서울(동일 위치)과 경기(약 60km 거리) 2개가 잡혀야 함!
        // 강릉, 부산, 제주는 여전히 100km 밖이므로 안 나와야 함
        assertThat(result).hasSize(2);

        // 정렬 1순위(ONGOING)인 서울이 먼저 나오고, 2순위(UPCOMING)인 경기가 뒤에 나오는지도 덤으로 확인
        assertThat(result.get(0).getTitle()).isEqualTo("진행중인 서울 벚꽃축제");
        assertThat(result.get(1).getTitle()).isEqualTo("예정된 경기 불꽃축제");
    }
}
