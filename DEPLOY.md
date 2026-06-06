# YouFare 배포 가이드 (Render + Vercel)

백엔드(Spring Boot + Postgres)는 **Render**, 프론트엔드(React/Vite)는 **Vercel**에 올린다.
두 서비스 모두 GitHub 레포에서 코드를 가져가므로, **먼저 GitHub에 push**가 되어 있어야 한다.

> ⚠️ 순서가 중요하다. 백엔드 주소와 프론트 주소가 서로를 참조(CORS·OAuth 리다이렉트)하기 때문에,
> 아래 순서대로 진행하고 마지막에 환경변수를 교차로 채운다.

---

## 0. GitHub push

```bash
git add .
git commit -m "chore: 배포 설정 추가 (render.yaml, vercel.json)"
git push origin main
```

---

## 1. 백엔드 → Render

1. [Render 대시보드](https://dashboard.render.com) → **New +** → **Blueprint**
2. 이 GitHub 레포 선택 → Render가 루트의 `render.yaml`을 읽어 **웹 서비스 + Postgres**를 자동 구성한다.
3. **Apply** 누르면 `sync: false`로 표시된 시크릿 입력 화면이 뜬다. 일단 아래만 채우고 나머지는 2단계 후로 미룬다:
   - `GROQ_API_KEY`, `KAKAO_CLIENT_ID/SECRET`, `NAVER_CLIENT_ID/SECRET`, `PUBLIC_DATA_API_KEY`
   - `JWT_SECRET` → 자동 생성(건드릴 필요 없음)
   - `FRONTEND_URL`, `KAKAO_REDIRECT_URI`, `NAVER_REDIRECT_URI` → **아직 모르니 임시값(`https://example.com`)** 넣고 넘어간다.
4. 빌드(Docker)가 끝나면 백엔드 주소가 나온다. 예: `https://youfare-api.onrender.com` — **이 주소를 복사**.

---

## 2. 프론트엔드 → Vercel

1. [Vercel](https://vercel.com/new) → 같은 GitHub 레포 **Import**
2. **Root Directory** 를 `frontend` 로 지정 (중요 — 레포 루트가 아니다).
3. Framework는 **Vite** 자동 인식. `frontend/vercel.json`이 빌드·SPA 라우팅을 처리한다.
4. **Environment Variables** 에 추가:
   - `VITE_API_URL` = `https://youfare-api.onrender.com` (1단계의 백엔드 주소, 끝에 슬래시 없이)
5. **Deploy** → 프론트 주소가 나온다. 예: `https://youfare.vercel.app` — **이 주소를 복사**.

---

## 3. 교차 환경변수 채우기 (Render로 복귀)

Render → `youfare-api` → **Environment** 에서 임시값으로 넣었던 것들을 진짜 주소로 교체:

| 키 | 값 |
|----|----|
| `FRONTEND_URL` | `https://youfare.vercel.app` (2단계 프론트 주소) |
| `KAKAO_REDIRECT_URI` | `https://youfare-api.onrender.com/login/oauth2/code/kakao` |
| `NAVER_REDIRECT_URI` | `https://youfare-api.onrender.com/login/oauth2/code/naver` |

저장하면 Render가 자동 재배포된다.

---

## 4. 소셜 로그인 콘솔 설정

배포 주소를 카카오·네이버 콘솔에 등록해야 OAuth가 동작한다.

**카카오** ([developers.kakao.com](https://developers.kakao.com))
- 내 애플리케이션 → 카카오 로그인 → **Redirect URI** 에 `https://youfare-api.onrender.com/login/oauth2/code/kakao` 추가
- 플랫폼 → Web → 사이트 도메인에 `https://youfare.vercel.app` 추가

**네이버** ([developers.naver.com](https://developers.naver.com))
- 내 애플리케이션 → API 설정 → **Callback URL** 에 `https://youfare-api.onrender.com/login/oauth2/code/naver` 추가

---

## 5. 동작 확인

- `https://youfare.vercel.app` 접속 → 복지 목록 보이는지 (게스트 공개)
- AI 상담 → Groq 응답 오는지
- 카카오/네이버 로그인 → 콜백 후 프론트로 정상 복귀하는지

---

## 주의사항

- **Render 무료 티어는 15분 무활동 시 슬립**된다. 첫 요청에 30초~1분 콜드스타트가 걸린다(포트폴리오 데모엔 무방).
- **이미지 업로드(`STORAGE_TYPE=local`)는 재배포 시 사라진다.** Render 디스크는 휘발성이다.
  커뮤니티 첨부 이미지를 영구 보관하려면 `STORAGE_TYPE=s3` + Cloudflare R2(무료) 설정 후
  `S3_*` 환경변수를 채워라(`.env.example` 하단 참고). 데모만 할 거면 그냥 둬도 된다.
- **Postgres 무료 플랜은 90일 후 만료**된다(Render 정책). 장기 운영이면 유료 또는 외부 DB(Supabase/Neon) 고려.
