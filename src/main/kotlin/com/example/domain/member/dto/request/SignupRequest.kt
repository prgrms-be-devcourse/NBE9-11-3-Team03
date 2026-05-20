package com.example.domain.member.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SignupRequest(
    @field:NotBlank(message = "이름을 입력해주세요.")
    val userName: String,

    @field:NotBlank(message = "아이디를 입력해주세요.")
    val loginId: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String,

    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:NotBlank(message = "이메일을 입력해주세요.")
    val email: String,

    @field:NotBlank(message = "닉네임을 입력해주세요.")
    val nickname: String
)
