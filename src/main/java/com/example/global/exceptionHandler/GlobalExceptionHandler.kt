package com.example.global.exceptionHandler

import com.example.global.exception.*
import com.example.global.rsData.RsData
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException.TooManyRequests
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    //400 Bad Request (잘못된 요청값, 비즈니스상 잘못된 인자)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<RsData<Void>> =
        ResponseEntity.badRequest().body(RsData.fail(e.message))

    // 400 Bad Request (비즈니스 로직상 잘못된 요청)
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(e: BadRequestException): ResponseEntity<RsData<Void>> =
        ResponseEntity.badRequest().body(RsData.fail(e.message))

    // 404 Not Found (요청한 데이터를 찾을 수 없을 때 사용합니다.)
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(RsData<Void>("404", e.message, null))

    // 401 Unauthorized (로그인이 필요한데 회원 정보를 확인할 수 없을 때 사용합니다.)
    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(e: UnauthorizedException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RsData<Void>("401", e.message, null))

    // 403 Forbidden (로그인은 했지만 해당 작업 권한이 없을 때 사용합니다.)
    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(e: ForbiddenException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(RsData<Void>("403", e.message, null))

    // 409 Conflict (이미 신고한 리뷰처럼 같은 요청이 중복될 때 사용합니다.)
    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(e: ConflictException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(RsData<Void>("409", e.message, null))

    // 400 Bad Request (@Valid 검증 실패 시 사용합니다.)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
        return ResponseEntity.badRequest().body(RsData.fail(message))
    }

    // 400 Bad Request (@RequestParam, @PathVariable 등의 제약조건 검증 실패)
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<RsData<Void>> =
        ResponseEntity.badRequest().body(RsData.fail(e.message))

    //400 Bad Request (JSON 형식 오류, 잘못된 요청 본문)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<RsData<Void>> =
        ResponseEntity.badRequest().body(RsData.fail("잘못된 요청 본문입니다."))

    // 400 Bad Request(필수 요청 파라미터 누락)
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        e: MissingServletRequestParameterException
    ): ResponseEntity<RsData<Void>> =
        ResponseEntity.badRequest().body(RsData.fail("필수 요청 파라미터가 누락되었습니다."))

    //400 Bad Request (@RequestParam, @PathVariable 타입 변환 실패)
    //클라이언트가 int, long 등의 숫자 파라미터에 문자열 등 잘못된 값을 전달했을 때 발생
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException
    ): ResponseEntity<RsData<Void>> =
        ResponseEntity.badRequest().body(RsData.fail("요청 파라미터 타입이 올바르지 않습니다."))

    // 400 Bad Request (파라미터 타입 불일치 - 예: 숫자에 문자 입력, Enum 불일치)
    @ExceptionHandler(TypeMismatchException::class)
    fun handleTypeMismatchException(e: TypeMismatchException): ResponseEntity<RsData<Void>> {
        val invalidValue = e.value?.toString() ?: "null"

        return ResponseEntity.badRequest().body(RsData.fail("잘못된 파라미터 값입니다. 입력값: [$invalidValue]"))
    }

    // 404 Not Found (정상 요청이지만 대상 리소스를 찾을 수 없음)
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(e: NoSuchElementException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(RsData("404", e.message, null))

    //404 존재하지않는 엔티티일시
    @ExceptionHandler(CustomNotFoundException::class)
    fun handleCustomNotFoundException(e: CustomNotFoundException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(RsData(e.status, e.message, null))

    //405 Method Not Allowed (지원하지 않는 HTTP 메서드 호출)
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(
        e: HttpRequestMethodNotSupportedException
    ): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(RsData("405", "지원하지 않는 HTTP 메서드입니다.", null))

    // 409 Conflict (데이터 중복)
    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResourceException(e: DuplicateResourceException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(RsData(e.statusCode, e.message, null))

    // 409 Conflict (데이터베이스 제약 조건 위반 - 예: 중복 키, 외래키 위반)
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(RsData("409", "데이터 무결성 위반이 발생했습니다.", null))

    //413에러 사진용량초과
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSizeException(e: MaxUploadSizeExceededException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(RsData("413", "사진용량이 초과되었습니다.5MB미만으로 다시올려주세요", null))

    //429 Too Many Requests (외부 API 호출 한도 초과)
    @ExceptionHandler(TooManyRequests::class)
    fun handleTooManyRequests(e: TooManyRequests): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(RsData("429", "외부 API 호출 한도를 초과로 인해 동기화가 중단되었습니다.", null))


    // 보안 필터가 가로채기 전에 500 에러와 진짜 이유를 먼저 응답함
    @ExceptionHandler(InvalidDataAccessApiUsageException::class)
    fun handleJpaError(e: InvalidDataAccessApiUsageException): ResponseEntity<RsData<Void>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RsData("500", "서버 쿼리 오류: " + e.message, null))


    //502 Bad Gateway (외부 API 서버 오류)
    @ExceptionHandler(HttpServerErrorException::class)
    fun handleHttpServerErrorException(e: HttpServerErrorException): ResponseEntity<RsData<Void>> {
        val statusCode = e.statusCode.value()
        val message = if (statusCode in setOf(502, 503, 504)) {
            "외부 API 서버 응답이 불안정하여 동기화에 실패했습니다."
        } else {
            "외부 API 서버 오류가 발생했습니다."
        }

        return ResponseEntity.status(e.statusCode).body(RsData(statusCode.toString(), message, null))
    }

    //처리되지 않은 예외의 최종 방어 (500 Internal Server Error)
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<RsData<Void>> {
        log.error("[500] 서버 내부 오류 발생 - message={}", e.message, e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RsData("500", "서버 오류가 발생했습니다.", null))
    }

}
