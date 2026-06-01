import { useState, useRef, useEffect } from 'react'
import api from '../api/axios'

function Message({ msg }) {
  const isUser = msg.role === 'user'
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
      {!isUser && (
        <div className="w-9 h-9 rounded-full bg-gradient-to-br from-emerald-400 to-teal-500 flex items-center justify-center text-white font-bold text-sm mr-3 shrink-0 mt-1">
          AI
        </div>
      )}
      <div
        className={`max-w-[75%] px-4 py-3 rounded-2xl text-sm leading-relaxed ${
          isUser
            ? 'bg-emerald-500 text-white rounded-br-md'
            : 'bg-white border border-slate-100 text-slate-700 rounded-bl-md'
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
    content: '안녕하세요! 저는 YouFare AI 복지 상담사예요 😊\n궁금한 복지 혜택에 대해 무엇이든 물어보세요. 내 프로필에 맞는 정보를 드릴게요!',
  },
]

export default function ChatPage() {
  const [messages, setMessages] = useState(INITIAL)
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  async function send() {
    const text = input.trim()
    if (!text || loading) return
    setInput('')
    setMessages((prev) => [...prev, { role: 'user', content: text }])
    setLoading(true)
    try {
      const res = await api.post('/chat', { message: text })
      setMessages((prev) => [...prev, { role: 'assistant', content: res.data.data.message }])
    } catch {
      setMessages((prev) => [...prev, { role: 'assistant', content: '죄송해요, 일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요.' }])
    } finally {
      setLoading(false)
    }
  }

  function handleKey(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  const suggestions = ['청년 주거 지원 혜택 알려줘', '취업 준비생이 받을 수 있는 혜택은?', '내 조건에 맞는 금융 혜택 뭐가 있어?']

  return (
    <div className="flex flex-col h-[calc(100vh-8rem)]">
      <div className="mb-4">
        <h1 className="text-3xl font-black text-slate-900">AI 복지 상담</h1>
        <p className="text-slate-500 mt-1">내 상황에 맞는 복지 혜택을 AI가 안내해 드려요</p>
      </div>

      <div className="flex-1 overflow-y-auto bg-slate-50 rounded-3xl p-6 mb-4">
        {messages.map((m, i) => <Message key={i} msg={m} />)}
        {loading && (
          <div className="flex justify-start mb-4">
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-emerald-400 to-teal-500 flex items-center justify-center text-white font-bold text-sm mr-3 shrink-0">
              AI
            </div>
            <div className="bg-white border border-slate-100 px-4 py-3 rounded-2xl rounded-bl-md">
              <div className="flex gap-1">
                <span className="w-2 h-2 bg-slate-300 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-2 h-2 bg-slate-300 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-2 h-2 bg-slate-300 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {messages.length === 1 && (
        <div className="flex gap-2 mb-3 flex-wrap">
          {suggestions.map((s) => (
            <button
              key={s}
              onClick={() => { setInput(s) }}
              className="text-xs bg-white border border-slate-200 text-slate-600 px-3 py-2 rounded-xl hover:bg-emerald-50 hover:border-emerald-300 hover:text-emerald-600 transition-colors"
            >
              {s}
            </button>
          ))}
        </div>
      )}

      <div className="flex gap-3">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKey}
          placeholder="궁금한 복지 혜택을 물어보세요..."
          rows={1}
          className="flex-1 border border-slate-200 rounded-2xl px-4 py-3 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-emerald-400 bg-white"
        />
        <button
          onClick={send}
          disabled={!input.trim() || loading}
          className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white px-6 py-3 rounded-2xl font-bold hover:opacity-90 disabled:opacity-40 transition-opacity shrink-0"
        >
          전송
        </button>
      </div>
    </div>
  )
}
