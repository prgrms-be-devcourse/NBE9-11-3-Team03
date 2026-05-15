package com.example.domain.bookmark.controller

import com.example.domain.bookmark.dto.response.FestivalBookmarkResponse
import com.example.domain.bookmark.service.FestivalBookmarkService
import com.example.global.response.ApiRes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "FestivalBookmark", description = "축제 찜 API")
class FestivalBookmarkController(
    private val festivalBookmarkService: FestivalBookmarkService
) {

    @PostMapping("/festivals/{festivalId}/bookmark")
    @Operation(summary = "축제 찜(북마크) 수행", description = "특정 축제를 찜 처리(북마크) 합니다.")
    fun bookmarkFestival(
        @PathVariable festivalId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiRes<FestivalBookmarkResponse>> =
        ResponseEntity.ok(
            ApiRes(
                200,
                "축제 찜 되었습니다.",
                festivalBookmarkService.bookmarkFestival(festivalId, authentication.name)
            )
        )

    @DeleteMapping("/festivals/{festivalId}/bookmark")
    @Operation(summary = "축제 찜(북마크) 취소", description = "특정 축제의 찜 처리(북마크)를 취소합니다.")
    fun cancelBookmark(
        @PathVariable festivalId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiRes<FestivalBookmarkResponse>> =
        ResponseEntity.ok(
            ApiRes(
                200,
                "축제 찜이 취소되었습니다.",
                festivalBookmarkService.cancelBookmark(festivalId, authentication.name)
            )
        )
}