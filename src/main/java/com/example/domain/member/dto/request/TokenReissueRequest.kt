package com.example.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// access token을 다시 발급받을 때 refresh token을 받는 요청 DTO입니다.
public class TokenReissueRequest {

    @NotBlank(message = "refresh token을 입력해주세요.")
    private String refreshToken;
}
