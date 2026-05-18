package com.example.global.init

import com.example.domain.festival.entity.Festival
import com.example.domain.festival.repository.FestivalRepository
import com.example.domain.member.entity.Member
import com.example.domain.member.entity.Role
import com.example.domain.member.repository.MemberRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

//초기 데이터용 추후 삭제
@Configuration
@Profile("!test")
class InitDataConfig {
    @Bean
    fun initData(
        memberRepository: MemberRepository,
        festivalRepository: FestivalRepository,  // 초기 회원도 일반 회원가입과 똑같이 암호화된 비밀번호를 저장해야 한다.
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner {
        return CommandLineRunner {
            //  APi 확인용 Member 데이터
            if (memberRepository.count() == 0L) {
                val dummyMembers = listOf(
                    Member(
                        "홍길순",
                        passwordEncoder.encode("1234"),
                        "wjdahr",
                        "qwer1234@naver.com",
                        "테스트",
                        Role.USER
                    ),  // 2. 관리자 계정 (Role.ADMIN 적용)
                    Member(
                        "관리자",
                        passwordEncoder.encode("1234"),
                        "admin01",
                        "admin@festival.com",
                        "운영마스터",
                        Role.ADMIN
                    ),  // 3. 일반 사용자 A
                    Member(
                        "홍길동",
                        passwordEncoder.encode("1234"),
                        "hong123",
                        "hong@gmail.com",
                        "길동이",
                        Role.USER
                    ),  // 4. 일반 사용자 B
                    Member("김철수", passwordEncoder.encode("1234"), "chulsoo77", "kim@daum.net", "철수철수", Role.USER),

                    Member("테스트회원", passwordEncoder.encode("1234"), "test01", "test01@example.com", "축제왕", Role.USER)
                )

                memberRepository.saveAll(dummyMembers)
                log.info("[Init] 초기 더미 회원 5명 DB 저장 완료")
            }

            // APi 확인용 Festival 데이터
            if (festivalRepository.count() == 0L) {
                val festival = Festival(
                    "FESTIVAL_001",  // contentId (유니크)
                    "서울 봄꽃 축제",  // title
                    "서울에서 열리는 봄꽃 축제입니다.",  // overview
                    "서울시 중구 서울광장",  // address
                    LocalDateTime.now(),  // 시작일
                    LocalDateTime.now().plusDays(7),  // 종료일
                    126.9780,  // 경도
                    37.5665 // 위도
                )
                festivalRepository.save(festival)
            }
        }
    }
    companion object {
        private val log = LoggerFactory.getLogger(InitDataConfig::class.java)
    }

}