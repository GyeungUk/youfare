import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

const categories = [
  { value: '', label: '전체', icon: '✨' },
  { value: 'HOUSING', label: '주거', icon: '🏡' },
  { value: 'EMPLOYMENT', label: '취업', icon: '💼' },
  { value: 'FINANCE', label: '금융', icon: '💰' },
  { value: 'EDUCATION', label: '교육', icon: '📚' },
  { value: 'ETC', label: '기타', icon: '🎁' },
]

const catColor = {
  HOUSING: 'bg-sky-50 text-sky-600',
  EMPLOYMENT: 'bg-violet-50 text-violet-600',
  FINANCE: 'bg-amber-50 text-amber-600',
  EDUCATION: 'bg-pink-50 text-pink-600',
  ETC: 'bg-slate-100 text-slate-500',
}

const catLabel = (v) => categories.find((c) => c.value === v)?.label ?? v

export function WelfareCard({ item, onClick }) {
  const dday = item.applyEndDate
    ? Math.ceil((new Date(item.applyEndDate) - new Date()) / 86400000)
    : null

  return (
    <div
      onClick={onClick}
      className="lift group bg-white rounded-3xl ring-1 ring-slate-100 p-6 hover:ring-emerald-200 hover:shadow-xl hover:shadow-emerald-500/5 cursor-pointer"
    >
      <div className="flex items-start justify-between gap-3 mb-4">
        <span className={`text-xs font-bold px-2.5 py-1 rounded-lg ${catColor[item.category] ?? catColor.ETC}`}>
          {catLabel(item.category)}
        </span>
        {dday !== null && (
          <span
            className={`text-xs font-bold px-2.5 py-1 rounded-lg ${
              dday <= 0
                ? 'bg-slate-100 text-slate-400'
                : dday <= 7
                ? 'bg-red-50 text-red-500 ring-1 ring-red-100'
                : 'bg-slate-50 text-slate-500'
            }`}
          >
            {dday <= 0 ? '마감' : `D-${dday}`}
          </span>
        )}
      </div>
      <h3 className="font-bold text-slate-900 text-lg mb-2 line-clamp-2 group-hover:text-emerald-700 transition-colors">
        {item.title}
      </h3>
      <p className="text-slate-500 text-sm line-clamp-2 leading-relaxed min-h-[2.5rem]">
        {item.description || '자세한 내용은 상세 페이지에서 확인하세요.'}
      </p>
      <div className="flex items-center justify-between mt-4 pt-4 border-t border-slate-50">
        <span className="text-xs text-slate-400 font-medium">📍 {item.region || '전국'}</span>
        <span className="text-xs font-semibold text-emerald-600 opacity-0 group-hover:opacity-100 transition-opacity">
          자세히 보기 →
        </span>
      </div>
    </div>
  )
}

function CardSkeleton() {
  return (
    <div className="bg-white rounded-3xl ring-1 ring-slate-100 p-6">
      <div className="skeleton h-5 w-16 rounded-lg mb-4" />
      <div className="skeleton h-5 w-3/4 rounded mb-2" />
      <div className="skeleton h-4 w-full rounded mb-1.5" />
      <div className="skeleton h-4 w-2/3 rounded" />
      <div className="skeleton h-3 w-20 rounded mt-5" />
    </div>
  )
}

export default function WelfareListPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [category, setCategory] = useState('')
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  async function load(cat, p) {
    setLoading(true)
    try {
      const params = { page: p, size: 9 }
      if (cat) params.category = cat
      const res = await api.get('/welfare', { params })
      setItems(res.data.data.content)
      setTotalPages(res.data.data.totalPages)
    } catch {
      setItems([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load(category, page) }, [category, page])

  function changeCategory(cat) {
    setCategory(cat)
    setPage(0)
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
        <h1 className="text-3xl font-black text-slate-900">복지 혜택 목록</h1>
        <p className="text-slate-500 mt-1.5">공공데이터 기반 청년 복지 혜택을 확인하세요</p>
      </div>

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
          {Array.from({ length: 9 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-24 text-slate-400">
          <p className="text-6xl mb-4">📭</p>
          <p className="font-semibold text-slate-600">해당 조건의 혜택이 없습니다</p>
          <p className="text-sm mt-1">다른 카테고리를 선택해 보세요</p>
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {items.map((item) => (
            <WelfareCard key={item.id} item={item} onClick={() => navigate(`/welfare/${item.id}`)} />
          ))}
        </div>
      )}

      {pageWindow.length > 0 && (
        <div className="flex justify-center items-center gap-1.5 mt-10">
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="w-9 h-9 rounded-xl text-slate-500 hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
          >
            ←
          </button>
          {pageWindow.map((i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
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
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            className="w-9 h-9 rounded-xl text-slate-500 hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
          >
            →
          </button>
        </div>
      )}
    </div>
  )
}
