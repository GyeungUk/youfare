import { useNavigate, useSearchParams } from 'react-router-dom'
import { BACKEND_BASE } from '../api/axios'

export default function LoginPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const hasError = params.get('error') != null
  const redirect = params.get('redirect')

  function loginWith(provider) {
    // OAuth 왕복 동안 쿼리 파라미터가 사라지므로, 로그인 후 돌아갈 경로를 잠시 저장해 둔다.
    // (콜백 페이지에서 꺼내 사용)
    if (redirect) localStorage.setItem('postLoginRedirect', redirect)
    window.location.href = `${BACKEND_BASE}/oauth2/authorization/${provider}`
  }

  return (
    <div className="relative min-h-screen flex items-center justify-center px-4 overflow-hidden bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50">
      {/* 배경 블롭 */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-20 -left-16 w-96 h-96 bg-emerald-300/40 rounded-full blur-3xl animate-blob" />
        <div className="absolute -bottom-24 -right-10 w-96 h-96 bg-cyan-300/40 rounded-full blur-3xl animate-blob" style={{ animationDelay: '6s' }} />
      </div>

      <div className="relative w-full max-w-md animate-scale-in">
        <div className="glass ring-1 ring-white/60 rounded-[2rem] shadow-2xl shadow-emerald-500/10 p-10">
          <button
            onClick={() => navigate('/')}
            className="text-slate-400 hover:text-slate-600 text-sm mb-8 flex items-center gap-1 transition-colors"
          >
            ← 홈으로
          </button>

          <div className="text-center mb-10">
            <div className="inline-flex w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 items-center justify-center text-3xl shadow-lg shadow-emerald-500/30 mb-5 animate-float">
              🌱
            </div>
            <h1 className="text-3xl font-black text-gradient">YouFare</h1>
            <p className="text-slate-500 mt-3 text-base">나에게 맞는 복지 혜택을 찾아보세요</p>
          </div>

          {hasError && (
            <div className="mb-5 rounded-2xl bg-red-50 ring-1 ring-red-200 px-4 py-3 text-sm text-red-600 text-center">
              로그인에 실패했어요. 잠시 후 다시 시도해 주세요.
            </div>
          )}

          <div className="space-y-3">
            <button
              onClick={() => loginWith('kakao')}
              className="w-full flex items-center justify-center gap-3 bg-[#FEE500] hover:brightness-95 text-[#3C1E1E] font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base"
            >
              <svg width="20" height="20" viewBox="0 0 22 22" fill="none">
                <path d="M11 1.5C5.753 1.5 1.5 4.91 1.5 9.14c0 2.698 1.743 5.068 4.365 6.42l-.898 3.343a.3.3 0 0 0 .456.326l3.93-2.616A11.87 11.87 0 0 0 11 16.78c5.247 0 9.5-3.41 9.5-7.64S16.247 1.5 11 1.5z" fill="#3C1E1E" />
              </svg>
              카카오로 로그인
            </button>

            <button
              onClick={() => loginWith('naver')}
              className="w-full flex items-center justify-center gap-3 bg-[#03C75A] hover:brightness-95 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base"
            >
              <span className="font-black text-white text-lg">N</span>
              네이버로 로그인
            </button>
          </div>

          <div className="flex items-center gap-3 my-7">
            <div className="flex-1 h-px bg-slate-200/70" />
            <span className="text-xs text-slate-400">간편하고 안전하게</span>
            <div className="flex-1 h-px bg-slate-200/70" />
          </div>

          <p className="text-center text-slate-400 text-xs leading-relaxed">
            로그인 시 서비스 이용약관 및<br />개인정보 처리방침에 동의하게 됩니다.
          </p>
        </div>
      </div>
    </div>
  )
}
