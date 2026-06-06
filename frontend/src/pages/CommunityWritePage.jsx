import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api/axios'
import { postCategories } from './communityMeta'

const writableCategories = postCategories.filter((item) => item.value)

// 백엔드 MediaUploadValidator와 동일한 정책. 서버가 최종 방어선이고, 여기선 UX용 1차 차단.
const MAX_FILES = 10
const MAX_FILE_SIZE = 20 * 1024 * 1024     // 20MB
const MAX_TOTAL_SIZE = 50 * 1024 * 1024    // 50MB
// 이미지·동영상은 type prefix로, 문서는 확장자로 허용한다(서버 화이트리스트와 일치).
const FILE_EXTENSIONS = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'hwp', 'hwpx', 'txt', 'csv', 'zip']
const ACCEPT = `image/*,video/*,${FILE_EXTENSIONS.map((e) => `.${e}`).join(',')}`

function extOf(name) {
  const dot = name.lastIndexOf('.')
  return dot >= 0 ? name.slice(dot + 1).toLowerCase() : ''
}

// 첨부물 종류 판별: 미리보기/렌더링 방식 결정용.
function kindOf(file) {
  if (file.type.startsWith('image/')) return 'IMAGE'
  if (file.type.startsWith('video/')) return 'VIDEO'
  return 'FILE'
}

function isAllowed(file) {
  if (file.type.startsWith('image/') || file.type.startsWith('video/')) return true
  return FILE_EXTENSIONS.includes(extOf(file.name))
}

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)}KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
}

