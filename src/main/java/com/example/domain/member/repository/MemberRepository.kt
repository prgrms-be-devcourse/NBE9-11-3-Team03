package com.example.domain.member.repository;

import com.example.domain.member.entity.Member;
import com.example.domain.member.entity.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Member 엔티티에 대한 조회/저장 책임을 가지는 Repository다.
// Spring Data JPA가 메서드 이름을 해석해서 필요한 쿼리를 자동으로 만들어준다.
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 관리자 페이지에서 신고 누적 횟수가 기준 이상이고,
    // 특정 상태를 가진 회원만 페이지 단위로 조회할 때 사용한다.
    Page<Member> findAllByReportCountGreaterThanEqualAndStatus(
            int reportCount,
            MemberStatus status,
            Pageable pageable
    );

    // loginId로 회원 한 명을 조회한다.
    // 로그인 시도 시 회원 존재 여부를 확인할 때 사용한다.
    Optional<Member> findByLoginId(String loginId);

    // 회원가입 시 loginId 중복 여부를 확인한다.
    boolean existsByLoginId(String loginId);

    // 회원가입 시 email 중복 여부를 확인한다.
    boolean existsByEmail(String email);

    // 회원가입 시 nickname 중복 여부를 확인한다.
    boolean existsByNickname(String nickname);

    //DB에서 직접 신고횟수를 1 증가시키는 로직
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.reportCount = m.reportCount + 1 WHERE m.id = :id")
    void incrementReportCount(@Param("id") Long id);

}
