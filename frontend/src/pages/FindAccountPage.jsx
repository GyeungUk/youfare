import { useState, useEffect, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'

// 비밀번호 재설정. 가입 이메일로 인증번호를 받아 본인을 확인한 뒤 새 비밀번호로 교체한다.
//  - 회원가입과 동일한 /auth/email/send · /auth/email/verify 흐름을 재사용한다.
//  - (아이디=이메일 이므로 '아이디 찾기'는 제공하지 않는다.)
export default function FindAccountPage() {
  const navigate = useNavigate()

  return (
    <div className="relative min-h-screen flex items-center justify-center px-4 py-10 overflow-hidden bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-20 -left-16 w-96 h-96 bg-emerald-300/40 rounded-full blur-3xl animate-blob" />
        <div className="absolute -bottom-24 -right-10 w-96 h-96 bg-cyan-300/40 rounded-full blur-3xl animate-blob" style={{ animationDelay: '6s' }} />
      </div>

      <div className="relative w-full max-w-md animate-scale-in">
        <div className="glass ring-1 ring-white/60 rounded-[2rem] shadow-2xl shadow-emerald-500/10 p-9">
          <button
            onClick={() => navigate('/login')}
            className="text-slate-400 hover:text-slate-600 text-sm mb-6 flex items-center gap-1.5 transition-colors"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 18l-6-6 6-6" />
            </svg>
            로그인으로
          </button>

          <div className="text-center mb-7">
            <h1 className="text-2xl font-black text-gradient">비밀번호 재설정</h1>
            <p className="text-slate-500 mt-2 text-sm">가입한 이메일로 인증한 뒤 새 비밀번호를 설정해요</p>
          </div>

          <ResetPasswordFlow navigate={navigate} />
        </div>

        <p className="text-center text-sm text-slate-500 mt-5">
          비밀번호가 기억났나요?{' '}
          <Link to="/login" className="font-bold text-emerald-600 hover:text-emerald-700">로그인</Link>
        </p>
      </div>
    </div>
  )
}

/* ---------- 비밀번호 재설정 ---------- */
function ResetPasswordFlow({ navigate }) {
  const [emailToken, setEmailToken] = useState('')
  const [pw, setPw] = useState('')
  const [pw2, setPw2] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const pwMismatch = pw2.length > 0 && pw !== pw2
  const canSubmit = emailToken && pw.length >= 8 && !pwMismatch && !submitting

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (pw !== pw2) { setError('비밀번호가 일치하지 않습니다.'); return }
    setSubmitting(true)
    try {
      await api.post('/auth/reset-password', {
        emailVerificationToken: emailToken,
        newPassword: pw,
      })
      alert('비밀번호가 변경됐어요. 새 비밀번호로 로그인해 주세요.')
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message || '비밀번호 변경에 실패했어요.')
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <EmailVerify onVerified={setEmailToken} ctaLabel="인증하기" />

      {/* 이메일 인증을 마쳐야 새 비밀번호 입력을 연다 */}
      {emailToken && (
        <>
          <input
            type="password"
            value={pw}
            onChange={(e) => setPw(e.target.value)}
            placeholder="새 비밀번호 (8자 이상)"
            autoComplete="new-password"
            required
            className="w-full px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm transition-all"
          />
          <input
            type="password"
            value={pw2}
            onChange={(e) => setPw2(e.target.value)}
            placeholder="새 비밀번호 확인"
            autoComplete="new-password"
            required
            className={`w-full px-4 py-3.5 rounded-2xl bg-white ring-1 outline-none text-sm focus:ring-2 ${
              pwMismatch ? 'ring-red-300 focus:ring-red-400' : 'ring-slate-200 focus:ring-emerald-400'
            }`}
          />
          {pwMismatch && <p className="text-xs text-red-500 px-1">비밀번호가 일치하지 않습니다.</p>}
        </>
      )}

      {error && <p className="text-sm text-red-600 px-1">{error}</p>}

      <button
        type="submit"
        disabled={!canSubmit}
        className="w-full mt-1 bg-gradient-to-br from-emerald-500 to-teal-600 hover:brightness-105 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base disabled:opacity-50 disabled:hover:translate-y-0"
      >
        {submitting ? '변경 중...' : '비밀번호 변경'}
      </button>
    </form>
  )
}

