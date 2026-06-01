import { useNavigate } from 'react-router-dom'

const features = [
  {
    icon: '🎯',
    title: '개인화 추천',
    desc: '나이·지역·소득·취업 상태를 분석해, 지금 신청 가능한 혜택만 정확하게 골라드립니다.',
    accent: 'from-emerald-400 to-teal-500',
  },
  {
    icon: '🤖',
    title: 'AI 복지 상담',
    desc: '내 프로필과 맞춤 혜택을 이해한 AI가, 막연한 질문에도 내 상황에 맞춰 답해드립니다.',
    accent: 'from-teal-400 to-cyan-500',
  },
  {
    icon: '📌',
    title: '혜택 스크랩',
    desc: '관심 혜택을 저장하고 마감일을 한눈에. 활동할수록 포인트와 등급이 쌓입니다.',
    accent: 'from-cyan-400 to-sky-500',
  },
]

const steps = [
  { num: '01', title: '소셜 로그인', desc: '카카오·네이버로 3초 만에 시작' },
  { num: '02', title: '프로필 입력', desc: '나이·지역·소득·취업 상태 한 번만' },
  { num: '03', title: '맞춤 혜택 확인', desc: '나에게 딱 맞는 혜택을 바로 추천' },
]

const stats = [
  { value: '1,000+', label: '큐레이션된 복지 혜택' },
  { value: '17개', label: '전국 시·도 커버리지' },
  { value: '24/7', label: 'AI 복지 상담' },
]

