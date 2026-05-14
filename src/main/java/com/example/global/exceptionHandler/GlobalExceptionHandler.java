package com.example.global.exceptionHandler;

import com.example.global.exception.*;
import com.example.global.rsData.RsData;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //400 Bad Request (잘못된 요청값, 비즈니스상 잘못된 인자)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<com.example.global.rsData.RsData<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
                .badRequest()
                .body(RsData.fail(e.getMessage()));
    }

    // 400 Bad Request (비즈니스 로직상 잘못된 요청)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<RsData<Void>> handleBadRequestException(BadRequestException e) {
        return ResponseEntity
                .badRequest()
                .body(RsData.fail(e.getMessage()));
    }

    // 404 Not Found (요청한 데이터를 찾을 수 없을 때 사용합니다.)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<RsData<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new RsData<>("404", e.getMessage(), null));
    }

    // 401 Unauthorized (로그인이 필요한데 회원 정보를 확인할 수 없을 때 사용합니다.)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<RsData<Void>> handleUnauthorizedException(UnauthorizedException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new RsData<>("401", e.getMessage(), null));
    }


    // 403 Forbidden (로그인은 했지만 해당 작업 권한이 없을 때 사용합니다.)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<RsData<Void>> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new RsData<>("403", e.getMessage(), null));
    }

    // 409 Conflict (이미 신고한 리뷰처럼 같은 요청이 중복될 때 사용합니다.)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<RsData<Void>> handleConflictException(ConflictException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new RsData<>("409", e.getMessage(), null));
    }

    // 400 Bad Request (@Valid 검증 실패 시 사용합니다.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .get(0) // 첫 번째 에러 메시지 하나만 가져오기
                .getDefaultMessage();

        return ResponseEntity
                .badRequest()
                .body(RsData.fail(message));
    }

    // 400 Bad Request (@RequestParam, @PathVariable 등의 제약조건 검증 실패)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RsData<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        return ResponseEntity
                .badRequest()
                .body(RsData.fail(e.getMessage()));
    }

    //400 Bad Request (JSON 형식 오류, 잘못된 요청 본문)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity
                .badRequest()
                .body(RsData.fail("잘못된 요청 본문입니다."));
    }

    // 400 Bad Request(필수 요청 파라미터 누락)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RsData<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        String message = "필수 요청 파라미터가 누락되었습니다.";

        return ResponseEntity
                .badRequest()
                .body(RsData.fail(message));
    }

    //400 Bad Request (@RequestParam, @PathVariable 타입 변환 실패)
    //클라이언트가 int, long 등의 숫자 파라미터에 문자열 등 잘못된 값을 전달했을 때 발생
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RsData<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        String message = "요청 파라미터 타입이 올바르지 않습니다.";

        return ResponseEntity
                .badRequest()
                .body(RsData.fail(message));
    }

    // 400 Bad Request (파라미터 타입 불일치 - 예: 숫자에 문자 입력, Enum 불일치)
    @ExceptionHandler(org.springframework.beans.TypeMismatchException.class)
    public ResponseEntity<RsData<Void>> handleTypeMismatchException(org.springframework.beans.TypeMismatchException e) {
        String invalidValue = e.getValue() != null ? e.getValue().toString() : "null";
        String message = String.format("잘못된 파라미터 값입니다. 입력값: [%s]", invalidValue);
        return ResponseEntity
                .badRequest()
                .body(RsData.fail(message));
    }

    // 404 Not Found (정상 요청이지만 대상 리소스를 찾을 수 없음)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handleNoSuchElementException(NoSuchElementException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new RsData<>("404", e.getMessage(), null));
    }

    //404 존재하지않는 엔티티일시
    @ExceptionHandler(CustomNotFoundException.class)
    public ResponseEntity<RsData<Void>> handleCustomNotFoundException(CustomNotFoundException e){
        RsData res = new RsData<>(e.getStatus(),e.getMessage());
        return new ResponseEntity<>(res,HttpStatus.NOT_FOUND);
    }

    //405 Method Not Allowed (지원하지 않는 HTTP 메서드 호출)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RsData<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new RsData<>("405", "지원하지 않는 HTTP 메서드입니다.", null));
    }

    // 409 Conflict (데이터 중복)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<RsData<Void>> handleDuplicateResourceException(DuplicateResourceException e) {
        RsData res = new RsData<>(e.getStatusCode(),e.getMessage());
        return new ResponseEntity<>(res,HttpStatus.CONFLICT);
    }

    // 409 Conflict (데이터베이스 제약 조건 위반 - 예: 중복 키, 외래키 위반)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<RsData<Void>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new RsData<>("409", "데이터 무결성 위반이 발생했습니다.", null));
    }

    //처리되지 않은 예외의 최종 방어 (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleException(Exception e) {
        e.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RsData<>("500", "서버 오류가 발생했습니다.", null));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
    //429 Too Many Requests (외부 API 호출 한도 초과)
    @ExceptionHandler(HttpClientErrorException.TooManyRequests.class)
    public ResponseEntity<RsData<Void>> handleTooManyRequests(HttpClientErrorException.TooManyRequests e) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new RsData<>("429", "외부 API 호출 한도를 초과로 인해 동기화가 중단되었습니다.", null));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<RsData<Void>> handleJpaError(InvalidDataAccessApiUsageException e) {
        // 보안 필터가 가로채기 전에 500 에러와 진짜 이유를 먼저 응답함
        return ResponseEntity.status(500).body(
                new RsData<>("500", "서버 쿼리 오류: " + e.getMessage(), null)
        );
    }

    //502 Bad Gateway (외부 API 서버 오류)
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<RsData<Void>> handleHttpServerErrorException(HttpServerErrorException e) {
        int statusCode = e.getStatusCode().value();

        if (statusCode == 502 || statusCode == 503 || statusCode == 504) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(new RsData<>(String.valueOf(statusCode), "외부 API 서버 응답이 불안정하여 동기화에 실패했습니다.", null));
        }

        return ResponseEntity
                .status(e.getStatusCode())
                .body(new RsData<>(
                        String.valueOf(statusCode),
                        "외부 API 서버 오류가 발생했습니다.",
                        null
                ));
    }
}
