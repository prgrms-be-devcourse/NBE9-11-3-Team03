package com.example.domain.member.dto.request

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "아이디를 입력해주세요.")
    val loginId: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String
)