## Docker 실행 (prod)

1. .env 파일 작성 (루트)

```
DB_HOST=db
DB_PORT=3306
DB_NAME=guineafigma
DB_USERNAME=root
DB_PASSWORD=your_db_password

AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
S3_BUCKET=divary-file-bucket
AWS_REGION=ap-northeast-2

OPENAI_API_KEY=your_openai_api_key
OPENAI_MODEL=gpt-4o-mini

JWT_SECRET=divarySecretKeyForJWTTokenGenerationAndValidation2025
JWT_EXPIRATION=14400000

SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
```

2. 빌드 및 실행

```
./gradlew clean bootJar
docker compose up -d --build
```

3. 종료

```
docker compose down -v
```

# 🍃 Guineafigma-Spring 🍃

guineafigma의 Spring Boot 3.5.3 기반 백엔드 API 서버입니다.

## 프로젝트 정보

- **프로젝트명**: Guineafigma Spring Boot
- **Spring Boot 버전**: 3.5.3
- **Java 버전**: 21
- **빌드 도구**: Gradle
- **데이터베이스**: H2 (개발용 인메모리)

</div>
<br/>

## 💻 Convention 💻

## 🌲 Branch Convention 🌲

1. **기본 브랜치 설정**
   - main : 배포 가능한 안정적인 코드가 유지되는 브랜치
   - develop: 기본 브랜치로, 기능을 개발하는 브랜치
2. **작업 순서**

   1. 작업할 이슈 작성

   예) `#111 사용자 로그인 기능 구현`

   2. 작업 브랜치 생성
      - 기능 개발: `feat/#[이슈번호]-title`
        - ex) feat/#111-login
      - 버그 수정: `fix/#[이슈번호]-title`
        - ex) fix/#111-login
      - 리팩토링: `refac/#[이슈번호]-title`
        - ex) refac/#111-login
   3. **생성한 브랜치에서 작업 수행**
   4. **원격 저장소에 작업 브랜치 푸시**
   5. **Pull Request 생성**

   - `develop` 브랜치 대상으로 Pull Request 생성
   - 리뷰어의 리뷰를 받은 후 PR을 승인 받고 `develop` 브랜치에 병합 후 브랜치 삭제

---

## 🧑‍💻 Code Convention 🧑‍💻

[Based](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)

**네이밍 규칙**

- **변수/상수**: 카멜케이스 (예: `userName`)
- **클래스/구조체**: 파스칼케이스 (예: `UserProfile`)
- **함수/메서드**: 동사로 시작하며 카멜케이스 (예: `fetchData()`)

  **코드 스타일**

- **명시적 타입 선언**: 가능하면 타입 명시 (예: `var name : String = “name”`)
- **옵셔널 처리**: `guard`나 `if let`을 사용하여 안전하게 언래핑
- **함수 파라미터**: 간결하고 직관적인 이름 사용

---

## 💬 Issue Convention 💬

1. **Feature**: 기능 추가 시 작성
   - **Issue**: Feature
   - **내용**: 작업하고자 하는 기능을 입력
   - **TODO**:
     - [ ] 구현 내용 입력
   - **ETC**: 논의가 필요한 사항이나 참고 내용 작성
2. **Fix/Bug**: 오류/버그 발생 시 작성
   - **Issue**: 🐞 Fix / Bug
   - **내용**: 발생한 문제 설명
   - **원인 파악**
   - **해결 방안**
   - **결과 확인**
   - **ETC**: 논의할 사항 작성
3. **Refactor**: 리팩토링 작업 시 작성
   - **Issue**: Refactor
   - **내용**: 리팩토링이 필요한 부분 작성
   - **Before**: 변경 전 상태 및 이유 설명
   - **After**: 변경 후 예상되는 구조 설명
   - **TODO**:
     - [ ] 변경 내용 입력
   - **ETC**: 논의할 사항 작성
4. **Document**: 문서 작업시 작성
   - **Issue**: 📋 Document
   - **내용**: 작성/변경된 문서
   - **TODO**:
     - [ ] 변경 내용 입력
   - **ETC**: 논의할 사항 작성

---

## 🫷 PR Convention 🫸

```markdown
**🔗 관련 이슈**

연관된 이슈 번호를 적어주세요. (예: #123)

---

**📌 PR 요약**

PR에 대한 간략한 설명을 작성해주세요.

(예: 해당 변경 사항의 목적이나 주요 내용)

---

**📑 작업 내용**

작업의 세부 내용을 작성해주세요.

1. 작업 내용 1
2. 작업 내용 2
3. 작업 내용 3

---

**스크린샷 (선택)**

---

**💡 추가 참고 사항**

PR에 대해 추가적으로 논의하거나 참고해야 할 내용을 작성해주세요. (예: 변경사항이 코드베이스에 미치는 영향, 테스트 방법 등)
```

