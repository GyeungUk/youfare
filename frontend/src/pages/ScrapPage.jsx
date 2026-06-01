import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

const catMeta = {
  HOUSING: { label: '주거', color: 'bg-sky-50 text-sky-600' },
  EMPLOYMENT: { label: '취업', color: 'bg-violet-50 text-violet-600' },
  FINANCE: { label: '금융', color: 'bg-amber-50 text-amber-600' },
  EDUCATION: { label: '교육', color: 'bg-pink-50 text-pink-600' },
  ETC: { label: '기타', color: 'bg-slate-100 text-slate-500' },
}

export default function ScrapPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  async function load() {
    try {
      const res = await api.get('/scraps/me')
      setItems(res.data.data)
    } catch {
      setItems([])
    } finally {
      setLoading(false)
    }
  }

  async function removeScrap(welfareId) {
    try {
      await api.delete(`/scraps/${welfareId}`)
      setItems((prev) => prev.filter((s) => s.welfare.id !== welfareId))
    } catch {
      alert('삭제에 실패했습니다.')
    }
  }

  useEffect(() => { load() }, [])

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-black text-slate-900">스크랩</h1>
        <p className="text-slate-500 mt-1.5">저장한 복지 혜택 목록이에요</p>
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="skeleton rounded-2xl h-24" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-3xl ring-1 ring-slate-100">
          <p className="text-6xl mb-4">📭</p>
          <p className="text-slate-700 font-bold text-lg mb-2">스크랩한 혜택이 없어요</p>
          <p className="text-slate-400 text-sm mb-7">관심 있는 혜택을 저장해 보세요</p>
          <button
            onClick={() => navigate('/welfare')}
            className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-6 py-3 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
          >
            혜택 둘러보기 →
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item) => {
            const meta = catMeta[item.welfare.category] ?? catMeta.ETC
            return (
              <div
                key={item.scrapId}
                className="lift group bg-white rounded-2xl ring-1 ring-slate-100 p-5 flex items-center justify-between gap-4 hover:ring-emerald-200 hover:shadow-lg hover:shadow-emerald-500/5"
              >
                <div
                  className="flex-1 cursor-pointer min-w-0"
                  onClick={() => navigate(`/welfare/${item.welfare.id}`)}
                >
                  <div className="flex items-center gap-2 mb-1.5">
                    <span className={`text-xs font-bold px-2 py-0.5 rounded-md ${meta.color}`}>
                      {meta.label}
                    </span>
                    {item.scrappedAt && (
                      <span className="text-xs text-slate-400">
                        {new Date(item.scrappedAt).toLocaleDateString('ko-KR')} 저장
                      </span>
                    )}
                  </div>
                  <p className="font-bold text-slate-900 truncate group-hover:text-emerald-700 transition-colors">
                    {item.welfare.title}
                  </p>
                </div>
                <button
                  onClick={() => removeScrap(item.welfare.id)}
                  className="shrink-0 text-slate-300 hover:text-red-500 hover:bg-red-50 w-9 h-9 rounded-xl flex items-center justify-center transition-colors"
                  aria-label="스크랩 삭제"
                >
                  🗑
                </button>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
