import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

export default function RecommendPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/welfare/recommend')
      .then((r) => setItems(r.data.data.content))
      .catch(() => setItems([]))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-black text-slate-900">맞춤 추천</h1>
        <p className="text-slate-500 mt-1">내 프로필에 맞는 혜택만 골라드렸어요</p>
      </div>

      {loading ? (
        <div className="grid md:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="bg-slate-100 rounded-2xl h-48 animate-pulse" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20">
          <p className="text-5xl mb-4">🔍</p>
          <p className="text-slate-600 font-semibold mb-2">추천할 혜택이 없어요</p>
          <p className="text-slate-400 text-sm mb-6">온보딩 정보를 입력하면 맞춤 추천을 받을 수 있어요</p>
          <button
            onClick={() => navigate('/onboarding')}
            className="bg-emerald-500 text-white font-bold px-6 py-3 rounded-xl hover:bg-emerald-600 transition-colors"
          >
            프로필 설정하기
          </button>
        </div>
      ) : (
        <>
          <div className="bg-emerald-50 border border-emerald-100 rounded-2xl p-4 mb-6 flex items-center gap-3">
            <span className="text-2xl">✨</span>
            <p className="text-emerald-700 font-medium text-sm">
              총 <span className="font-bold">{items.length}개</span>의 혜택이 내 조건에 맞아요
            </p>
          </div>
          <div className="grid md:grid-cols-3 gap-4">
            {items.map((item) => {
              const dday = item.applyEndDate
                ? Math.ceil((new Date(item.applyEndDate) - new Date()) / 86400000)
                : null
              return (
                <div
                  key={item.id}
                  onClick={() => navigate(`/welfare/${item.id}`)}
                  className="bg-white rounded-2xl border border-slate-100 p-6 hover:shadow-lg hover:border-emerald-200 transition-all cursor-pointer"
                >
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <span className="text-xs font-semibold bg-emerald-50 text-emerald-600 px-2.5 py-1 rounded-lg">
                      {item.category}
                    </span>
                    {dday !== null && (
                      <span className={`text-xs font-bold px-2.5 py-1 rounded-lg ${dday <= 7 ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-500'}`}>
                        {dday <= 0 ? '마감' : `D-${dday}`}
                      </span>
                    )}
                  </div>
                  <h3 className="font-bold text-slate-900 mb-2 line-clamp-2">{item.title}</h3>
                  <p className="text-slate-500 text-sm line-clamp-2">{item.description}</p>
                </div>
              )
            })}
          </div>
        </>
      )}
    </div>
  )
}
