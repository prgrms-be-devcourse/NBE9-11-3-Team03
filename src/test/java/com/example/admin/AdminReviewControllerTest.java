package com.example.admin;

import com.example.domain.admin.dto.ReviewProcessRequest;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import com.example.domain.review.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class AdminReviewControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private FestivalRepository festivalRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("신고 누적5회이상인 리뷰 조회")
    public void t1()throws Exception{
        Member author = new Member("user1", "1234", "이름1", "user1@test.com", "작성자", 0);
        memberRepository.save(author);

        Festival festival = new Festival("F_001", "축제", "설명", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                127.0, 37.0);
        festivalRepository.save(festival);


        Review lowReport = new Review(author, festival, "정상리뷰", null, 5);
        Review midReport = new Review(author, festival, "중간신고", null, 3);
        Review highReport = new Review(author, festival, "고신고리뷰", null, 1);


        ReflectionTestUtils.setField(lowReport, "reportCount", 0);
        ReflectionTestUtils.setField(midReport, "reportCount", 8);
        ReflectionTestUtils.setField(highReport, "reportCount", 15);

        reviewRepository.saveAll(List.of(lowReport, midReport, highReport));

        mockMvc.perform(get("/api/admin/reviews/reported")
                .param("page","0")
                        .header("Authorization","Bearer dev-temp-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("신고된 리뷰 목록 조회가 완료되었습니다."))
                //신고 5회 미만인 lowReport는 제외되어 총 2개여야 함
                .andExpect(jsonPath("$.data.totalElements").value(2))
                //신고가 15회인 고신고리뷰가 0번리스트
                .andExpect(jsonPath("$.data.content[0].reportCount").value(15))
                .andExpect(jsonPath("$.data.content[0].content").value("고신고리뷰"))
                // 두번째는 중간신고
                .andExpect(jsonPath("$.data.content[1].reportCount").value(8))
                .andDo(print());

    }

    @Test
    @DisplayName("리뷰 블라인드 처리 - 리뷰 상태 변경 및 작성자 신고 횟수 증가 확인")
    public void t2() throws Exception {
        // Given: 리뷰 하나 생성
        Member author = new Member("user2", "1234", "이름2", "user2@test.com", "작성자2", 0);
        memberRepository.save(author);

        Festival festival = new Festival("F_002", "축제2", "설명", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0);
        festivalRepository.save(festival);

        Review targetReview = new Review(author, festival, "신고 대상 리뷰", null, 5);
        reviewRepository.save(targetReview);

        // API 요청 데이터 (PATCH /api/admin/reviews/{id}/process)
        var request = new ReviewProcessRequest("BLIND");

        // When
        mockMvc.perform(patch("/api/admin/reviews/" + targetReview.getId() + "/status")
                        .header("Authorization","Bearer dev-temp-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value("리뷰가 블라인드 처리되었습니다."))
                .andDo(print());

        // Then: DB 상태 검증
        Review updatedReview = reviewRepository.findById(targetReview.getId()).get();
        Member updatedAuthor = memberRepository.findById(author.getId()).get();

        // 1. 리뷰 상태가 BLIND인지 확인
        assertThat(updatedReview.getStatus()).isEqualTo(ReviewStatus.BLIND);
        // 2. 작성자의 신고 카운트가 0에서 1로 올랐는지 확인
        assertThat(updatedAuthor.getReportCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("신고 초기화(무혐의) - 리뷰 신고수 리셋 확인")
    public void t3() throws Exception {
        // Given
        Member author = new Member("user3", "1234", "이름3", "user3@test.com", "작성자3", 0);
        memberRepository.save(author);

        Festival festival = new Festival("F_003", "축제3", "설명", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0);
        festivalRepository.save(festival);

        Review targetReview = new Review(author, festival, "억울한 리뷰", null, 5);
        ReflectionTestUtils.setField(targetReview, "reportCount", 10);
        reviewRepository.save(targetReview);

        var request = new ReviewProcessRequest("DISMISS");

        // When
        mockMvc.perform(patch("/api/admin/reviews/" + targetReview.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization","Bearer dev-temp-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리뷰 신고횟수가 초기화 되었습니다."))
                .andDo(print());

        // Then
        Review updatedReview = reviewRepository.findById(targetReview.getId()).get();
        assertThat(updatedReview.getReportCount()).isEqualTo(0); // 0으로 초기화 확인
    }
    @Test
    @DisplayName("전체 리뷰 목록 조회 시 블라인드된 리뷰는 노출되지 않아야 한다")
    @WithMockUser(username = "user4", roles = "USER") // 헤더에 토큰을 넣는 대신 스프링 시큐리티 가짜 유저 주입
    public void t4() throws Exception {
        // Given: 리뷰 2개 생성 (하나 정상, 하나 블라인드)
        Member author = new Member("user4", "1234", "이름4", "user4@test.com", "작성자4", 0);
        memberRepository.save(author);

        Festival festival = new Festival("F_004", "축제4", "설명", "주소",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 127.0, 37.0);
        festivalRepository.save(festival);

        Review normalReview = new Review(author, festival, "정상 리뷰", null, 5);
        Review blindReview = new Review(author, festival, "블라인드 리뷰", null, 5);

        // 블라인드 상태 강제 설정
        ReflectionTestUtils.setField(blindReview, "status", ReviewStatus.BLIND);

        reviewRepository.saveAll(List.of(normalReview, blindReview));

        // When: 일반 사용자용 리뷰 목록 조회 API 호출 (에러 로그에 맞춰 URL과 파라미터 적용)
        mockMvc.perform(get("/api/festivals/" + festival.getId() + "/reviews")
                        .param("festivalId", festival.getContentId()))
                // Then
                .andExpect(status().isOk())
                // 전체 개수가 1개여야 함 (blindReview 제외)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                // 노출되는 리뷰는 '정상 리뷰'여야 함
                .andExpect(jsonPath("$.data.content[0].content").value("정상 리뷰"))
                .andDo(print());
    }
}
