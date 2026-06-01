import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'

export default function WelfareDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [item, setItem] = useState(null)
  const [scrapped, setScrapped] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get(`/welfare/${id}`).then((r) => { setItem(r.data.data); setLoading(false) })
  }, [id])

  async function toggleScrap() {
    try {
      if (scrapped) {
        await api.delete(`/scraps/${id}`)
        setScrapped(false)
      } else {
        await api.post(`/scraps/${id}`)
        setScrapped(true)
      }
    } catch {
      alert('처리 중 오류가 발생했습니다.')
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )

  if (!item) return <div className="text-center py-32 text-slate-400">혜택을 찾을 수 없습니다.</div>

  return (
    <div className="max-w-2xl mx-auto">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-slate-500 hover:text-slate-700 mb-6 text-sm">
        ← 뒤로가기
      </button>

      <div className="bg-white rounded-3xl border border-slate-100 p-8">
        <div className="flex items-center justify-between mb-6">
          <span className="text-sm font-semibold bg-emerald-50 text-emerald-600 px-3 py-1.5 rounded-lg">
            {item.category}
          </span>
          <button
            onClick={toggleScrap}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold transition-colors ${
              scrapped ? 'bg-emerald-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {scrapped ? '📌 스크랩됨' : '📌 스크랩'}
          </button>
        </div>

        <h1 className="text-2xl font-black text-slate-900 mb-6">{item.title}</h1>

        <div className="grid grid-cols-2 gap-4 mb-6">
          {item.region && (
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-1">지역</p>
              <p className="font-semibold text-slate-700">📍 {item.region}</p>
            </div>
          )}
          {item.targetAgeMin != null && (
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-1">대상 연령</p>
              <p className="font-semibold text-slate-700">
                {item.targetAgeMin}세 ~ {item.targetAgeMax ?? '제한없음'}세
              </p>
            </div>
          )}
          {item.applyStartDate && (
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-1">신청 기간</p>
              <p className="font-semibold text-slate-700">
                {item.applyStartDate} ~ {item.applyEndDate ?? '상시'}
              </p>
            </div>
          )}
          {item.incomeCondition && (
            <div className="bg-slate-50 rounded-xl p-4">
              <p className="text-xs text-slate-400 mb-1">소득 조건</p>
              <p className="font-semibold text-slate-700">{item.incomeCondition}</p>
            </div>
          )}
        </div>

        <div className="mb-6">
          <h2 className="font-bold text-slate-900 mb-3">상세 내용</h2>
          <p className="text-slate-600 leading-relaxed whitespace-pre-line">{item.description}</p>
        </div>

        {item.sourceUrl && (
          <a
            href={item.sourceUrl}
            target="_blank"
            rel="noreferrer"
            className="block w-full text-center bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold py-4 rounded-2xl hover:opacity-90 transition-opacity"
          >
            신청 페이지 바로가기 →
          </a>
        )}
      </div>
    </div>
  )
}
