import { useState, useEffect, useRef } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import api from '../api/axios'

// 아이디(이메일) 찾기 + 비밀번호 재설정. 둘 다 전화번호 인증으로 본인을 확인한다.
//  - 회원가입과 동일한 /auth/phone/send · /auth/phone/verify 흐름을 재사용한다.
export default function FindAccountPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  // ?tab=password 로 들어오면 비밀번호 탭으로 시작 (로그인 화면의 '비밀번호 찾기' 링크용)
  const [tab, setTab] = useState(params.get('tab') === 'password' ? 'password' : 'email')

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
            <h1 className="text-2xl font-black text-gradient">계정 찾기</h1>
            <p className="text-slate-500 mt-2 text-sm">가입할 때 인증한 휴대폰 번호로 확인해요</p>
          </div>

          {/* 탭 */}
          <div className="flex gap-1 p-1 rounded-2xl bg-slate-100 mb-6">
            <TabBtn active={tab === 'email'} onClick={() => setTab('email')}>아이디 찾기</TabBtn>
            <TabBtn active={tab === 'password'} onClick={() => setTab('password')}>비밀번호 재설정</TabBtn>
          </div>

          {/* key로 탭 전환 시 내부 상태를 초기화 */}
          {tab === 'email'
            ? <FindEmailFlow key="email" />
            : <ResetPasswordFlow key="password" navigate={navigate} />}
        </div>

        <p className="text-center text-sm text-slate-500 mt-5">
          비밀번호가 기억났나요?{' '}
          <Link to="/login" className="font-bold text-emerald-600 hover:text-emerald-700">로그인</Link>
        </p>
      </div>
    </div>
  )
}

function TabBtn({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      className={`flex-1 py-2.5 rounded-xl text-sm font-bold transition-all ${
        active ? 'bg-white text-emerald-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'
      }`}
    >
      {children}
    </button>
  )
}

/* ---------- 아이디(이메일) 찾기 ---------- */
function FindEmailFlow() {
  const [result, setResult] = useState('')   // 가린 이메일
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onVerified(phoneToken) {
    setError(''); setBusy(true)
    try {
      const r = await api.post('/auth/find-email', { phoneVerificationToken: phoneToken })
      setResult(r.data?.data?.email || '')
    } catch (err) {
      setError(err.response?.data?.message || '계정을 찾지 못했어요.')
    } finally {
      setBusy(false)
    }
  }

  if (result) {
    return (
      <div className="space-y-4 text-center">
        <p className="text-sm text-slate-500">가입한 이메일이에요</p>
        <div className="rounded-2xl bg-emerald-50 ring-1 ring-emerald-200 px-4 py-5 text-lg font-bold text-emerald-700 tracking-wide">
          {result}
        </div>
        <Link
          to="/login"
          className="block w-full bg-gradient-to-br from-emerald-500 to-teal-600 hover:brightness-105 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base"
        >
          로그인하러 가기
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <PhoneVerify onVerified={onVerified} disabled={busy} ctaLabel="이메일 찾기" />
      {error && <p className="text-sm text-red-600 px-1">{error}</p>}
    </div>
  )
}

/* ---------- 비밀번호 재설정 ---------- */
function ResetPasswordFlow({ navigate }) {
  const [email, setEmail] = useState('')
  const [phoneToken, setPhoneToken] = useState('')
  const [pw, setPw] = useState('')
  const [pw2, setPw2] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const pwMismatch = pw2.length > 0 && pw !== pw2
  const canSubmit = email && phoneToken && pw.length >= 8 && !pwMismatch && !submitting

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (pw !== pw2) { setError('비밀번호가 일치하지 않습니다.'); return }
    setSubmitting(true)
    try {
      await api.post('/auth/reset-password', {
        email,
        phoneVerificationToken: phoneToken,
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
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="가입한 이메일"
        autoComplete="email"
        required
        className="w-full px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm transition-all"
      />

      <PhoneVerify onVerified={setPhoneToken} ctaLabel="인증하기" />

      {/* 전화 인증을 마쳐야 새 비밀번호 입력을 연다 */}
      {phoneToken && (
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

/* ---------- 공통: 전화번호 인증 블록 ----------
 * 인증요청 → 6자리 코드 확인까지 끝나면 onVerified(phoneVerificationToken)로 토큰을 올려준다.
 * 데모이므로 발급 코드를 화면에 노출한다(SignupPage와 동일).
 */
function PhoneVerify({ onVerified, disabled, ctaLabel = '인증하기' }) {
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [sent, setSent] = useState(false)
  const [devCode, setDevCode] = useState('')
  const [verified, setVerified] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const timerRef = useRef(null)

  const digits = phone.replace(/[^0-9]/g, '')
  const phoneValid = /^01[016789][0-9]{7,8}$/.test(digits)

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
    setError(''); setBusy(true)
    try {
      const r = await api.post('/auth/phone/send', { phoneNumber: digits })
      setSent(true)
      setDevCode(r.data?.data?.devCode || '')
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
      const r = await api.post('/auth/phone/verify', { phoneNumber: digits, code })
      setVerified(true)
      clearInterval(timerRef.current)
      onVerified(r.data?.data?.phoneVerificationToken)
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
        전화번호 인증 완료
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex gap-2">
        <input
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="휴대폰 번호 (01012345678)"
          className="flex-1 px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm transition-all"
        />
        <button
          type="button"
          onClick={sendCode}
          disabled={!phoneValid || busy || disabled}
          className="px-4 rounded-2xl bg-slate-800 text-white text-sm font-bold whitespace-nowrap disabled:opacity-40"
        >
          {sent ? '재전송' : '인증요청'}
        </button>
      </div>

      {sent && (
        <>
          {devCode && (
            <div className="rounded-2xl bg-amber-50 ring-1 ring-amber-200 px-4 py-2.5 text-xs text-amber-700">
              데모용 인증번호: <span className="font-bold tracking-widest">{devCode}</span>
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
