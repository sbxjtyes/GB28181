import { Component } from 'react';

/**
 * 错误边界组件
 * 捕获子组件的渲染错误，防止整个应用崩溃白屏
 */
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('应用错误:', error, errorInfo);
  }

  handleReload = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100vh',
          fontFamily: 'system-ui, sans-serif',
          color: '#333',
          padding: '20px',
          textAlign: 'center'
        }}>
          <h2>⚠️ 页面出现错误</h2>
          <p style={{ color: '#666', maxWidth: '400px' }}>
            {this.state.error?.message || '未知错误'}
          </p>
          <button
            onClick={this.handleReload}
            style={{
              marginTop: '16px',
              padding: '8px 24px',
              borderRadius: '6px',
              border: 'none',
              background: '#1890ff',
              color: '#fff',
              cursor: 'pointer',
              fontSize: '14px'
            }}
          >
            🔄 重新加载
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
