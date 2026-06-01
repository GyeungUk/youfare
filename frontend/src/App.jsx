import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
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

function PrivateRoute({ children }) {
  return localStorage.getItem('token') ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/oauth/callback" element={<OAuthCallback />} />
          <Route path="/onboarding" element={<PrivateRoute><OnboardingPage /></PrivateRoute>} />
          <Route element={<PrivateRoute><Layout /></PrivateRoute>}>
            <Route path="/welfare" element={<WelfareListPage />} />
            <Route path="/welfare/:id" element={<WelfareDetailPage />} />
            <Route path="/recommend" element={<RecommendPage />} />
            <Route path="/scraps" element={<ScrapPage />} />
            <Route path="/community" element={<CommunityPage />} />
            <Route path="/community/new" element={<CommunityWritePage />} />
            <Route path="/community/:id" element={<CommunityDetailPage />} />
            <Route path="/community/:id/edit" element={<CommunityWritePage />} />
            <Route path="/chat" element={<ChatPage />} />
          </Route>
          {/* 정의되지 않은 경로는 홈으로 */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
