export const postCategories = [
  { value: '', label: '전체', icon: '✨', color: 'bg-slate-100 text-slate-500' },
  { value: 'FREE', label: '자유', icon: '💬', color: 'bg-emerald-50 text-emerald-600' },
  { value: 'QUESTION', label: '질문', icon: '❔', color: 'bg-sky-50 text-sky-600' },
  { value: 'REVIEW', label: '후기', icon: '📝', color: 'bg-violet-50 text-violet-600' },
  { value: 'TIP', label: '꿀팁', icon: '💡', color: 'bg-amber-50 text-amber-600' },
  { value: 'ETC', label: '기타', icon: '🎁', color: 'bg-slate-100 text-slate-500' },
]

export const postCategoryMeta = postCategories.reduce((acc, item) => {
  if (item.value) acc[item.value] = item
  return acc
}, {})

export function formatPostDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '방금 전'
  if (diff < hour) return `${Math.floor(diff / minute)}분 전`
  if (diff < day) return `${Math.floor(diff / hour)}시간 전`
  if (diff < day * 7) return `${Math.floor(diff / day)}일 전`

  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
  })
}

export function unwrapPage(res) {
  const data = res.data.data
  return {
    content: data?.content ?? [],
    totalPages: data?.totalPages ?? 0,
    number: data?.number ?? 0,
  }
}
