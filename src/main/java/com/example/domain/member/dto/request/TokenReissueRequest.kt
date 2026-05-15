package com.example.domain.member.dto.request

import jakarta.validation.constraints.NotBlank

@JvmRecord
data class TokenReissueRequest(
    @field:NotBlank(message = "refresh token을 입력해주세요.")
    val refreshToken: String?
)
