package com.example.review;

import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.Role;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.entity.Review;
import com.example.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private Member savedMember;
    private Festival savedFestival;
    private Review savedReview;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        savedMember = memberRepository.save(
                new Member(
                        "유저2",
                        "1234",
                        "user2",
                        "user2@test.com",
                        "닉네임2",
                        Role.USER
                )
        );

        otherMember = memberRepository.save(
                new Member(
                        "유저3",
                        "1234",
                        "user3",
                        "user3@test.com",
                        "닉네임3",
                        Role.USER
                )
        );

        savedFestival = festivalRepository.save(
                Festival.builder()
                        .contentId("FEST-001")
                        .overview("리뷰 테스트용 축제")
                        .mapX(126.9780)
                        .mapY(37.5665)
                        .title("리뷰 테스트 축제")
                        .address("서울 테스트구")
                        .status(FestivalStatus.ONGOING)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(10))
                        .viewCount(0)
                        .bookMarkCount(0)
                        .averageRate(0.0)
                        .build()
        );

        savedReview = reviewRepository.save(
                new Review(
                        savedMember,
                        savedFestival,
                        "삭제 전 리뷰",
                        "old_review_image.jpg",
                        5
                )
        );
    }

    @Test
    @DisplayName("리뷰 작성 성공 - 로그인한 사용자가 축제 리뷰를 작성한다.")
    void createReview_success() throws Exception {
        Festival newFestival = festivalRepository.save(
                Festival.builder()
                        .contentId("FEST-002")
                        .overview("리뷰 작성 성공 테스트용 축제")
                        .mapX(127.0000)
                        .mapY(37.5000)
                        .title("리뷰 작성 성공 축제")
                        .address("서울 테스트동")
                        .status(FestivalStatus.ONGOING)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(5))
                        .viewCount(0)
                        .bookMarkCount(0)
                        .averageRate(0.0)
                        .build()
        );

        String requestBody = """
        {
          "content": "리뷰 작성 테스트 내용",
          "rating": 4
        }
        """;

        MockMultipartFile requestDtoPart = new MockMultipartFile(
                "requestDto",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "create.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/api/festivals/{festivalId}/reviews", newFestival.getId())
                        .file(requestDtoPart)
                        .file(imagePart)
                        .with(user("user2").roles("USER"))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("리뷰 작성이 완료 되었습니다."))
                .andExpect(jsonPath("$.data.festivalId").value(newFestival.getId()))
                .andExpect(jsonPath("$.data.memberId").value(savedMember.getId()))
                .andExpect(jsonPath("$.data.content").value("리뷰 작성 테스트 내용"))
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.image").isNotEmpty());
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - 특정 축제의 리뷰 목록을 조회한다.")
    void getReviewList_success() throws Exception {
        reviewRepository.save(
                new Review(
                        savedMember,
                        savedFestival,
                        "두번째 리뷰",
                        "https://example.com/review2.jpg",
                        4
                )
        );

        mockMvc.perform(get("/api/festivals/{festivalId}/reviews", savedFestival.getId())
                        .with(user("user2").roles("USER"))
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("축제 리뷰 목록 조회 성공"))
                .andExpect(jsonPath("$.data.festivalId").value(savedFestival.getId()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("리뷰 수정 성공 - 본인 리뷰를 수정한다.")
    void updateReview_success() throws Exception {
        // 1. JSON DTO 준비 (이미지 URL은 DTO가 아닌 MultipartFile로 넘어가므로 제외)
        String requestBody = """
            {
              "content": "수정된 리뷰 내용입니다.",
              "rating": 3
            }
            """;
        // 2. MockMultipartFile 생성 (requestDto 부분)
        MockMultipartFile requestDtoPart = new MockMultipartFile(
                "requestDto", // 컨트롤러의 @RequestPart("requestDto") 와 이름이 똑같아야 함
                "",
                MediaType.APPLICATION_JSON_VALUE, // 이 파트의 타입은 JSON
                requestBody.getBytes(StandardCharsets.UTF_8)
        );
        // 3. MockMultipartFile 생성 (image 부분 - 더미 파일)
        MockMultipartFile imagePart = new MockMultipartFile(
                "image", // 컨트롤러의 @RequestPart("image") 와 이름이 똑같아야 함
                "updated.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );
        // 4. When: MockMvc 요청
        // 주의: multipart()는 기본적으로 POST 요청을 생성하므로 PATCH로 덮어씌워야 합니다.
        mockMvc.perform(multipart("/api/reviews/{reviewId}", savedReview.getId())
                        .file(requestDtoPart) // DTO 첨부
                        .file(imagePart)      // 이미지 첨부
                        .with(user("user2").roles("USER"))
                        .with(csrf())
                        .with(request -> {
                            request.setMethod(HttpMethod.PATCH.name());
                            return request;
                        }))
                .andDo(print())
                // 5. Then
                .andExpect(status().isOk())
                // 응답 상태코드가 숫자인지 문자열인지 공통응답객체(ApiRes) 설정에 따라 다를 수 있으니 .value(200) 또는 .value("200") 확인
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리뷰 수정 완료")) // 컨트롤러에 적힌 메시지와 일치시킴
                .andExpect(jsonPath("$.data.content").value("수정된 리뷰 내용입니다."))
                .andExpect(jsonPath("$.data.rating").value(3));
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 본인 리뷰를 논리 삭제한다.")
    void deleteReview_success() throws Exception {
        mockMvc.perform(delete("/api/reviews/{reviewId}", savedReview.getId())
                        .with(user("user2").roles("USER"))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리뷰 삭제가 완료되었습니다."))
                .andExpect(jsonPath("$.data.reviewId").value(savedReview.getId()))
                .andExpect(jsonPath("$.data.status").value("DELETED"));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 작성자가 아닌 회원은 수정할 수 없다.")
    void updateReview_fail_notAuthor() throws Exception {
        String requestBody = """
        {
          "content": "남의 리뷰 수정 시도",
          "rating": 3,
          "deleteImage": false
        }
        """;

        MockMultipartFile requestDtoPart = new MockMultipartFile(
                "requestDto",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/reviews/{reviewId}", savedReview.getId())
                        .file(requestDtoPart)
                        .with(user("user3").roles("USER"))
                        .with(csrf())
                        .with(request -> {
                            request.setMethod(HttpMethod.PATCH.name());
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인이 작성한 리뷰만 수정할 수 있습니다."));
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 작성자가 아닌 회원은 삭제할 수 없다.")
    void deleteReview_fail_notAuthor() throws Exception {
        mockMvc.perform(delete("/api/reviews/{reviewId}", savedReview.getId())
                        .with(user("user3").roles("USER"))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인이 작성한 리뷰만 삭제할 수 있습니다."));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 같은 회원은 같은 축제에 중복 리뷰를 작성할 수 없다.")
    void createReview_fail_duplicateReview() throws Exception {
        String requestBody = """
        {
          "content": "중복 리뷰 작성 시도",
          "rating": 4
        }
        """;

        MockMultipartFile requestDtoPart = new MockMultipartFile(
                "requestDto",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/festivals/{festivalId}/reviews", savedFestival.getId())
                        .file(requestDtoPart)
                        .with(user("user2").roles("USER"))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 해당 축제에 리뷰를 작성했습니다."));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 비로그인 사용자는 리뷰를 작성할 수 없다.")
    void createReview_fail_unauthorized() throws Exception {
        Festival newFestival = festivalRepository.save(
                Festival.builder()
                        .contentId("FEST-003")
                        .overview("비로그인 리뷰 작성 테스트용 축제")
                        .mapX(127.1000)
                        .mapY(37.6000)
                        .title("비로그인 테스트 축제")
                        .address("서울 테스트구")
                        .status(FestivalStatus.ONGOING)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(5))
                        .viewCount(0)
                        .bookMarkCount(0)
                        .averageRate(0.0)
                        .build()
        );

        String requestBody = """
        {
          "content": "비로그인 리뷰 작성 시도",
          "rating": 4
        }
        """;

        MockMultipartFile requestDtoPart = new MockMultipartFile(
                "requestDto",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                requestBody.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/festivals/{festivalId}/reviews", newFestival.getId())
                        .file(requestDtoPart)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
