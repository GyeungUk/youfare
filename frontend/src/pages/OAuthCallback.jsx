import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import api from '../api/axios'

export default function OAuthCallback() {
  const [params] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    const token = params.get('token')
    if (!token) {
      navigate('/login', { replace: true })
      return
    }
    localStorage.setItem('token', token)

    // 게스트 상태에서 로그인 전용 메뉴를 누르고 들어왔다면, 로그인 후 그 경로로 돌려보낸다.
    const redirect = localStorage.getItem('postLoginRedirect')
    localStorage.removeItem('postLoginRedirect')

    // 이미 온보딩(생년 등 프로필 입력)을 마친 유저는 매번 온보딩을 다시 거치지 않고
    // 원래 가려던 곳(없으면 복지 목록)으로 보낸다. 신규/미완료 유저만 온보딩으로 안내.
    api.get('/users/me')
      .then((r) => {
        const onboarded = r.data?.data?.birthYear != null
        navigate(onboarded ? (redirect || '/welfare') : '/onboarding', { replace: true })
      })
      .catch(() => navigate('/onboarding', { replace: true }))
  }, [])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50">
      <div className="text-center animate-fade-in">
        <div className="inline-flex w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 items-center justify-center text-3xl shadow-lg shadow-emerald-500/30 mb-6 animate-float">
          🌱
        </div>
        <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-slate-500 font-medium">로그인 처리 중...</p>
      </div>
    </div>
  )
}
