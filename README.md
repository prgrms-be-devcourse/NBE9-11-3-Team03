## 🎆 오늘의 축제

* 전국 축제 정보를 한눈에 확인하고, 리뷰와 찜을 통해 나만의 축제 경험을 관리하는 서비스입니다.

---

## 🥤 프로젝트 소개

* 공공데이터포털 관광공사 API를 활용하여 전국 축제 정보를 제공합니다.
* 사용자는 축제 목록/상세 조회, 지역/상태/월별 검색, 내 주변 축제 조회를 할 수 있습니다.
* 로그인한 사용자는 축제를 찜하거나 리뷰를 작성하고, 마이페이지에서 활동 내역을 확인할 수 있습니다.
* 관리자는 축제 데이터 동기화, 신고 리뷰 관리, 신고 회원 관리 기능을 사용할 수 있습니다.
* 기존 Java/Spring Boot 기반 프로젝트를 Kotlin으로 점진적으로 마이그레이션하여 코드 가독성, 유지보수성, 테스트 신뢰도를 개선했습니다.

---

## 📚 3차 프로젝트 개요

본 프로젝트는 2차 프로젝트에서 구현한 Java 기반 축제 플랫폼 API 서버를 Kotlin으로 점진적으로 마이그레이션하는 것을 목표로 진행했습니다.

단순히 Java 파일을 Kotlin으로 변환하는 것에 그치지 않고, Kotlin의 `data class`, `property access`, `named arguments`, `scope function` 등 언어적 장점을 활용하여 코드의 반복을 줄이고 가독성을 개선했습니다.

또한 기존 프로젝트에서 확인된 개선 포인트였던 로깅, Slack 알림, Access Token Blacklist, 파일 업로드 검증, 동시성 테스트 구조도 함께 보완했습니다.

### 핵심 목표

* Java 기반 REST API 서버를 Kotlin으로 점진적 마이그레이션
* 기존 기능 동작을 유지하면서 코드 품질 및 유지보수성 개선
* DTO, Controller, Service, Repository, Entity, Global, Test 계층별 Kotlin 전환
* 로깅 및 Slack 알림 도입으로 운영 관찰성 개선
* 동시성 테스트와 k6 테스트를 통한 검증 신뢰도 향상

---

## ⚙️ 기술 스택

