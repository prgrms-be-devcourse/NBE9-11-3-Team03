package com.example.admin;

import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.controller.ReviewController;
import com.example.domain.review.entity.Review;
import com.example.domain.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class AdminMemberControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private FestivalRepository festivalRepository;

    @Test
    @DisplayName("관리자 전체회원 목록조회")
    void t1() throws Exception{
        mockMvc.perform(get("/api/admin/members")
                .param("page","0")
                        .header("Authorization","Bearer dev-temp-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("회원 목록 조회 성공"))
                .andDo(print());
    }
    @Test
    @DisplayName("신고누적순")
    void t2() throws Exception{
        Member lowReport = new Member("user1", "1234","이름1","user1@test.com", "저신고자", 1);
        Member midReport = new Member("user2", "1234","이름2","user2@test.com", "중신고자", 5);
        Member highReport = new Member("user3", "1234","이름3","user3@test.com", "고신고자", 10);
        memberRepository.saveAll(List.of(lowReport, midReport, highReport));

        mockMvc.perform(get("/api/admin/members/reported")
                        .param("page", "0")
                        .header("Authorization","Bearer dev-temp-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("신고 누적회원 조회 성공"))
                .andExpect(jsonPath("$.data.content[0].nickname").value("고신고자"))
                .andExpect(jsonPath("$.data.content[0].reportCount").value(10))
                .andExpect(jsonPath("$.data.content[1].nickname").value("중신고자"))
                .andDo(print());
    }

    @Test
    @DisplayName("관리자가 회원을 강제 탈퇴시키면 상태가 WITHDRAWN으로 변경되고 닉네임이 마스킹된다.")
    public void t4() throws Exception {
        Member member = new Member("user4", "1234", "이름4", "user4@test.com", "활동중인회원", 0);
        memberRepository.save(member);
        Long memberId = member.getId();
        mockMvc.perform(patch("/api/admin/members/" + memberId + "/withdraw")
                        .header("Authorization","Bearer dev-temp-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("회원이 강제 탈퇴 처리되었습니다."))
                .andDo(print());

        // Then:
        Member withdrawnMember = memberRepository.findById(memberId).get();

        // 1. 상태가 WITHDRAWN인지 확인
        assertThat(withdrawnMember.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);

        // 2. DB의 닉네임이 '탈퇴회원_ID' 형식으로 변경되었는지 확인
        assertThat(withdrawnMember.getNickname()).isEqualTo("탈퇴한회원_" + memberId);
    }

    @Test
    @DisplayName("탈퇴한 회원이 작성한 리뷰를 조회하면 닉네임이 '탈퇴된 회원입니다.'로 표시된다.")
    @WithMockUser(username = "visitor", roles = "USER") // 1. API를 호출할 유저 지정
    public void t5() throws Exception {
        Member visitor = new Member("visitor", "1234", "방문자", "visitor@test.com", "방문자닉넴", 0);
        memberRepository.save(visitor);
        Member member = new Member("user5", "1234", "이름5", "user5@test.com", "탈퇴전이름", 0);
        memberRepository.save(member);
        member.withdraw(); // 상태: WITHDRAWN 변경
        memberRepository.save(member);

        // 4. 축제 생성
        Festival festival = new Festival("F_005", "축제5", "설명", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0);
        festivalRepository.save(festival);

        // 5. 리뷰 생성
        Review review = new Review(member, festival, "탈퇴한 사람이 쓴 리뷰", null, 5);
        reviewRepository.save(review);

        // 6. When: 축제 리뷰 목록 조회 API 호출
        mockMvc.perform(get("/api/festivals/" + festival.getId() + "/reviews")
                        .param("page", "0")
                        .param("size", "10")
                        .param("memberId", member.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.content[0].nickname").value("탈퇴된 회원입니다."))
                .andDo(print());
    }

}
