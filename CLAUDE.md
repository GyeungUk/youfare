# YouFare

청년 복지 추천 서비스. AI 챗봇이 사용자 프로필 기반으로 청년정책(복지)을 추천하고, 스크랩·커뮤니티 기능을 제공. 포트폴리오용 프로젝트.

> 이 파일이 프로젝트 사실 관계의 **단일 출처(single source of truth)**다. README.md는 사람용 상세 소개이며 내용이 겹치므로, 작업 시 README를 통째로 읽지 말고 이 파일을 우선 참고한다.
> 넓은 코드 탐색("이 기능 어디 있나")은 `Explore`/`cavecrew` 서브에이전트에 위임해, 파일 덤프 대신 결론만 메인 컨텍스트로 받는다.

## 스택

- **백엔드**: Spring Boot 3.3.5, Java 17, Gradle, Spring Data JPA, Spring Security, OAuth2 Client, WebFlux(WebClient)
- **DB**: PostgreSQL (로컬·운영), H2 (테스트)
- **프론트엔드**: React 19 + Vite 8 + React Router 7 + Tailwind 4 + axios (`frontend/`)
- **외부 연동**: Groq(OpenAI 호환 API, `llama-3.3-70b`), 청년정책 공공데이터 OpenAPI, 카카오/네이버 OAuth2, S3 호환 스토리지(AWS S3 / Cloudflare R2 / MinIO)
- **인증**: JWT (jjwt 0.12.6) + OAuth2 소셜로그인
- **배포**: Render(백엔드, `render.yaml`/`Dockerfile`) + Vercel(프론트). 상세는 `DEPLOY.md`

## 실행

출력이 장황한 명령은 조용한 플래그를 쓴다(컨텍스트 토큰 절약).

```bash
# 백엔드 (8080) — .env 필요 (.env.example 복사해서 채움)
./gradlew bootRun
./gradlew build -q                      # 빌드 (조용히)
./gradlew test --console=plain          # H2 인메모리 테스트

# 프론트엔드 (5173) — /api, /oauth2, /login/oauth2 를 8080으로 프록시
cd frontend && npm install --silent && npm run dev
npm run build --silent                  # 프로덕션 빌드
```

## 구조 (`src/main/java/com/youfare/`)

도메인별 패키지 구성. 각 도메인은 Controller/Service/Repository/Entity + DTO를 같은 패키지에 둠.

- `domain/auth` — 회원가입/로그인, 이메일 인증(OTP, Gmail SMTP 실발송), 비번 재설정
- `domain/user` — 사용자, 온보딩(소득·고용상태·등급 계산)
- `domain/welfare` — 복지(청년정책) 조회
- `domain/chat` — AI 상담. 프로필 기반 프롬프트(`PersonalizedPromptBuilder`), 게스트 IP 일일 제한(`GuestChatRateLimiter`)
- `domain/scrap` — 복지 스크랩, 포인트 정책
- `domain/community` — 게시판(글/댓글/좋아요/첨부), 닉네임 마스킹, 미디어 업로드 검증
- `external/openai` — Groq/OpenAI 호환 클라이언트
- `external/publicdata` — 공공데이터 동기화(시더·수동 sync)
- `global/jwt` — JWT 발급·필터, `global/oauth` — 소셜로그인(카카오/네이버), `global/storage` — `FileStorage` 추상화(Local/S3), `global/config`, `global/response`, `global/exception`

프론트: `frontend/src/` — `pages/`, `components/`, `api/`(axios 인스턴스·로그인 플로우), `hooks/`

## 규약

- **API 응답**: 항상 `ApiResponse<T>` 래퍼 (`ApiResponse.ok(data)` / `.fail(errorCode)`). 성공 시 `{success, code:"SUCCESS", message, data}`
- **에러**: `BusinessException(ErrorCode)` 던지면 `GlobalExceptionHandler`가 변환. 새 에러는 `ErrorCode` enum에 도메인 prefix 코드(U/A/W/CH/S/P...)로 추가
- **설정**: `application.yml`의 `${ENV}` 자리표시자를 `.env`로 채움(spring-dotenv). 새 설정값은 yml에 기본값과 함께, `.env.example`에도 문서화
- **스토리지**: `STORAGE_TYPE`(local/s3)만 바꾸면 전환. 검증은 종류 무관(`MediaUploadValidator`: 10개/각 20MB/총 50MB)
- **언어**: 코드 주석·커밋·응답 메시지 모두 한국어
- Lombok 사용. 엔티티는 `BaseEntity`(JPA Auditing) 상속

## Swagger

`http://localhost:8080/swagger-ui.html`
