import api from './axios'

/**
 * 로그인/회원가입 성공 후 공통 처리.
 * 토큰을 저장하고, 온보딩 완료 여부에 따라 목적지로 이동한다.
 * (OAuthCallback·LoginPage·SignupPage가 동일 규칙을 공유)
 *
 * @param {string} token       accessToken
 * @param {function} navigate  react-router navigate
 * @param {string} [redirect]  로그인 후 돌아갈 경로(없으면 /welfare)
 */
export async function completeLogin(token, navigate, redirect) {
  localStorage.setItem('token', token)
  try {
    const r = await api.get('/users/me')
    const onboarded = r.data?.data?.birthYear != null
    navigate(onboarded ? (redirect || '/welfare') : '/onboarding', { replace: true })
  } catch {
    navigate('/onboarding', { replace: true })
  }
}
