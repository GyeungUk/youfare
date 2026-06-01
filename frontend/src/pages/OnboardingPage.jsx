import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

const regions = ['서울', '부산', '인천', '대구', '대전', '광주', '울산', '세종', '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주']
const incomes = [
  { value: 'LOW', label: '저소득 (중위소득 50% 이하)' },
  { value: 'MIDDLE', label: '중간 (중위소득 50~150%)' },
  { value: 'HIGH', label: '고소득 (중위소득 150% 초과)' },
  { value: 'UNKNOWN', label: '모름 / 해당없음' },
]
const employments = [
  { value: 'STUDENT', label: '학생' },
  { value: 'JOB_SEEKER', label: '구직 중' },
  { value: 'EMPLOYED', label: '재직 중' },
]

export default function OnboardingPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ birthYear: '', region: '', incomeBracket: '', employmentStatus: '' })
  const [loading, setLoading] = useState(false)

  function set(key, val) {
    setForm((f) => ({ ...f, [key]: val }))
  }

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

  const isValid = form.birthYear && form.region && form.incomeBracket && form.employmentStatus

  return (
    <div className="min-h-screen bg-gradient-to-br from-emerald-50 to-teal-50 flex items-center justify-center px-4">
      <div className="bg-white rounded-3xl shadow-xl p-10 w-full max-w-lg">
        <div className="mb-8">
          <h1 className="text-3xl font-black text-slate-900">맞춤 설정</h1>
          <p className="text-slate-500 mt-2">정보를 입력하면 나에게 맞는 혜택을 추천해 드려요</p>
        </div>

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">출생연도</label>
            <input
              type="number"
              placeholder="예: 1998"
              value={form.birthYear}
              onChange={(e) => set('birthYear', e.target.value)}
              className="w-full border border-slate-200 rounded-xl px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-400"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">거주 지역</label>
            <div className="flex flex-wrap gap-2">
              {regions.map((r) => (
                <button
                  key={r}
                  onClick={() => set('region', r)}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                    form.region === r
                      ? 'bg-emerald-500 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">소득 수준</label>
            <div className="space-y-2">
              {incomes.map((i) => (
                <button
                  key={i.value}
                  onClick={() => set('incomeBracket', i.value)}
                  className={`w-full text-left px-4 py-3 rounded-xl border transition-colors ${
                    form.incomeBracket === i.value
                      ? 'border-emerald-500 bg-emerald-50 text-emerald-700'
                      : 'border-slate-200 hover:bg-slate-50 text-slate-600'
                  }`}
                >
                  {i.label}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">취업 상태</label>
            <div className="flex gap-2">
              {employments.map((e) => (
                <button
                  key={e.value}
                  onClick={() => set('employmentStatus', e.value)}
                  className={`flex-1 py-3 rounded-xl text-sm font-medium transition-colors ${
                    form.employmentStatus === e.value
                      ? 'bg-emerald-500 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {e.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        <button
          onClick={submit}
          disabled={!isValid || loading}
          className="mt-8 w-full bg-gradient-to-r from-emerald-500 to-teal-600 text-white font-bold py-4 rounded-2xl hover:opacity-90 disabled:opacity-40 transition-opacity text-lg"
        >
          {loading ? '저장 중...' : '맞춤 혜택 보러가기 →'}
        </button>
      </div>
    </div>
  )
}
