import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import api from '../api/axios'
import { WelfareCard } from './WelfareListPage'
import LoginRequired from '../components/LoginRequired'

const PAGE_SIZE = 12

const categories = [
  { value: '', label: '전체', icon: '✨' },
  { value: 'HOUSING', label: '주거', icon: '🏡' },
  { value: 'EMPLOYMENT', label: '취업', icon: '💼' },
  { value: 'FINANCE', label: '금융', icon: '💰' },
  { value: 'EDUCATION', label: '교육', icon: '📚' },
  { value: 'ETC', label: '기타', icon: '🎁' },
]

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
  // 섹터/페이지를 URL 쿼리에 보관 → 상세에서 뒤로가기 시 보던 섹터로 복원된다.
  const [searchParams, setSearchParams] = useSearchParams()
  const category = searchParams.get('category') || ''
  const page = Number(searchParams.get('page')) || 0
  const isAuthed = !!localStorage.getItem('token')
  const [items, setItems] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [totalPages, setTotalPages] = useState(0)

  // 카테고리/페이지 변경 시 해당 섹터의 추천만 다시 불러온다.
  // totalElements가 섹터별 혜택 개수이므로 상단 배지에 그대로 반영된다.
  useEffect(() => {
    if (!isAuthed) {
      setLoading(false)
      return
    }
    setLoading(true)
    const params = { page, size: PAGE_SIZE }
    if (category) params.category = category
    api.get('/welfare/recommend', { params })
      .then((r) => {
        const data = r.data.data
        setItems(data.content)
        setTotal(data.totalElements)
        setTotalPages(data.totalPages)
      })
      .catch(() => { setItems([]); setTotal(0); setTotalPages(0) })
      .finally(() => setLoading(false))
  }, [isAuthed, category, page])

  // replace:true → 섹터 전환마다 히스토리가 쌓이지 않게 현재 항목을 갱신만 한다.
  function changeCategory(cat) {
    const p = new URLSearchParams(searchParams)
    if (cat) p.set('category', cat); else p.delete('category')
    p.delete('page')
    setSearchParams(p, { replace: true })
  }

  function goPage(n) {
    const p = new URLSearchParams(searchParams)
    if (n) p.set('page', String(n)); else p.delete('page')
    setSearchParams(p, { replace: true })
  }

  // 현재 페이지 주변만 노출 (최대 5개 윈도우)
  const pageWindow = (() => {
    if (totalPages <= 1) return []
    const start = Math.max(0, Math.min(page - 2, totalPages - 5))
    const end = Math.min(totalPages, start + 5)
    return Array.from({ length: end - start }, (_, i) => start + i)
  })()

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
      ) : (
        <>
          <div className="flex gap-2 mb-7 flex-wrap">
            {categories.map((c) => (
              <button
                key={c.value}
                onClick={() => changeCategory(c.value)}
                className={`px-4 py-2 rounded-full text-sm font-semibold transition-all flex items-center gap-1.5 ${
                  category === c.value
                    ? 'bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-md shadow-emerald-500/25'
                    : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:ring-emerald-300 hover:text-emerald-600'
                }`}
              >
                <span>{c.icon}</span>
                {c.label}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {Array.from({ length: 6 }).map((_, i) => <CardSkeleton key={i} />)}
            </div>
          ) : items.length === 0 ? (
            <div className="text-center py-20 bg-white rounded-3xl ring-1 ring-slate-100">
              <p className="text-6xl mb-4">🔍</p>
              <p className="text-slate-700 font-bold text-lg mb-2">추천할 혜택이 없어요</p>
              <p className="text-slate-400 text-sm mb-7">
                {category ? '다른 카테고리를 선택해 보세요' : '온보딩 정보를 입력하면 맞춤 추천을 받을 수 있어요'}
              </p>
              {!category && (
                <button
                  onClick={() => navigate('/onboarding')}
                  className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-6 py-3 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
                >
                  프로필 설정하기 →
                </button>
              )}
            </div>
          ) : (
            <>
              <div className="relative overflow-hidden bg-gradient-to-r from-emerald-500 to-teal-600 rounded-3xl p-6 mb-6 text-white shadow-lg shadow-emerald-500/20">
                <div className="pointer-events-none absolute -top-10 -right-6 w-40 h-40 bg-white/10 rounded-full blur-2xl" />
                <div className="relative flex items-center gap-4">
                  <span className="text-4xl">✨</span>
                  <div>
                    <p className="font-bold text-lg">
                      {category
                        ? `${categories.find((c) => c.value === category)?.label} 혜택 ${total}개가 내 조건에 맞아요`
                        : `총 ${total}개의 혜택이 내 조건에 맞아요`}
                    </p>
                    <p className="text-emerald-50 text-sm mt-0.5">내 지역 혜택을 먼저, 전국 혜택을 그다음으로 보여드려요</p>
                  </div>
                </div>
              </div>
              <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
                {items.map((item) => (
                  <WelfareCard key={item.id} item={item} onClick={() => navigate(`/welfare/${item.id}`)} />
                ))}
              </div>

              {pageWindow.length > 0 && (
                <div className="flex justify-center items-center gap-1.5 mt-10">
                  <button
                    disabled={page === 0}
                    onClick={() => goPage(Math.max(0, page - 1))}
                    className="w-9 h-9 rounded-xl text-slate-500 hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
                  >
                    ←
                  </button>
                  {pageWindow.map((i) => (
                    <button
                      key={i}
                      onClick={() => goPage(i)}
                      className={`w-9 h-9 rounded-xl text-sm font-semibold transition-all ${
                        page === i
                          ? 'bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-md shadow-emerald-500/25'
                          : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:ring-emerald-300'
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                  <button
                    disabled={page >= totalPages - 1}
                    onClick={() => goPage(Math.min(totalPages - 1, page + 1))}
                    className="w-9 h-9 rounded-xl text-slate-500 hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
                  >
                    →
                  </button>
                </div>
              )}
            </>
          )}
        </>
      )}
    </div>
  )
}