export default function LandingPage() {
  const navigate = useNavigate()
  const isLoggedIn = !!localStorage.getItem('token')
  const start = () => navigate(isLoggedIn ? '/welfare' : '/login')

  return (
    <div className="min-h-screen bg-white overflow-x-hidden">
      {/* Nav */}
      <header className="sticky top-0 z-50 glass border-b border-white/40">
        <div className="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between">
          <span className="text-2xl font-black tracking-tight text-gradient">YouFare</span>
          <button
            onClick={start}
            className="text-sm font-semibold text-emerald-700 hover:text-emerald-800 px-4 py-2 rounded-full hover:bg-emerald-50 transition-colors"
          >
            {isLoggedIn ? '서비스 이용하기 →' : '로그인'}
          </button>
        </div>
      </header>

      {/* Hero */}
      <section className="relative px-5 pt-24 pb-32 text-center">
        {/* 배경 블롭 */}
        <div className="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
          <div className="absolute -top-24 -left-24 w-96 h-96 bg-emerald-300/40 rounded-full blur-3xl animate-blob" />
          <div className="absolute top-10 -right-20 w-80 h-80 bg-cyan-300/40 rounded-full blur-3xl animate-blob" style={{ animationDelay: '4s' }} />
          <div className="absolute bottom-0 left-1/3 w-72 h-72 bg-teal-300/40 rounded-full blur-3xl animate-blob" style={{ animationDelay: '8s' }} />
        </div>

        <div className="max-w-3xl mx-auto">
          <span className="inline-flex items-center gap-2 bg-white/80 ring-1 ring-emerald-200 text-emerald-700 text-sm font-semibold px-4 py-1.5 rounded-full mb-8 shadow-sm animate-fade-up">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            청년 복지·금융 맞춤 큐레이션
          </span>

          <h1 className="text-5xl sm:text-6xl font-black text-slate-900 leading-[1.1] mb-6 animate-fade-up" style={{ animationDelay: '0.05s' }}>
            지금 나에게 맞는<br />
            <span className="text-gradient">복지 혜택</span>을 찾아보세요
          </h1>

          <p className="text-lg sm:text-xl text-slate-500 mb-10 leading-relaxed animate-fade-up" style={{ animationDelay: '0.1s' }}>
            수많은 복지 혜택 중 내 나이·지역·상황에 맞는 것만 추려드립니다.<br className="hidden sm:block" />
            AI 챗봇으로 궁금한 점도 바로 해결하세요.
          </p>

          <div className="flex flex-col sm:flex-row gap-3 justify-center animate-fade-up" style={{ animationDelay: '0.15s' }}>
            <button
              onClick={start}
              className="group bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-8 py-4 rounded-2xl shadow-lg shadow-emerald-500/30 hover:shadow-xl hover:shadow-emerald-500/40 hover:-translate-y-0.5 transition-all text-lg"
            >
              무료로 시작하기
              <span className="inline-block ml-1 group-hover:translate-x-1 transition-transform">→</span>
            </button>
            <button
              onClick={() => navigate('/welfare')}
              className="bg-white/80 text-slate-700 font-bold px-8 py-4 rounded-2xl ring-1 ring-slate-200 hover:bg-white hover:ring-slate-300 transition-all text-lg"
            >
              혜택 둘러보기
            </button>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-4 max-w-xl mx-auto mt-20 animate-fade-up" style={{ animationDelay: '0.2s' }}>
            {stats.map((s) => (
              <div key={s.label} className="text-center">
                <p className="text-3xl sm:text-4xl font-black text-gradient">{s.value}</p>
                <p className="text-xs sm:text-sm text-slate-400 mt-1 font-medium">{s.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="max-w-6xl mx-auto px-5 py-24">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl font-black text-slate-900 mb-3">
            YouFare가 <span className="text-gradient">특별한 이유</span>
          </h2>
          <p className="text-slate-500 text-lg">정보 비대칭은 줄이고, 혜택 접근성은 높입니다</p>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {features.map((f) => (
            <div
              key={f.title}
              className="lift group relative bg-white rounded-3xl p-8 ring-1 ring-slate-100 hover:ring-emerald-200 hover:shadow-xl hover:shadow-emerald-500/5"
            >
              <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${f.accent} flex items-center justify-center text-2xl mb-5 shadow-lg shadow-emerald-500/20 group-hover:scale-110 transition-transform`}>
                {f.icon}
              </div>
              <h3 className="text-xl font-bold text-slate-900 mb-3">{f.title}</h3>
              <p className="text-slate-500 leading-relaxed">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section className="bg-slate-50 py-24">
        <div className="max-w-5xl mx-auto px-5">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-black text-slate-900 mb-3">3단계면 충분해요</h2>
            <p className="text-slate-500 text-lg">복잡한 절차 없이, 바로 시작하세요</p>
          </div>
          <div className="grid md:grid-cols-3 gap-6 relative">
            {steps.map((s) => (
              <div key={s.num} className="relative bg-white rounded-3xl p-8 ring-1 ring-slate-100">
                <span className="text-5xl font-black text-emerald-100 absolute top-5 right-6 select-none">{s.num}</span>
                <div className="relative">
                  <h3 className="text-xl font-bold text-slate-900 mb-2">{s.title}</h3>
                  <p className="text-slate-500">{s.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="px-5 py-24">
        <div className="relative max-w-5xl mx-auto rounded-[2.5rem] overflow-hidden bg-gradient-to-br from-emerald-500 via-teal-600 to-cyan-600 px-8 py-20 text-center text-white shadow-2xl shadow-emerald-500/30">
          <div className="pointer-events-none absolute inset-0 opacity-30">
            <div className="absolute -top-16 left-10 w-64 h-64 bg-white/20 rounded-full blur-3xl animate-blob" />
            <div className="absolute bottom-0 right-10 w-72 h-72 bg-cyan-200/30 rounded-full blur-3xl animate-blob" style={{ animationDelay: '5s' }} />
          </div>
          <div className="relative">
            <h2 className="text-3xl sm:text-4xl font-black mb-4">지금 바로 시작하세요</h2>
            <p className="text-emerald-50 mb-10 text-lg">카카오 또는 네이버 계정으로 1초 만에 로그인</p>
            <button
              onClick={start}
              className="bg-white text-emerald-600 font-bold px-10 py-4 rounded-2xl hover:bg-emerald-50 hover:-translate-y-0.5 transition-all text-lg shadow-lg"
            >
              무료로 시작하기 →
            </button>
          </div>
        </div>
      </section>

      <footer className="border-t border-slate-100 text-center py-10 text-slate-400 text-sm">
        <span className="font-black text-gradient text-base">YouFare</span>
        <p className="mt-2">© 2026 YouFare. 청년 복지 맞춤 추천 서비스.</p>
      </footer>
    </div>
  )
}
