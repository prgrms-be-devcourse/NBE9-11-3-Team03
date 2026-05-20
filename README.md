## 🎆 오늘의 축제

* 전국 축제 정보를 한눈에 확인하고, 리뷰와 찜을 통해 나만의 축제 경험을 관리하는 서비스입니다.

---

## 🥤 프로젝트 소개

* 공공데이터포털 관광공사 API를 활용하여 전국 축제 정보를 제공합니다.
* 사용자는 축제 목록/상세 조회, 지역/상태/월별 검색, 내 주변 축제 조회를 할 수 있습니다.
* 로그인한 사용자는 축제를 찜하거나 리뷰를 작성하고, 마이페이지에서 활동 내역을 확인할 수 있습니다.
* 관리자는 축제 데이터 동기화, 신고 리뷰 관리, 신고 회원 관리 기능을 사용할 수 있습니다.
* 기존 Java/Spring Boot 기반 프로젝트를 Kotlin으로 점진적으로 마이그레이션하여 코드 가독성과 유지보수성을 개선했습니다.

---

## 📚 프로젝트 개요

* 이 서비스는 전국 축제 정보를 편리하게 탐색하고, 사용자 경험을 리뷰와 찜으로 관리할 수 있도록 하는 것을 목표로 합니다.
* 축제 데이터는 공공데이터포털 관광공사 API를 통해 가져오고, 서비스 DB에 저장하여 관리합니다.
* 축제 검색은 지역, 진행 상태, 월, 키워드, 내 위치 기반 조건을 지원합니다.
* 회원 인증은 JWT 기반으로 구현하였으며, Access Token과 Refresh Token을 분리하여 인증과 재발급을 처리합니다.
* 리뷰, 좋아요, 신고, 관리자 블라인드 처리 등 사용자 참여와 운영 관리를 위한 기능을 제공합니다.
* Java와 Kotlin이 함께 동작하는 점진적 마이그레이션 방식으로 기존 기능을 유지하면서 코드 구조를 개선했습니다.

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

### 🐳 Infra

![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)

### 🌐 Frontend

