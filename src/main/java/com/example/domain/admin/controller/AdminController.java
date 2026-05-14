package com.example.domain.admin.controller;

import com.example.domain.admin.dto.*;
import com.example.domain.member.service.MemberService;
import com.example.domain.review.service.ReviewService;
import com.example.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자API")
public class AdminController {
    private final MemberService memberService;
    private final ReviewService reviewService;


    /**
     * 전체 회원 목록을 조회(기본 최근 생성일순,)
     * 파라미터로 페이지번호,정렬방법,사이즈 변경 가능
     *
     * @param pageable 페이지수 및  정렬정보(page,sort)
     * @return 회원 목록 정보를 담은 RsData객체
     */
    @GetMapping("/members")
    @Operation(summary = "전체 회원조회", description = "회원 목록을 전체 조회합니다.")
    public ResponseEntity<RsData<MemberPageResponse>> getMemberList(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        MemberPageResponse memberPage = memberService.getAllMembers(pageable);
        return ResponseEntity.ok(
                new RsData<>(
                        "200",
                        "회원 목록 조회 성공",
                        memberPage
                )
        );
    }

    /**
     * 전체 회원목록중 신고횟수가 5번 이상이며 활동중인 회원 조회(기본:신고 누적 내림차순)
     * 파라미터로 페이지,사이즈 조절 가능
     *
     * @param pageable 페이지수
     * @return 신고 누적횟수가5회이상인 활동중인 회원목록을 RsData객체
     */
    @GetMapping("/members/reported")
    @Operation(summary = "신고 누적 회원조회", description = "신고 count가 누적된 회원을 조회합니다.")
    public ResponseEntity<RsData<MemberPageResponse>> getReportMemberList(
            @PageableDefault(
                    size = 10,
                    sort = "reportCount",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        MemberPageResponse memberList = memberService.getReportMembers(pageable);
        return ResponseEntity.ok(
                new RsData<>(
                        "200",
                        "신고 누적회원 조회 성공",
                        memberList
                )
        );
    }

    /**
     * 관리자가 신고 횟수가 일정횟수(현재는 5 ) 이상인 리뷰목록을 조회할수있다.(신고 순 내림차순)
     *
     * @param pageable 페이지수, 사이즈등을 입력받을 수 있다.
     * @return 신고 누적횟수가5회이상인 활동중인 리뷰목록을 RsData객체
     */
    @GetMapping("/reviews/reported")
    @Operation(summary = "누적신고된 리뷰 조회", description = "신고횟수가 N개 이상인 리뷰를 조회합니다.")
    public ResponseEntity<RsData<AdminReviewReportPageRes>> getReportReview(
            @PageableDefault(size = 10, sort = "reportCount", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminReviewReportPageRes reviewList = reviewService.getReportReview(pageable);
        return ResponseEntity.ok(
                new RsData<>(
                        "200",
                        "신고된 리뷰 목록 조회가 완료되었습니다.",
                        reviewList
                )
        );
    }

    /**
     *관리자가 Blind혹은 신고수를 초기화 하는 컨트롤러 메서드
     * @param reviewId:검토할 리뷰 id
     * @param req= BLIND혹은 DISMISS 중하나
     * @return 해당 리뷰 id, 상태,신고 횟수를 담은 RsData반환
     */
    @PatchMapping("/reviews/{reviewId}/status")
    @Operation(summary = "악성리뷰 블라인드처리", description = "관리자가 악성리뷰를 블라인드처리할 수 있습니다.")
    public ResponseEntity<RsData<AdminReviewBlindRes>> processReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewProcessRequest req
    ) {
        AdminReviewBlindRes res = reviewService.processReviewAction(reviewId, req.action());

        String msg = "Blind".equalsIgnoreCase(req.action())
                ? "리뷰가 블라인드 처리되었습니다."
                : "리뷰 신고횟수가 초기화 되었습니다.";
        return ResponseEntity.ok(
                new RsData<>(
                        "200",
                        msg,
                        res
                )
        );
    }

    /**
     *회원을 강제 탈퇴 시킵니다. 탈퇴된 회원은 탈퇴한회원_memberId의 닉네임을 가지게 되고 , 리뷰글들은 탈퇴된회원으로 바뀌게 됩니다.
     * @param memberId 탈퇴시킬 회원 아이다.
     * @return memberId와 status를 가진 RsData를 ResponseEntity로반환
     */
    @PatchMapping("/members/{memberId}/withdraw")
    @Operation(summary = "회원 강제 탈퇴 처리", description = "관리자가 회원을 강제 탈퇴처리할 수 있습니다.")
    public ResponseEntity<RsData<AdminMemberWithdrawnRes>> memberWithdraw(
            @PathVariable Long memberId
    ){
        AdminMemberWithdrawnRes res= memberService.memberWithdraw(memberId);
        return ResponseEntity.ok(
                new RsData<>(
                        "200",
                        "회원이 강제 탈퇴 처리되었습니다.",
                        res
                )
        );
    }


}
