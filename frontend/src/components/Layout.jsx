import { Outlet, NavLink, useNavigate } from 'react-router-dom'

const navItems = [
  { to: '/welfare', label: '복지 목록' },
  { to: '/recommend', label: '맞춤 추천' },
  { to: '/scraps', label: '스크랩' },
  { to: '/chat', label: 'AI 상담' },
]

export default function Layout() {
  const navigate = useNavigate()

  function logout() {
    localStorage.removeItem('token')
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white border-b border-slate-200 sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <NavLink to="/welfare" className="flex items-center gap-2">
            <span className="text-2xl font-black bg-gradient-to-r from-emerald-500 to-teal-600 bg-clip-text text-transparent">
              YouFare
            </span>
          </NavLink>
          <nav className="hidden md:flex items-center gap-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-emerald-50 text-emerald-600'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <button
            onClick={logout}
            className="text-sm text-slate-500 hover:text-slate-700 transition-colors"
          >
            로그아웃
          </button>
        </div>
      </header>

      <main className="flex-1 max-w-6xl mx-auto w-full px-4 py-8">
        <Outlet />
      </main>

      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200">
        <div className="flex">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex-1 py-3 text-xs text-center font-medium transition-colors ${
                  isActive ? 'text-emerald-600' : 'text-slate-500'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      </nav>
    </div>
  )
}
