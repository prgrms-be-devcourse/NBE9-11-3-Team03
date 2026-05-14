package com.example.domain.bookmark.controller;

import com.example.domain.bookmark.dto.FestivalBookmarkResponseDto;
import com.example.domain.bookmark.service.FestivalBookmarkService;
import com.example.global.response.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "FestivalBookmark", description = "축제 찜 API")
public class FestivalBookmarkController {

    private final FestivalBookmarkService festivalBookmarkService;

    @PostMapping("/festivals/{festivalId}/bookmark")
    @Operation(summary = "축제 찜(북마크) 수행", description = "특정 축제를 찜 처리(북마크) 합니다.")
    public ResponseEntity<ApiRes<FestivalBookmarkResponseDto>> bookmarkFestival(
            @PathVariable Long festivalId,
            Authentication authentication
    ) {
        String loginId = authentication.getName();

        FestivalBookmarkResponseDto response =
                festivalBookmarkService.bookmarkFestival(festivalId, loginId);

        return ResponseEntity.ok(
                new ApiRes<>(200, "축제 찜 되었습니다.", response)
        );
    }

    @DeleteMapping("/festivals/{festivalId}/bookmark")
    @Operation(summary = "축제 찜(북마크) 취소", description = "특정 축제의 찜 처리(북마크)를 취소합니다.")
    public ResponseEntity<ApiRes<FestivalBookmarkResponseDto>> cancelBookmark(
            @PathVariable Long festivalId,
            Authentication authentication
    ) {
        String loginId = authentication.getName();

        FestivalBookmarkResponseDto response =
                festivalBookmarkService.cancelBookmark(festivalId, loginId);

        return ResponseEntity.ok(
                new ApiRes<>(200, "축제 찜이 취소되었습니다.", response)
        );
    }
}