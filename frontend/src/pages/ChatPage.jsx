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

const suggestions = [
  '청년 주거 지원 혜택 알려줘',
  '취업 준비생이 받을 수 있는 혜택은?',
  '내 조건에 맞는 금융 혜택 뭐가 있어?',
]

export default function ChatPage() {
  const [messages, setMessages] = useState(INITIAL)
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [limitReached, setLimitReached] = useState(false) // 게스트 횟수 소진
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

      {messages.length === 1 && (
        <div className="flex gap-2 mt-3 flex-wrap">
          {suggestions.map((s) => (
            <button
              key={s}
              onClick={() => send(s)}
              className="text-xs bg-white ring-1 ring-slate-200 text-slate-600 px-3.5 py-2 rounded-full hover:ring-emerald-300 hover:text-emerald-600 hover:bg-emerald-50 transition-all"
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
