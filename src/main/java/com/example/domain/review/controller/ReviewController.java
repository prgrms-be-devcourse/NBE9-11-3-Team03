package com.example.domain.review.controller;

import com.example.domain.review.dto.*;
import com.example.domain.review.service.ReviewService;
import com.example.global.response.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Review", description = "리뷰 API")
public class ReviewController {

    private final ReviewService reviewService;



    @PostMapping(value = "/festivals/{festivalId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "축제 리뷰 작성", description = "특정 축제에 리뷰와 사진을 함께 작성합니다.")
    public ResponseEntity<ApiRes<ReviewResponseDto>> createReview(
            @PathVariable Long festivalId,
            @Valid @RequestPart("requestDto") ReviewCreateRequestDto requestDto, // @RequestBody 대신 @RequestPart 사용
            @RequestPart(value = "image", required = false) MultipartFile image, // 이미지 파일 추가
            Authentication authentication
    ){
        String loginId = authentication.getName();

        // 서비스 메서드에 image 파일 추가 전달
        ReviewResponseDto response = reviewService.createReview(festivalId, loginId, requestDto, image);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiRes<>(201, "리뷰 작성이 완료 되었습니다.", response));
    }


    @GetMapping("/festivals/{festivalId}/reviews")
    @Operation(summary = "축제 리뷰 목록 조회", description = "특정 축제의 리뷰 목록을 페이징하여 조회합니다.")
    public ResponseEntity<ApiRes<ReviewPageResponseDto>> getReviewList(
            @PathVariable Long festivalId,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        String loginId = authentication.getName();

        ReviewPageResponseDto response = reviewService.getReviewList(festivalId, loginId, page, size);

        return ResponseEntity.ok(
                new ApiRes<>(200, "축제 리뷰 목록 조회 성공", response)
        );
    }

    //리뷰수정
    @PatchMapping(value = "/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "축제 리뷰 수정", description = "본인이 작성한 리뷰를 수정합니다.")
    public ResponseEntity<ApiRes<ReviewUpdateResponseDto>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestPart("requestDto") ReviewUpdateRequestDto requestDto,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication
    ){
        String loginId = authentication.getName();
        // 서비스에 image 파일도 같이 넘겨줍니다.
        ReviewUpdateResponseDto response = reviewService.updateReview(reviewId, loginId, requestDto, image);
        return ResponseEntity.ok(new ApiRes<>(200, "리뷰 수정 완료", response));
    }


    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "축제 리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다.")
    public ResponseEntity<ApiRes<ReviewDeleteResponseDto>> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        String loginId = authentication.getName();

        ReviewDeleteResponseDto response = reviewService.deleteReview(reviewId, loginId);

        return ResponseEntity.ok(
                new ApiRes<>(200, "리뷰 삭제가 완료되었습니다.", response)
        );
    }


}
