# 📚 BookBook - 도서 대여 서비스

## 📖 프로젝트 소개
BookBook은 사용자 간 도서 대여를 편리하게 관리할 수 있는 웹 애플리케이션입니다. 
사용자들은 자신의 도서를 등록하고, 다른 사용자의 도서를 대여할 수 있으며, 
대여 기록과 상태를 실시간으로 확인할 수 있습니다.

### 주요 기능
- 📚 도서 등록 및 대여 관리
- 🔍 도서 검색 및 필터링
- 📅 대여/반납 상태 관리
- 👤 사용자 인증 및 프로필 관리
- 💬 채팅 기능
- ⭐ 리뷰 시스템
- 📢 알림 기능
- 🚨 신고 및 정지 관리

<br>

## 🛠 기술 스택

### Backend
- **Language:** Kotlin 1.9.25
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL / H2 (테스트용)
- **ORM:** Spring Data JPA
- **Build Tool:** Gradle
- **JDK:** 21
- **Security:** Spring Security + JWT

### Frontend
- **Framework:** Next.js 14
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **State Management:** Zustand
- **HTTP Client:** Axios

### Infrastructure
- **Container:** Docker
- **CI/CD:** GitHub Actions
- **Version Control:** Git & GitHub

<br>

## 🚀 설치 및 실행 방법

### Prerequisites
- JDK 21
- Node.js 18+
- Docker & Docker Compose (선택사항)
- PostgreSQL (로컬 실행 시)

### Backend 실행
```bash
# 프로젝트 클론
git clone https://github.com/your-org/NBE6-8-3-Team01.git
cd NBE6-8-3-Team01/backend

# 의존성 설치 및 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

### Frontend 실행
```bash
cd NBE6-8-3-Team01/frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 프로덕션 빌드
npm run build
npm start
```

### 환경 변수 설정
```bash
# backend/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bookbook
  profiles:
    active: dev

jwt:
  secret: your-secret-key
```

<br>

## 📋 API 문서

### 주요 엔드포인트

#### 인증
- `POST /api/v1/auth/signup` - 회원가입
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/logout` - 로그아웃
- `GET /api/v1/auth/refresh` - 토큰 갱신

#### 사용자
- `GET /api/v1/user/{id}` - 사용자 정보 조회
- `PUT /api/v1/user/{id}` - 사용자 정보 수정
- `DELETE /api/v1/user/{id}` - 회원 탈퇴
- `GET /api/v1/user/profile/{id}` - 프로필 상세 조회

#### 대여 관리
- `GET /api/v1/rent` - 대여 목록 조회
- `POST /api/v1/rent` - 대여 게시글 생성
- `GET /api/v1/rent/{id}` - 대여 상세 조회
- `PATCH /api/v1/rent/{id}/status` - 대여 상태 변경

#### 리뷰
- `POST /api/v1/review/lender/{lenderId}/rent/{rentId}` - 대여자 리뷰 작성
- `POST /api/v1/review/borrower/{borrowerId}/rent/{rentId}` - 대여받은 사람 리뷰 작성

#### 관리자
- `GET /api/v1/admin/rent` - 전체 대여 목록 조회 (관리자)
- `GET /api/v1/admin/report` - 신고 관리
- `GET /api/v1/admin/user/suspended` - 정지 사용자 관리

<br>

## 📁 프로젝트 구조

```
NBE6-8-3-Team01/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/bookbook/
│   │   │   │       ├── domain/
│   │   │   │       │   ├── user/
│   │   │   │       │   ├── rent/
│   │   │   │       │   ├── review/
│   │   │   │       │   ├── chat/
│   │   │   │       │   ├── notification/
│   │   │   │       │   ├── report/
│   │   │   │       │   ├── wishList/
│   │   │   │       │   └── lendList/
│   │   │   │       └── global/
│   │   │   │           ├── security/
│   │   │   │           ├── exception/
│   │   │   │           └── config/
│   │   │   └── resources/
│   │   └── test/
│   └── build.gradle.kts
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   └── bookbook/
│   │   ├── components/
│   │   └── lib/
│   └── package.json
└── docker-compose.yml
```

<br>

## 🧪 테스트

