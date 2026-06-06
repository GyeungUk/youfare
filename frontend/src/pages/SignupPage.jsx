import { useState, useEffect, useRef } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import api from '../api/axios'
import { completeLogin } from '../api/loginFlow'

const STEPS = ['약관 동의', '전화번호 인증', '계정 정보']

export default function SignupPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = params.get('redirect')

  const [step, setStep] = useState(0)

  return (
    <div className="relative min-h-screen flex items-center justify-center px-4 py-10 overflow-hidden bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-20 -left-16 w-96 h-96 bg-emerald-300/40 rounded-full blur-3xl animate-blob" />
        <div className="absolute -bottom-24 -right-10 w-96 h-96 bg-cyan-300/40 rounded-full blur-3xl animate-blob" style={{ animationDelay: '6s' }} />
      </div>

      <div className="relative w-full max-w-md animate-scale-in">
        <div className="glass ring-1 ring-white/60 rounded-[2rem] shadow-2xl shadow-emerald-500/10 p-9">
          <button
            onClick={() => (step === 0 ? navigate('/login') : setStep(step - 1))}
            className="text-slate-400 hover:text-slate-600 text-sm mb-6 flex items-center gap-1.5 transition-colors"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 18l-6-6 6-6" />
            </svg>
            {step === 0 ? '로그인으로' : '이전'}
          </button>

          <div className="text-center mb-7">
            <h1 className="text-2xl font-black text-gradient">회원가입</h1>
          </div>

          {/* 스텝 진행 표시 */}
          <Stepper step={step} />

          <div className="mt-7">
            <SignupFlow step={step} setStep={setStep} redirect={redirect} navigate={navigate} />
          </div>
        </div>

        <p className="text-center text-sm text-slate-500 mt-5">
          이미 회원이신가요?{' '}
          <Link to="/login" className="font-bold text-emerald-600 hover:text-emerald-700">로그인</Link>
        </p>
      </div>
    </div>
  )
}

