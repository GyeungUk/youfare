import { useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import api, { BACKEND_BASE } from '../api/axios'
import { completeLogin } from '../api/loginFlow'

export default function LoginPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const hasError = params.get('error') != null
  const redirect = params.get('redirect')

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [formError, setFormError] = useState('')
  const [loading, setLoading] = useState(false)

  function loginWith(provider) {
    // OAuth 왕복 동안 쿼리 파라미터가 사라지므로, 로그인 후 돌아갈 경로를 잠시 저장해 둔다.
    if (redirect) localStorage.setItem('postLoginRedirect', redirect)
    window.location.href = `${BACKEND_BASE}/oauth2/authorization/${provider}`
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setFormError('')
    setLoading(true)
    try {
      const r = await api.post('/auth/login', { email, password })
      await completeLogin(r.data?.data?.accessToken, navigate, redirect)
    } catch (err) {
      setFormError(err.response?.data?.message || '로그인에 실패했어요.')
      setLoading(false)
    }
  }

  const signupHref = redirect ? `/signup?redirect=${encodeURIComponent(redirect)}` : '/signup'

  return (
    <div className="relative min-h-screen flex items-center justify-center px-4 overflow-hidden bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-20 -left-16 w-96 h-96 bg-emerald-300/40 rounded-full blur-3xl animate-blob" />
        <div className="absolute -bottom-24 -right-10 w-96 h-96 bg-cyan-300/40 rounded-full blur-3xl animate-blob" style={{ animationDelay: '6s' }} />
      </div>

      <div className="relative w-full max-w-md animate-scale-in">
        <div className="glass ring-1 ring-white/60 rounded-[2rem] shadow-2xl shadow-emerald-500/10 p-10">
          <button
            onClick={() => navigate('/')}
            className="text-slate-400 hover:text-slate-600 text-sm mb-8 flex items-center gap-1.5 transition-colors"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 18l-6-6 6-6" />
            </svg>
            홈으로
          </button>

          <div className="text-center mb-9">
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

          <form onSubmit={handleSubmit} className="space-y-3">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="이메일"
              autoComplete="email"
              required
              className="w-full px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm transition-all"
            />
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호"
                autoComplete="current-password"
                required
                className="w-full px-4 py-3.5 pr-12 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm transition-all"
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors p-1"
              >
                {showPassword ? (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
                    <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
                    <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
                    <line x1="2" y1="2" x2="22" y2="22" />
                  </svg>
                ) : (
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                )}
              </button>
            </div>

            {formError && <p className="text-sm text-red-600 px-1">{formError}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-br from-emerald-500 to-teal-600 hover:brightness-105 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base disabled:opacity-60 disabled:hover:translate-y-0"
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <div className="flex items-center justify-center gap-3 mt-4 text-sm text-slate-500">
            <Link to="/find-account" className="hover:text-emerald-600 transition-colors">아이디 찾기</Link>
            <span className="text-slate-300">|</span>
            <Link to="/find-account?tab=password" className="hover:text-emerald-600 transition-colors">비밀번호 찾기</Link>
          </div>

          <p className="text-center text-sm text-slate-500 mt-5">
            아직 회원이 아니신가요?{' '}
            <Link to={signupHref} className="font-bold text-emerald-600 hover:text-emerald-700">
              회원가입
            </Link>
          </p>

          <div className="flex items-center gap-3 my-7">
            <div className="flex-1 h-px bg-slate-200/70" />
            <span className="text-xs text-slate-400">소셜 계정으로</span>
            <div className="flex-1 h-px bg-slate-200/70" />
          </div>

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
        </div>
      </div>
    </div>
  )
}