### 🔙 Backend

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.5.13-6DB33F?logo=springboot&logoColor=white)
![Spring Web](https://img.shields.io/badge/Spring%20Web-6DB33F?logo=spring)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?logo=spring)
![Gradle](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?logo=gradle)

### 🔐 Security

![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white)
![Validation](https://img.shields.io/badge/Jakarta%20Validation-FF6F00)

### 🗄 Database

![H2](https://img.shields.io/badge/H2-Database-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)

### 🔎 Query

![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD)

### 📄 API 문서화

![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=black)

### 🐳 Infra / Monitoring

![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)
![Slack](https://img.shields.io/badge/Slack%20Webhook-4A154B?logo=slack&logoColor=white)
![k6](https://img.shields.io/badge/k6-Performance%20Test-7D64FF?logo=k6&logoColor=white)

### 🌐 Frontend

![Next.js](https://img.shields.io/badge/Next.js-black?logo=nextdotjs)
![React](https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=black)

---

## 🔄 Kotlin Migration

본 프로젝트는 기존 Java/Spring Boot 기반 코드를 Kotlin으로 점진적으로 마이그레이션했습니다.

기존 기능 동작은 유지하면서 DTO, Controller, Service, Repository, Entity, Global, Test 코드를 Kotlin 문법에 맞게 전환하고, 일부 테스트 구조와 운영 보조 기능을 개선했습니다.

### 🎯 마이그레이션 목표

* Java 코드의 반복적인 getter, 생성자, Lombok 의존을 줄이고 코드 가독성 개선
* Kotlin `data class`, `property access`, `named arguments`, `scope function` 등을 활용한 표현력 향상
* Java와 Kotlin이 함께 동작하는 점진적 마이그레이션 구조 경험
* 테스트 코드 Kotlin 전환을 통한 유지보수성 향상
* 동시성 테스트 보완을 통한 검증 신뢰도 향상
* 로깅 및 Slack 알림을 통한 운영 관찰성 개선

---

### 📌 주요 마이그레이션 범위

| 계층 | 전환 내용 |
|---|---|
| Build / Config | Kotlin JVM, Spring, JPA, all-open, kapt, Lombok Kotlin 플러그인 설정 |
| DTO | Festival, Member/Auth, MyPage, Review, Admin, Bookmark, Report DTO를 Kotlin data class로 전환 |
| Controller | FestivalAdminController, AdminController, ReviewReportController, FestivalController, FestivalBookmarkController, ReviewController, MyPageController, AuthController 등 전환 |
| Service | ReviewLikeService, FestivalSyncService, SlackNotificationService, FestivalService, MyPageService, FestivalBookmarkService, ReviewReportService, FileStorageService, AuthService, MemberService 등 전환 |
| Repository | FestivalRepository, FestivalRepositoryCustom, FestivalRepositoryImpl, MemberRepository, RefreshTokenRepository, AccessTokenBlacklistRepository 등 전환 |
| Entity | Festival, Member, ReviewReport, FestivalBookmark, RefreshToken, AccessTokenBlacklist 등 주요 Entity Kotlin 호환성 보완 및 일부 전환 |
| Global | BaseEntity, BaseCreatedEntity, ApiRes, Security/JWT/Exception/Config 관련 코드 전환 진행 |
| Test | ReviewControllerTest, ReviewLikeTest, ReviewLikeCancelTest, ReviewReportTest 등 Kotlin 전환 |
| Concurrency Test | 좋아요, 좋아요 취소, 신고 동시성 테스트에 `startLatch` / `doneLatch` 구조 적용 |

---

### ✅ DTO Kotlin 전환 목록

<details>
<summary>DTO Kotlin 전환 목록 보기</summary>

### Festival

* FestivalSearchRequest
* FestivalDetailResponse
* FestivalListResponse
* FestivalMarkerResponse
* FestivalPageResponse
* FestivalSyncResponse
* FestivalSyncResultResponse
* FestivalSyncStatusResponse
* FestivalApiResponse
* FestivalApiHeader
* FestivalApiBody
* FestivalApiItems
* FestivalApiItem

### Member / Auth / MyPage

* SignupRequest
* LoginRequest
* TokenReissueRequest
* WithdrawRequest
* SignupResponse
* LoginResponse
* TokenReissueResponse
* WithdrawResponse
* MyPageResponse
* MyBookMarkItemResponse
* MyBookMarkPageResponse
* MyReviewItemResponse
* MyReviewPageResponse

### Review / Like

* ReviewCreateRequest
* ReviewUpdateRequest
* ReviewResponse
* ReviewListResponse
* ReviewPageResponse
* ReviewUpdateResponse
* ReviewDeleteResponse
* ReviewLikeResponse

### Admin

* ReviewProcessRequest
* AdminMemberWithdrawnResponse
* AdminReviewBlindResponse
* AdminReviewReportResponse
* AdminReviewReportPageResponse
* MemberDetailResponse
* MemberPageResponse

### Bookmark / Report

* FestivalBookmarkResponse
* ReviewReportResponse

</details>

---



### 📈 마이그레이션 개선 효과

| 개선 항목 | Before | After |
|---|---|---|
| DTO 표현 | Java class + Lombok | Kotlin data class |
| 의존성 주입 | `@RequiredArgsConstructor` | Kotlin 주 생성자 |
| Getter 접근 | `getId()`, `getStatus()` | `id`, `status` |
| 응답 생성 | 생성자 순서 의존 | named arguments |
| Controller 응답 | 명시적 return + new | 단일 표현식 함수 + let |
| 테스트 코드 | Java 기반 테스트 | Kotlin 기반 테스트 |
| 동시성 테스트 | 완료 대기 latch만 사용 | 시작/완료 latch 분리 |
| 로그 관리 | 단순 처리 또는 print | SLF4J 기반 레벨별 로그 |

---

## 🛠 기능 보완 사항

### 1. 로깅 도입

기존 `System.out.println` 중심의 출력 방식에서 SLF4J 기반 로깅으로 전환하고, 상황에 따라 `info`, `warn`, `error` 레벨을 구분했습니다.

| 영역 | 로그 내용 | 레벨 |
|---|---|---|
| 축제 동기화 | 스케줄러 시작/완료/실패 | info / error |
| 공공 API | 429, 5xx 응답 감지 | warn |
| 축제 상태 | 예정 → 진행중, 진행중 → 종료 전환 건수 | info |
| 회원 | 회원가입, 로그인, 로그아웃, 탈퇴, 토큰 재발급 | info / warn |
| 보안 | 블랙리스트 토큰 접근, 유효하지 않은 토큰 접근 | warn |
| 관리자 | 회원 강제 탈퇴, 리뷰 블라인드 처리 | info |
| 리뷰 | 신고 5회 임계치 도달 | warn |
| 예외 | 500 서버 오류 | error |

### 2. Slack 알림 도입

공공데이터 축제 동기화 결과를 Slack Webhook을 통해 알림 받을 수 있도록 구성했습니다.

* Incoming Webhook 기반 Slack 알림
* 축제 데이터 동기화 성공/실패 결과 전달
* 스케줄링 작업 결과 모니터링 가능
* 운영 중 동기화 실패 상황을 빠르게 확인 가능

### 3. Access Token Blacklist 보완

로그아웃 또는 회원 탈퇴 이후에도 기존 Access Token이 만료 전까지 사용될 수 있는 문제를 보완하기 위해 Access Token Blacklist를 도입했습니다.

* 로그아웃/탈퇴 시 Access Token blacklist 저장
* JWT 인증 필터에서 blacklist 토큰 차단
* 회원 상태가 ACTIVE인 경우에만 인증 처리
* 만료된 blacklist token 정리 스케줄러 추가

### 4. 파일 업로드 검증 보완

리뷰 이미지 업로드 시 실제 이미지 파일 여부를 검증하도록 보완하여 잘못된 파일 업로드를 방지했습니다.

* 이미지 MIME 타입 검증
* 실제 이미지 파일 시그니처 검증
* 비정상 파일 업로드 시 예외 처리
* 테스트 코드에서 실제 이미지 바이트 기반 MockMultipartFile 사용

### 5. 리뷰 신고 임계치 로그 추가

리뷰 신고 수가 5회 이상 누적되는 시점에 warn 로그를 남겨 관리자 제재 대상 리뷰를 추적할 수 있도록 보완했습니다.

```kotlin
log.warn("[Review] 신고 5회 임계치 - reviewId={}, reportCount={}", reviewId, reportCount)
```

---

## 📊 성능 테스트

축제 검색 API에 대해 k6를 사용하여 Java 버전과 Kotlin 마이그레이션 버전의 성능을 비교했습니다.

### 테스트 대상

* 축제 검색 API
* Java 기반 기존 코드
* Kotlin 마이그레이션 이후 코드

### 테스트 목적

* Kotlin 전환 후 API 응답 성능 저하 여부 확인
* 축제 검색 API의 평균 응답 시간과 처리량 비교
* 마이그레이션 이후에도 기존 기능이 안정적으로 동작하는지 검증

### 테스트 결과

> k6 결과 이미지는 `docs/images` 또는 별도 문서에 추가 후 연결 예정입니다.

```md
![Java 축제 검색 k6 테스트 결과](./docs/images/k6-java-festival-search.png)
![Kotlin 축제 검색 k6 테스트 결과](./docs/images/k6-kotlin-festival-search.png)
```

---

## 🧪 테스트 및 검증

```bash
./gradlew test
```

마이그레이션 후 컴파일 및 주요 테스트를 통해 정상 동작을 확인했습니다.

```bash
./gradlew clean compileJava compileKotlin
./gradlew test
```

### 주요 테스트 범위

* Auth API
* Festival API
* Festival 동기화/재시도
* QueryDSL 검색
* Bookmark
* Review
* Review Like
* Review Report
* Admin
* Global Exception
* Kotlin Migration Test
* Concurrency Test



---

## 🕸️ 아키텍처

```text
Client
  └─> Spring Boot API Server
        ├─ Auth / Member
        │    ├─ 회원가입
        │    ├─ 로그인
        │    ├─ JWT 인증
        │    ├─ 토큰 재발급
        │    ├─ Access Token Blacklist
        │    └─ 마이페이지
        │
        ├─ Festival
        │    ├─ 축제 목록 조회
        │    ├─ 축제 상세 조회
        │    ├─ QueryDSL 기반 검색
        │    ├─ 주변 축제 조회
        │    ├─ 공공데이터 API 동기화
        │    └─ Slack 동기화 알림
        │
        ├─ Review
        │    ├─ 리뷰 작성
        │    ├─ 리뷰 조회
        │    ├─ 리뷰 수정
        │    ├─ 리뷰 삭제
        │    ├─ 리뷰 좋아요
        │    ├─ 리뷰 신고
        │    └─ 신고 5회 임계치 로그
        │
        ├─ Bookmark
        │    ├─ 축제 찜 등록
        │    └─ 축제 찜 취소
        │
        ├─ Admin
        │    ├─ 회원 관리
        │    ├─ 신고 리뷰 관리
        │    ├─ 리뷰 블라인드 처리
        │    └─ 축제 데이터 동기화 관리
        │
        └─ Global
             ├─ Security
             ├─ JWT
             ├─ Exception Handler
             ├─ Scheduler
             ├─ Logging
             └─ WebMvc
```

---

## 📂 프로젝트 구조

> 

```text
src/
├─ main/
│  ├─ kotlin/com/example/              # Java / Kotlin source root
│  │  ├─ FestivalApplication.java
│  │  ├─ domain/
│  │  │  ├─ admin/
│  │  │  │  ├─ controller/           # 관리자 API Controller
│  │  │  │  ├─ dto/                  # Java DTO / Kotlin data class
│  │  │  │  └─ service/              # 관리자 비즈니스 로직
│  │  │  ├─ bookmark/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ dto/
│  │  │  │  ├─ entity/
│  │  │  │  ├─ repository/
│  │  │  │  └─ service/
│  │  │  ├─ festival/
│  │  │  │  ├─ client/               # 공공데이터 API Client
│  │  │  │  ├─ controller/
│  │  │  │  ├─ converter/
│  │  │  │  ├─ dto/                  # Festival 요청/응답 DTO
│  │  │  │  ├─ entity/
│  │  │  │  ├─ event/
│  │  │  │  ├─ repository/           # JPA / QueryDSL Repository
│  │  │  │  └─ service/
│  │  │  ├─ member/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ dto/
│  │  │  │  ├─ entity/
│  │  │  │  ├─ repository/
│  │  │  │  └─ service/
│  │  │  ├─ review/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ dto/                  # Review 요청/응답 DTO Kotlin 전환
│  │  │  │  ├─ entity/
│  │  │  │  ├─ repository/
│  │  │  │  └─ service/              # Review / ReviewLike 비즈니스 로직
│  │  │  ├─ reviewlike/
│  │  │  │  ├─ dto/                  # ReviewLikeResponse Kotlin data class
│  │  │  │  ├─ entity/
│  │  │  │  └─ repository/
│  │  │  └─ reviewreport/
│  │  │     ├─ controller/           # ReviewReportController Kotlin 전환
│  │  │     ├─ dto/
│  │  │     ├─ entity/
│  │  │     ├─ repository/
│  │  │     └─ service/              # 신고 처리 및 임계치 로그
│  │  └─ global/
│  │     ├─ config/
│  │     ├─ entity/                  # BaseEntity Kotlin 호환성 보완
│  │     ├─ exception/
│  │     ├─ exceptionHandler/
│  │     ├─ init/
│  │     ├─ jwt/
│  │     ├─ response/
│  │     ├─ rsData/
│  │     ├─ scheduler/
│  │     ├─ security/
│  │     └─ webMvc/
│  └─ resources/
│     ├─ application.yaml
│     ├─ application-mysql.yaml
│     └─ application-test.yaml
└─ test/
   └─ java/com/example/              # Java / Kotlin test source root
      ├─ admin/
      ├─ festival/
      ├─ review/
      │  ├─ reviewLike/              # ReviewLike Kotlin 테스트
      │  └─ reviewReport/            # ReviewReport Kotlin 테스트
      └─ ...
```

---

## 🧩 주요 기능

### 👤 Member / Auth

* 회원가입
* 로그인
* JWT Access Token 발급
* Refresh Token HttpOnly Cookie 저장
* Access Token 재발급
* 로그아웃
* Access Token blacklist 처리
* 회원 탈퇴 및 탈퇴 회원 비식별화
* 마이페이지 조회
* 내가 작성한 리뷰 조회
* 내가 찜한 축제 조회

### 🎆 Festival

* 축제 목록 조회
* 축제 상세 조회
* 지역/월/상태/키워드 기반 검색
* 내 위치 기반 주변 축제 조회
* 공공데이터 API 축제 목록 동기화
* 축제 상세 정보 보강
* 동기화 실패/미처리 대상 관리
* Slack Webhook 기반 동기화 결과 알림

### ✍️ Review

* 축제 리뷰 작성
* 축제별 리뷰 목록 조회
* 리뷰 수정
* 리뷰 삭제
* 리뷰 좋아요
* 리뷰 좋아요 취소
* 리뷰 신고
* 신고 누적 5회 임계치 로그 기록

### 🔖 Bookmark

* 축제 찜 등록
* 축제 찜 취소
* 마이페이지 찜 목록 조회

### 🛠 Admin

* 회원 목록 조회
* 신고 누적 회원 조회
* 신고 리뷰 조회
* 리뷰 블라인드 처리
* 신고 리뷰 기각 및 신고 수 초기화
* 관리자 회원 탈퇴 처리
* 축제 데이터 동기화 실행
* 축제 상세 보강 실행
* 축제 동기화 상태 조회

---

## 🗄️ ERD

> TODO: ERD 이미지 추가

---

## 📄 API 문서

Swagger 접속:

```text
http://localhost:8080/swagger-ui/index.html
```

### Auth API

| 기능 | Method | URL |
|---|---|---|
| 회원가입 | POST | `/api/auth/signup` |
| 로그인 | POST | `/api/auth/login` |
| 토큰 재발급 | POST | `/api/auth/reissue` |
| 로그아웃 | POST | `/api/auth/logout` |

### Festival API

| 기능 | Method | URL |
|---|---|---|
| 축제 목록/검색 | GET | `/api/festivals` |
| 축제 상세 | GET | `/api/festivals/{id}` |
| 주변 축제 조회 | GET | `/api/festivals/nearby` |

### MyPage API

| 기능 | Method | URL |
|---|---|---|
| 내 정보 조회 | GET | `/api/users/me` |
| 내가 쓴 리뷰 조회 | GET | `/api/users/me/reviews` |
| 내가 찜한 축제 조회 | GET | `/api/users/me/bookmarks` |
| 회원 탈퇴 | DELETE | `/api/users/me/withdraw` |

### Review API

| 기능 | Method | URL |
|---|---|---|
| 리뷰 작성 | POST | `/api/festivals/{festivalId}/reviews` |
| 리뷰 목록 조회 | GET | `/api/festivals/{festivalId}/reviews` |
| 리뷰 수정 | PATCH | `/api/reviews/{reviewId}` |
| 리뷰 삭제 | DELETE | `/api/reviews/{reviewId}` |
| 리뷰 좋아요 | POST | `/api/reviews/{reviewId}/like` |
| 리뷰 좋아요 취소 | DELETE | `/api/reviews/{reviewId}/like` |
| 리뷰 신고 | POST | `/api/reviews/{reviewId}/reports` |

### Bookmark API

| 기능 | Method | URL |
|---|---|---|
| 축제 찜 | POST | `/api/festivals/{festivalId}/bookmark` |
| 축제 찜 취소 | DELETE | `/api/festivals/{festivalId}/bookmark` |

### Admin API

| 기능 | Method | URL |
|---|---|---|
| 회원 목록 조회 | GET | `/api/admin/members` |
| 신고 회원 조회 | GET | `/api/admin/members/reported` |
| 신고 리뷰 조회 | GET | `/api/admin/reviews/reported` |
| 리뷰 블라인드 처리 | PATCH | `/api/admin/reviews/{reviewId}/status` |
| 회원 강제 탈퇴 | PATCH | `/api/admin/members/{memberId}/withdraw` |
| 축제 목록+상세 동기화 | POST | `/api/admin/festivals/sync-and-enrich` |
| 축제 목록 동기화 | POST | `/api/admin/festivals/sync-list` |
| 미처리 상세 보강 | POST | `/api/admin/festivals/enrich-pending` |
| 단건 상세 보강 | POST | `/api/admin/festivals/{contentId}/enrich` |
| 동기화 상태 조회 | GET | `/api/admin/festivals/sync-status` |

---

## 🎬 프로젝트 기능 구현 영상

> TODO: 기능 시연 GIF 또는 시연 영상 링크 추가

### 주요 기능

* 축제 목록/상세 조회
* 지역/상태/월별 축제 검색
* 내 주변 축제 조회
* 회원가입/로그인/로그아웃
* 축제 찜 등록/취소
* 리뷰 작성/수정/삭제
* 리뷰 좋아요/신고
* 관리자 신고 리뷰 관리
* 관리자 축제 데이터 동기화
* Slack 기반 축제 동기화 결과 알림

---

## 🧭 와이어프레임

* 와이어프레임: [Figma](https://www.figma.com/design/dj9CBHwDSceItN3GitAeD0/%EC%98%A4%EB%8A%98%EC%9D%98-%EC%B6%95%EC%A0%9C?node-id=0-1&p=f&t=dSyCutIlGl2aYvPc-0)

---

## 📌 Commit Message Convention

| type | description |
|:-:|---|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| docs | 문서 수정 |
| style | 코드 포맷팅, 세미콜론 누락 등 코드 변경 없음 |
| refactor | 코드 리팩토링 |
| test | 테스트 코드 추가 및 수정 |
| chore | 빌드 설정, 패키지 매니저 설정 등 기타 작업 |

---

## 👨‍💻 팀원 소개

| 이름 | GitHub | 역할 |
|---|---|---|
| 박현준 | [github.com/Phj1225](https://github.com/Phj1225) | Member / Auth: 회원가입, 로그인, JWT 인증/인가, Access Token Blacklist 보완 |
| 김지영 | [github.com/jyeoniop](https://github.com/jyeoniop) | Festival 조회: 다중 필터 검색, 정렬, 지도 탐색, Festival Repository/Global 일부 전환 |
| 김진세 | [github.com/wlstp8473](https://github.com/wlstp8473) | Festival 공공 API 동기화: 데이터 수집/적재, Slack 알림, 동기화 서비스 전환 |
| 한정목 | [github.com/mokmok2yam](https://github.com/mokmok2yam) | Member / MyPage / Admin / Bookmark: 신고 리뷰 제재, 회원 강제 탈퇴, 관리자 DTO/Service 전환 |
| 김민혁 | [github.com/zenesix](https://github.com/zenesix) | Review / ReviewLike / ReviewReport: 리뷰 DTO, 좋아요 서비스, 신고 로그, 테스트 Kotlin 전환 |