![Next.js](https://img.shields.io/badge/Next.js-black?logo=nextdotjs)
![React](https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=black)

---

## 🔄 Kotlin Migration

본 프로젝트는 기존 Java/Spring Boot 기반 코드를 Kotlin으로 점진적으로 마이그레이션했습니다.  
기존 기능 동작은 유지하면서 DTO, Service, Controller, Repository, Test 코드를 Kotlin 문법에 맞게 전환하고, 일부 테스트 구조를 개선했습니다.

### 🎯 마이그레이션 목표

* Java 코드의 반복적인 getter, 생성자, Lombok 의존을 줄이고 코드 가독성 개선
* Kotlin `data class`, `property access`, `named arguments`, `scope function` 등을 활용한 표현력 향상
* Java와 Kotlin이 함께 동작하는 점진적 마이그레이션 구조 경험
* 테스트 코드 Kotlin 전환을 통한 유지보수성 향상
* 동시성 테스트 보완을 통한 검증 신뢰도 향상

---

### 📌 주요 마이그레이션 범위

| 구분 | 전환 내용 |
|---|---|
| Build / Config | Kotlin JVM, Spring, JPA, all-open, kapt, Lombok Kotlin 플러그인 설정 |
| Global | BaseEntity getter 직접 구현 및 Kotlin 호환성 보완 |
| Member | Member Entity Lombok getter 제거 및 직접 getter 구현, MyPageService Kotlin 전환 |
| Admin | 관리자 요청/응답 DTO Kotlin 전환, 신고 리뷰/회원 관리 응답 구조 Kotlin화 |
| Festival | FestivalSearchRequest Kotlin 전환, FestivalRepositoryImpl 및 일부 Festival 관련 코드 Kotlin 전환 |
| Review | 리뷰 작성/수정 요청 DTO 및 응답 DTO Kotlin 전환 |
| ReviewLike | ReviewLikeResponse DTO, ReviewLikeService Kotlin 전환 |
| ReviewReport | ReviewReportController, ReviewReportService Kotlin 전환 및 신고 5회 임계치 warn 로그 추가 |
| Test | Review / ReviewLike / ReviewReport 테스트 코드 Kotlin 전환 |
| Concurrency Test | 좋아요, 좋아요 취소, 신고 동시성 테스트에 `startLatch` / `doneLatch` 구조 적용 |

---

### 🧩 주요 전환 사례

#### 1. Java DTO class → Kotlin data class

기존 Java DTO는 Lombok의 `@Getter`, `@Builder`, `@AllArgsConstructor` 등에 의존했습니다.  
Kotlin 전환 후에는 `data class`를 사용하여 생성자, getter, equals/hashCode 등 반복 코드를 줄였습니다.



---

### 📈 마이그레이션 개선 효과

| 개선 항목 | Before | After |
|---|---|---|
| DTO 표현 | Java class + Lombok | Kotlin data class |
| 의존성 주입 | `@RequiredArgsConstructor` | Kotlin 주 생성자 |
| Getter 접근 | `getId()`, `getStatus()` | `id`, `status` |
| 응답 생성 | 생성자 순서 의존 | named arguments |
| 테스트 코드 | Java 기반 테스트 | Kotlin 기반 테스트 |
| 동시성 테스트 | 완료 대기 latch만 사용 | 시작/완료 latch 분리 |
| 로그 관리 | 단순 신고 처리 | 신고 5회 임계치 warn 로그 추가 |

---

### 🧪 마이그레이션 검증

마이그레이션 후 컴파일 및 주요 테스트를 통해 정상 동작을 확인했습니다.

```bash
./gradlew clean compileJava compileKotlin
./gradlew test
```

주요 개별 테스트:

```bash
./gradlew test --tests "com.example.review.reviewLike.ReviewLikeControllerTest"
./gradlew test --tests "com.example.review.reviewLike.ReviewLikeCancelTest"
./gradlew test --tests "com.example.review.reviewLike.ReviewLikeTest"
./gradlew test --tests "com.example.review.reviewReport.ReviewReportControllerTest"
./gradlew test --tests "com.example.review.reviewReport.ReviewReportTest"
```

---

### ✅ Kotlin Migration Summary

* DTO를 Kotlin `data class`로 전환하여 반복 코드 감소
* Service 계층에 Kotlin 주 생성자 기반 DI 적용
* Java getter 호출을 Kotlin property access로 변경
* named arguments를 활용해 DTO 필드 매핑 명확화
* Review 신고 5회 임계치 warn 로그 추가
* 테스트 코드 Kotlin 전환 및 동시성 테스트 신뢰도 개선

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
        │    └─ 마이페이지
        │
        ├─ Festival
        │    ├─ 축제 목록 조회
        │    ├─ 축제 상세 조회
        │    ├─ QueryDSL 기반 검색
        │    ├─ 주변 축제 조회
        │    └─ 공공데이터 API 동기화
        │
        ├─ Review
        │    ├─ 리뷰 작성
        │    ├─ 리뷰 조회
        │    ├─ 리뷰 수정
        │    ├─ 리뷰 삭제
        │    ├─ 리뷰 좋아요
        │    └─ 리뷰 신고
        │
        ├─ Bookmark
        │    ├─ 축제 찜 등록
        │    └─ 축제 찜 취소
        │
        ├─ Admin
        │    ├─ 회원 관리
        │    ├─ 신고 리뷰 관리
        │    └─ 축제 데이터 동기화 관리
        │
        └─ Global
             ├─ Security
             ├─ JWT
             ├─ Exception Handler
             ├─ Scheduler
             └─ WebMvc
```

---

## 📂 프로젝트 구조

> Java와 Kotlin 파일은 점진적 마이그레이션 방식으로 동일한 패키지 구조 내에서 함께 관리합니다.

```text
src/
├─ main/
│  ├─ java/com/example/
│  │  ├─ FestivalApplication.java
│  │  ├─ domain/
│  │  │  ├─ admin/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ dto/
│  │  │  │  └─ service/
│  │  │  ├─ bookmark/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ dto/
│  │  │  │  ├─ entity/
│  │  │  │  ├─ repository/
│  │  │  │  └─ service/
│  │  │  ├─ festival/
│  │  │  │  ├─ client/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ converter/
│  │  │  │  ├─ dto/
│  │  │  │  ├─ entity/
│  │  │  │  ├─ event/
│  │  │  │  ├─ repository/
│  │  │  │  └─ service/
│  │  │  ├─ member/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ dto/
│  │  │  │  ├─ entity/
│  │  │  │  ├─ repository/
│  │  │  │  └─ service/
│  │  │  ├─ review/
│  │  │  │  ├─ controller/
│  │  │  │  ├─ dto/
│  │  │  │  ├─ entity/
│  │  │  │  ├─ repository/
│  │  │  │  └─ service/
│  │  │  ├─ reviewlike/
│  │  │  │  ├─ dto/
│  │  │  │  ├─ entity/
│  │  │  │  └─ repository/
│  │  │  └─ reviewreport/
│  │  │     ├─ controller/
│  │  │     ├─ dto/
│  │  │     ├─ entity/
│  │  │     ├─ repository/
│  │  │     └─ service/
│  │  └─ global/
│  │     ├─ config/
│  │     ├─ entity/
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
   └─ java/com/example/
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

### ✍️ Review

* 축제 리뷰 작성
* 축제별 리뷰 목록 조회
* 리뷰 수정
* 리뷰 삭제
* 리뷰 좋아요
* 리뷰 신고

### 🔖 Bookmark

* 축제 찜 등록
* 축제 찜 취소
* 마이페이지 찜 목록 조회

### 🛠 Admin

* 회원 목록 조회
* 신고 누적 회원 조회
* 신고 리뷰 조회
* 리뷰 블라인드 처리
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

## 🧪 테스트

```bash
./gradlew test
```

주요 테스트 범위:

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



동시성 테스트는 `startLatch`와 `doneLatch`를 분리하여 요청 시작 시점을 맞추고, 좋아요/좋아요 취소/신고 카운트 정합성을 검증했습니다.

---

## 🎬 프로젝트 기능 구현 영상

> 

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
| 박현준 | [github.com/Phj1225](https://github.com/Phj1225) | Member / Auth: 회원가입, 로그인, JWT 인증 인가 |
| 김지영 | [github.com/jyeoniop](https://github.com/jyeoniop) | Festival 조회: 다중 필터 검색, 정렬, 지도 탐색 |
| 김진세 | [github.com/wlstp8473](https://github.com/wlstp8473) | Festival 공공 API 동기화: 데이터 수집/적재, 에러 핸들링 |
| 한정목 | [github.com/mokmok2yam](https://github.com/mokmok2yam) | Member / Auth, Admin: 신고 리뷰 제재, 회원 강제 탈퇴 |
| 김민혁 | [github.com/zenesix](https://github.com/zenesix) | Review / Like / Bookmark: 리뷰 CRUD, 좋아요/찜, 평점 계산, 신고 접수 |