/* ---------- 공통: 이메일 인증 블록 ----------
 * 인증요청 → 6자리 코드 확인까지 끝나면 onVerified(emailVerificationToken)로 토큰을 올려준다.
 */
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function EmailVerify({ onVerified, disabled, ctaLabel = '인증하기' }) {
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [sent, setSent] = useState(false)
  const [info, setInfo] = useState('')
  const [verified, setVerified] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const timerRef = useRef(null)

  const emailValid = EMAIL_RE.test(email.trim())

  useEffect(() => () => clearInterval(timerRef.current), [])

  function startTimer(sec) {
    setSecondsLeft(sec)
    clearInterval(timerRef.current)
    timerRef.current = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) { clearInterval(timerRef.current); return 0 }
        return s - 1
      })
    }, 1000)
  }

  async function sendCode() {
    setError(''); setInfo(''); setBusy(true)
    try {
      const r = await api.post('/auth/email/send', { email: email.trim() })
      setSent(true)
      setInfo('인증번호를 메일로 보냈어요. 메일함(스팸함 포함)을 확인하세요.')
      startTimer(r.data?.data?.ttlSeconds || 180)
      setCode('')
    } catch (err) {
      setError(err.response?.data?.message || '인증번호 발송에 실패했어요.')
    } finally {
      setBusy(false)
    }
  }

  async function verify() {
    setError(''); setBusy(true)
    try {
      const r = await api.post('/auth/email/verify', { email: email.trim(), code })
      setVerified(true)
      clearInterval(timerRef.current)
      onVerified(r.data?.data?.emailVerificationToken)
    } catch (err) {
      setError(err.response?.data?.message || '인증에 실패했어요.')
    } finally {
      setBusy(false)
    }
  }

  const mm = String(Math.floor(secondsLeft / 60)).padStart(1, '0')
  const ss = String(secondsLeft % 60).padStart(2, '0')

  if (verified) {
    return (
      <div className="rounded-2xl bg-emerald-50 ring-1 ring-emerald-200 px-4 py-3 text-sm text-emerald-700 flex items-center gap-2">
        <span className="w-5 h-5 shrink-0 rounded-full bg-emerald-500 text-white flex items-center justify-center text-[11px] font-bold">✓</span>
        이메일 인증 완료
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex gap-2">
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="가입한 이메일 (name@example.com)"
          autoComplete="email"
          className="flex-1 px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm transition-all"
        />
        <button
          type="button"
          onClick={sendCode}
          disabled={!emailValid || busy || disabled}
          className="px-4 rounded-2xl bg-slate-800 text-white text-sm font-bold whitespace-nowrap disabled:opacity-40"
        >
          {sent ? '재전송' : '인증요청'}
        </button>
      </div>

      {sent && (
        <>
          {info && (
            <div className="rounded-2xl bg-sky-50 ring-1 ring-sky-200 px-4 py-2.5 text-xs text-sky-700">
              {info}
            </div>
          )}
          <div className="flex gap-2 items-center">
            <div className="relative flex-1">
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, ''))}
                placeholder="인증번호 6자리"
                className="w-full px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm tracking-widest"
              />
              {secondsLeft > 0 && (
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs text-red-500 font-medium">
                  {mm}:{ss}
                </span>
              )}
            </div>
            <button
              type="button"
              onClick={verify}
              disabled={code.length !== 6 || busy || disabled}
              className="px-5 rounded-2xl bg-emerald-500 text-white text-sm font-bold disabled:opacity-40"
            >
              {ctaLabel}
            </button>
          </div>
        </>
      )}

      {error && <p className="text-sm text-red-600 px-1">{error}</p>}
    </div>
  )
}
