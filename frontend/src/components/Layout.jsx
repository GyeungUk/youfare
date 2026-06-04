import { useEffect, useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import api from '../api/axios'

// 모든 메뉴는 게스트도 진입 가능 — 로그인 유도는 각 페이지 안에서 처리한다.
// (authOnly 플래그는 더 이상 쓰지 않지만, 향후 완전 비공개 메뉴가 생기면 재사용할 수 있게 로직은 남겨둠)
const navItems = [
  { to: '/welfare', label: '복지 목록', icon: '🏠' },
  { to: '/recommend', label: '맞춤 추천', icon: '🎯' },
  { to: '/scraps', label: '스크랩', icon: '📌' },
  { to: '/community', label: '커뮤니티', icon: '💬' },
  { to: '/chat', label: 'AI 상담', icon: '🤖' },
]

const gradeStyle = {
  새싹: 'bg-emerald-50 text-emerald-600 ring-emerald-200',
  잎새: 'bg-teal-50 text-teal-600 ring-teal-200',
  꽃: 'bg-pink-50 text-pink-600 ring-pink-200',
  열매: 'bg-amber-50 text-amber-600 ring-amber-200',
  나무: 'bg-violet-50 text-violet-600 ring-violet-200',
}

export default function Layout() {
  const navigate = useNavigate()
  const [me, setMe] = useState(null)
  const isAuthed = !!localStorage.getItem('token')

  useEffect(() => {
    // 게스트는 /users/me(인증 필요)를 호출하지 않는다 — 불필요한 401 방지
    if (!isAuthed) return
    api.get('/users/me').then((r) => setMe(r.data.data)).catch(() => {})
  }, [isAuthed])

  function logout() {
    localStorage.removeItem('token')
    navigate('/login')
  }

  // authOnly 메뉴를 게스트가 누르면 로그인 페이지로 (로그인 후 원래 메뉴로 복귀)
  const navTarget = (item) =>
    item.authOnly && !isAuthed
      ? `/login?redirect=${encodeURIComponent(item.to)}`
      : item.to

  const linkClass = ({ isActive }) =>
    `px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
      isActive
        ? 'bg-emerald-50 text-emerald-700 shadow-sm'
        : 'text-slate-500 hover:bg-slate-100 hover:text-slate-700'
    }`

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <header className="sticky top-0 z-50 glass border-b border-white/50">
        <div className="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between gap-4">
          <NavLink to="/welfare" className="flex items-center gap-1.5 shrink-0">
            <span className="text-xl">🌱</span>
            <span className="text-2xl font-black tracking-tight text-gradient">YouFare</span>
          </NavLink>

          <nav className="hidden md:flex items-center gap-1">
            {navItems.map((item) => (
              <NavLink key={item.to} to={navTarget(item)} className={linkClass}>
                {item.label}
                {item.authOnly && !isAuthed && <span className="ml-1 opacity-60">🔒</span>}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-3">
            {isAuthed ? (
              <>
                {me && (
                  <div className="hidden sm:flex items-center gap-2">
                    <span className={`text-xs font-bold px-2.5 py-1 rounded-full ring-1 ${gradeStyle[me.grade] ?? gradeStyle['새싹']}`}>
                      {me.grade}
                    </span>
                    <span className="text-xs font-semibold text-slate-400">{me.point ?? 0}P</span>
                  </div>
                )}
                <button
                  onClick={logout}
                  className="text-sm text-slate-400 hover:text-slate-700 transition-colors font-medium"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <NavLink
                to="/login"
                className="text-sm font-semibold text-white bg-gradient-to-r from-emerald-500 to-teal-600 px-4 py-2 rounded-full shadow-sm shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
              >
                로그인
              </NavLink>
            )}
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-6xl mx-auto w-full px-5 py-8 pb-nav-safe md:pb-8 animate-fade-in">
        <Outlet />
      </main>

      {/* 모바일 하단 네비 */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 glass border-t border-white/50 pb-safe px-safe">
        <div className="flex">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={navTarget(item)}
              className={({ isActive }) =>
                `flex-1 py-2.5 flex flex-col items-center gap-0.5 text-[11px] font-semibold transition-colors ${
                  isActive ? 'text-emerald-600' : 'text-slate-400'
                }`
              }
            >
              <span className="text-lg leading-none">
                {item.authOnly && !isAuthed ? '🔒' : item.icon}
              </span>
              {item.label}
            </NavLink>
          ))}
        </div>
      </nav>
    </div>
  )
}
