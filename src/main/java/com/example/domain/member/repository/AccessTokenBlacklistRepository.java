package com.example.domain.member.repository;

import com.example.domain.member.entity.AccessTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AccessTokenBlacklistRepository extends JpaRepository<AccessTokenBlacklist, Long> {

    boolean existsByToken(String token);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
