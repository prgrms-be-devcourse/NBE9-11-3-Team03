## 🎆 오늘의 축제
* 전국 축제 정보를 한눈에 확인하고, 리뷰와 찜을 통해 나만의 축제 경험을 관리하는 서비스입니다.

---

## 🥤 프로젝트 소개
* 공공데이터포털 관광공사 API를 활용하여 전국 축제 정보를 제공합니다.
* 사용자는 축제 목록/상세 조회, 지역/상태/월별 검색, 내 주변 축제 조회를 할 수 있습니다.
* 로그인한 사용자는 축제를 찜하거나 리뷰를 작성하고, 마이페이지에서 활동 내역을 확인할 수 있습니다.
* 관리자는 축제 데이터 동기화, 신고 리뷰 관리, 신고 회원 관리 기능을 사용할 수 있습니다.

---

## 📚 프로젝트 개요
* 이 서비스는 전국 축제 정보를 편리하게 탐색하고, 사용자 경험을 리뷰와 찜으로 관리할 수 있도록 하는 것을 목표로 합니다.
* 축제 데이터는 공공데이터포털 관광공사 API를 통해 가져오고, 서비스 DB에 저장하여 관리합니다.
* 축제 검색은 지역, 진행 상태, 월, 키워드, 내 위치 기반 조건을 지원합니다.
* 회원 인증은 JWT 기반으로 구현하였으며, Access Token과 Refresh Token을 분리하여 인증과 재발급을 처리합니다.
* 리뷰, 좋아요, 신고, 관리자 블라인드 처리 등 사용자 참여와 운영 관리를 위한 기능을 제공합니다.

---

## ⚙️ 기술 스택

### 🔙 Backend
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
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


## 📂 프로젝트 구조

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

---

## 🎬 프로젝트 기능 구현 영상

> TODO: 기능 시연 GIF 추가

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
