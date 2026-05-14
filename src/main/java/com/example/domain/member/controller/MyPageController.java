package com.example.domain.member.controller;

import com.example.domain.bookmark.entity.FestivalBookmark;
import com.example.domain.member.dto.request.WithdrawReq;
import com.example.domain.member.dto.response.MyBookMarkPageRes;
import com.example.domain.member.dto.response.MyPageRes;
import com.example.domain.member.dto.response.MyReviewPageRes;
import com.example.domain.member.dto.response.WithdrawRes;
import com.example.domain.member.service.AuthService;
import com.example.domain.member.service.MemberService;
import com.example.domain.member.service.MyPageService;
import com.example.domain.review.service.ReviewService;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ForbiddenException;
import com.example.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "MyPage", description = "마이페이지 API")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService myPageService;
    private final ReviewService reviewService;
    private final MemberService memberService;
    private final AuthService authService;

    /**
     * 마이페이지 조회를 하여 사용자가 자신의 정보를 얻을 수 있다.
     *
     * @param authentication 로그인 토큰 로그인 토큰을 통해 loginId를 얻는다.
     * @return 로그인id, 이메일, 닉네임, 리뷰 수 ,찜수를 담은 DTo를 RSDATA로 감싸여 반환
     */
    @GetMapping()
    @Operation(summary = "내정보 보기", description = "로그인한 사용자가 자신의 정보(닉네임, 이메일)를 봅니다.")
    public ResponseEntity<RsData<MyPageRes>> getMyPage(
            Authentication authentication
    ) {
        String loginId = authentication.getName();
        MyPageRes res = myPageService.getMyPage(loginId);
        return ResponseEntity.ok(
                new RsData<>(
                        "200",
                        "마이페이지 조회에 성공하였습니다.",
                        res
                )
        );
    }

    /**
     * @param authentication 회원 인증정보를 header로 받습니다.
     * @param pageable       페이지 정보를 헤더로 받습니다.(defatult: 사이즈:5, 페이지0, 정렬:최근에 단 댓글순서)
     * @return 내가 쓴 리뷰와, 페이지 정보를 담은 객체를 반환하여 줍니다.
     */
    @GetMapping("/reviews")
    @Operation(summary = "내가 작성한 리뷰 목록 조회", description = "로그인한 사용자가 작성한 리뷰 목록을 조회합니다.")
    public ResponseEntity<RsData<MyReviewPageRes>> getMyReview(
            Authentication authentication,
            @PageableDefault(
                    size = 5,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        String loginid = authentication.getName();
        MyReviewPageRes res = myPageService.getMyReviews(loginid, pageable);
        return ResponseEntity.ok(
                new RsData<>("200", "내가 쓴 리뷰 목록 조회 성공", res)
        );
    }

    /**
     * @param authentication 사용자 loginId를 담은 JWT토큰
     * @param pageable       페이지 (사이즈 5 , 정렬 최근 찜한순)
     * @return 내가 찜한 축제정보, 페이지 정보를 담은 객체를 반환하여 줍니다.
     */
    @GetMapping("/bookmarks")
    @Operation(summary = "내가 찜한 축제 목록 조회", description = "로그인한 사용자가 자신이 찜한 축제의 목록을 조회합니다.")
    public ResponseEntity<RsData<MyBookMarkPageRes>> getMyBookMark(
            Authentication authentication,
            @PageableDefault(
                    size = 5,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        String loginId = authentication.getName();
        MyBookMarkPageRes res = myPageService.getMyBookMark(loginId, pageable);
        return ResponseEntity.ok(
                new RsData<>("200", "찜한 축제 목록 조회 성공", res)
        );
    }

    /**
     *
     * @param req (비밀번호와, 비밀번호 확인을 받습니다.)
     * @param authentication
     * @return memberId와상태를 반환합니다. 리프레시토큰또한 제거합니다.
     */
    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "회원은 자신의 상태를 withdraw로변경(탈퇴)를 진행할 수 있습니다.")
    public ResponseEntity<RsData<WithdrawRes>> selfWithdraw(
            @Valid @RequestBody WithdrawReq req,
            Authentication authentication
    ) {
        //비밀번호, 비밀번호 확인이 맞나 확인
        if (!req.password().equals(req.passwordConfirm())) {
            throw new BadRequestException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        WithdrawRes res = authService.selfWithdraw(authentication.getName(), req.password());
        return ResponseEntity.ok(
                new RsData<>(
                        "200",
                        "탈퇴처리가 성공적으로 수행되었습니다.",
                        res
                )
        );
    }

}
