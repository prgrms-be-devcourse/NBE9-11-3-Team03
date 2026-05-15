package com.example.domain.member.controller

import com.example.domain.member.dto.request.WithdrawRequest
import com.example.domain.member.dto.response.MyBookMarkPageResponse
import com.example.domain.member.dto.response.MyPageResponse
import com.example.domain.member.dto.response.MyReviewPageResponse
import com.example.domain.member.dto.response.WithdrawResponse
import com.example.domain.member.service.AuthService
import com.example.domain.member.service.MyPageService
import com.example.global.exception.BadRequestException
import com.example.global.rsData.RsData
import com.example.global.security.TokenCookieManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "MyPage", description = "마이페이지 API")
class MyPageController(
    private val myPageService: MyPageService,
    private val authService: AuthService,
    private val tokenCookieManager: TokenCookieManager
) {
    /**
     * 마이페이지 조회를 하여 사용자가 자신의 정보를 얻을 수 있다.
     */
    @GetMapping
    @Operation(summary = "내정보 보기", description = "로그인한 사용자가 자신의 정보(닉네임, 이메일)를 봅니다.")
    fun getMyPage(
        authentication: Authentication
    ): ResponseEntity<RsData<MyPageResponse>> =
        ResponseEntity.ok(
            RsData(
                "200",
                "마이페이지 조회에 성공하였습니다.",
                myPageService.getMyPage(authentication.name)
            )
        )

    /**
     * 내가 작성한 리뷰 목록 조회
     */
    @GetMapping("/reviews")
    @Operation(summary = "내가 작성한 리뷰 목록 조회", description = "로그인한 사용자가 작성한 리뷰 목록을 조회합니다.")
    fun getMyReview(
        authentication: Authentication,
        @PageableDefault(size = 5, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<RsData<MyReviewPageResponse>> =
        ResponseEntity.ok(
            RsData(
                "200",
                "내가 쓴 리뷰 목록 조회 성공",
                myPageService.getMyReviews(authentication.name, pageable)
            )
        )

    /**
     * 내가 찜한 축제 목록 조회
     */
    @GetMapping("/bookmarks")
    @Operation(summary = "내가 찜한 축제 목록 조회", description = "로그인한 사용자가 자신이 찜한 축제의 목록을 조회합니다.")
    fun getMyBookMark(
        authentication: Authentication,
        @PageableDefault(size = 5, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<RsData<MyBookMarkPageResponse>> =
        ResponseEntity.ok(
            RsData(
                "200",
                "찜한 축제 목록 조회 성공",
                myPageService.getMyBookMark(authentication.name, pageable)
            )
        )

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴", description = "회원은 자신의 상태를 withdraw로변경(탈퇴)를 진행할 수 있습니다.")
    fun selfWithdraw(
        @Valid @RequestBody req: WithdrawRequest,
        authentication: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<RsData<WithdrawResponse>> {

        if (req.password != req.passwordConfirm) {
            throw BadRequestException("비밀번호와 비밀번호 확인이 일치하지 않습니다.")
        }

        val res = authService.selfWithdraw(
            authentication.name,
            req.password!!,
            tokenCookieManager.resolveAccessToken(request)
        )

        // 탈퇴 후 브라우저에 남아있는 refresh token 쿠키도 제거.
        tokenCookieManager.clearRefreshTokenCookie(response)

        return ResponseEntity.ok(
            RsData(
                "200",
                "탈퇴처리가 성공적으로 수행되었습니다.",
                res
            )
        )
    }
}
