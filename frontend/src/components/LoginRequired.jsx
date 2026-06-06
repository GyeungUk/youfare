import { useLocation, useNavigate } from 'react-router-dom'

/**
 * 로그인이 있어야 의미가 있는 페이지(맞춤 추천·스크랩)에서
 * 게스트에게 보여주는 인라인 안내 상태.
 * 페이지 자체는 열리되 콘텐츠 대신 "로그인하면 쓸 수 있어요"를 노출한다.
 */
export default function LoginRequired({ icon = '🔒', title, desc }) {
  const navigate = useNavigate()
  const loc = useLocation()

  return (
    <div className="text-center py-20 bg-white rounded-3xl ring-1 ring-slate-100">
      <p className="text-6xl mb-4">{icon}</p>
      <p className="text-slate-700 font-bold text-lg mb-2">{title}</p>
      <p className="text-slate-400 text-sm mb-7 px-6 leading-relaxed whitespace-pre-line">{desc}</p>
      <button
        onClick={() => navigate(`/login?redirect=${encodeURIComponent(loc.pathname)}`)}
        className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-6 py-3 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
      >
        로그인하기 →
      </button>
    </div>
  )
}
