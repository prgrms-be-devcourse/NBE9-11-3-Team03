package com.example.domain.member.service;

import com.example.domain.admin.dto.AdminMemberWithdrawnRes;
import com.example.domain.admin.dto.MemberPageResponse;
import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import com.example.domain.member.repository.MemberRepository;
import com.example.domain.review.repository.ReviewRepository;
import com.example.global.exception.CustomNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    //저장된 모든 회원정보를 페이징하여 조회하는 함수
    public MemberPageResponse getAllMembers(Pageable pageable) {
        return MemberPageResponse.from(memberRepository.findAll(pageable));
    }
    //저장된 회원중 신고횟수가 5이상이며, 활동중인 회원 조회하는 함수
    public MemberPageResponse getReportMembers(Pageable pageable) {
        Page<Member> memberPage = memberRepository.findAllByReportCountGreaterThanEqualAndStatus(5,MemberStatus.ACTIVE,pageable);
        return MemberPageResponse.from(memberPage);
    }

    //회원을 강제 탈퇴처리하는 메서드
    @Transactional
    public AdminMemberWithdrawnRes memberWithdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new CustomNotFoundException("404","존재하지 않는 회원입니다."));
        if(member.getStatus()==MemberStatus.WITHDRAWN){
            throw new IllegalArgumentException("이미 탈퇴 처리된 회원입니다.");
        }
        member.withdraw();
        return AdminMemberWithdrawnRes.from(member);
    }

}
