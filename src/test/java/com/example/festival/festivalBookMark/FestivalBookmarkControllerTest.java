package com.example.festival.festivalBookMark;

import com.example.domain.bookmark.entity.FestivalBookmark;
import com.example.domain.bookmark.repository.FestivalBookmarkRepository;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.Role;
import com.example.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FestivalBookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FestivalBookmarkRepository festivalBookmarkRepository;

    private Festival savedFestival;
    private Member savedMember;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 유저 생성
        Member member = new Member(
                "테스트유저",
                "1234",
                "testUser",
                "test@test.com",
                "테스트닉네임",
                Role.USER);
        savedMember = memberRepository.save(member);

        // 2. 테스트용 축제 생성
        Festival festival = Festival.builder()
                .contentId("FEST-BM-API-001")
                .overview("찜 컨트롤러 테스트용 축제")
                .mapX(126.9780)
                .mapY(37.5665)
                .title("찜 API 축제")
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
    @DisplayName("1. [찜하기 성공] 로그인한 유저가 축제를 찜하면 200 OK와 함께 true가 반환된다.")
    @WithMockUser(username = "testUser")
    void bookmarkFestival_Success() throws Exception {
        mockMvc.perform(post("/api/festivals/{id}/bookmark", savedFestival.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("축제 찜 되었습니다."))
                .andExpect(jsonPath("$.data.isBookmarked").value(true))
                .andDo(print());
    }

    @Test
    @DisplayName("2. [찜하기 실패 - 이미 찜한 축제] 중복 찜 요청 시 409 Conflict 예외가 발생한다.")
    @WithMockUser(username = "testUser")
    void bookmarkFestival_AlreadyBookmarked() throws Exception {
        // given: 1차로 찜을 미리 해둔 상태로 셋팅
        festivalBookmarkRepository.save(new FestivalBookmark(savedMember, savedFestival));

        // when & then: 똑같은 축제에 또 찜하기 요청
        mockMvc.perform(post("/api/festivals/{id}/bookmark", savedFestival.getId()))
                .andExpect(status().isConflict()) // HTTP 상태코드 409 확인
                .andDo(print());
    }

    @Test
    @DisplayName("3. [찜하기 실패 - 비로그인] 인증되지 않은 요청은 401 Unauthorized 예외가 발생한다.")
    void bookmarkFestival_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/festivals/{id}/bookmark", savedFestival.getId()))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    @DisplayName("4. [찜취소 성공] 로그인한 유저가 찜한 축제를 취소하면 200 OK와 함께 false가 반환된다.")
    @WithMockUser(username = "testUser")
    void cancelBookmark_Success() throws Exception {
        // given: 찜을 미리 해둔 상태로 셋팅
        festivalBookmarkRepository.save(new FestivalBookmark(savedMember, savedFestival));

        // when & then: 찜 취소(DELETE) 요청
        mockMvc.perform(delete("/api/festivals/{id}/bookmark", savedFestival.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("축제 찜이 취소되었습니다."))
                .andExpect(jsonPath("$.data.isBookmarked").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("5. [찜취소 실패 - 찜하지 않은 축제] 찜이 없는 상태에서 취소 요청 시 400 BadRequest 예외가 발생한다.")
    @WithMockUser(username = "testUser")
    void cancelBookmark_NotBookmarked() throws Exception {
        // given: setUp 상태 그대로 아무것도 찜하지 않음

        // when & then: 찜 취소 요청
        mockMvc.perform(delete("/api/festivals/{id}/bookmark", savedFestival.getId()))
                .andExpect(status().isBadRequest()) // HTTP 상태코드 400 확인
                .andDo(print());
    }

}
