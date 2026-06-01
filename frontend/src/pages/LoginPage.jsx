import { useNavigate } from 'react-router-dom'

export default function LoginPage() {
  const navigate = useNavigate()

  function loginWith(provider) {
    window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-emerald-50 to-teal-50 flex items-center justify-center px-4">
      <div className="bg-white rounded-3xl shadow-xl p-10 w-full max-w-md">
        <button
          onClick={() => navigate('/')}
          className="text-slate-400 hover:text-slate-600 text-sm mb-8 flex items-center gap-1"
        >
          ← 홈으로
        </button>

        <div className="text-center mb-10">
          <span className="text-4xl font-black bg-gradient-to-r from-emerald-500 to-teal-600 bg-clip-text text-transparent">
            YouFare
          </span>
          <p className="text-slate-500 mt-3 text-lg">나에게 맞는 복지 혜택을 찾아보세요</p>
        </div>

        <div className="space-y-3">
          <button
            onClick={() => loginWith('kakao')}
            className="w-full flex items-center justify-center gap-3 bg-[#FEE500] hover:bg-[#F0D900] text-[#3C1E1E] font-bold py-4 rounded-2xl transition-colors text-lg"
          >
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
              <path d="M11 1.5C5.753 1.5 1.5 4.91 1.5 9.14c0 2.698 1.743 5.068 4.365 6.42l-.898 3.343a.3.3 0 0 0 .456.326l3.93-2.616A11.87 11.87 0 0 0 11 16.78c5.247 0 9.5-3.41 9.5-7.64S16.247 1.5 11 1.5z" fill="#3C1E1E"/>
            </svg>
            카카오로 로그인
          </button>

          <button
            onClick={() => loginWith('naver')}
            className="w-full flex items-center justify-center gap-3 bg-[#03C75A] hover:bg-[#02b351] text-white font-bold py-4 rounded-2xl transition-colors text-lg"
          >
            <span className="font-black text-white text-xl">N</span>
            네이버로 로그인
          </button>
        </div>

        <p className="text-center text-slate-400 text-xs mt-8">
          로그인 시 서비스 이용약관 및 개인정보 처리방침에 동의하게 됩니다.
        </p>
      </div>
    </div>
  )
}
