import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api/axios'
import { formatPostDate, postCategoryMeta } from './communityMeta'
import LoginPrompt from '../components/LoginPrompt'

function CommentItem({ comment, onReply }) {
  return (
    <div className="rounded-2xl bg-slate-50 p-4">
      <div className="flex items-center justify-between gap-3 mb-2">
        <span className="text-sm font-bold text-slate-700">
          {comment.deleted ? '삭제된 댓글' : comment.authorNickname}
        </span>
        <span className="text-xs text-slate-400">{formatPostDate(comment.createdAt)}</span>
      </div>
      <p className={`text-sm leading-relaxed whitespace-pre-line ${comment.deleted ? 'text-slate-400' : 'text-slate-600'}`}>
        {comment.content}
      </p>
      {!comment.deleted && (
        <button
          onClick={() => onReply(comment)}
          className="text-xs font-bold text-emerald-600 mt-3 hover:underline"
        >
          답글 달기
        </button>
      )}
      {comment.children?.length > 0 && (
        <div className="mt-3 space-y-2 border-l-2 border-emerald-100 pl-3">
          {comment.children.map((child) => (
            <div key={child.id} className="bg-white rounded-2xl p-3 ring-1 ring-slate-100">
              <div className="flex items-center justify-between gap-3 mb-1.5">
                <span className="text-xs font-bold text-slate-700">
                  {child.deleted ? '삭제된 댓글' : child.authorNickname}
                </span>
                <span className="text-xs text-slate-400">{formatPostDate(child.createdAt)}</span>
              </div>
              <p className={`text-sm leading-relaxed whitespace-pre-line ${child.deleted ? 'text-slate-400' : 'text-slate-600'}`}>
                {child.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default function CommunityDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isAuthed = !!localStorage.getItem('token')
  const [showLogin, setShowLogin] = useState(false)
  const [post, setPost] = useState(null)
  const [comments, setComments] = useState([])
  const [comment, setComment] = useState('')
  const [replyTo, setReplyTo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  async function load() {
    setLoading(true)
    try {
      const [detail, commentRes] = await Promise.all([
        api.get(`/posts/${id}`),
        api.get(`/posts/${id}/comments`).catch(() => ({ data: { data: [] } })),
      ])
      setPost(detail.data.data)
      setComments(commentRes.data.data ?? [])
    } catch {
      setPost(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [id])

  async function toggleLike() {
    if (!post || busy) return
    if (!isAuthed) { setShowLogin(true); return }   // 게스트는 로그인 유도
    setBusy(true)
    const previous = post
    setPost((cur) => ({
      ...cur,
      liked: !cur.liked,
      likeCount: cur.liked ? Math.max(0, cur.likeCount - 1) : cur.likeCount + 1,
    }))
    try {
      const res = await api.post(`/posts/${id}/like`)
      setPost((cur) => ({
        ...cur,
        liked: res.data.data.liked,
        likeCount: res.data.data.likeCount,
      }))
    } catch {
      setPost(previous)
      alert('좋아요 처리 중 오류가 발생했습니다.')
    } finally {
      setBusy(false)
    }
  }

  async function submitComment(e) {
    e.preventDefault()
    if (!isAuthed) { setShowLogin(true); return }   // 게스트는 로그인 유도
    const content = comment.trim()
    if (!content || busy) return
    setBusy(true)
    try {
      await api.post(`/posts/${id}/comments`, {
        content,
        parentId: replyTo?.id ?? null,
      })
      setComment('')
      setReplyTo(null)
      const res = await api.get(`/posts/${id}/comments`)
      setComments(res.data.data ?? [])
      setPost((cur) => cur ? { ...cur, commentCount: (cur.commentCount ?? 0) + 1 } : cur)
    } catch {
      alert('댓글 작성에 실패했습니다.')
    } finally {
      setBusy(false)
    }
  }

  async function deletePost() {
    if (!window.confirm('게시글을 삭제할까요?')) return
    try {
      await api.delete(`/posts/${id}`)
      navigate('/community')
    } catch {
      alert('게시글 삭제에 실패했습니다.')
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-32">
        <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (!post) {
    return (
      <div className="text-center py-32 text-slate-400">
        <p className="text-5xl mb-3">🫥</p>
        <p className="font-semibold text-slate-600">게시글을 찾을 수 없습니다.</p>
        <button onClick={() => navigate('/community')} className="mt-5 text-emerald-600 font-semibold hover:underline">
          커뮤니티로 돌아가기
        </button>
      </div>
    )
  }

  const meta = postCategoryMeta[post.category] ?? postCategoryMeta.ETC

  return (
    <div className="max-w-3xl mx-auto animate-fade-up">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-slate-500 hover:text-slate-700 mb-5 text-sm transition-colors">
        ← 뒤로가기
      </button>

      <article className="bg-white rounded-[2rem] ring-1 ring-slate-100 overflow-hidden shadow-sm">
        <div className="relative bg-gradient-to-br from-emerald-500 to-teal-600 px-7 pt-7 pb-8 text-white">
          <div className="pointer-events-none absolute -top-8 -right-6 w-40 h-40 bg-white/10 rounded-full blur-2xl" />
          <div className="relative">
            <div className="flex flex-wrap items-center gap-2 mb-4">
              <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-white/20 backdrop-blur">
                {meta.icon} {meta.label}
              </span>
              <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-white/20 backdrop-blur">
                조회 {post.viewCount ?? 0}
              </span>
              <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-white/20 backdrop-blur">
                댓글 {post.commentCount ?? 0}
              </span>
            </div>
            <h1 className="text-2xl md:text-3xl font-black leading-snug">{post.title}</h1>
            <div className="flex flex-wrap items-center gap-2 mt-4 text-sm text-emerald-50/90">
              <span className="font-bold">{post.anonymous ? '익명 청년' : post.authorNickname}</span>
              <span>·</span>
              <span>{formatPostDate(post.createdAt)}</span>
            </div>
          </div>
        </div>

        <div className="p-7">
          {post.relatedWelfare && (
            <button
              onClick={() => navigate(`/welfare/${post.relatedWelfare.id}`)}
              className="w-full text-left bg-emerald-50 rounded-2xl p-4 mb-6 hover:bg-emerald-100 transition-colors"
            >
              <p className="text-xs font-bold text-emerald-600 mb-1">연결된 복지 혜택</p>
              <p className="font-bold text-slate-800 line-clamp-1">{post.relatedWelfare.title}</p>
            </button>
          )}

          <p className="text-slate-700 leading-8 whitespace-pre-line min-h-40">{post.content}</p>

          {post.attachments?.length > 0 && (
            <div className="mt-6 space-y-3">
              {/* 이미지·동영상은 인라인 표시, 파일은 다운로드 카드. */}
              {post.attachments.some((a) => a.kind === 'IMAGE' || a.kind === 'VIDEO') && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {post.attachments
                    .filter((a) => a.kind === 'IMAGE' || a.kind === 'VIDEO')
                    .map((a, idx) => (
                      a.kind === 'IMAGE' ? (
                        <a
                          key={`${a.url}-${idx}`}
                          href={a.url}
                          target="_blank"
                          rel="noreferrer"
                          className="block rounded-2xl overflow-hidden ring-1 ring-slate-100 bg-slate-50"
                        >
                          <img
                            src={a.url}
                            alt={a.name || `첨부 이미지 ${idx + 1}`}
                            loading="lazy"
                            className="w-full max-h-96 object-cover hover:opacity-95 transition-opacity"
                          />
                        </a>
                      ) : (
                        <video
                          key={`${a.url}-${idx}`}
                          src={a.url}
                          controls
                          className="w-full max-h-96 rounded-2xl ring-1 ring-slate-100 bg-black"
                        />
                      )
                    ))}
                </div>
              )}

              {post.attachments
                .filter((a) => a.kind === 'FILE')
                .map((a, idx) => (
                  <a
                    key={`${a.url}-${idx}`}
                    href={a.url}
                    target="_blank"
                    rel="noreferrer"
                    download={a.name}
                    className="flex items-center gap-3 rounded-2xl ring-1 ring-slate-100 bg-slate-50 px-4 py-3 hover:bg-slate-100 transition-colors"
                  >
                    <span className="text-2xl leading-none">📄</span>
                    <span className="flex-1 min-w-0">
                      <span className="block text-sm font-semibold text-slate-700 truncate">{a.name || '첨부파일'}</span>
                      <span className="block text-xs text-slate-400">다운로드</span>
                    </span>
                    <span className="text-slate-400">↓</span>
                  </a>
                ))}
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-3 mt-8 pt-6 border-t border-slate-100">
            <button
              onClick={toggleLike}
              disabled={busy}
              className={`px-5 py-3 rounded-2xl text-sm font-black transition-all disabled:opacity-60 ${
                post.liked
                  ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/25'
                  : 'bg-slate-100 text-slate-600 hover:bg-emerald-50 hover:text-emerald-700'
              }`}
            >
              좋아요 {post.likeCount ?? 0}
            </button>

            {post.mine && (
              <div className="flex gap-2">
                <button
                  onClick={() => navigate(`/community/${id}/edit`)}
                  className="px-4 py-3 rounded-2xl text-sm font-bold bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
                >
                  수정
                </button>
                <button
                  onClick={deletePost}
                  className="px-4 py-3 rounded-2xl text-sm font-bold bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
                >
                  삭제
                </button>
              </div>
            )}
          </div>
        </div>
      </article>

      <section className="mt-5 bg-white rounded-[2rem] ring-1 ring-slate-100 p-6">
        <div className="flex items-center justify-between gap-3 mb-5">
          <div>
            <h2 className="text-xl font-black text-slate-900">댓글</h2>
            <p className="text-sm text-slate-400 mt-1">서로에게 필요한 정보를 부드럽게 나눠보세요</p>
          </div>
          <span className="text-sm font-bold text-emerald-600">{comments.length}개</span>
        </div>

        {isAuthed ? (
        <form onSubmit={submitComment} className="mb-5">
          {replyTo && (
            <div className="flex items-center justify-between gap-3 bg-emerald-50 text-emerald-700 text-sm font-semibold rounded-2xl px-4 py-3 mb-2">
              <span>{replyTo.authorNickname}님에게 답글 쓰는 중</span>
              <button type="button" onClick={() => setReplyTo(null)} className="text-emerald-500 hover:text-emerald-700">
                취소
              </button>
            </div>
          )}
          <div className="flex items-end gap-2">
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={2}
              maxLength={500}
              placeholder="댓글을 입력하세요..."
              className="flex-1 ring-1 ring-slate-200 rounded-2xl px-4 py-3 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-emerald-400 bg-white shadow-sm"
            />
            <button
              disabled={!comment.trim() || busy}
              className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-5 py-3 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 disabled:opacity-40 disabled:translate-y-0 transition-all shrink-0"
            >
              등록
            </button>
          </div>
        </form>
        ) : (
          <button
            onClick={() => setShowLogin(true)}
            className="w-full mb-5 rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm font-semibold text-slate-500 hover:bg-slate-100 hover:text-emerald-600 transition-colors"
          >
            로그인하고 댓글 남기기 →
          </button>
        )}

        {comments.length === 0 ? (
          <div className="text-center py-12 text-slate-400">
            <p className="text-4xl mb-3">💬</p>
            <p className="font-semibold text-slate-600">아직 댓글이 없어요</p>
          </div>
        ) : (
          <div className="space-y-3">
            {comments.map((item) => (
              <CommentItem key={item.id} comment={item} onReply={setReplyTo} />
            ))}
          </div>
        )}
      </section>

      <LoginPrompt
        open={showLogin}
        onClose={() => setShowLogin(false)}
        message={'댓글·좋아요는 로그인 후 이용할 수 있어요.\n로그인하고 대화에 참여해 보세요.'}
      />
    </div>
  )
}
