package com.example.domain.member.dto.request

import jakarta.validation.constraints.NotBlank

 // access token을 다시 발급받을 때 refresh token을 받는 요청 DTO입니다.
class TokenReissueRequest {
    @field:NotBlank(message = "refresh token을 입력해주세요.")
    val refreshToken: String = ""
}
