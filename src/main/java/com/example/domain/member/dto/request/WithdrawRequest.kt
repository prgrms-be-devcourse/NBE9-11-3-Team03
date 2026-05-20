package com.example.domain.member.dto.request

import jakarta.validation.constraints.NotBlank

data class WithdrawRequest(
    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String, // String? -> String 으로 변경

    @field:NotBlank(message = "비밀번호 확인을 입력해주세요.")
    val passwordConfirm: String // String? -> String 으로 변경
)