### 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 도메인 테스트 실행
./gradlew test --tests "com.bookbook.domain.user.*"
```

### 테스트 구성

| 도메인 | 테스트 클래스 | 주요 테스트 항목 |
|--------|--------------|-----------------|
| **사용자** | UserServiceTest | • 사용자 조회 및 생성<br>• 프로필 관리<br>• 회원가입/탈퇴 |
| **인증** | JwtAuthenticationFilterTest | • JWT 토큰 검증<br>• 인증 필터링<br>• 토큰 갱신 |
| **대여** | RentAdminControllerTest<br>RentListControllerTest | • 대여 게시글 CRUD<br>• 대여 상태 관리<br>• 페이징 처리 |
| **리뷰** | ReviewControllerTest | • 대여자/대여받은 사람 리뷰<br>• 평점 관리 |
| **채팅** | ChatServiceTest | • 메시지 전송/수신<br>• 채팅방 관리 |
| **알림** | NotificationServiceTest | • 알림 발송<br>• 알림 조회 |
| **신고** | ReportServiceTest<br>ReportControllerTest | • 신고 접수<br>• 신고 처리 |
| **찜목록** | WishListControllerTest | • 찜하기/해제<br>• 찜목록 조회 |

### 테스트 커버리지
- Spring Boot Test를 활용한 통합 테스트
- MockMvc를 통한 Controller 계층 테스트
- @Transactional을 활용한 롤백 테스트
- @ActiveProfiles("test")로 테스트 환경 분리

<br>

### 작업 흐름 상세

1. **이슈 생성**

- GitHub 이슈 탭에서 **적절한 작업 태그를 지정**합니다.
- (`feat | fix | refactor | docs | test | build`)

2. **브랜치 생성**
- `feat/21-user-signup-api`와 같이 **적절한 브랜치 이름을** 생성합니다.

3. **커밋 메시지 작성**
- `회원가입 API 구현(#21)` (커밋 메시지에 `#이슈번호`를 포함하면 좋습니다.)

<br>

### ✅ 커밋 컨벤션 (Commit Convention)

| 태그       | 설명                                                                                |
| ---------- | ----------------------------------------------------------------------------------- |
| `feat`     | 새로운 기능 추가                                                                    |
| `fix`      | 버그 수정                                                                           |
| `refactor` | 리팩토링 (기능 변화 없이 코드 구조 개선, 가독성 향상 등)                            |
| `docs`     | 문서 수정 (README, Wiki, 주석 등)                                                   |
| `test`     | 테스트 코드 추가 또는 수정                                                          |
| `build`    | 빌드 시스템 또는 외부 종속성에 영향을 미치는 변경 (e.g., webpack, npm)              |

<br>

4. PR(Pull Request) 작성

- `[feat] 회원가입 API 구현 (#21)` 와 같이 작성.

<br>

5. 코드 리뷰 및 머지 상세
  - **Squash and Merge:** 여러 개의 커밋을 하나의 커밋으로 합쳐서 `main` 브랜치에 머지합니다.
  - `main` 브랜치의 커밋 히스토리를 깔끔하게 유지할 수 있습니다.

<br>

## 💭 JDK, 코틀린 컴파일러 버전
- JDK 21
- 코틀린 버전 1.9.25 (스프링부트 3.5 버전 호환성)
    - 2.0.0 부터는 K2 컴파일러의 도입으로 기존 대비 평균 2배 이상의 컴파일 속도 개선이 이루어졌고,  
    코틀린 코드로 좀 더 자연스럽게 마이그레이션 된다고 합니다.  
    - 스프링부트 4부터는 코틀린 2.2.0 버전을 기본으로 지원할 것이라 하네요 (25년 11월 경)

<br>

## 💻인텔리제이를 활용하기
- 자바 -> 코틀린로 마이그레이션 후 커밋하면, 내역은 파일 수정(확장자 변경 -> 코드 수정)이 아닌,  
기존 파일 삭제 -> 새 파일 생성으로 남습니다. 이는 PR 때 코드 리뷰 시 불편함을 발생시킬 수 있습니다.  
- 터미널로 커밋 메세지를 한글로 입력하게 되면 인코딩 문제로 인해, 깨진 상태로 커밋이 올라오는 경우가 있습니다.  
인텔리제이는 이러한 부분을 잘 지원해주기 때문에 버전 관리에 용이할 것으로 보입니다.

<br>

---
참고 그림 1  
<img
  src="https://cdn.discordapp.com/attachments/1399363484558299217/1409962038142435439/image.png?ex=68aff1b9&is=68aea039&hm=81c985fb38493bfbf342cba753d952b899da5f9f565c31e9068fad5898ae711f&"
  width="50%"
/>

참고 그림 2  
<img
  src="https://cdn.discordapp.com/attachments/1399363484558299217/1409962038469595236/image.png?ex=68aff1b9&is=68aea039&hm=efc3f153d3ab548d0457c89233a2bd15b25bae7b9b5225698ea537f9cedd5686&"
  width="80%"
/>