---

## 🙏 Commit Convention 🙏

- `feat` : 새로운 기능이 추가되는 경우
- `fix` : bug가 수정되는 경우
- `docs` : 문서에 변경 사항이 있는 경우
- `style` : 코드 스타일 변경하는 경우 (공백 제거 등)
- `refactor` : 코드 리팩토링하는 경우 (기능 변경 없이 구조 개선)
- `chore` : 자잘한 수정이나 설정, 빌드 업데이트
- `ci` : CI/CD 파이프라인 관련

```spring boot
// Format
[Type]/#[이슈번호]: [Description]

// Example
feat/#1: 로그인 기능 구현
fix/#32: 로그인 api 오류 수정
```

## 프로젝트 구조

아래는 기본 구조이고 필요한 도메인을 추가하면 됩니다.

```
guineafigma-server/
├── build.gradle                    # Gradle 빌드 설정
├── gradle/
│   └── wrapper/                    # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── guineafigma/
│   │   │           ├── GuineafigmaApplication.java          # 메인 애플리케이션 클래스
│   │   │           ├── common/                         # 공통 모듈
│   │   │           │   ├── entity/                     # 공통 엔티티
│   │   │           │   │   └── BaseEntity.java         # 기본 엔티티 (ID, 생성일, 수정일)
│   │   │           │   └── response/                   # 공통 응답 모듈
│   │   │           │       └── ApiResponse.java        # 표준 API 응답 클래스
│   │   │           ├── domain/                         # 도메인 모듈 (추후 도메인마다 controller, dto, service를 가지게 됨)
│   │   │           │   └── system/                     # 시스템 도메인
│   │   │           │       └── controller/             # 시스템 컨트롤러
│   │   │           │           └── SystemController.java
│   │   │           └── global/                         # 전역 설정 모듈
│   │   │               ├── config/                     # 전역 설정
│   │   │               │   └── SwaggerConfig.java      # Swagger API 문서 설정
│   │   │               ├── exception/                  # 전역 예외 처리
│   │   │               │   ├── BusinessException.java  # 비즈니스 예외 클래스
│   │   │               │   ├── ErrorCode.java          # 에러 코드 enum
│   │   │               │   └── GlobalExceptionHandler.java # 전역 예외 핸들러
│   │   │               └── intercepter/                # 인터셉터
│   │   │                   └── LoggingInterceptor.java # HTTP 요청 로깅 인터셉터
│   │   └── resources/
│   │       ├── application.yml             # 기본 애플리케이션 설정
│   │       ├── application-dev.yml         # 개발 환경 설정
│   │       ├── application-prod.yml        # 운영 환경 설정
│   │       └── logback-spring.xml          # 로깅 설정
```

## 주요 의존성

### Spring Boot Starters

- `spring-boot-starter-data-jpa`: JPA 데이터 액세스
- `spring-boot-starter-validation`: 입력값 검증
- `spring-boot-starter-web`: 웹 애플리케이션
- `spring-boot-starter-thymeleaf`: 템플릿 엔진
- `spring-boot-starter-devtools`: 개발 도구

### API 문서화

- `springdoc-openapi-starter-webmvc-ui:2.8.6`: Swagger UI

### 데이터베이스 (추후 설정된 DB에 따라 추가)

- `com.h2database:h2`: H2 인메모리 데이터베이스

## 패키지 구조 설명

### common 패키지 (여러 곳에서 재사용되는 모듈들)

공통으로 사용되는 모듈들을 포함합니다.

- **entity**: 모든 엔티티가 상속받는 기본 클래스
- **response**: 표준화된 API 응답 형식

### domain 패키지

비즈니스 도메인별로 구분된 모듈들을 포함합니다.

- **system**: 시스템 관련 기능 (헬스체크, 에러 테스트 등)

### global 패키지 (애플리케이션 전체에 영향을 주는 모듈들)

전역적으로 적용되는 설정과 예외 처리를 포함

- **config**: 애플리케이션 전역 설정
- **exception**: 전역 예외 처리 및 에러 코드 정의
- **intercepter**: HTTP 요청/응답 인터셉터 (로깅 등)

## API 문서화 (Swagger)

### Swagger 설정

`SwaggerConfig.java`에서 OpenAPI 3.0 스펙을 기반으로 API 문서를 설정합니다.

```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Divary API")
                        .description("다이빙 서포트 앱 Divary의 REST API 문서")
                        .version("v1.0.0"));
    }
}
```

### Swagger 어노테이션 사용법

#### 에러 응답 정의

시스템 컨트롤러에 예제 있습니다. 한 번 살펴봐주세요.

```java
@ApiErrorResponses({
    ErrorCode.INTERNAL_SERVER_ERROR,
    ErrorCode.DATABASE_ERROR
})
```

