package com.example.domain.review.service

import com.example.domain.admin.dto.response.AdminReviewBlindResponse
import com.example.domain.admin.dto.response.AdminReviewReportPageResponse
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.repository.MemberRepository
import com.example.domain.review.dto.request.ReviewCreateRequest
import com.example.domain.review.dto.request.ReviewUpdateRequest
import com.example.domain.review.dto.response.*
import com.example.domain.review.entity.Review
import com.example.domain.review.entity.ReviewStatus
import com.example.domain.review.repository.ReviewRepository
import com.example.domain.reviewlike.repository.ReviewLikeRepository
import com.example.global.exception.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val memberRepository: MemberRepository,
    private val festivalRepository: FestivalRepository,
    private val reviewLikeRepository: ReviewLikeRepository,
    private val fileStorageService: FileStorageService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 리뷰 작성
    @Transactional
    fun createReview(
        festivalId: Long,
        loginId: String,
        requestDto: ReviewCreateRequest,
        imageFile: MultipartFile?
    ): ReviewResponse {

        // 1. 로그인한 회원 조회
        val member = memberRepository.findByLoginId(loginId)
            ?: throw UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.")

        // 2. 축제 존재 여부 확인
        val festival = festivalRepository.findByIdOrNull(festivalId)
            ?: throw CustomNotFoundException("축제가 존재하지 않습니다.")

        // ++ 같은 회원이 같은 축제에 중복 리뷰 작성 불가
        if (reviewRepository.existsByMemberIdAndFestivalIdAndStatus(member.id, festivalId, ReviewStatus.ACTIVE)) {
            throw ConflictException("이미 해당 축제에 리뷰를 작성했습니다.")
        }

        // 3. 이미지 파일 저장 로직 추가
        val savedImagePath = imageFile
            ?.takeIf { !it.isEmpty }
            ?.let { fileStorageService.storeFile(it) }

        // 4. Review 객체 생성 (빌더 대신 코틀린 주 생성자 사용 + String? 예외 처리)
        val review = Review(
            member = member,
            festival = festival,
            content = requestDto.content ?: throw BadRequestException("리뷰 내용이 누락되었습니다."),
            image = savedImagePath,
            rating = requestDto.rating ?: throw BadRequestException("평점이 누락되었습니다.")
        )

        val savedReview = reviewRepository.save(review)

        return ReviewResponse(savedReview)
    }

    // 리뷰 목록조회
    fun getReviewList(festivalId: Long, loginId: String?, page: Int, size: Int): ReviewPageResponse {
        // 1. 로그인 체크
        if (loginId.isNullOrBlank() || loginId == "anonymousUser") {
            throw UnauthorizedException("리뷰 조회는 로그인 후 이용 가능합니다.")
        }

        // 2. 축제 존재 체크
        festivalRepository.findByIdOrNull(festivalId)
            ?: throw CustomNotFoundException("존재하지 않는 축제입니다.")

        // 3. 리뷰 조회
        val pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val reviewPage = reviewRepository.findByFestivalIdAndStatus(
            festivalId,
            ReviewStatus.ACTIVE,
            pageRequest
        )

        val loginMember = memberRepository.findByLoginId(loginId)
            ?: throw UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.")

        return ReviewPageResponse(
            festivalId,
            reviewPage.content.map { review ->
                val liked = reviewLikeRepository.existsByMemberIdAndReviewId(loginMember.id, review.id)
                ReviewListResponse.from(review, liked)
            },
            reviewPage.number,
            reviewPage.size,
            reviewPage.totalElements,
            reviewPage.totalPages,
            reviewPage.hasNext()
        )
    }

    // 리뷰 수정
    @Transactional
    fun updateReview(
        reviewId: Long,
        loginId: String?,
        requestDto: ReviewUpdateRequest,
        imageFile: MultipartFile?
    ): ReviewUpdateResponse {
        // 1. 로그인한 회원 조회
        if (loginId == null) throw UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.")
        val member = memberRepository.findByLoginId(loginId)
            ?: throw UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.")

        // 2. 리뷰 존재 여부 확인
        val review = reviewRepository.findByIdOrNull(reviewId)
            ?: throw CustomNotFoundException("존재하지 않는 리뷰입니다.")

        // 3. 작성자 본인 여부 확인
        if (review.member.id != member.id) {
            throw ForbiddenException("본인이 작성한 리뷰만 수정할 수 있습니다.")
        }

        // 4. 삭제된 리뷰 수정 불가
        // 5. 블라인드 리뷰 수정 불가
        when (review.status) {
            ReviewStatus.DELETED -> throw BadRequestException("삭제된 리뷰는 수정할 수 없습니다.")
            ReviewStatus.BLIND -> throw ForbiddenException("블라인드 처리된 리뷰는 수정할 수 없습니다.")
            else -> {}
        }

        // 6. 평점 검증
        val rating = requestDto.rating ?: throw BadRequestException("평점이 누락되었습니다.")
        if (rating !in 1..5) {
            throw BadRequestException("평점은 1점부터 5점까지 입력 가능합니다.")
        }

        var updateImagePath = review.image

        // 케이스 1: 클라이언트가 "기존 이미지를 삭제해달라"고 요청한 경우
        if (requestDto.isDeleteImage) {
            updateImagePath?.let { fileStorageService.deleteFile(it) }
            updateImagePath = null
        } else if (imageFile?.isEmpty == false) {
            updateImagePath?.let { fileStorageService.deleteFile(it) }
            updateImagePath = fileStorageService.storeFile(imageFile)
        }

        // 7. 리뷰 수정 로직 (String? 예외 처리)
        review.updateReview(
            content = requestDto.content ?: throw BadRequestException("리뷰 내용이 누락되었습니다."),
            image = updateImagePath,
            rating = rating
        )

        // 8. 평균 평점 재계산
        val festival = review.festival
        val averageRating = reviewRepository.calculateAverageRatingByFestivalId(festival.id) ?: 0.0
        festival.updateAverageRating(averageRating)

        return ReviewUpdateResponse.from(review)
    }

    // 리뷰 삭제
    @Transactional
    fun deleteReview(reviewId: Long, loginId: String?): ReviewDeleteResponse {
        // 2. 로그인한 회원 조회
        if (loginId == null) throw UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.")
        val member = memberRepository.findByLoginId(loginId)
            ?: throw UnauthorizedException("로그인한 회원 정보를 찾을 수 없습니다.")

        // 3. 리뷰 존재 여부 확인
        val review = reviewRepository.findByIdOrNull(reviewId)
            ?: throw CustomNotFoundException("존재하지 않는 리뷰입니다.")

        // 4. 작성자 본인 여부 확인
        if (review.member.id != member.id) {
            throw ForbiddenException("본인이 작성한 리뷰만 삭제할 수 있습니다.")
        }

        // 5. 이미 삭제된 리뷰인지 확인
        // 6. 블라인드 리뷰 삭제 불가
        when (review.status) {
            ReviewStatus.DELETED -> throw BadRequestException("이미 삭제된 리뷰입니다.")
            ReviewStatus.BLIND -> throw ForbiddenException("블라인드 처리된 리뷰는 삭제할 수 없습니다.")
            else -> {}
        }

        // 7. 리뷰 논리 삭제
        review.deleteReview()

        // 8. 축제 평균 평점 재계산
        val festival = review.festival
        val averageRating = reviewRepository.calculateAverageRatingByFestivalId(festival.id) ?: 0.0
        festival.updateAverageRating(averageRating)

        return ReviewDeleteResponse.from(review)
    }

    // 신고횟수가 5이상인 review리스트를 DTO로 반환하여 주는 함수 (Pageable? -> Pageable 수정)
    fun getReportReview(pageable: Pageable): AdminReviewReportPageResponse {
        val reviews = reviewRepository.findAllByReportCountGreaterThanEqualAndStatus(5, ReviewStatus.ACTIVE, pageable)
        return AdminReviewReportPageResponse.from(reviews)
    }

    // 리뷰를 검토하여 블라인드처리, 신고횟수 초기화하는 함수 (action 파라미터 String? -> String 수정)
    @Transactional
    fun processReviewAction(reviewId: Long, action: String): AdminReviewBlindResponse {
        // 1. 사전 검증을 위한 최초 조회
        val review = reviewRepository.findByIdOrNull(reviewId)
            ?: throw CustomNotFoundException("404", "존재하지 않는 리뷰입니다.")

        if (review.status == ReviewStatus.DELETED) {
            throw BadRequestException("삭제된 리뷰는 상태를 변경할 수 없습니다.")
        }

        // 연관 엔티티의 ID를 미리 확보 (영속성 컨텍스트 격리 안전성 확보)
        val memberId = review.member.id
        val actionType = action.uppercase()

        // 2. 원자적 업데이트 실행
        val updatedCount = when (actionType) {
            "BLIND" -> {
                val count = reviewRepository.updateStatusToBlindActive(reviewId)
                if (count > 0) {
                    // 작성자의 신고 횟수 증가 (원자적 쿼리 사용 권장)
                    memberRepository.incrementReportCount(memberId)
                }
                count
            }
            "DISMISS" -> {
                reviewRepository.resetReportCountIfActive(reviewId)
            }
            else -> throw IllegalArgumentException("허용되지 않은 리뷰 상태입니다.: $action")
        }

        // 3. 동시성 충돌 또는 처리 실패 예외 처리
        if (updatedCount == 0) {
            log.warn(
                "[ADMIN] 리뷰 상태 변경 실패(이미 처리된 리뷰 또는 동시성 충돌) - reviewId={}, action={}",
                reviewId,
                action
            )
            throw ConflictException("이미 다른 관리자가 처리한 리뷰입니다.")
        }

        // 4. 영속성 컨텍스트와 DB 상태 동기화
        val updatedReview = reviewRepository.findByIdOrNull(reviewId)
            ?: throw CustomNotFoundException("404", "존재하지 않는 리뷰입니다.")

        return AdminReviewBlindResponse(
            reviewId = updatedReview.id,
            status = updatedReview.status,
            reportCount = updatedReview.reportCount
        )
    }
}