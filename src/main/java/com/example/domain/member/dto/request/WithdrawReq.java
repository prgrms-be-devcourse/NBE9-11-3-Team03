package com.example.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WithdrawReq(
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,
        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String passwordConfirm
) {
}
