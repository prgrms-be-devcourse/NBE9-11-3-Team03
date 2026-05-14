package com.example.myPage;

import com.example.domain.bookmark.entity.FestivalBookmark;
import com.example.domain.bookmark.repository.FestivalBookmarkRepository;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class MyPageTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    FestivalRepository festivalRepository;
    @Autowired
    ReviewRepository reviewRepository;
    @Autowired
    FestivalBookmarkRepository festivalBookmarkRepository;
    @Autowired
    PasswordEncoder passwordEncoder;



    @Test
    @DisplayName("마이페이지 조회 - 회원 정보와 함께 리뷰/북마크 개수가 정확히 조회된다.")
    @WithMockUser(username = "myPageUser")
    void t1() throws Exception {
        // 1. Given: 테스트용 회원 생성
        Member member = new Member("myPageUser", "1234", "홍길동", "mypage@test.com", "길동이t1", 0);
        memberRepository.save(member);

        // 2. Given: 해당 회원이 작성한 리뷰 2개 생성
        Festival festival = new Festival("F_006", "서울 세계불꽃축제", "설명", "여의도",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 126.92, 37.52);
        festivalRepository.save(festival);

        Review review1 = new Review(member, festival, "정말 재밌어요!", null, 5);
        Review review2 = new Review(member, festival, "또 가고 싶네요.", null, 4);
        reviewRepository.saveAll(List.of(review1, review2));

        // 3. Given: 해당 회원이 북마크(찜)한 내역 1개 생성
        // FestivalBookmarkRepository가 member와 festival을 받는다고 가정
        FestivalBookmark bookmark = new FestivalBookmark(member, festival);
        festivalBookmarkRepository.save(bookmark);

        // 4. When: 마이페이지 조회 API 호출
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                // 5. Then: RsData 구조 및 데이터 검증
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("마이페이지 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.nickname").value("길동이t1"))
                .andExpect(jsonPath("$.data.email").value("mypage@test.com"))
                .andExpect(jsonPath("$.data.reviewCount").value(2)) // 리뷰 2개
                .andExpect(jsonPath("$.data.bookMarkCount").value(1)) // 북마크 1개
                .andDo(print());
    }

    @Test
    @DisplayName("내가 쓴 리뷰 조회 - 작성한 리뷰 목록이 최신순으로 페이징되어 조회된다.")
    @WithMockUser(username = "myPageUser")
    void getMyReviewsTest() throws Exception {
        // 1. Given: 테스트용 회원 및 축제 생성
        Member member = new Member("myPageUser", "1234", "홍길동", "mypage@test.com", "길동이t1", 0);
        memberRepository.save(member);

        Festival festival = new Festival("F_TEST", "테스트 축제", "설명", "주소",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 126.0, 37.0);
        festivalRepository.save(festival);

        // 2. Given: 해당 회원이 작성한 리뷰 2개 생성 (시간차를 두어 생성)
        Review review1 = new Review(member, festival, "첫 번째 리뷰", null, 5);
        Review review2 = new Review(member, festival, "두 번째 리뷰", null, 4);
        reviewRepository.saveAll(List.of(review1, review2));

        // 3. When: 내가 쓴 리뷰 조회 API 호출 (페이지 0, 사이즈 10)
        mockMvc.perform(get("/api/users/me/reviews")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                // 4. Then: 응답 구조 및 데이터 검증
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("내가 쓴 리뷰 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                // 최신순 정렬 가정 시 두 번째 리뷰가 먼저 나와야 함
                .andExpect(jsonPath("$.data.content[0].content").value("두 번째 리뷰"))
                .andExpect(jsonPath("$.data.content[0].festivalTitle").value("테스트 축제"))
                .andExpect(jsonPath("$.data.content[1].content").value("첫 번째 리뷰"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andDo(print());
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 비밀번호가 일치하면 상태가 WITHDRAWN으로 변경된다.")
    @WithMockUser(username = "withdrawUser") // 인증 컨텍스트에 담길 이름
    void withdrawSuccessTest() throws Exception {
        // 1. Given: 실제 DB에 테스트용 회원 저장
        String loginId = "withdrawUser";
        String rawPassword = "password123";

        // PasswordEncoder를 주입받아 암호화해서 저장해야 서비스의 matches를 통과합니다.
        Member member = new Member(
                loginId,
                passwordEncoder.encode(rawPassword), // 암호화 필수
                "탈퇴전닉네임",
                "withdraw@test.com",
                "길동",
                0
        );
        memberRepository.save(member);

        // 2. When: 탈퇴 API 호출
        String requestBody = String.format("""
            {
                "password": "%s",
                "passwordConfirm": "%s"
            }
            """, rawPassword, rawPassword);

        mockMvc.perform(delete("/api/users/me/withdraw") // 경로 확인: /api/v1/mypage/me/withdraw
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("탈퇴처리가 성공적으로 수행되었습니다."))
                .andDo(print());

        // 3. Then: 실제 DB 상태 검증
        // 영속성 컨텍스트를 갱신하기 위해 다시 조회합니다.
        Member withdrawnMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(withdrawnMember.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(withdrawnMember.getNickname()).isEqualTo("탈퇴한회원_%d".formatted(member.getId()));
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 비밀번호가 일치하지 않으면 400 에러를 반환한다.")
    @WithMockUser(username = "failUser")
    void withdrawFailTest() throws Exception {
        // 1. Given: 회원 저장
        Member member = new Member(
                "failUser",
                passwordEncoder.encode("correctPassword"),
                "실패테스트",
                "fail@test.com",
                "길동",
                0
        );
        memberRepository.save(member);

        // 틀린 비밀번호 요청
        String requestBody = """
            {
                "password": "wrongPassword",
                "passwordConfirm": "wrongPassword"
            }
            """;

        // 2. When & Then
        mockMvc.perform(delete("/api/users/me/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
                .andDo(print());
    }
}
