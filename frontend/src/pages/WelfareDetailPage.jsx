import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/axios'

const catMeta = {
  HOUSING: { label: '주거', color: 'bg-sky-50 text-sky-600', dot: 'bg-sky-500', emoji: '🏠', intro: '내 집 마련부터 월세 부담까지, 주거 걱정을 덜어주는 혜택이에요.' },
  EMPLOYMENT: { label: '취업', color: 'bg-violet-50 text-violet-600', dot: 'bg-violet-500', emoji: '💼', intro: '취업·진로를 준비하는 청년이라면 꼭 챙겨봐야 할 혜택이에요.' },
  FINANCE: { label: '금융', color: 'bg-amber-50 text-amber-600', dot: 'bg-amber-500', emoji: '💸', intro: '목돈 마련과 금융 부담을 든든하게 받쳐주는 혜택이에요.' },
  EDUCATION: { label: '교육', color: 'bg-pink-50 text-pink-600', dot: 'bg-pink-500', emoji: '📚', intro: '배우고 성장하고 싶은 청년을 응원하는 혜택이에요.' },
  ETC: { label: '기타', color: 'bg-slate-100 text-slate-500', dot: 'bg-slate-400', emoji: '✨', intro: '청년이라면 놓치기 아까운 알찬 혜택이에요.' },
}

// 백엔드가 코드값 대신 내려주는 막연한 소득 라벨 → 친절한 안내 문구로 변환
const GENERIC_INCOME = {
  '제한 없음': { tone: 'ok', title: '소득 제한 없음', desc: '소득에 관계없이 누구나 신청할 수 있어요.' },
  '별도 소득 기준 있음': { tone: 'warn', title: '소득 기준 있음', desc: '소득 기준을 충족해야 신청할 수 있어요. 정확한 금액 기준은 아래 신청 페이지에서 꼭 확인하세요.' },
  // 아래 둘은 구버전 동기화 데이터 호환용
  '소득 조건 있음': { tone: 'warn', title: '소득 기준 있음', desc: '소득 기준을 충족해야 신청할 수 있어요. 정확한 금액 기준은 아래 신청 페이지에서 꼭 확인하세요.' },
  '기타': { tone: 'warn', title: '별도 소득 기준', desc: '소득 관련 별도 기준이 있어요. 자세한 내용은 신청 페이지에서 확인하세요.' },
}

/**
 * 소득 조건 텍스트를 화면에 친절하게 보여주기 위해 분석한다.
 * - 막연한 라벨('소득 조건 있음' 등)이면 안내 문구로 치환
 * - 실제 상세 텍스트면 핵심 문장만 추려 요약(summary)하고 전체(full)도 함께 제공
 */
function analyzeIncome(raw) {
  if (!raw) return null
  const text = String(raw).replace(/\s+/g, ' ').trim()
  if (!text) return null

  const generic = GENERIC_INCOME[text]
  if (generic) return { kind: 'generic', ...generic }

  // 구분자(마침표·중점·줄바꿈)로 쪼갠 뒤 소득 관련 핵심 키워드가 든 문장만 추출
  const sentences = text
    .split(/(?<=[.。!?])\s+|\n+|[·ㆍ•]/)
    .map((s) => s.trim())
    .filter(Boolean)
  const KEY = /(중위\s*소득|소득|만원|[0-9][0-9,]*\s*원|이하|이상|미만|초과|건강보험|재산|자산|%|퍼센트|분위|무관|제한)/
  const key = sentences.filter((s) => KEY.test(s))
  const isLong = text.length > 70

  return {
    kind: 'detail',
    full: text,
    summary: isLong && key.length ? key.slice(0, 3) : null,
  }
}

