import { useNavigate } from 'react-router-dom'

const features = [
  {
    icon: '🎯',
    title: '개인화 추천',
    desc: '나이, 지역, 소득 수준에 맞는 복지 혜택만 골라서 추천해 드립니다.',
  },
  {
    icon: '🤖',
    title: 'AI 복지 상담',
    desc: '궁금한 점을 AI에게 물어보세요. 내 상황에 맞는 답변을 드립니다.',
  },
  {
    icon: '📌',
    title: '혜택 스크랩',
    desc: '관심 있는 혜택을 저장하고 나중에 한 번에 확인하세요.',
  },
]

export default function LandingPage() {
  const navigate = useNavigate()
  const isLoggedIn = !!localStorage.getItem('token')

  return (
    <div className="min-h-screen bg-white">
      <header className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <span className="text-2xl font-black bg-gradient-to-r from-emerald-500 to-teal-600 bg-clip-text text-transparent">
          YouFare
        </span>
        <button
          onClick={() => navigate(isLoggedIn ? '/welfare' : '/login')}
          className="text-sm font-medium text-emerald-600 hover:text-emerald-700"
        >
          {isLoggedIn ? '서비스 이용하기 →' : '로그인'}
        </button>
      </header>

      {/* Hero */}
      <section className="bg-gradient-to-br from-emerald-50 via-teal-50 to-white py-24 px-4 text-center">
        <div className="max-w-3xl mx-auto">
          <span className="inline-block bg-emerald-100 text-emerald-700 text-sm font-semibold px-4 py-1.5 rounded-full mb-6">
            청년 복지 맞춤 추천 서비스
          </span>
          <h1 className="text-5xl font-black text-slate-900 leading-tight mb-6">
            지금 나에게 맞는<br />
            <span className="bg-gradient-to-r from-emerald-500 to-teal-600 bg-clip-text text-transparent">
              복지 혜택
            </span>
            을 찾아보세요
          </h1>
          <p className="text-xl text-slate-500 mb-10">
            수백 가지 복지 혜택 중 내 나이, 지역, 상황에 맞는 것만 추려드립니다.<br />
            AI 챗봇으로 궁금한 점도 바로 해결하세요.
          </p>
          <div className="flex gap-4 justify-center">
            <button
              onClick={() => navigate(isLoggedIn ? '/welfare' : '/login')}
              className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-8 py-4 rounded-2xl hover:opacity-90 transition-opacity shadow-lg shadow-emerald-200 text-lg"
            >
              무료로 시작하기
            </button>
            <button
              onClick={() => navigate('/welfare')}
              className="bg-white text-slate-700 font-bold px-8 py-4 rounded-2xl border border-slate-200 hover:bg-slate-50 transition-colors text-lg"
            >
              혜택 둘러보기
            </button>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="max-w-6xl mx-auto px-4 py-24">
        <h2 className="text-3xl font-black text-center text-slate-900 mb-16">
          YouFare가 특별한 이유
        </h2>
        <div className="grid md:grid-cols-3 gap-8">
          {features.map((f) => (
            <div key={f.title} className="bg-slate-50 rounded-3xl p-8 hover:shadow-lg transition-shadow">
              <div className="text-5xl mb-4">{f.icon}</div>
              <h3 className="text-xl font-bold text-slate-900 mb-3">{f.title}</h3>
              <p className="text-slate-500 leading-relaxed">{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="bg-gradient-to-r from-emerald-500 to-teal-600 py-20 px-4 text-center text-white">
        <h2 className="text-3xl font-black mb-4">지금 바로 시작하세요</h2>
        <p className="text-emerald-100 mb-8 text-lg">카카오 또는 네이버 계정으로 1초 만에 로그인</p>
        <button
          onClick={() => navigate(isLoggedIn ? '/welfare' : '/login')}
          className="bg-white text-emerald-600 font-bold px-10 py-4 rounded-2xl hover:bg-emerald-50 transition-colors text-lg"
        >
          시작하기
        </button>
      </section>

      <footer className="text-center py-8 text-slate-400 text-sm">
        © 2024 YouFare. 청년 복지 맞춤 추천 서비스.
      </footer>
    </div>
  )
}
