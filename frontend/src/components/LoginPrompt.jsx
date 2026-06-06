import { useLocation, useNavigate } from 'react-router-dom'

/**
 * 게스트가 '쓰기' 동작(글 작성·댓글·좋아요)을 시도할 때 띄우는 로그인 유도 모달.
 * 페이지를 떠나지 않고 로그인 창을 띄워 자연스럽게 로그인으로 이어준다.
 */
export default function LoginPrompt({ open, onClose, title = '로그인이 필요해요', message }) {
  const navigate = useNavigate()
  const loc = useLocation()
  if (!open) return null

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center px-5" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm animate-fade-in" onClick={onClose} />
      <div className="relative w-full max-w-sm bg-white rounded-[2rem] shadow-2xl shadow-emerald-500/10 p-8 text-center animate-scale-in">
        <div className="inline-flex w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 items-center justify-center text-3xl shadow-lg shadow-emerald-500/30 mb-5">
          🔒
        </div>
        <h2 className="text-xl font-black text-slate-900">{title}</h2>
        <p className="text-slate-500 text-sm mt-2 leading-relaxed whitespace-pre-line">{message}</p>
        <div className="flex gap-2 mt-7">
          <button
            onClick={onClose}
            className="px-5 py-3 rounded-2xl text-sm font-bold bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
          >
            닫기
          </button>
          <button
            onClick={() => navigate(`/login?redirect=${encodeURIComponent(loc.pathname)}`)}
            className="flex-1 bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold py-3 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
          >
            로그인하러 가기 →
          </button>
        </div>
      </div>
    </div>
  )
}
