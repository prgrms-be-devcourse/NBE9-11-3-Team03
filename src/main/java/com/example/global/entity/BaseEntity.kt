package com.example.global.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long =0
        protected set

    // 엔티티가 처음 저장될 때의 시간
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    // 엔티티가 수정될 때마다 갱신되는 시간
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set
}
