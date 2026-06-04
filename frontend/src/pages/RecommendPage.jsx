import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { WelfareCard } from './WelfareListPage'
import LoginRequired from '../components/LoginRequired'

function CardSkeleton() {
  return (
    <div className="bg-white rounded-3xl ring-1 ring-slate-100 p-6">
      <div className="skeleton h-5 w-16 rounded-lg mb-4" />
      <div className="skeleton h-5 w-3/4 rounded mb-2" />
      <div className="skeleton h-4 w-full rounded mb-1.5" />
      <div className="skeleton h-4 w-2/3 rounded" />
    </div>
  )
}

export default function RecommendPage() {
  const navigate = useNavigate()
  const isAuthed = !!localStorage.getItem('token')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // 게스트는 추천 API(로그인 필요)를 호출하지 않고 로그인 안내만 보여준다.
    if (!isAuthed) {
      setLoading(false)
      return
    }
    api.get('/welfare/recommend')
      .then((r) => setItems(r.data.data.content))
      .catch(() => setItems([]))
      .finally(() => setLoading(false))
  }, [isAuthed])

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-black text-slate-900">맞춤 추천</h1>
        <p className="text-slate-500 mt-1.5">내 프로필에 맞는 혜택만 골라드렸어요</p>
      </div>

      {!isAuthed ? (
        <LoginRequired
          icon="🎯"
          title="로그인하고 맞춤 추천 받기"
          desc={'나이·지역·상황에 맞는 혜택만 골라드려요.\n로그인하면 바로 확인할 수 있어요.'}
        />
      ) : loading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {Array.from({ length: 6 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-3xl ring-1 ring-slate-100">
          <p className="text-6xl mb-4">🔍</p>
          <p className="text-slate-700 font-bold text-lg mb-2">추천할 혜택이 없어요</p>
          <p className="text-slate-400 text-sm mb-7">온보딩 정보를 입력하면 맞춤 추천을 받을 수 있어요</p>
          <button
            onClick={() => navigate('/onboarding')}
            className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-6 py-3 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
          >
            프로필 설정하기 →
          </button>
        </div>
      ) : (
        <>
          <div className="relative overflow-hidden bg-gradient-to-r from-emerald-500 to-teal-600 rounded-3xl p-6 mb-6 text-white shadow-lg shadow-emerald-500/20">
            <div className="pointer-events-none absolute -top-10 -right-6 w-40 h-40 bg-white/10 rounded-full blur-2xl" />
            <div className="relative flex items-center gap-4">
              <span className="text-4xl">✨</span>
              <div>
                <p className="font-bold text-lg">
                  총 {items.length}개의 혜택이 내 조건에 맞아요
                </p>
                <p className="text-emerald-50 text-sm mt-0.5">나이·지역·신청 기간을 모두 분석한 결과예요</p>
              </div>
            </div>
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {items.map((item) => (
              <WelfareCard key={item.id} item={item} onClick={() => navigate(`/welfare/${item.id}`)} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
