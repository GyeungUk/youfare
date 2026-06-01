import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'

const catMeta = {
  HOUSING: { label: '주거', color: 'bg-sky-50 text-sky-600' },
  EMPLOYMENT: { label: '취업', color: 'bg-violet-50 text-violet-600' },
  FINANCE: { label: '금융', color: 'bg-amber-50 text-amber-600' },
  EDUCATION: { label: '교육', color: 'bg-pink-50 text-pink-600' },
  ETC: { label: '기타', color: 'bg-slate-100 text-slate-500' },
}

function InfoCard({ icon, label, value }) {
  return (
    <div className="bg-slate-50 rounded-2xl p-4">
      <p className="text-xs text-slate-400 mb-1">{icon} {label}</p>
      <p className="font-semibold text-slate-700">{value}</p>
    </div>
  )
}

export default function WelfareDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [item, setItem] = useState(null)
  const [scrapped, setScrapped] = useState(false)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const [detail, scraps] = await Promise.all([
          api.get(`/welfare/${id}`),
          api.get('/scraps/me').catch(() => ({ data: { data: [] } })),
        ])
        if (!active) return
        setItem(detail.data.data)
        const scrapList = scraps.data.data ?? []
        setScrapped(scrapList.some((s) => String(s.welfare?.id) === String(id)))
      } catch {
        if (active) setItem(null)
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [id])

  async function toggleScrap() {
    if (busy) return
    setBusy(true)
    const next = !scrapped
    setScrapped(next) // 낙관적 업데이트
    try {
      if (next) await api.post(`/scraps/${id}`)
      else await api.delete(`/scraps/${id}`)
    } catch (err) {
      const status = err.response?.status
      if (status === 409) setScrapped(true)
      else if (status === 404) setScrapped(false)
      else {
        setScrapped(!next) // 롤백
        alert('처리 중 오류가 발생했습니다.')
      }
    } finally {
      setBusy(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )

  if (!item) return (
    <div className="text-center py-32 text-slate-400">
      <p className="text-5xl mb-3">🫥</p>
      <p className="font-semibold text-slate-600">혜택을 찾을 수 없습니다.</p>
      <button onClick={() => navigate('/welfare')} className="mt-5 text-emerald-600 font-semibold hover:underline">
        목록으로 돌아가기
      </button>
    </div>
  )

  const meta = catMeta[item.category] ?? catMeta.ETC
  const dday = item.applyEndDate
    ? Math.ceil((new Date(item.applyEndDate) - new Date()) / 86400000)
    : null

  return (
    <div className="max-w-2xl mx-auto animate-fade-up">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-slate-500 hover:text-slate-700 mb-5 text-sm transition-colors">
        ← 뒤로가기
      </button>

      <div className="bg-white rounded-[2rem] ring-1 ring-slate-100 overflow-hidden shadow-sm">
        {/* 그라데이션 헤더 */}
        <div className="relative bg-gradient-to-br from-emerald-500 to-teal-600 px-8 pt-8 pb-10 text-white">
          <div className="pointer-events-none absolute -top-8 -right-6 w-40 h-40 bg-white/10 rounded-full blur-2xl" />
          <div className="relative">
            <div className="flex items-center gap-2 mb-4">
              <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-white/20 backdrop-blur">
                {meta.label}
              </span>
              {dday !== null && (
                <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-white/20 backdrop-blur">
                  {dday <= 0 ? '마감' : `D-${dday}`}
                </span>
              )}
            </div>
            <h1 className="text-2xl font-black leading-snug">{item.title}</h1>
          </div>
        </div>

        <div className="p-8">
          <div className="grid grid-cols-2 gap-3 mb-7">
            {item.region && <InfoCard icon="📍" label="지역" value={item.region} />}
            {item.targetAgeMin != null && (
              <InfoCard icon="👤" label="대상 연령" value={`${item.targetAgeMin}세 ~ ${item.targetAgeMax ?? '제한없음'}세`} />
            )}
            {item.applyStartDate && (
              <InfoCard icon="🗓" label="신청 기간" value={`${item.applyStartDate} ~ ${item.applyEndDate ?? '상시'}`} />
            )}
            {item.incomeCondition && <InfoCard icon="💰" label="소득 조건" value={item.incomeCondition} />}
          </div>

          {item.description && (
            <div className="mb-7">
              <h2 className="font-bold text-slate-900 mb-3 flex items-center gap-2">
                <span className="w-1 h-4 bg-emerald-500 rounded-full" />
                상세 내용
              </h2>
              <p className="text-slate-600 leading-relaxed whitespace-pre-line">{item.description}</p>
            </div>
          )}

          <div className="flex gap-3">
            <button
              onClick={toggleScrap}
              disabled={busy}
              className={`shrink-0 flex items-center gap-2 px-5 py-4 rounded-2xl text-sm font-bold transition-all disabled:opacity-60 ${
                scrapped
                  ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/25'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {scrapped ? '📌 스크랩됨' : '📌 스크랩'}
            </button>
            {item.sourceUrl && (
              <a
                href={item.sourceUrl}
                target="_blank"
                rel="noreferrer"
                className="flex-1 text-center bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold py-4 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
              >
                신청 페이지 바로가기 →
              </a>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
