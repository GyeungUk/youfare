import { useState, useEffect, useRef } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import api from '../api/axios'
import PasswordInput from '../components/PasswordInput'

// 계정 찾기. 가입 이메일로 인증번호를 받아 본인을 확인한 뒤,
//  - 아이디 찾기(?mode=id): 그 이메일로 가입한 아이디를 보여주고
//  - 비밀번호 재설정(기본): 새 비밀번호로 교체한다.
// 로그인 페이지에서 '아이디 찾기'/'비밀번호 찾기' 각각의 링크로 진입한다.
// 두 흐름 모두 회원가입과 동일한 /auth/email/send · /auth/email/verify 인증을 재사용한다.
export default function FindAccountPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const mode = params.get('mode') === 'id' ? 'id' : 'pw'

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
            <h1 className="text-2xl font-black text-gradient">{mode === 'id' ? '아이디 찾기' : '비밀번호 재설정'}</h1>
            <p className="text-slate-500 mt-2 text-sm">
              {mode === 'id'
                ? '가입한 이메일로 인증하면 아이디를 알려드려요'
                : '가입한 이메일로 인증한 뒤 새 비밀번호를 설정해요'}
            </p>
          </div>

          {mode === 'id'
            ? <FindUsernameFlow navigate={navigate} />
            : <ResetPasswordFlow navigate={navigate} />}
        </div>

        <p className="text-center text-sm text-slate-500 mt-5">
          {mode === 'id' ? (
            <>비밀번호를 잊으셨나요?{' '}
              <Link to="/find-account" className="font-bold text-emerald-600 hover:text-emerald-700">비밀번호 재설정</Link></>
          ) : (
            <>아이디가 기억나지 않나요?{' '}
              <Link to="/find-account?mode=id" className="font-bold text-emerald-600 hover:text-emerald-700">아이디 찾기</Link></>
          )}
        </p>
      </div>
    </div>
  )
}

/* ---------- 소셜 가입 계정 안내 카드 ----------
 * 백엔드가 A007(이미 소셜로 가입된 이메일)을 반환하면, 빨간 에러 대신
 * 해당 소셜(네이버/카카오) 브랜드 색으로 "그쪽으로 로그인하세요"를 안내한다.
 * 아이디찾기·비밀번호 재설정 두 흐름이 공유한다. */
const PROVIDER_THEME = {
  네이버: { bg: '#03C75A', fg: '#ffffff', soft: 'bg-emerald-50', ring: 'ring-emerald-200', mark: 'N' },
  카카오: { bg: '#FEE500', fg: '#3C1E1E', soft: 'bg-amber-50', ring: 'ring-amber-200', mark: 'K' },
}

function SocialAccountNotice({ message, navigate }) {
  const provider = message.includes('카카오') ? '카카오' : '네이버'
  const t = PROVIDER_THEME[provider]
  return (
    <div className={`rounded-2xl ${t.soft} ring-1 ${t.ring} px-6 py-7 text-center animate-scale-in`}>
      <div
        className="mx-auto mb-4 w-14 h-14 rounded-2xl flex items-center justify-center text-2xl font-black shadow-sm"
        style={{ backgroundColor: t.bg, color: t.fg }}
      >
        {t.mark}
      </div>
      <p className="text-base font-bold text-slate-800">{provider} 계정으로 가입돼 있어요</p>
      <p className="text-sm text-slate-500 mt-1.5 leading-relaxed">
        이 이메일은 <b className="text-slate-700">{provider} 로그인</b>으로 가입한 계정이에요.<br />
        아이디·비밀번호 대신 {provider}로 로그인해 주세요.
      </p>
      <button
        type="button"
        onClick={() => navigate('/login')}
        className="mt-5 w-full font-bold py-3.5 rounded-2xl transition-all hover:-translate-y-0.5 hover:brightness-95 shadow-sm active:translate-y-0"
        style={{ backgroundColor: t.bg, color: t.fg }}
      >
        {provider}로 로그인하러 가기
      </button>
    </div>
  )
}

