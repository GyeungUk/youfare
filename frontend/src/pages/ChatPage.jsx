import { useState, useRef, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/axios'

// 비로그인 게스트의 1일 상담 한도(백엔드 chat.guest-daily-limit 기본값과 맞춤). 안내 문구용.
const GUEST_DAILY_LIMIT = 5

function Avatar() {
  return (
    <div className="w-9 h-9 rounded-2xl bg-gradient-to-br from-emerald-400 to-teal-500 flex items-center justify-center text-white font-bold text-xs shrink-0 shadow-md shadow-emerald-500/20">
      AI
    </div>
  )
}

function Message({ msg }) {
  const isUser = msg.role === 'user'
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4 animate-fade-up`}>
      {!isUser && <div className="mr-2.5 mt-0.5"><Avatar /></div>}
      <div
        className={`max-w-[78%] px-4 py-3 text-sm leading-relaxed whitespace-pre-line shadow-sm ${
          isUser
            ? 'bg-gradient-to-br from-emerald-500 to-teal-600 text-white rounded-2xl rounded-br-md'
            : 'bg-white ring-1 ring-slate-100 text-slate-700 rounded-2xl rounded-bl-md'
        }`}
      >
        {msg.content}
      </div>
    </div>
  )
}

const INITIAL = [
  {
    role: 'assistant',
    content: '안녕하세요! 저는 YouFare AI 복지 상담사예요 😊\n궁금한 복지 혜택에 대해 무엇이든 물어보세요. 내 프로필에 맞는 정보를 알려드릴게요!',
  },
]

// 주제별 추천 질문 풀. 사용자가 고른 질문의 "주제"를 인식해, 같은 주제의 더 깊은 질문을 이어서 추천한다.
const TOPICS = {
  housing: {
    icon: '🏠',
    keywords: ['주거', '월세', '전세', '집', '임대', '행복주택', '청약', '기숙사', '주택', '보증금'],
    questions: [
      '청년 월세 지원은 얼마나 받을 수 있어?',
      '전세자금 대출(버팀목) 조건이 궁금해',
      '행복주택 신청 자격과 방법 알려줘',
      '청년 주거급여 분리지급이 뭐야?',
      '청년 우대형 청약통장 혜택이 궁금해',
    ],
  },
  job: {
    icon: '💼',
    keywords: ['취업', '일자리', '구직', '면접', '창업', '직업훈련', '인턴', '채용', '내일채움', '취준'],
    questions: [
      '국민취업지원제도로 뭘 받을 수 있어?',
      '청년내일채움공제 조건이 궁금해',
      '면접 정장·구직활동 지원도 있어?',
      '청년 창업 지원금은 어떻게 신청해?',
      'K-디지털 트레이닝 직업훈련 알려줘',
    ],
  },
  finance: {
    icon: '💰',
    keywords: ['금융', '대출', '적금', '통장', '자산', '저축', '도약계좌', '희망적금', '햇살론', '소득', '목돈'],
    questions: [
      '청년도약계좌 가입 조건과 혜택 알려줘',
      '도약계좌랑 희망적금은 뭐가 달라?',
      '햇살론 유스 대출이 궁금해',
      '내 소득이면 어떤 금융 혜택을 받을 수 있어?',
      '청년 우대형 청약통장으로 목돈 모으는 법',
    ],
  },
  education: {
    icon: '🎓',
    keywords: ['교육', '학자금', '장학', '학비', '자격증', '어학', '국가장학금', '내일배움', '등록금', '공부'],
    questions: [
      '국가장학금 신청 자격이 궁금해',
      '내일배움카드로 뭘 배울 수 있어?',
      '청년 어학·자격증 응시료 지원 있어?',
      '학자금 대출 이자 지원도 받을 수 있어?',
    ],
  },
  life: {
    icon: '🌱',
    keywords: ['건강', '심리', '상담', '의료', '문화', '교통', '복지', '바우처', '검진', '마음'],
    questions: [
      '청년 마음건강 심리상담 바우처 알려줘',
      '청년 국가건강검진 대상도 되나요?',
      '문화누리카드 같은 문화 지원이 궁금해',
      '청년 대중교통비 지원(K-패스) 알려줘',
    ],
  },
}

// 처음 보여줄 추천 질문 (주거·취업·금융 한 개씩)
const INITIAL_SUGGESTIONS = [
  '청년 주거 지원 혜택 알려줘',
  '취업 준비생이 받을 수 있는 혜택은?',
  '내 조건에 맞는 금융 혜택 뭐가 있어?',
]

// 질문 텍스트에서 주제를 추론 (키워드 매칭). 못 찾으면 null.
function topicOf(text) {
  for (const [key, t] of Object.entries(TOPICS)) {
    if (t.keywords.some((k) => text.includes(k))) return key
  }
  return null
}

function shuffle(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

// 방금 한 질문(text)을 바탕으로 "이어서 물어볼" 새 추천 질문 3개를 만든다.
// asked: 이미 물어본 질문 집합 (같은 질문 반복 추천 방지).
function followupsFor(text, asked) {
  const key = topicOf(text)
  const primary = key ? TOPICS[key].questions : []
  const others = Object.entries(TOPICS)
    .filter(([k]) => k !== key)
    .flatMap(([, t]) => t.questions)
  const fresh = (list) => list.filter((q) => q !== text && !asked.has(q))

  // 1순위: 같은 주제의 안 물어본 질문 → 2순위: 다른 주제 → 3순위: 모자라면 중복 허용
  let picked = shuffle(fresh(primary)).slice(0, 3)
  if (picked.length < 3) {
    picked = picked.concat(shuffle(fresh(others)).slice(0, 3 - picked.length))
  }
  if (picked.length < 3) {
    const rest = shuffle([...primary, ...others].filter((q) => q !== text && !picked.includes(q)))
    picked = picked.concat(rest.slice(0, 3 - picked.length))
  }
  return picked.slice(0, 3)
}

export default function ChatPage() {
  const [messages, setMessages] = useState(INITIAL)
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [limitReached, setLimitReached] = useState(false) // 게스트 횟수 소진
  const [suggestions, setSuggestions] = useState(INITIAL_SUGGESTIONS) // 현재 보여줄 추천 질문 (선택에 따라 갱신)
  const askedRef = useRef(new Set()) // 이미 물어본 질문 — 중복 추천 방지
  const bottomRef = useRef(null)
  const taRef = useRef(null)
  const isAuthed = !!localStorage.getItem('token')

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  function autoGrow(el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  }

  async function send(preset) {
    const text = (preset ?? input).trim()
    if (!text || loading) return
    setInput('')
    if (taRef.current) taRef.current.style.height = 'auto'
    setMessages((prev) => [...prev, { role: 'user', content: text }])
    setLoading(true)
    try {
      const res = await api.post('/chat', { message: text })
      setMessages((prev) => [...prev, { role: 'assistant', content: res.data.data.answer }])
      // 방금 한 질문을 바탕으로, 같은 주제의 새로운 추천 질문 3개로 교체한다.
      askedRef.current.add(text)
      setSuggestions(followupsFor(text, askedRef.current))
    } catch (err) {
      // 429 = 게스트 횟수 소진 → 서버가 내려준 안내 문구 그대로 노출 + 로그인 유도 배너 표시
      if (err.response?.status === 429) {
        setLimitReached(true)
        const msg = err.response?.data?.message
          ?? `비로그인 상태에서는 AI 상담을 하루 ${GUEST_DAILY_LIMIT}회까지만 이용할 수 있어요. 로그인하면 계속 이용할 수 있어요.`
        setMessages((prev) => [...prev, { role: 'assistant', content: `${msg} 🔒` }])
      } else {
        setMessages((prev) => [...prev, { role: 'assistant', content: '죄송해요, 일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요. 🙏' }])
      }
    } finally {
      setLoading(false)
    }
  }

  function handleKey(e) {
    // 한글 등 IME 조합 중에는 Enter를 무시 (마지막 글자가 남는 현상 방지)
    if (e.nativeEvent.isComposing) return
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  return (
    <div className="flex flex-col h-[calc(100vh-9rem)] md:h-[calc(100vh-8rem)]">
      <div className="flex items-center gap-3 mb-4">
        <Avatar />
        <div>
          <h1 className="text-xl font-black text-slate-900 leading-tight">AI 복지 상담</h1>
          <p className="text-xs text-emerald-600 font-semibold flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            {isAuthed ? '내 상황에 맞춰 안내해 드려요' : `로그인 없이 하루 ${GUEST_DAILY_LIMIT}회까지 상담할 수 있어요`}
          </p>
        </div>
      </div>

      {!isAuthed && (
        <div className="mb-3 flex items-center justify-between gap-3 rounded-2xl bg-emerald-50 ring-1 ring-emerald-100 px-4 py-2.5">
          <p className="text-xs text-emerald-700 leading-5">
            🔓 비로그인 체험 중이에요. <span className="font-bold">로그인하면 내 프로필 기반 맞춤 상담 + 횟수 제한 없이</span> 이용할 수 있어요.
          </p>
          <Link
            to="/login?redirect=/chat"
            className="shrink-0 text-xs font-bold text-white bg-gradient-to-r from-emerald-500 to-teal-600 px-3 py-1.5 rounded-full shadow-sm hover:-translate-y-0.5 transition-all"
          >
            로그인
          </Link>
        </div>
      )}

      <div className="flex-1 overflow-y-auto bg-slate-100/70 rounded-3xl p-5">
        {messages.map((m, i) => <Message key={i} msg={m} />)}
        {loading && (
          <div className="flex justify-start mb-4">
            <div className="mr-2.5"><Avatar /></div>
            <div className="bg-white ring-1 ring-slate-100 px-4 py-3.5 rounded-2xl rounded-bl-md shadow-sm">
              <div className="flex gap-1">
                <span className="w-2 h-2 bg-emerald-300 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-2 h-2 bg-emerald-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* key가 바뀌면 새로 마운트되어 fade-up 애니메이션이 다시 재생된다 */}
      {!limitReached && suggestions.length > 0 && (
        <div key={suggestions.join('|')} className="flex gap-2 mt-3 flex-wrap">
          {suggestions.map((s, i) => (
            <button
              key={s}
              onClick={() => send(s)}
              disabled={loading}
              style={{ animationDelay: `${i * 60}ms` }}
              className="animate-fade-up text-xs bg-white ring-1 ring-slate-200 text-slate-600 px-3.5 py-2 rounded-full hover:ring-emerald-300 hover:text-emerald-600 hover:bg-emerald-50 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {limitReached ? (
        <div className="mt-3 flex flex-col sm:flex-row items-center justify-center gap-3 rounded-2xl bg-slate-900 px-5 py-4 text-center">
          <p className="text-sm text-white font-semibold">
            오늘의 비로그인 상담을 모두 사용했어요. 로그인하면 계속 이어서 상담할 수 있어요!
          </p>
          <Link
            to="/login?redirect=/chat"
            className="shrink-0 text-sm font-bold text-slate-900 bg-white px-5 py-2.5 rounded-full hover:-translate-y-0.5 transition-all"
          >
            로그인하고 계속하기 →
          </Link>
        </div>
      ) : (
        <div className="flex items-end gap-2.5 mt-3">
          <textarea
            ref={taRef}
            value={input}
            onChange={(e) => { setInput(e.target.value); autoGrow(e.target) }}
            onKeyDown={handleKey}
            placeholder="궁금한 복지 혜택을 물어보세요..."
            rows={1}
            className="flex-1 ring-1 ring-slate-200 rounded-2xl px-4 py-3.5 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-emerald-400 bg-white shadow-sm transition-shadow"
          />
          <button
            onClick={() => send()}
            disabled={!input.trim() || loading}
            className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white w-12 h-12 rounded-2xl font-bold hover:-translate-y-0.5 disabled:opacity-40 disabled:translate-y-0 transition-all shrink-0 shadow-md shadow-emerald-500/25 flex items-center justify-center text-lg"
            aria-label="전송"
          >
            ↑
          </button>
        </div>
      )}
    </div>
  )
}