// ISO 날짜(2025-12-22) → 2025.12.22 / 비정상 값은 그대로 반환
function fmtDate(d) {
  if (!d) return null
  const m = String(d).match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[1]}.${m[2]}.${m[3]}` : d
}

function InfoCard({ icon, label, value }) {
  return (
    <div className="bg-slate-50 rounded-2xl p-4">
      <p className="text-xs text-slate-400 mb-1">{icon} {label}</p>
      <p className="font-semibold text-slate-700">{value}</p>
    </div>
  )
}

function IncomeSection({ raw }) {
  const [open, setOpen] = useState(false)
  const info = analyzeIncome(raw)
  if (!info) return null

  return (
    <div className="mb-7">
      <h2 className="font-bold text-slate-900 mb-3 flex items-center gap-2">
        <span className="w-1 h-4 bg-emerald-500 rounded-full" />
        소득 조건은요?
      </h2>

      {info.kind === 'generic' ? (
        <div
          className={`flex items-start gap-3 rounded-2xl px-5 py-4 ring-1 ${
            info.tone === 'ok'
              ? 'bg-emerald-50 ring-emerald-100'
              : 'bg-amber-50 ring-amber-100'
          }`}
        >
          <span className="text-xl leading-6">{info.tone === 'ok' ? '✅' : '💰'}</span>
          <div>
            <p className={`font-bold text-sm ${info.tone === 'ok' ? 'text-emerald-700' : 'text-amber-700'}`}>
              {info.title}
            </p>
            <p className={`text-xs leading-5 mt-0.5 ${info.tone === 'ok' ? 'text-emerald-600/90' : 'text-amber-700/90'}`}>
              {info.desc}
            </p>
          </div>
        </div>
      ) : info.summary ? (
        <div className="rounded-2xl bg-amber-50/70 ring-1 ring-amber-100 px-5 py-4">
          <p className="text-xs font-bold text-amber-700 mb-2.5">💰 핵심만 콕!</p>
          <ul className="space-y-1.5">
            {info.summary.map((line, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-slate-700 leading-6">
                <span className="mt-2 w-1.5 h-1.5 rounded-full bg-amber-400 shrink-0" />
                <span>{line}</span>
              </li>
            ))}
          </ul>
          <button
            onClick={() => setOpen((v) => !v)}
            className="mt-3 text-xs font-semibold text-amber-700 hover:underline"
          >
            {open ? '접기 ▲' : '소득 조건 원문 전체 보기 ▾'}
          </button>
          {open && (
            <p className="mt-2 text-xs leading-6 text-slate-500 whitespace-pre-line border-t border-amber-100 pt-2">
              {info.full}
            </p>
          )}
        </div>
      ) : (
        <div className="flex items-start gap-3 rounded-2xl bg-amber-50/70 ring-1 ring-amber-100 px-5 py-4">
          <span className="text-xl leading-6">💰</span>
          <p className="text-sm text-slate-700 leading-6">{info.full}</p>
        </div>
      )}
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

  const isAuthed = !!localStorage.getItem('token')

  useEffect(() => {
    let active = true
    async function load() {
      try {
        // 스크랩 여부는 로그인 유저만 조회 (게스트는 빈 목록 취급)
        const [detail, scraps] = await Promise.all([
          api.get(`/welfare/${id}`),
          isAuthed ? api.get('/scraps/me').catch(() => ({ data: { data: [] } })) : Promise.resolve({ data: { data: [] } }),
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
    // 게스트는 스크랩 불가 → 로그인 페이지로 (로그인 후 이 상세로 복귀)
    if (!isAuthed) {
      navigate(`/login?redirect=${encodeURIComponent(`/welfare/${id}`)}`)
      return
    }
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
  const statusMeta = {
    ONGOING: { label: '진행중', emoji: '🟢' },
    UPCOMING: { label: '신청 예정', emoji: '🔵' },
    CLOSED: { label: '신청 마감', emoji: '⚪️' },
  }[item.status]
  const ddayLabel = item.status === 'ONGOING' && item.dDay != null
    ? (item.dDay <= 0 ? '오늘 마감' : `D-${item.dDay}`)
    : null
  // 동기화 데이터에 빈 문자열·비정상 값이 섞여 있어 http(s)로 시작하는 경우만 신청 링크로 인정
  const applyUrl = /^https?:\/\//i.test(item.sourceUrl ?? '') ? item.sourceUrl : null

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
              {statusMeta && (
                <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-white/20 backdrop-blur">
                  {statusMeta.emoji} {statusMeta.label}
                </span>
              )}
              {ddayLabel && (
                <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-white/25 backdrop-blur ring-1 ring-white/30">
                  {ddayLabel}
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
              <InfoCard icon="🗓" label="신청 기간" value={`${fmtDate(item.applyStartDate)} ~ ${fmtDate(item.applyEndDate) ?? '상시'}`} />
            )}
          </div>

          <IncomeSection raw={item.incomeCondition} />

          <div className="mb-7">
              <h2 className="font-bold text-slate-900 mb-3 flex items-center gap-2">
                <span className="w-1 h-4 bg-emerald-500 rounded-full" />
                어떤 혜택인가요?
              </h2>

              {/* 친근한 한 줄 소개 */}
              <div className="flex items-center gap-3 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-600 text-white px-5 py-4 mb-3 shadow-sm shadow-emerald-500/20">
                <span className="text-2xl leading-none">{meta.emoji}</span>
                <p className="text-sm font-semibold leading-snug">{meta.intro}</p>
              </div>

              {/* 정책 원문 설명 (없으면 안내 문구) */}
              <div className="relative rounded-2xl bg-gradient-to-br from-emerald-50/80 to-teal-50/40 ring-1 ring-emerald-100/70 p-5 pl-6">
                <span className="absolute left-0 top-5 bottom-5 w-1 rounded-full bg-gradient-to-b from-emerald-400 to-teal-500" />
                {item.description ? (
                  <p className="text-slate-700 leading-7 whitespace-pre-line">{item.description}</p>
                ) : (
                  <p className="text-slate-500 leading-7">
                    이 혜택의 자세한 설명은 아직 제공되지 않아요. 아래 <span className="font-semibold text-emerald-700">신청 페이지</span>에서 정확한 내용을 확인해 주세요.
                  </p>
                )}
              </div>

              {/* 이런 분께 딱! — 핵심 정보 요약 칩 */}
              <p className="mt-5 mb-2 text-sm font-bold text-slate-800">이런 분께 딱이에요 👇</p>
              <div className="flex flex-wrap gap-2">
                <span className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full bg-white ring-1 ring-slate-200 text-slate-600">
                  <span className={`w-1.5 h-1.5 rounded-full ${meta.dot}`} />
                  {meta.label}에 관심 있는 청년
                </span>
                {item.region && (
                  <span className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-full bg-white ring-1 ring-slate-200 text-slate-600">
                    📍 {item.region === '전국' ? '어디서나 신청 OK' : `${item.region} 거주 청년`}
                  </span>
                )}
                {item.targetAgeMin != null && (
                  <span className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-full bg-white ring-1 ring-slate-200 text-slate-600">
                    🎂 {item.targetAgeMin}~{item.targetAgeMax ?? '제한없음'}세
                  </span>
                )}
                {item.incomeCondition && (
                  <span className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-full bg-white ring-1 ring-slate-200 text-slate-600">
                    💰 {item.incomeCondition === '제한 없음' ? '소득 제한 없음' : '소득 기준 확인 필요'}
                  </span>
                )}
              </div>

              {/* 친근한 안내 배너 */}
              <div className="mt-5 flex items-start gap-2.5 rounded-2xl bg-amber-50 ring-1 ring-amber-100 px-4 py-3">
                <span className="text-base leading-5">💡</span>
                <p className="text-xs leading-5 text-amber-700">
                  <span className="font-bold">잠깐!</span> 자세한 자격·신청 방법은 기관마다 조금씩 달라요.
                  신청 전에 아래 <span className="font-bold">신청 페이지</span>에서 최신 공고를 한 번 더 확인해 주세요 :)
                </p>
              </div>
            </div>

          {/* 신청 유도 한마디 — 상태에 따라 문구가 달라져요 */}
          <p className="text-center text-sm font-semibold text-slate-500 mb-3">
            {item.status === 'CLOSED'
              ? '이번 모집은 마감됐어요. 다음 기회를 위해 스크랩해 두세요 📌'
              : item.status === 'UPCOMING'
              ? '곧 신청이 시작돼요. 미리 스크랩해 두면 놓치지 않아요! 🔔'
              : '마음에 든다면, 지금 바로 신청해 보세요! 🌱'}
          </p>
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
            {applyUrl ? (
              <a
                href={applyUrl}
                target="_blank"
                rel="noreferrer"
                className={`flex-1 text-center text-white font-bold py-4 rounded-2xl shadow-md transition-all ${
                  item.status === 'CLOSED'
                    ? 'bg-slate-400 shadow-slate-400/25 hover:-translate-y-0.5'
                    : 'bg-gradient-to-r from-emerald-500 to-teal-600 shadow-emerald-500/25 hover:-translate-y-0.5'
                }`}
              >
                {item.status === 'CLOSED' ? '공고 페이지 보기 →' : '신청 페이지 바로가기 →'}
              </a>
            ) : (
              <div className="flex-1 text-center bg-slate-100 text-slate-400 font-bold py-4 rounded-2xl cursor-not-allowed">
                신청 링크가 제공되지 않아요
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
