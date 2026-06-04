import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    // 터널(cloudflared/ngrok)의 임의 호스트명으로 들어오는 요청을 vite가 막지 않도록 허용.
    // (미설정 시 "Blocked request. This host is not allowed." 로 거부됨)
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // OAuth는 axios가 아니라 브라우저 전체 이동(top-level navigation)이라 프록시로 백엔드에 직접 넘긴다.
      //  · /oauth2/authorization/{provider} : 로그인 시작 → 카카오/네이버로 리다이렉트
      //  · /login/oauth2/code/{provider}    : 소셜 인증 후 콜백 수신
      '/oauth2': { target: 'http://localhost:8080', changeOrigin: true },
      '/login/oauth2': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
