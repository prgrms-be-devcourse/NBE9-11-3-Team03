package com.example.global.scheduler;

import com.example.domain.member.repository.AccessTokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistCleanupScheduler {

    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredBlacklistedTokens() {
        // 만료된 access token 차단 기록은 더 이상 필요 없으므로 삭제함.
        accessTokenBlacklistRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
