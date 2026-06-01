import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

const regions = ['서울', '부산', '인천', '대구', '대전', '광주', '울산', '세종', '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주']
const incomes = [
  { value: 'LOW', label: '저소득', desc: '중위소득 50% 이하' },
  { value: 'MIDDLE', label: '중간', desc: '중위소득 50~150%' },
  { value: 'HIGH', label: '고소득', desc: '중위소득 150% 초과' },
  { value: 'UNKNOWN', label: '모름', desc: '해당없음 / 잘 모르겠어요' },
]
const employments = [
  { value: 'STUDENT', label: '학생', icon: '🎓' },
  { value: 'JOB_SEEKER', label: '구직 중', icon: '🔍' },
  { value: 'EMPLOYED', label: '재직 중', icon: '💼' },
]

export default function OnboardingPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ birthYear: '', region: '', incomeBracket: '', employmentStatus: '' })
  const [loading, setLoading] = useState(false)

  const set = (key, val) => setForm((f) => ({ ...f, [key]: val }))

  const filled = ['birthYear', 'region', 'incomeBracket', 'employmentStatus'].filter((k) => form[k]).length
  const progress = (filled / 4) * 100
  const isValid = filled === 4

  async function submit() {
    setLoading(true)
    try {
      await api.put('/users/me/onboarding', {
        birthYear: Number(form.birthYear),
        region: form.region,
        incomeBracket: form.incomeBracket,
        employmentStatus: form.employmentStatus,
      })
      navigate('/welfare')
    } catch {
      alert('저장에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative min-h-screen flex items-center justify-center px-4 py-10 overflow-hidden bg-gradient-to-br from-emerald-50 via-teal-50 to-cyan-50">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-20 -left-16 w-80 h-80 bg-emerald-300/30 rounded-full blur-3xl animate-blob" />
        <div className="absolute -bottom-24 -right-10 w-80 h-80 bg-cyan-300/30 rounded-full blur-3xl animate-blob" style={{ animationDelay: '6s' }} />
      </div>

      <div className="relative bg-white rounded-[2rem] shadow-2xl shadow-emerald-500/10 ring-1 ring-white/60 p-8 sm:p-10 w-full max-w-lg animate-scale-in">
        <div className="mb-7">
          <h1 className="text-3xl font-black text-slate-900">맞춤 설정</h1>
          <p className="text-slate-500 mt-2">정보를 입력하면 나에게 맞는 혜택을 추천해 드려요</p>
          {/* 진행률 */}
          <div className="mt-5 flex items-center gap-3">
            <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-emerald-500 to-teal-600 rounded-full transition-all duration-500"
                style={{ width: `${progress}%` }}
              />
            </div>
            <span className="text-xs font-bold text-emerald-600 tabular-nums">{filled}/4</span>
          </div>
        </div>

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">출생연도</label>
            <input
              type="number"
              inputMode="numeric"
              placeholder="예: 1998"
              value={form.birthYear}
              onChange={(e) => set('birthYear', e.target.value)}
              className="w-full ring-1 ring-slate-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-400 transition-shadow"
            />
          </div>

          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">거주 지역</label>
            <div className="flex flex-wrap gap-2">
              {regions.map((r) => (
                <button
                  key={r}
                  onClick={() => set('region', r)}
                  className={`px-3.5 py-1.5 rounded-full text-sm font-medium transition-all ${
                    form.region === r
                      ? 'bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-sm shadow-emerald-500/25'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">소득 수준</label>
            <div className="grid grid-cols-2 gap-2">
              {incomes.map((i) => (
                <button
                  key={i.value}
                  onClick={() => set('incomeBracket', i.value)}
                  className={`text-left px-4 py-3 rounded-xl ring-1 transition-all ${
                    form.incomeBracket === i.value
                      ? 'ring-emerald-500 bg-emerald-50'
                      : 'ring-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <span className={`block font-bold text-sm ${form.incomeBracket === i.value ? 'text-emerald-700' : 'text-slate-700'}`}>
                    {i.label}
                  </span>
                  <span className="block text-xs text-slate-400 mt-0.5">{i.desc}</span>
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-bold text-slate-700 mb-2">취업 상태</label>
            <div className="flex gap-2">
              {employments.map((e) => (
                <button
                  key={e.value}
                  onClick={() => set('employmentStatus', e.value)}
                  className={`flex-1 py-3 rounded-xl text-sm font-semibold transition-all flex flex-col items-center gap-1 ${
                    form.employmentStatus === e.value
                      ? 'bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-sm shadow-emerald-500/25'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  <span className="text-lg">{e.icon}</span>
                  {e.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        <button
          onClick={submit}
          disabled={!isValid || loading}
          className="mt-8 w-full bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold py-4 rounded-2xl shadow-lg shadow-emerald-500/25 hover:-translate-y-0.5 disabled:opacity-40 disabled:translate-y-0 disabled:shadow-none transition-all text-lg"
        >
          {loading ? '저장 중...' : isValid ? '맞춤 혜택 보러가기 →' : '모든 항목을 입력해주세요'}
        </button>
      </div>
    </div>
  )
}
