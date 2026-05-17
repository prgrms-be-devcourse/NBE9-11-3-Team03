package com.example.domain.member.dto.response;

import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.member.entity.Role;
import lombok.Getter;

@Getter
// 회원가입 성공 시 클라이언트에 내려줄 데이터를 담는 DTO다.
public class SignupResponse {

    private final Long memberId;
    private final String loginId;
    private final String nickname;
    private final Role role;
    private final MemberStatus status;

    private SignupResponse(
            Long memberId,
            String loginId,
            String nickname,
            Role role,
            MemberStatus status
    ) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    // 저장된 Member 엔티티를 회원가입 응답 DTO로 변환한다.
    public static SignupResponse from(Member member) {
        return new SignupResponse(
                member.getId(),
                member.getLoginId(),
                member.getNickname(),
                member.getRole(),
                member.getStatus()
        );
    }
}
