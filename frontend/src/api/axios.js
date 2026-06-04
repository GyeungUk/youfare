import axios from 'axios'

// 백엔드 origin.
//  - 개발: 미설정 → '' → axios baseURL '/api' 로 vite 프록시(→ localhost:8080) 사용.
//  - 배포(분리 호스팅): VITE_API_URL=https://백엔드-도메인 으로 빌드하면 그 origin으로 직접 호출.
export const API_ORIGIN = import.meta.env.VITE_API_URL || ''

// OAuth 로그인 이동 등 axios를 거치지 않는 전체 URL이 필요할 때 쓰는 백엔드 베이스.
//  - 미설정 → '' → 현재 접속한 origin 기준 상대경로(/oauth2/...)로 이동.
//    vite 프록시가 /oauth2·/login/oauth2 를 백엔드로 넘기므로 localhost·LAN·터널(https) 어디서 접속해도 동작한다.
//    (예전엔 http://localhost:8080 으로 하드코딩돼 있어 핸드폰·터널에선 "사이트에 연결할 수 없음"이 떴다.)
//  - 배포(분리 호스팅): VITE_API_URL=https://백엔드-도메인 으로 빌드하면 그 origin으로 이동.
export const BACKEND_BASE = API_ORIGIN

const api = axios.create({ baseURL: API_ORIGIN || '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    // 401 처리: 토큰을 들고 있었는데 거부됐다면(=만료/무효) 로그아웃 후 로그인 유도.
    // 게스트(토큰 없음)는 공개 페이지에서 스크랩 조회 같은 선택적 호출이 401로 떨어져도
    // 강제 리다이렉트하지 않고 호출자(.catch)가 알아서 처리하도록 그냥 reject만 한다.
    if (err.response?.status === 401) {
      const hadToken = !!localStorage.getItem('token')
      if (hadToken) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default api