/* ---------- 아이디 찾기 ---------- */
function FindUsernameFlow({ navigate }) {
  const [username, setUsername] = useState('')
  const [socialMsg, setSocialMsg] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // 이메일 인증을 마치면 토큰으로 아이디를 조회한다.
  async function handleVerified(token) {
    setError(''); setLoading(true)
    try {
      const r = await api.post('/auth/find-username', { emailVerificationToken: token })
      const found = r.data?.data?.username
      if (found) setUsername(found)
      else setError('이 이메일로 가입한 아이디를 찾을 수 없어요.')
    } catch (err) {
      const data = err.response?.data
      // 소셜 가입 이메일이면 전용 안내 카드로 보여준다.
      if (data?.code === 'A007') setSocialMsg(data.message)
      else setError(data?.message || '아이디를 찾지 못했어요.')
    } finally {
      setLoading(false)
    }
  }

  if (socialMsg) return <SocialAccountNotice message={socialMsg} navigate={navigate} />

  if (username) {
    return (
      <div className="space-y-4">
        <div className="rounded-2xl bg-emerald-50 ring-1 ring-emerald-200 px-5 py-6 text-center">
          <p className="text-sm text-slate-500 mb-1.5">회원님의 아이디는</p>
          <p className="text-xl font-black text-emerald-700 break-all">{username}</p>
        </div>
        <button
          onClick={() => navigate('/login')}
          className="w-full bg-gradient-to-br from-emerald-500 to-teal-600 hover:brightness-105 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base"
        >
          로그인하러 가기
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <EmailVerify onVerified={handleVerified} ctaLabel="아이디 찾기" disabled={loading} />
      {loading && <p className="text-sm text-slate-500 px-1">아이디 조회 중...</p>}
      {error && <p className="text-sm text-red-600 px-1">{error}</p>}
    </div>
  )
}

/* ---------- 비밀번호 재설정 ---------- */
function ResetPasswordFlow({ navigate }) {
  const [emailToken, setEmailToken] = useState('')
  const [pw, setPw] = useState('')
  const [pw2, setPw2] = useState('')
  const [error, setError] = useState('')
  const [socialMsg, setSocialMsg] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [checking, setChecking] = useState(false)

  const pwMismatch = pw2.length > 0 && pw !== pw2
  const canSubmit = emailToken && pw.length >= 8 && !pwMismatch && !submitting

  // 이메일 인증 직후, 새 비밀번호 입력칸을 열기 전에 재설정 가능한 계정인지 먼저 확인한다.
  // 소셜(네이버/카카오) 가입 계정이면 여기서 안내해, 비밀번호를 헛입력하지 않게 한다.
  async function handleVerified(token) {
    setError(''); setChecking(true)
    try {
      await api.post('/auth/reset-password/check', { emailVerificationToken: token })
      setEmailToken(token)
    } catch (err) {
      const data = err.response?.data
      if (data?.code === 'A007') setSocialMsg(data.message)
      else setError(data?.message || '비밀번호를 재설정할 수 없는 계정이에요.')
    } finally {
      setChecking(false)
    }
  }

  if (socialMsg) return <SocialAccountNotice message={socialMsg} navigate={navigate} />

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
      <EmailVerify onVerified={handleVerified} ctaLabel="인증하기" disabled={checking} />

      {checking && <p className="text-sm text-slate-500 px-1">계정 확인 중...</p>}

      {/* 이메일 인증 + 재설정 가능 계정 확인까지 마쳐야 새 비밀번호 입력을 연다 */}
      {emailToken && (
        <>
          <PasswordInput
            value={pw}
            onChange={(e) => setPw(e.target.value)}
            placeholder="새 비밀번호 (8자 이상)"
            required
          />
          <PasswordInput
            value={pw2}
            onChange={(e) => setPw2(e.target.value)}
            placeholder="새 비밀번호 확인"
            invalid={pwMismatch}
            required
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
