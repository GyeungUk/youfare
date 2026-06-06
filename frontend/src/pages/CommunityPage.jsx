import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import { formatPostDate, postCategories, postCategoryMeta, unwrapPage } from './communityMeta'
import LoginPrompt from '../components/LoginPrompt'

const sortOptions = [
  { value: 'latest', label: '최신순' },
  { value: 'likes', label: '공감순' },
]

function CategoryPill({ item, active, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 rounded-full text-sm font-semibold transition-all flex items-center gap-1.5 ${
        active
          ? 'bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-md shadow-emerald-500/25'
          : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:ring-emerald-300 hover:text-emerald-600'
      }`}
    >
      <span>{item.icon}</span>
      {item.label}
    </button>
  )
}

function PostCard({ post, onClick }) {
  const meta = postCategoryMeta[post.category] ?? postCategoryMeta.ETC

  return (
    <button
      onClick={onClick}
      className="lift group w-full text-left bg-white rounded-3xl ring-1 ring-slate-100 p-5 hover:ring-emerald-200 hover:shadow-xl hover:shadow-emerald-500/5"
    >
      <div className="flex items-start justify-between gap-3">
        <span className={`text-xs font-bold px-2.5 py-1 rounded-lg ${meta.color}`}>
          {meta.icon} {meta.label}
        </span>
        <span className="text-xs text-slate-400 font-medium shrink-0">{formatPostDate(post.createdAt)}</span>
      </div>

      <h3 className="mt-4 text-lg font-bold text-slate-900 line-clamp-2 group-hover:text-emerald-700 transition-colors">
        {post.title}
      </h3>

      <div className="mt-4 pt-4 border-t border-slate-50 flex items-center justify-between gap-3">
        <span className="text-sm font-semibold text-slate-500 truncate">
          {post.anonymous ? '익명 청년' : post.authorNickname}
        </span>
        <div className="flex items-center gap-3 text-xs font-semibold text-slate-400 shrink-0">
          <span>좋아요 {post.likeCount ?? 0}</span>
          <span>조회 {post.viewCount ?? 0}</span>
        </div>
      </div>
    </button>
  )
}

function PostSkeleton() {
  return (
    <div className="bg-white rounded-3xl ring-1 ring-slate-100 p-5">
      <div className="skeleton h-6 w-20 rounded-lg" />
      <div className="skeleton h-5 w-4/5 rounded mt-5" />
      <div className="skeleton h-5 w-2/3 rounded mt-2" />
      <div className="skeleton h-4 w-full rounded mt-6" />
    </div>
  )
}

function HotPostList({ posts, onSelect }) {
  return (
    <aside className="bg-white rounded-3xl ring-1 ring-slate-100 p-5 h-fit">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="font-black text-slate-900">인기글</h2>
          <p className="text-xs text-slate-400 mt-0.5">최근 7일 기준</p>
        </div>
        <span className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
          HOT
        </span>
      </div>

      {posts.length === 0 ? (
        <p className="text-sm text-slate-400 py-8 text-center">아직 인기글이 없어요</p>
      ) : (
        <div className="space-y-2.5">
          {posts.slice(0, 5).map((post, index) => (
            <button
              key={post.id}
              onClick={() => onSelect(post.id)}
              className="w-full text-left flex gap-3 rounded-2xl p-3 hover:bg-slate-50 transition-colors"
            >
              <span className="w-6 h-6 rounded-lg bg-slate-100 text-slate-500 text-xs font-black flex items-center justify-center shrink-0">
                {index + 1}
              </span>
              <span className="min-w-0">
                <span className="block text-sm font-bold text-slate-800 line-clamp-2">{post.title}</span>
                <span className="block text-xs text-slate-400 mt-1">좋아요 {post.likeCount ?? 0}</span>
              </span>
            </button>
          ))}
        </div>
      )}
    </aside>
  )
}

export default function CommunityPage() {
  const navigate = useNavigate()
  const isAuthed = !!localStorage.getItem('token')
  const [showLogin, setShowLogin] = useState(false)
  const [posts, setPosts] = useState([])
  const [hotPosts, setHotPosts] = useState([])
  const [category, setCategory] = useState('')
  const [sort, setSort] = useState('latest')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)

  // 글쓰기: 로그인 유저는 작성 페이지로, 게스트는 로그인 모달.
  function goWrite() {
    if (isAuthed) navigate('/community/new')
    else setShowLogin(true)
  }

  async function loadPosts(nextCategory, nextSort, nextPage) {
    setLoading(true)
    try {
      const params = { sort: nextSort, page: nextPage }
      if (nextCategory) params.category = nextCategory
      const res = await api.get('/posts', { params })
      const pageData = unwrapPage(res)
      setPosts(pageData.content)
      setTotalPages(pageData.totalPages)
    } catch {
      setPosts([])
      setTotalPages(0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPosts(category, sort, page)
  }, [category, sort, page])

  useEffect(() => {
    api.get('/posts/hot')
      .then((res) => setHotPosts(res.data.data ?? []))
      .catch(() => setHotPosts([]))
  }, [])

  function changeCategory(next) {
    setCategory(next)
    setPage(0)
  }

  function changeSort(next) {
    setSort(next)
    setPage(0)
  }

  const pageWindow = (() => {
    if (totalPages <= 1) return []
    const start = Math.max(0, Math.min(page - 2, totalPages - 5))
    const end = Math.min(totalPages, start + 5)
    return Array.from({ length: end - start }, (_, i) => start + i)
  })()

  return (
    <div>
      <section className="relative overflow-hidden rounded-[2rem] bg-gradient-to-br from-emerald-500 to-teal-600 px-6 py-7 md:px-8 md:py-8 text-white mb-7 shadow-lg shadow-emerald-500/15">
        <div className="pointer-events-none absolute -top-10 -right-8 w-44 h-44 bg-white/10 rounded-full blur-2xl" />
        <div className="relative flex flex-col md:flex-row md:items-end md:justify-between gap-5">
          <div>
            <p className="text-sm font-bold text-emerald-50/90 mb-2">YouFare 커뮤니티</p>
            <h1 className="text-3xl font-black leading-tight">혜택을 찾는 청년들의 게시판</h1>
            <p className="text-sm text-emerald-50/90 mt-2 max-w-xl">
              질문, 후기, 꿀팁을 나누고 나에게 맞는 복지 정보를 더 빠르게 찾아보세요.
            </p>
          </div>
          <button
            onClick={goWrite}
            className="bg-white text-emerald-700 font-black px-5 py-3 rounded-2xl shadow-md shadow-emerald-900/10 hover:-translate-y-0.5 transition-all"
          >
            글쓰기 →
          </button>
        </div>
      </section>

      <div className="flex flex-col lg:flex-row gap-6">
        <div className="flex-1 min-w-0">
          <div className="flex gap-2 mb-4 flex-wrap">
            {postCategories.map((item) => (
              <CategoryPill
                key={item.value}
                item={item}
                active={category === item.value}
                onClick={() => changeCategory(item.value)}
              />
            ))}
          </div>

          <div className="flex items-center justify-between gap-3 mb-5">
            <p className="text-sm font-semibold text-slate-500">
              {category ? `${postCategoryMeta[category]?.label} 글` : '전체 글'}
            </p>
            <div className="flex gap-1.5 bg-white rounded-2xl ring-1 ring-slate-200 p-1">
              {sortOptions.map((item) => (
                <button
                  key={item.value}
                  onClick={() => changeSort(item.value)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                    sort === item.value
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'text-slate-400 hover:text-slate-600'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 5 }).map((_, i) => <PostSkeleton key={i} />)}
            </div>
          ) : posts.length === 0 ? (
            <div className="text-center py-20 bg-white rounded-3xl ring-1 ring-slate-100">
              <p className="text-5xl mb-4">📭</p>
              <p className="text-slate-700 font-bold text-lg mb-2">아직 게시글이 없어요</p>
              <p className="text-slate-400 text-sm mb-7">첫 질문이나 후기를 남겨보세요</p>
              <button
                onClick={goWrite}
                className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold px-6 py-3 rounded-2xl shadow-md shadow-emerald-500/25 hover:-translate-y-0.5 transition-all"
              >
                첫 글 작성하기 →
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {posts.map((post) => (
                <PostCard key={post.id} post={post} onClick={() => navigate(`/community/${post.id}`)} />
              ))}
            </div>
          )}

          {pageWindow.length > 0 && (
            <div className="flex justify-center items-center gap-1.5 mt-9">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="w-9 h-9 rounded-xl text-slate-500 hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
              >
                ←
              </button>
              {pageWindow.map((i) => (
                <button
                  key={i}
                  onClick={() => setPage(i)}
                  className={`w-9 h-9 rounded-xl text-sm font-semibold transition-all ${
                    page === i
                      ? 'bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-md shadow-emerald-500/25'
                      : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:ring-emerald-300'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                className="w-9 h-9 rounded-xl text-slate-500 hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
              >
                →
              </button>
            </div>
          )}
        </div>

        <div className="lg:w-80">
          <HotPostList posts={hotPosts} onSelect={(id) => navigate(`/community/${id}`)} />
        </div>
      </div>

      <LoginPrompt
        open={showLogin}
        onClose={() => setShowLogin(false)}
        message={'글을 작성하려면 로그인이 필요해요.\n로그인하고 청년들과 정보를 나눠보세요.'}
      />
    </div>
  )
}
