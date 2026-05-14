package com.example.domain.festival.entity;

// 상세 보강 재처리 대상이 된 사유를 구분
public enum DetailSyncPendingReason {
    RATE_LIMIT,     // 429로 인해 실패 또는 미시도
    SERVER_ERROR,   // 5xx 오류
    EXCEPTION,      // 기타 예외
    UNPROCESSED     // 429 중단 이후 미시도 대상
}