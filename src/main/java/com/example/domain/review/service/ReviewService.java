package com.example.domain.review.service;

import com.example.domain.admin.dto.AdminReviewBlindRes;
import com.example.domain.admin.dto.AdminReviewReportPageRes;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.dto.*;
import com.example.domain.review.entity.Review;
import com.example.domain.review.entity.ReviewStatus;
import com.example.domain.review.repository.ReviewRepository;
import com.example.domain.reviewlike.repository.ReviewLikeRepository;
import com.example.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final FestivalRepository festivalRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final FileStorageService fileStorageService;

    //리뷰 작성
    @Transactional
    public ReviewResponseDto createReview(Long festivalId, String loginId,
                                          ReviewCreateRequestDto requestDto,
                                          MultipartFile imageFile) { // 1. 파일 매개변수 추가

        // 1. 로그인한 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다."));

        // 2. 축제 존재 여부 확인
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new CustomNotFoundException("축제가 존재하지 않습니다."));

        // ++ 같은 회원이 같은 축제에 중복 리뷰 작성 불가
        if (reviewRepository.existsByMemberIdAndFestivalIdAndStatus(member.getId(), festivalId, ReviewStatus.ACTIVE)) {
            throw new ConflictException("이미 해당 축제에 리뷰를 작성했습니다.");
        }

        // 3. 이미지 파일 저장 로직 추가
        String savedImagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            // 이전에 만든 fileStorageService.storeFile(imageFile)를 호출합니다.
            savedImagePath = fileStorageService.storeFile(imageFile);
        }

        // 4. Review 객체 생성 (저장된 파일 경로 사용)
        Review review = Review.builder() // @Builder 사용 권장
                .member(member)
                .festival(festival)
                .content(requestDto.getContent())
                .image(savedImagePath) // 실제 저장된 파일명/경로
                .rating(requestDto.getRating())
                .build();

        Review savedReview = reviewRepository.save(review);

        return new ReviewResponseDto(savedReview);

    }


    //리뷰 목록조회
    public ReviewPageResponseDto getReviewList(Long festivalId, String loginId, int page, int size) {

        // 1. 로그인 체크
        if (loginId == null || loginId.equals("anonymousUser")) {
            throw new UnauthorizedException("리뷰 조회는 로그인 후 이용 가능합니다.");
        }


        // 2. 축제 존재 체크
        festivalRepository.findById(festivalId)
                .orElseThrow(() -> new CustomNotFoundException("존재하지 않는 축제입니다."));

        // 3. 리뷰 조회
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Review> reviewPage = reviewRepository.findByFestivalIdAndStatus(
                festivalId,
                ReviewStatus.ACTIVE,
                pageRequest);

        Member loginMember = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다."));

        return ReviewPageResponseDto.builder()
                .festivalId(festivalId)
                .content(reviewPage.getContent().stream()
                        .map(review -> {
                            boolean liked = reviewLikeRepository.existsByMemberIdAndReviewId(
                                    loginMember.getId(),
                                    review.getId()
                            );
                            return ReviewListResponseDto.from(review, liked);
                        })
                        .toList())
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .hasNext(reviewPage.hasNext())
                .build();
    }



    //리뷰 수정
    @Transactional
    public ReviewUpdateResponseDto updateReview(Long reviewId, String loginId, ReviewUpdateRequestDto requestDto, MultipartFile imageFile) {

        // 1. 로그인한 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다."));

        // 2. 리뷰 존재 여부 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomNotFoundException("존재하지 않는 리뷰입니다."));

        // 3. 작성자 본인 여부 확인
        if (!review.getMember().getId().equals(member.getId())) {
            throw new ForbiddenException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        // 4. 삭제된 리뷰 수정 불가
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new BadRequestException("삭제된 리뷰는 수정할 수 없습니다.");
        }

        // 5. 블라인드 리뷰 수정 불가
        if (review.getStatus() == ReviewStatus.BLIND) {
            throw new ForbiddenException("블라인드 처리된 리뷰는 수정할 수 없습니다.");
        }

        // 6. 평점 검증
        if (requestDto.getRating() < 1 || requestDto.getRating() > 5) {
            throw new BadRequestException("평점은 1점부터 5점까지 입력 가능합니다.");
        }


        String updateImagePath = review.getImage(); // 기본값은 '기존 이미지 유지'

        // 케이스 1: 클라이언트가 "기존 이미지를 삭제해달라"고 요청한 경우
        if (requestDto.isDeleteImage()) {
            if (updateImagePath != null) {
                fileStorageService.deleteFile(updateImagePath); // 실제 서버에서 파일 삭제
                updateImagePath = null; // DB에 들어갈 경로도 null로 비워줌
            }
        }
        // 케이스 2: 이미지 삭제 요청은 없지만, '새로운 이미지 파일'이 들어온 경우
        else if (imageFile != null && !imageFile.isEmpty()) {
            if (updateImagePath != null) {
                fileStorageService.deleteFile(updateImagePath); // 기존 파일이 있으면 덮어써야 하니 먼저 삭제
            }
            updateImagePath = fileStorageService.storeFile(imageFile); // 새 이미지를 저장하고 경로 업데이트
        }
        // 7. 리뷰 수정 로직 (위에서 결정된 updateImagePath 적용)
        review.updateReview(
                requestDto.getContent(),
                updateImagePath,
                requestDto.getRating()
        );

        // 8. 평균 평점 재계산
        Festival festival = review.getFestival();
        Double averageRating = reviewRepository.calculateAverageRatingByFestivalId(festival.getId());
        festival.updateAverageRating(averageRating == null ? 0.0 : averageRating);

        return ReviewUpdateResponseDto.from(review);
    }

    //리뷰 삭제
    @Transactional
    public ReviewDeleteResponseDto deleteReview(Long reviewId, String loginId) {



        // 2. 로그인한 회원 조회
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다."));

        // 3. 리뷰 존재 여부 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomNotFoundException("존재하지 않는 리뷰입니다."));

        // 4. 작성자 본인 여부 확인
        if (!review.getMember().getId().equals(member.getId())) {
            throw new ForbiddenException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        // 5. 이미 삭제된 리뷰인지 확인
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new BadRequestException("이미 삭제된 리뷰입니다.");
        }

        // 6. 블라인드 리뷰 삭제 불가
        if (review.getStatus() == ReviewStatus.BLIND) {
            throw new ForbiddenException("블라인드 처리된 리뷰는 삭제할 수 없습니다.");
        }

        // 7. 리뷰 논리 삭제
        review.deleteReview();

        // 8. 축제 평균 평점 재계산
        Festival festival = review.getFestival();
        Double averageRating = reviewRepository.calculateAverageRatingByFestivalId(festival.getId());
        festival.updateAverageRating(averageRating == null ? 0.0 : averageRating);

        return ReviewDeleteResponseDto.from(review);
    }




    // 신고횟수가 5이상인 review리스트를 DTO로 반환하여 주는 함수
    public AdminReviewReportPageRes getReportReview(Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAllByReportCountGreaterThanEqualAndStatus(5,ReviewStatus.ACTIVE,pageable);
        return AdminReviewReportPageRes.from(reviews);
    }

    //리뷰를 검토하여 블라인드처리, 신고횟수 초기화하는 함수
    @Transactional
    public AdminReviewBlindRes processReviewAction(Long reviewId, String action) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(()->new CustomNotFoundException("404","존재하지 않는 리뷰입니다."));//추후 변경 예정
        if(review.getStatus()==ReviewStatus.DELETED){
            throw new BadRequestException("삭제된 리뷰는 상태를 변경할 수 없습니다.");
        }
        int updatedCount =0 ;

        if ("BLIND".equalsIgnoreCase(action)) {
            updatedCount= reviewRepository.updateStatusToBlindActive(reviewId);
            if(updatedCount>0){
                Member author = review.getMember();
                if(author!=null){
                    memberRepository.incrementReportCount(author.getId());
                }
                review.reviewBlind();
            }
        }
        else if ("DISMISS".equalsIgnoreCase(action)) {
            updatedCount = reviewRepository.resetReportCountIfActive(reviewId);
            if(updatedCount>0){
                review.reportCountReset();
            }
        }
        else {
            throw new IllegalArgumentException("허용되지 않은 리뷰 상태입니다.: " + action);
        }
        if(updatedCount==0){
            throw new ConflictException("이미 다른 관리자가 처리한 리뷰입니다.");
        }
        return new AdminReviewBlindRes(
                review.getId(),
                review.getStatus(),
                review.getReportCount()
        );


    }
}

