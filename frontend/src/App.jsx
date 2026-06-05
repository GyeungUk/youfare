import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import FindAccountPage from './pages/FindAccountPage'
import OnboardingPage from './pages/OnboardingPage'
import WelfareListPage from './pages/WelfareListPage'
import WelfareDetailPage from './pages/WelfareDetailPage'
import RecommendPage from './pages/RecommendPage'
import ScrapPage from './pages/ScrapPage'
import ChatPage from './pages/ChatPage'
import CommunityPage from './pages/CommunityPage'
import CommunityDetailPage from './pages/CommunityDetailPage'
import CommunityWritePage from './pages/CommunityWritePage'
import Layout from './components/Layout'
import OAuthCallback from './pages/OAuthCallback'
import ErrorBoundary from './components/ErrorBoundary'

// 로그인 필요한 페이지 가드. 비로그인이면 로그인 페이지로 보내되,
// 로그인 후 원래 가려던 곳으로 돌아오도록 redirect 경로를 쿼리로 전달한다.
function PrivateRoute({ children }) {
  const loc = useLocation()
  if (localStorage.getItem('token')) return children
  return <Navigate to={`/login?redirect=${encodeURIComponent(loc.pathname)}`} replace />
}

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/find-account" element={<FindAccountPage />} />
          <Route path="/oauth/callback" element={<OAuthCallback />} />
          <Route path="/onboarding" element={<PrivateRoute><OnboardingPage /></PrivateRoute>} />
          {/* Layout(헤더/네비)은 게스트도 공유 — 페이지별로 로그인 여부를 가른다 */}
          <Route element={<Layout />}>
            {/* 비로그인 공개: 복지 목록/상세 + AI 상담 */}
            <Route path="/welfare" element={<WelfareListPage />} />
            <Route path="/welfare/:id" element={<WelfareDetailPage />} />
            <Route path="/chat" element={<ChatPage />} />
            {/* 게스트 열람 허용 — 콘텐츠는 보이되 로그인 유도(페이지 내부에서 처리) */}
            {/*   · 맞춤 추천/스크랩: 로그인 안내 상태 노출  */}
            {/*   · 커뮤니티 목록/상세: 글·댓글 읽기 가능, '쓰기' 시도 시 로그인 모달 */}
            <Route path="/recommend" element={<RecommendPage />} />
            <Route path="/scraps" element={<ScrapPage />} />
            <Route path="/community" element={<CommunityPage />} />
            <Route path="/community/:id" element={<CommunityDetailPage />} />
            {/* 쓰기/수정은 로그인 필수 — 직접 URL 접근 시 가드가 로그인으로 보냄 */}
            <Route path="/community/new" element={<PrivateRoute><CommunityWritePage /></PrivateRoute>} />
            <Route path="/community/:id/edit" element={<PrivateRoute><CommunityWritePage /></PrivateRoute>} />
          </Route>
          {/* 정의되지 않은 경로는 홈으로 */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