export default function CommunityWritePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editing = Boolean(id)
  const [form, setForm] = useState({
    category: 'FREE',
    title: '',
    content: '',
    isAnonymous: false,
    relatedWelfareId: '',
  })
  const [loading, setLoading] = useState(editing)
  const [saving, setSaving] = useState(false)
  // 첨부는 새 글 작성에서만 지원(선택). 항목: { file, kind, name, url(이미지·동영상 미리보기) }
  const [attachments, setAttachments] = useState([])
  const [attachError, setAttachError] = useState('')
  const fileInputRef = useRef(null)

  // 미리보기용 objectURL은 컴포넌트 unmount 시 정리해 메모리 누수를 막는다.
  const attachmentsRef = useRef([])
  useEffect(() => { attachmentsRef.current = attachments }, [attachments])
  useEffect(() => () => {
    attachmentsRef.current.forEach((it) => { if (it.url) URL.revokeObjectURL(it.url) })
  }, [])

  function addFiles(fileList) {
    setAttachError('')
    const next = [...attachments]
    let total = next.reduce((sum, it) => sum + it.file.size, 0)
    for (const file of Array.from(fileList)) {
      if (next.length >= MAX_FILES) { setAttachError('첨부는 최대 10개까지 올릴 수 있어요.'); break }
      if (!isAllowed(file)) { setAttachError('이미지·동영상·문서(pdf·doc·xls·ppt·hwp·txt·csv·zip)만 올릴 수 있어요.'); continue }
      if (file.size > MAX_FILE_SIZE) { setAttachError('각 파일은 20MB 이하로 올려주세요.'); continue }
      if (total + file.size > MAX_TOTAL_SIZE) { setAttachError('첨부 총 용량은 50MB 이하여야 해요.'); break }
      const kind = kindOf(file)
      const url = (kind === 'IMAGE' || kind === 'VIDEO') ? URL.createObjectURL(file) : null
      next.push({ file, kind, name: file.name, url })
      total += file.size
    }
    setAttachments(next)
    if (fileInputRef.current) fileInputRef.current.value = ''   // 같은 파일 재선택 허용
  }

  function removeAttachment(idx) {
    setAttachments((prev) => {
      const target = prev[idx]
      if (target?.url) URL.revokeObjectURL(target.url)
      return prev.filter((_, i) => i !== idx)
    })
    setAttachError('')
  }

  useEffect(() => {
    if (!editing) return
    let active = true
    async function load() {
      try {
        const res = await api.get(`/posts/${id}`)
        if (!active) return
        const post = res.data.data
        setForm({
          category: post.category,
          title: post.title,
          content: post.content,
          isAnonymous: post.anonymous,
          relatedWelfareId: post.relatedWelfare?.id ? String(post.relatedWelfare.id) : '',
        })
      } catch {
        alert('게시글을 불러오지 못했습니다.')
        navigate('/community')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [editing, id, navigate])

  function update(name, value) {
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function submit(e) {
    e.preventDefault()
    const title = form.title.trim()
    const content = form.content.trim()
    if (!title || !content || saving) return

    setSaving(true)
    try {
      if (editing) {
        await api.put(`/posts/${id}`, { title, content })
        navigate(`/community/${id}`)
      } else {
        // 첨부 때문에 작성은 multipart/form-data로 전송한다.
        // (axios는 FormData를 넘기면 multipart 경계를 자동 설정하므로 Content-Type을 직접 지정하지 않는다.)
        const fd = new FormData()
        fd.append('category', form.category)
        fd.append('title', title)
        fd.append('content', content)
        fd.append('anonymous', form.isAnonymous)
        if (form.category === 'REVIEW' && form.relatedWelfareId.trim()) {
          fd.append('relatedWelfareId', Number(form.relatedWelfareId))
        }
        attachments.forEach((it) => fd.append('attachments', it.file))
        const res = await api.post('/posts', fd)
        navigate(`/community/${res.data.data.id}`)
      }
    } catch {
      alert(editing ? '게시글 수정에 실패했습니다.' : '게시글 작성에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-32">
        <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto animate-fade-up">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-slate-500 hover:text-slate-700 mb-5 text-sm transition-colors">
        ← 뒤로가기
      </button>

      <form onSubmit={submit} className="bg-white rounded-[2rem] ring-1 ring-slate-100 overflow-hidden shadow-sm">
        <div className="relative bg-gradient-to-br from-emerald-500 to-teal-600 px-7 py-8 text-white">
          <div className="pointer-events-none absolute -top-8 -right-6 w-40 h-40 bg-white/10 rounded-full blur-2xl" />
          <div className="relative">
            <p className="text-sm font-bold text-emerald-50/90 mb-2">커뮤니티</p>
            <h1 className="text-3xl font-black">{editing ? '게시글 수정' : '새 글 작성'}</h1>
            <p className="text-sm text-emerald-50/90 mt-2">
              질문과 경험을 남겨두면 누군가에게 좋은 출발점이 됩니다.
            </p>
          </div>
        </div>

        <div className="p-7 space-y-6">
          {!editing && (
            <div>
              <label className="block text-sm font-bold text-slate-700 mb-2">카테고리</label>
              <div className="flex gap-2 flex-wrap">
                {writableCategories.map((item) => (
                  <button
                    type="button"
                    key={item.value}
                    onClick={() => update('category', item.value)}
                    className={`px-4 py-2 rounded-full text-sm font-semibold transition-all flex items-center gap-1.5 ${
                      form.category === item.value
                        ? 'bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-md shadow-emerald-500/25'
                        : 'bg-slate-50 text-slate-600 ring-1 ring-slate-200 hover:ring-emerald-300 hover:text-emerald-600'
                    }`}
                  >
                    <span>{item.icon}</span>
                    {item.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div>
            <label htmlFor="post-title" className="block text-sm font-bold text-slate-700 mb-2">제목</label>
            <input
              id="post-title"
              value={form.title}
              onChange={(e) => update('title', e.target.value)}
              maxLength={100}
              placeholder="제목을 입력하세요"
              className="w-full ring-1 ring-slate-200 rounded-2xl px-4 py-3.5 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-400 bg-white shadow-sm"
            />
            <p className="text-xs text-slate-400 mt-1.5 text-right">{form.title.length}/100</p>
          </div>

          <div>
            <label htmlFor="post-content" className="block text-sm font-bold text-slate-700 mb-2">내용</label>
            <textarea
              id="post-content"
              value={form.content}
              onChange={(e) => update('content', e.target.value)}
              rows={12}
              placeholder="어떤 이야기를 나누고 싶나요?"
              className="w-full ring-1 ring-slate-200 rounded-2xl px-4 py-3.5 text-sm resize-y focus:outline-none focus:ring-2 focus:ring-emerald-400 bg-white shadow-sm leading-relaxed"
            />

            {/* 첨부: 클로드처럼 + 버튼으로 추가. 이미지·동영상·파일 모두 선택(필수 아님). */}
            {!editing && (
              <div className="mt-3">
                <div className="flex items-center gap-2 flex-wrap">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={attachments.length >= MAX_FILES}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-full text-sm font-semibold bg-slate-50 text-slate-600 ring-1 ring-slate-200 hover:ring-emerald-300 hover:text-emerald-600 transition-colors disabled:opacity-40"
                  >
                    <span className="text-lg leading-none">＋</span>
                    첨부
                  </button>
                  {attachments.length > 0 && (
                    <span className="text-xs text-slate-400">{attachments.length}/{MAX_FILES}</span>
                  )}
                </div>

                {attachments.length > 0 && (
                  <div className="flex gap-2.5 flex-wrap mt-3">
                    {attachments.map((it, idx) => (
                      <div
                        key={`${it.name}-${idx}`}
                        className="relative group"
                      >
                        {it.kind === 'IMAGE' ? (
                          <div className="w-20 h-20 rounded-2xl overflow-hidden ring-1 ring-slate-200">
                            <img src={it.url} alt={it.name} className="w-full h-full object-cover" />
                          </div>
                        ) : it.kind === 'VIDEO' ? (
                          <div className="w-20 h-20 rounded-2xl overflow-hidden ring-1 ring-slate-200 bg-black relative">
                            <video src={it.url} className="w-full h-full object-cover" muted />
                            <span className="absolute inset-0 flex items-center justify-center text-white text-xl">▶</span>
                          </div>
                        ) : (
                          <div className="w-20 h-20 rounded-2xl ring-1 ring-slate-200 bg-slate-50 flex flex-col items-center justify-center gap-1 px-1.5">
                            <span className="text-2xl leading-none">📄</span>
                            <span className="text-[10px] text-slate-500 font-medium truncate w-full text-center">{it.name}</span>
                          </div>
                        )}
                        <button
                          type="button"
                          onClick={() => removeAttachment(idx)}
                          className="absolute -top-1.5 -right-1.5 w-6 h-6 rounded-full bg-black/60 text-white text-xs font-bold flex items-center justify-center hover:bg-black/80 transition-colors"
                          aria-label="첨부 삭제"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ACCEPT}
                  multiple
                  onChange={(e) => addFiles(e.target.files)}
                  className="hidden"
                />

                {attachError
                  ? <p className="text-xs text-red-500 mt-2">{attachError}</p>
                  : <p className="text-xs text-slate-400 mt-2">이미지·동영상·파일 첨부(선택) · 최대 10개 · 각 20MB · 총 50MB</p>}
              </div>
            )}
          </div>

          {!editing && form.category === 'REVIEW' && (
            <div>
              <label htmlFor="related-welfare" className="block text-sm font-bold text-slate-700 mb-2">
                연결할 혜택 ID
              </label>
              <input
                id="related-welfare"
                value={form.relatedWelfareId}
                onChange={(e) => update('relatedWelfareId', e.target.value.replace(/[^0-9]/g, ''))}
                inputMode="numeric"
                placeholder="선택 사항"
                className="w-full ring-1 ring-slate-200 rounded-2xl px-4 py-3.5 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-400 bg-white shadow-sm"
              />
              <p className="text-xs text-slate-400 mt-1.5">복지 상세 페이지의 번호를 입력하면 후기와 혜택을 연결할 수 있어요.</p>
            </div>
          )}

          {!editing && (
            <label className="flex items-center justify-between gap-4 bg-slate-50 rounded-2xl px-4 py-3 cursor-pointer">
              <span>
                <span className="block text-sm font-bold text-slate-700">익명으로 작성</span>
                <span className="block text-xs text-slate-400 mt-0.5">게시판에는 익명 청년으로 표시됩니다</span>
              </span>
              <input
                type="checkbox"
                checked={form.isAnonymous}
                onChange={(e) => update('isAnonymous', e.target.checked)}
                className="w-5 h-5 accent-emerald-500"
              />
            </label>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="px-5 py-4 rounded-2xl text-sm font-bold bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
            >
              취소
            </button>
            <button
              disabled={!form.title.trim() || !form.content.trim() || saving}
              className="flex-1 bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold py-4 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 disabled:opacity-40 disabled:translate-y-0 transition-all"
            >
              {saving ? '저장 중...' : editing ? '수정 완료' : '게시하기'}
            </button>
          </div>
        </div>
      </form>
    </div>
  )
}
