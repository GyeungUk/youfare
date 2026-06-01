import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

const categories = [
  { value: '', label: '전체' },
  { value: 'HOUSING', label: '주거' },
  { value: 'EMPLOYMENT', label: '취업' },
  { value: 'FINANCE', label: '금융' },
  { value: 'EDUCATION', label: '교육' },
  { value: 'ETC', label: '기타' },
]

function WelfareCard({ item, onClick }) {
  const dday = item.applyEndDate
    ? Math.ceil((new Date(item.applyEndDate) - new Date()) / 86400000)
    : null

  return (
    <div
      onClick={onClick}
      className="bg-white rounded-2xl border border-slate-100 p-6 hover:shadow-lg hover:border-emerald-200 transition-all cursor-pointer"
    >
      <div className="flex items-start justify-between gap-3 mb-3">
        <span className="text-xs font-semibold bg-emerald-50 text-emerald-600 px-2.5 py-1 rounded-lg">
          {categories.find((c) => c.value === item.category)?.label ?? item.category}
        </span>
        {dday !== null && (
          <span className={`text-xs font-bold px-2.5 py-1 rounded-lg ${dday <= 7 ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-500'}`}>
            {dday <= 0 ? '마감' : `D-${dday}`}
          </span>
        )}
      </div>
      <h3 className="font-bold text-slate-900 text-lg mb-2 line-clamp-2">{item.title}</h3>
      <p className="text-slate-500 text-sm line-clamp-2">{item.description}</p>
      {item.region && (
        <p className="text-xs text-slate-400 mt-3">📍 {item.region}</p>
      )}
    </div>
  )
}

export default function WelfareListPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [category, setCategory] = useState('')
  const [loading, setLoading] = useState(false)
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

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-black text-slate-900">복지 혜택 목록</h1>
        <p className="text-slate-500 mt-1">공공데이터 기반 복지 혜택을 확인하세요</p>
      </div>

      <div className="flex gap-2 mb-6 flex-wrap">
        {categories.map((c) => (
          <button
            key={c.value}
            onClick={() => changeCategory(c.value)}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition-colors ${
              category === c.value
                ? 'bg-emerald-500 text-white'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {c.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="grid md:grid-cols-3 gap-4">
          {Array.from({ length: 9 }).map((_, i) => (
            <div key={i} className="bg-slate-100 rounded-2xl h-48 animate-pulse" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20 text-slate-400">
          <p className="text-5xl mb-4">📭</p>
          <p>해당 조건의 혜택이 없습니다</p>
        </div>
      ) : (
        <div className="grid md:grid-cols-3 gap-4">
          {items.map((item) => (
            <WelfareCard
              key={item.id}
              item={item}
              onClick={() => navigate(`/welfare/${item.id}`)}
            />
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-8">
          {Array.from({ length: totalPages }).map((_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
              className={`w-9 h-9 rounded-lg text-sm font-medium transition-colors ${
                page === i ? 'bg-emerald-500 text-white' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
              }`}
            >
              {i + 1}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
