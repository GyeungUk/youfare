import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import OnboardingPage from './pages/OnboardingPage'
import WelfareListPage from './pages/WelfareListPage'
import WelfareDetailPage from './pages/WelfareDetailPage'
import RecommendPage from './pages/RecommendPage'
import ScrapPage from './pages/ScrapPage'
import ChatPage from './pages/ChatPage'
import Layout from './components/Layout'
import OAuthCallback from './pages/OAuthCallback'

function PrivateRoute({ children }) {
  return localStorage.getItem('token') ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
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
          <Route path="/chat" element={<ChatPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
