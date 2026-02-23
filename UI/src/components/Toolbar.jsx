import './Toolbar.css';

/**
 * 工具栏组件
 * 提供连接状态显示和批量操作按钮
 * 
 * @param {Object} props - 组件属性
 * @param {boolean} props.connected - 服务器连接状态
 * @param {number} props.deviceCount - 设备总数
 * @param {number} props.onlineCount - 在线设备数
 * @param {boolean} props.loading - 是否正在加载
 * @param {Function} props.onRefresh - 刷新回调
 * @param {Function} props.onBatchRegister - 批量注册回调
 * @param {Function} props.onBatchStream - 批量推流回调
 * @param {Function} props.onOpenSettings - 打开设置回调
 */
function Toolbar({ 
  connected, 
  deviceCount, 
  onlineCount, 
  loading,
  onRefresh, 
  onBatchRegister, 
  onBatchStream,
  onOpenSettings 
}) {
  return (
    <div className="toolbar">
      <div className="toolbar-left">
        <h1 className="title">📡 GB28181 设备管理</h1>
        <div className="connection-status">
          <span className={`status-indicator ${connected ? 'connected' : 'disconnected'}`}></span>
          <span className="status-text">
            {connected ? '已连接' : '未连接'}
          </span>
        </div>
        {connected && (
          <div className="device-stats">
            设备: {onlineCount}/{deviceCount} 在线
          </div>
        )}
      </div>
      
      <div className="toolbar-right">
        <button 
          className="toolbar-btn refresh" 
          onClick={onRefresh}
          disabled={loading}
        >
          {loading ? '⏳' : '🔄'} 刷新
        </button>
        
        <button 
          className="toolbar-btn batch-register"
          onClick={onBatchRegister}
          disabled={loading || deviceCount === 0}
        >
          📋 批量注册
        </button>
        
        <button 
          className="toolbar-btn settings"
          onClick={onOpenSettings}
        >
          ⚙️
        </button>
      </div>
    </div>
  );
}

export default Toolbar;
