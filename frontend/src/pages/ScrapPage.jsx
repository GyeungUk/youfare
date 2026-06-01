import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

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
      setItems((prev) => prev.filter((s) => s.welfareId !== welfareId))
    } catch {
      alert('삭제에 실패했습니다.')
    }
  }

  useEffect(() => { load() }, [])

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-black text-slate-900">스크랩</h1>
        <p className="text-slate-500 mt-1">저장한 복지 혜택 목록이에요</p>
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="bg-slate-100 rounded-2xl h-24 animate-pulse" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20">
          <p className="text-5xl mb-4">📭</p>
          <p className="text-slate-600 font-semibold mb-2">스크랩한 혜택이 없어요</p>
          <p className="text-slate-400 text-sm mb-6">관심 있는 혜택을 저장해 보세요</p>
          <button
            onClick={() => navigate('/welfare')}
            className="bg-emerald-500 text-white font-bold px-6 py-3 rounded-xl hover:bg-emerald-600 transition-colors"
          >
            혜택 둘러보기
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-2xl border border-slate-100 p-5 flex items-center justify-between hover:border-emerald-200 transition-colors"
            >
              <div
                className="flex-1 cursor-pointer"
                onClick={() => navigate(`/welfare/${item.welfareId}`)}
              >
                <p className="font-bold text-slate-900">{item.welfareTitle}</p>
                <p className="text-sm text-slate-400 mt-0.5">{item.welfareCategory}</p>
              </div>
              <button
                onClick={() => removeScrap(item.welfareId)}
                className="ml-4 text-slate-400 hover:text-red-500 transition-colors text-sm font-medium"
              >
                삭제
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