### Swagger UI 접근

- URL: `http://localhost:8080/swagger-ui/index.html`
- API 문서 JSON: `http://localhost:8080/v3/api-docs`

## 표준 API 응답 형식

모든 API는 `ApiResponse<T>` 형식으로 응답합니다.

### 기본 응답 형식

```json
{
  "timestamp": "2025-06-30T01:43:07.956473",
  "status": 200,
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    // 실제 데이터 👍
  }
}
```

### 페이지네이션 응답 형식

목록 조회 API에서 페이지네이션이 필요한 경우 `PagedResponse<T>`를 사용합니다.

```java
@GetMapping("/members")
public ApiResponse<PagedResponse<MemberDto>> getMembers(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int limit) {

    List<MemberDto> members = memberService.getMembers(page, limit);
    int totalPage = memberService.getTotalPage(limit);

    PagedResponse<MemberDto> pagedData = PagedResponse.of(members, limit, page, totalPage);
    return ApiResponse.success(pagedData);
}
```

응답 예시:

```json
{
  "data": {
    "content": [...],
    "pagination": {
      "limit": 10,
      "currentPage": 1,
      "totalPage": 5
    }
  }
}
```

## 에러 처리

### 에러 처리 플로우

```
[컨트롤러 에러 발생]
       ↓
[GlobalExceptionHandler가 자동 캐치]
       ↓
[ErrorCode로 표준화된 에러 정보 추출]
       ↓
[ApiResponse로 일관된 응답 형식 생성]
       ↓
[클라이언트에게 JSON 응답 전달]
```

### 에러 코드 체계

추후 각 도메인에 맞게 추가하시면 됩니다.

- `COMMON_XXX`: 공통 에러
- `VALIDATION_XXX`: 검증 에러
- `BUSINESS_XXX`: 비즈니스 로직 에러
- `DATABASE_XXX`: 데이터베이스 에러

### 전역 예외 처리

`GlobalExceptionHandler`에서 모든 예외를 표준 형식으로 처리합니다.

## 데이터베이스 설정

### H2 콘솔 접근

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:guineafigma`
- Username: `sa`
- Password: (비어있음)

## 개발 가이드라인

### 컨트롤러 작성

1. `@RestController` 어노테이션 사용
2. `@Tag` 어노테이션으로 API 그룹 정의
3. `@Operation` 어노테이션으로 API 설명 추가
4. `ApiResponse<T>` 형식으로 응답

### 예외 처리

1. 비즈니스 로직 예외는 `BusinessException` 사용
2. `ErrorCode` enum에 새로운 에러 코드 추가
3. `GlobalExceptionHandler`에서 처리되지 않는 예외 추가

### 엔티티 작성

1. `BaseEntity`를 상속받아 기본 필드 활용
2. JPA 어노테이션 사용
3. Lombok 어노테이션으로 보일러플레이트 코드 제거

## 로깅 시스템

### 로깅 설정

**Logback**을 사용하여 로깅 시스템을 구축했습니다.

#### 로그 출력 대상

- **콘솔**: 개발 시 실시간 로그 확인 (색상 지원)
- **파일**: `logs/guineafigma.log`에 로그 저장 (일별, 크기별 롤링)

#### 로그 패턴

```
[시간] [스레드] [로그레벨] [traceId] [로거명] - 메시지
```

#### Trace ID 시스템

- 모든 HTTP 요청에 고유한 8자리 traceId 자동 생성
- 같은 요청에서 발생하는 모든 로그를 추적 가능
- `LoggingInterceptor`를 통해 자동 처리

### 환경별 로깅 설정

#### 개발 환경 (dev)

- **루트 레벨**: WARN
- **애플리케이션 로그**: DEBUG (com.guineafigma 패키지)
- **SQL 로그**: DEBUG (쿼리 및 파라미터)
- **Spring 프레임워크 로그**: INFO

#### 운영 환경 (prod)

- **루트 레벨**: INFO
- **애플리케이션 로그**: INFO
- **SQL 로그**: 비활성화
- **파일 출력만**: 콘솔 출력 비활성화

### 로그 파일 관리

- **최대 파일 크기**: 100MB
- **보관 기간**: 30일
- **총 용량 제한**: 3GB
- **파일명 패턴**: `guineafigma.yyyy-MM-dd.i.log`

## 환경 설정

### 개발 환경

- Java 21
- Spring Boot 3.5.3
- H2 인메모리 데이터베이스
- Swagger UI 활성화
- 로깅 활성화

### 프로덕션 환경

- MySQL 또는 PostgreSQL 데이터베이스가 될 것 같..죠?
- 개발을 마친 후 Swagger UI 비활성화
- 개발을 마친 후 로깅 레벨 조정

## 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다.
Discord 알림 테스트
GitHub Discord 연동 테스트
