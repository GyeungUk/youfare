import { Component } from 'react'

/**
 * 렌더링 중 예기치 못한 예외가 발생해도 앱 전체가 백지화되지 않도록 막는 안전망.
 * 하위 트리에서 throw된 에러를 잡아 폴백 UI를 보여준다.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary caught:', error, info)
  }

  handleReset = () => {
    this.setState({ hasError: false })
    window.location.href = '/'
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
          <div className="text-center">
            <p className="text-5xl mb-4">😵</p>
            <h1 className="text-xl font-bold text-slate-800 mb-2">문제가 발생했어요</h1>
            <p className="text-slate-500 text-sm mb-6">잠시 후 다시 시도해 주세요.</p>
            <button
              onClick={this.handleReset}
              className="bg-emerald-500 text-white font-bold px-6 py-3 rounded-xl hover:bg-emerald-600 transition-colors"
            >
              홈으로 돌아가기
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
