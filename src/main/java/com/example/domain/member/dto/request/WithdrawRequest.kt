package com.example.domain.member.dto.request

import jakarta.validation.constraints.NotBlank

@JvmRecord
data class WithdrawRequest(
    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String?,

    @field:NotBlank(message = "비밀번호 확인을 입력해주세요.")
    val passwordConfirm: String?
)