function Stepper({ step }) {
  return (
    <div className="flex items-center justify-center gap-2">
      {STEPS.map((label, i) => (
        <div key={label} className="flex items-center gap-2">
          <div className="flex flex-col items-center">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all ${
                i < step ? 'bg-emerald-500 text-white'
                  : i === step ? 'bg-emerald-500 text-white ring-4 ring-emerald-200'
                  : 'bg-slate-200 text-slate-400'
              }`}
            >
              {i < step ? '✓' : i + 1}
            </div>
            <span className={`mt-1.5 text-[11px] ${i === step ? 'text-emerald-600 font-bold' : 'text-slate-400'}`}>
              {label}
            </span>
          </div>
          {i < STEPS.length - 1 && (
            <div className={`w-8 h-0.5 mb-4 ${i < step ? 'bg-emerald-400' : 'bg-slate-200'}`} />
          )}
        </div>
      ))}
    </div>
  )
}

// 회원가입 전체 상태를 한 곳에서 관리하고 스텝별 화면을 렌더
function SignupFlow({ step, setStep, redirect, navigate }) {
  // 약관
  const [agree, setAgree] = useState({ terms: false, privacy: false, marketing: false })
  // 전화 인증
  const [phone, setPhone] = useState('')
  const [phoneToken, setPhoneToken] = useState('')
  // 계정
  const [account, setAccount] = useState({ email: '', password: '', password2: '', nickname: '' })
  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submitSignup() {
    setSubmitError('')
    if (account.password !== account.password2) {
      setSubmitError('비밀번호가 일치하지 않습니다.')
      return
    }
    setSubmitting(true)
    try {
      const r = await api.post('/auth/signup', {
        email: account.email,
        password: account.password,
        nickname: account.nickname,
        phoneNumber: phone.replace(/[^0-9]/g, ''),
        phoneVerificationToken: phoneToken,
        agreeTerms: agree.terms,
        agreePrivacy: agree.privacy,
        agreeMarketing: agree.marketing,
      })
      await completeLogin(r.data?.data?.accessToken, navigate, redirect)
    } catch (err) {
      setSubmitError(err.response?.data?.message || '회원가입에 실패했어요.')
      setSubmitting(false)
    }
  }

  if (step === 0) {
    return <TermsStep agree={agree} setAgree={setAgree} onNext={() => setStep(1)} />
  }
  if (step === 1) {
    return (
      <PhoneStep
        phone={phone} setPhone={setPhone}
        phoneToken={phoneToken} setPhoneToken={setPhoneToken}
        onNext={() => setStep(2)}
      />
    )
  }
  return (
    <AccountStep
      account={account} setAccount={setAccount}
      onSubmit={submitSignup} submitting={submitting} error={submitError}
    />
  )
}

/* ---------- STEP 1: 약관 동의 ---------- */
function TermsStep({ agree, setAgree, onNext }) {
  const allChecked = agree.terms && agree.privacy && agree.marketing
  const requiredOk = agree.terms && agree.privacy

  function toggleAll() {
    const next = !allChecked
    setAgree({ terms: next, privacy: next, marketing: next })
  }

  return (
    <div className="space-y-3">
      <button
        onClick={toggleAll}
        className={`w-full flex items-center gap-3 px-4 py-4 rounded-2xl ring-1 transition-all ${
          allChecked ? 'bg-emerald-50 ring-emerald-300' : 'bg-white ring-slate-200'
        }`}
      >
        <Check checked={allChecked} />
        <span className="font-bold text-slate-700">약관 전체 동의</span>
      </button>

      <div className="space-y-2 px-1">
        <TermRow
          label="이용약관 동의" required
          checked={agree.terms} onToggle={() => setAgree({ ...agree, terms: !agree.terms })}
        />
        <TermRow
          label="개인정보 수집·이용 동의" required
          checked={agree.privacy} onToggle={() => setAgree({ ...agree, privacy: !agree.privacy })}
        />
        <TermRow
          label="마케팅 정보 수신 동의" required={false}
          checked={agree.marketing} onToggle={() => setAgree({ ...agree, marketing: !agree.marketing })}
        />
      </div>

      <button
        disabled={!requiredOk}
        onClick={onNext}
        className="w-full mt-3 bg-gradient-to-br from-emerald-500 to-teal-600 hover:brightness-105 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base disabled:opacity-50 disabled:hover:translate-y-0"
      >
        다음
      </button>
    </div>
  )
}

function TermRow({ label, required, checked, onToggle }) {
  return (
    <button onClick={onToggle} className="w-full flex items-center gap-3 py-1.5">
      <Check checked={checked} small />
      <span className="text-sm text-slate-600">
        <span className={required ? 'text-emerald-600 font-medium' : 'text-slate-400'}>
          [{required ? '필수' : '선택'}]
        </span>{' '}
        {label}
      </span>
    </button>
  )
}

function Check({ checked, small }) {
  const size = small ? 'w-5 h-5 text-[11px]' : 'w-6 h-6 text-sm'
  return (
    <span className={`${size} shrink-0 rounded-full flex items-center justify-center font-bold transition-all ${
      checked
        ? 'bg-emerald-500 text-white ring-0'
        : 'bg-white text-transparent ring-2 ring-slate-300'
    }`}>
      ✓
    </span>
  )
}

/* ---------- STEP 2: 전화번호 인증 ---------- */
function PhoneStep({ phone, setPhone, phoneToken, setPhoneToken, onNext }) {
  const [code, setCode] = useState('')
  const [sent, setSent] = useState(false)
  const [devCode, setDevCode] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const timerRef = useRef(null)

  const verified = !!phoneToken
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
      setPhoneToken('')
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
      setPhoneToken(r.data?.data?.phoneVerificationToken)
      clearInterval(timerRef.current)
    } catch (err) {
      setError(err.response?.data?.message || '인증에 실패했어요.')
    } finally {
      setBusy(false)
    }
  }

  const mm = String(Math.floor(secondsLeft / 60)).padStart(1, '0')
  const ss = String(secondsLeft % 60).padStart(2, '0')

  return (
    <div className="space-y-3">
      <label className="block text-sm font-medium text-slate-600 px-1">휴대폰 번호</label>
      <div className="flex gap-2">
        <input
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="01012345678"
          disabled={verified}
          className="flex-1 px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm transition-all disabled:bg-slate-50 disabled:text-slate-400"
        />
        <button
          onClick={sendCode}
          disabled={!phoneValid || verified || busy}
          className="px-4 rounded-2xl bg-slate-800 text-white text-sm font-bold whitespace-nowrap disabled:opacity-40"
        >
          {sent ? '재전송' : '인증요청'}
        </button>
      </div>

      {sent && !verified && (
        <>
          {/* 데모 안내: 실제 SMS 대신 발급된 코드를 노출 */}
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
              onClick={verify}
              disabled={code.length !== 6 || busy}
              className="px-5 rounded-2xl bg-emerald-500 text-white text-sm font-bold disabled:opacity-40"
            >
              확인
            </button>
          </div>
        </>
      )}

      {verified && (
        <div className="rounded-2xl bg-emerald-50 ring-1 ring-emerald-200 px-4 py-3 text-sm text-emerald-700 flex items-center gap-2">
          <Check checked small /> 전화번호 인증 완료
        </div>
      )}

      {error && <p className="text-sm text-red-600 px-1">{error}</p>}

      <button
        disabled={!verified}
        onClick={onNext}
        className="w-full mt-2 bg-gradient-to-br from-emerald-500 to-teal-600 hover:brightness-105 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base disabled:opacity-50 disabled:hover:translate-y-0"
      >
        다음
      </button>
    </div>
  )
}

/* ---------- STEP 3: 계정 정보 ---------- */
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function AccountStep({ account, setAccount, onSubmit, submitting, error }) {
  const set = (k) => (e) => setAccount({ ...account, [k]: e.target.value })
  const emailValid = EMAIL_RE.test(account.email)
  const emailInvalid = account.email.length > 0 && !emailValid
  const pwMismatch = account.password2.length > 0 && account.password !== account.password2
  const canSubmit =
    emailValid && account.password.length >= 8 && account.nickname && !pwMismatch && !submitting

  return (
    <form
      onSubmit={(e) => { e.preventDefault(); onSubmit() }}
      noValidate
      className="space-y-3"
    >
      <div>
        <input
          type="email" value={account.email} onChange={set('email')}
          placeholder="이메일" autoComplete="email" aria-invalid={emailInvalid}
          className={`w-full px-4 py-3.5 rounded-2xl bg-white ring-1 outline-none text-sm focus:ring-2 transition-all ${
            emailInvalid ? 'ring-red-300 focus:ring-red-400' : 'ring-slate-200 focus:ring-emerald-400'
          }`}
        />
        <FieldError show={emailInvalid}>
          올바른 이메일 형식이 아니에요. 예: <span className="font-semibold">name@example.com</span>
        </FieldError>
      </div>

      <input
        type="password" value={account.password} onChange={set('password')}
        placeholder="비밀번호 (8자 이상)" autoComplete="new-password"
        className="w-full px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm"
      />

      <div>
        <input
          type="password" value={account.password2} onChange={set('password2')}
          placeholder="비밀번호 확인" autoComplete="new-password" aria-invalid={pwMismatch}
          className={`w-full px-4 py-3.5 rounded-2xl bg-white ring-1 outline-none text-sm focus:ring-2 transition-all ${
            pwMismatch ? 'ring-red-300 focus:ring-red-400' : 'ring-slate-200 focus:ring-emerald-400'
          }`}
        />
        <FieldError show={pwMismatch}>비밀번호가 일치하지 않아요.</FieldError>
      </div>

      <input
        type="text" value={account.nickname} onChange={set('nickname')}
        placeholder="닉네임"
        className="w-full px-4 py-3.5 rounded-2xl bg-white ring-1 ring-slate-200 focus:ring-2 focus:ring-emerald-400 outline-none text-sm"
      />

      {error && (
        <div className="flex items-start gap-2 px-3.5 py-3 rounded-2xl bg-red-50 ring-1 ring-red-200 text-sm text-red-600">
          <WarnIcon />
          <span className="leading-snug">{error}</span>
        </div>
      )}

      <button
        type="submit"
        disabled={!canSubmit}
        className="w-full mt-2 bg-gradient-to-br from-emerald-500 to-teal-600 hover:brightness-105 text-white font-bold py-4 rounded-2xl transition-all hover:-translate-y-0.5 shadow-sm text-base disabled:opacity-50 disabled:hover:translate-y-0"
      >
        {submitting ? '가입 중...' : '가입 완료'}
      </button>
    </form>
  )
}

function WarnIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="shrink-0 mt-0.5">
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
  )
}

// 부드럽게 펼쳐지는 인라인 필드 에러 (브라우저 기본 말풍선 대체)
function FieldError({ show, children }) {
  return (
    <div className={`grid transition-all duration-200 ease-out ${show ? 'grid-rows-[1fr] opacity-100 mt-2' : 'grid-rows-[0fr] opacity-0'}`}>
      <div className="overflow-hidden">
        <p className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-red-50 ring-1 ring-red-100 text-xs font-medium text-red-500">
          <WarnIcon />
          <span>{children}</span>
        </p>
      </div>
    </div>
  )
}
