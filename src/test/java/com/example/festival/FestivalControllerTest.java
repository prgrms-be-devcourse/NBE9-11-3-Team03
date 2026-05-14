package com.example.festival;

import com.example.domain.bookmark.entity.FestivalBookmark;
import com.example.domain.bookmark.repository.FestivalBookmarkRepository;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.member.entity.Role;
import com.example.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FestivalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FestivalBookmarkRepository festivalBookmarkRepository;

    private Festival savedFestival;

    @BeforeEach
    void setUp() {
        // 테스트용 더미 데이터 세팅
        Festival festival = Festival.builder()
                .contentId("FEST-001")
                .overview("축제 상세조회 테스트용 축제입니다.")
                .mapX(126.9780)
                .mapY(37.5665)
                .title("상세조회 타겟 축제")
                .address("서울 테스트구")
                .status(FestivalStatus.ONGOING)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .viewCount(0)
                .bookMarkCount(0)
                .averageRate(0.0)
                .build();

        savedFestival = festivalRepository.save(festival);
    }

    @Test
    @DisplayName("축제 상세 조회 API - 성공 시 RsData 규격에 맞게 반환되어야 한다")
    void getFestivalDetail_Success() throws Exception {
        // given: 저장된 축제의 ID
        Long targetId = savedFestival.getId();

        // when:  GET /api/festivals/{id} 요청을 보냄
        ResultActions result = mockMvc.perform(get("/api/festivals/{id}", targetId)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 응답 검증 (HTTP 200 OK 인지, RsData 포맷이 맞는지 확인)
        result.andExpect(status().isOk())
                .andDo(print())
                // RsData의 공통 필드 검사
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("축제 상세 조회 성공"))
                // data 하위의 실제 축제 정보 검사
                .andExpect(jsonPath("$.data.id").value(targetId))
                .andExpect(jsonPath("$.data.title").value("상세조회 타겟 축제"))
                .andExpect(jsonPath("$.data.status").value("ONGOING"))
                .andExpect(jsonPath("$.data.isBookmarked").value(false));
    }

    @Test
    @DisplayName("축제 상세 조회 API - 로그인한 유저가 찜한 축제일 경우 isBookmarked는 true로 반환된다")
    @WithMockUser(username = "testUser123") // 💡 가짜 인증 유저 생성 (authentication.getName()이 "testUser123"이 됨)
    void getFestivalDetail_WithBookmark_Success() throws Exception {
        // given: 유저 생성 및 해당 축제에 대한 찜(Bookmark) 데이터 저장
        Member member = Member.builder()
                .loginId("testUser123")
                .password("1234")
                .nickname("테스터")
                .memberName("무기남")
                .email("test@test.com")
                .reportCount(0)
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build();
        memberRepository.save(member);

        FestivalBookmark bookmark = new FestivalBookmark(member, savedFestival);
        festivalBookmarkRepository.save(bookmark);

        Long targetId = savedFestival.getId();

        // when: 로그인된 상태로 GET /api/festivals/{id} 요청을 보냄
        ResultActions result = mockMvc.perform(get("/api/festivals/{id}", targetId)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 응답 검증
        result.andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.id").value(targetId))
                // 💡 핵심 검증: 찜한 데이터가 있으므로 true 로 반환되어야 함
                .andExpect(jsonPath("$.data.isBookmarked").value(true));
    }
}
