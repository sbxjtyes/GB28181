import { useState, useEffect, useCallback } from 'react';
import * as serviceApi from '../api/serviceManagerApi';
import './ServiceControlPanel.css';


/**
 * 服务控制面板组件
 * 用于启动/停止 SIPServer 和 ZKServer
 */
function ServiceControlPanel({ showMessage }) {
  // 状态
  const [managerAvailable, setManagerAvailable] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sipStatus, setSipStatus] = useState({ status: 'unknown', online: false, logs: [] });
  const [zkStatus, setZkStatus] = useState({ status: 'unknown', online: false, logs: [] });
  const [actionLoading, setActionLoading] = useState({
    sipStart: false, sipStop: false,
    zkStart: false, zkStop: false,
    allStart: false, allStop: false
  });
  
  // 服务地址配置相关状态
  const [managerUrl, setManagerUrl] = useState(serviceApi.getManagerUrl());
  const [isEditingUrl, setIsEditingUrl] = useState(false);
  const [tempUrl, setTempUrl] = useState(managerUrl);

  /**
   * 刷新服务状态
   */
  const refreshStatus = useCallback(async () => {
    setLoading(true);
    try {
      const result = await serviceApi.getServicesStatus();
      if (result.success) {
        setManagerAvailable(true);
        setSipStatus(result.sipServer || { status: 'unknown', online: false, logs: [] });
        setZkStatus(result.zkServer || { status: 'unknown', online: false, logs: [] });
      } else {
        setManagerAvailable(false);
      }
    } catch {
      setManagerAvailable(false);
    } finally {
      setLoading(false);
    }
  }, []);

  // 初始加载和定时刷新
  useEffect(() => {
    refreshStatus();
    const interval = setInterval(refreshStatus, 5000);
    return () => clearInterval(interval);
  }, [refreshStatus]);

  /**
   * 执行服务操作
   */
  const handleAction = async (action, actionName) => {
    setActionLoading(prev => ({ ...prev, [actionName]: true }));
    try {
      const result = await action();
      if (result.success) {
        showMessage(result.message || '操作成功', 'success');
      } else {
        showMessage(result.message || '操作失败', 'error');
      }
      // 刷新状态
      setTimeout(refreshStatus, 1000);
    } catch (err) {
      showMessage('操作失败: ' + err.message, 'error');
    } finally {
      setActionLoading(prev => ({ ...prev, [actionName]: false }));
    }
  };

  /**
   * 获取状态显示文本
   */
  const getStatusText = (status, online) => {
    if (online) return '运行中';
    if (status === 'starting') return '启动中';
    if (status === 'stopping') return '停止中';
    if (status === 'error') return '错误';
    return '已停止';
  };

  /**
   * 获取状态样式类名
   */
  const getStatusClass = (status, online) => {
    if (online) return 'running';
    if (status === 'starting') return 'starting';
    if (status === 'error') return 'error';
    return 'stopped';
  };

  /**
   * 保存服务管理器地址
   */
  const handleSaveUrl = () => {
    if (!tempUrl.trim()) return;
    
    // 如果没有协议前缀，默认添加 http://
    let urlToSave = tempUrl.trim();
    if (!/^https?:\/\//i.test(urlToSave)) {
      urlToSave = `http://${urlToSave}`;
    }
    
    serviceApi.setManagerUrl(urlToSave);
    setManagerUrl(urlToSave);
    setTempUrl(urlToSave);
    setIsEditingUrl(false);
    showMessage('服务管理器地址已更新', 'success');
    
    // 立即刷新状态
    setTimeout(refreshStatus, 100);
  };

  return (
    <div className="service-control-panel">
      {/* 地址配置栏 */}
      <div className="url-config-bar">
        {isEditingUrl ? (
          <div className="url-input-group">
            <input 
              type="text" 
              value={tempUrl} 
              onChange={(e) => setTempUrl(e.target.value)}
              placeholder="例: http://192.168.1.100:3001"
              className="url-input"
            />
            <button className="btn-save-sm" onClick={handleSaveUrl}>保存</button>
            <button className="btn-cancel-sm" onClick={() => setIsEditingUrl(false)}>取消</button>
          </div>
        ) : (
          <div className="current-url">
            <span className="label">管理器地址:</span>
            <span className="value">{managerUrl}</span>
            <button className="btn-edit-sm" onClick={() => setIsEditingUrl(true)}>✏️ 修改</button>
          </div>
        )}
      </div>

      {/* 服务管理器状态 */}
      <div className="manager-status-bar">
        <div className="manager-info">
          <span className={`status-dot ${managerAvailable ? 'connected' : 'disconnected'}`}></span>
          <span className="manager-text">
            服务管理器 {managerAvailable ? '已连接' : '未连接'}
          </span>
          {!managerAvailable && (
            <span className="manager-hint">
              请运行 <code>node server-manager.cjs</code> 启动管理器
            </span>
          )}
        </div>
        <div className="manager-actions">
          <button 
            className="refresh-btn"
            onClick={refreshStatus}
            disabled={loading}
          >
            {loading ? '⏳' : '🔄'} 刷新
          </button>
        </div>
      </div>

      {/* 快捷操作 */}
      <div className="quick-actions">
        <button
          className="action-btn start-all"
          onClick={() => handleAction(serviceApi.startAllServices, 'allStart')}
          disabled={!managerAvailable || actionLoading.allStart}
        >
          {actionLoading.allStart ? '⏳ 启动中...' : '🚀 启动所有服务'}
        </button>
        <button
          className="action-btn stop-all"
          onClick={() => handleAction(serviceApi.stopAllServices, 'allStop')}
          disabled={!managerAvailable || actionLoading.allStop}
        >
          {actionLoading.allStop ? '⏳ 停止中...' : '⏹️ 停止所有服务'}
        </button>
      </div>

      {/* 服务卡片 */}
      <div className="service-cards">
        {/* SIPServer 卡片 */}
        <div className="service-card">
          <div className="service-header">
            <div className="service-icon">📡</div>
            <div className="service-info">
              <h3 className="service-name">SIPServer</h3>
              <p className="service-desc">GB28181 SIP 信令服务器</p>
            </div>
          </div>
          
          <div className="service-status">
            <span className={`status-badge ${getStatusClass(sipStatus.status, sipStatus.online)}`}>
              {getStatusText(sipStatus.status, sipStatus.online)}
            </span>
            {sipStatus.pid && <span className="pid-badge">PID: {sipStatus.pid}</span>}
          </div>
          
          <div className="service-details">
            <div className="detail-item">
              <span className="detail-label">端口</span>
              <span className="detail-value">8080 (HTTP) / 5060 (SIP)</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">目录</span>
              <span className="detail-value">../SIPServer</span>
            </div>
          </div>

          <div className="service-actions">
            <button
              className="btn btn-start"
              onClick={() => handleAction(serviceApi.startSipServer, 'sipStart')}
              disabled={!managerAvailable || actionLoading.sipStart || sipStatus.online}
            >
              {actionLoading.sipStart ? '⏳' : '▶️'} 启动
            </button>
            <button
              className="btn btn-stop"
              onClick={() => handleAction(serviceApi.stopSipServer, 'sipStop')}
              disabled={!managerAvailable || actionLoading.sipStop}
            >
              {actionLoading.sipStop ? '⏳' : '⏹️'} 停止
            </button>
          </div>

          {/* 日志区域 */}
          {sipStatus.logs && sipStatus.logs.length > 0 && (
            <div className="service-logs">
              <div className="logs-header">📋 最近日志</div>
              <div className="logs-content">
                {sipStatus.logs.slice(-5).map((log, idx) => (
                  <div key={idx} className="log-line">{log}</div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* ZKServer 卡片 */}
        <div className="service-card">
          <div className="service-header">
            <div className="service-icon">🎥</div>
            <div className="service-info">
              <h3 className="service-name">ZKServer</h3>
              <p className="service-desc">ZLMediaKit 媒体服务器</p>
            </div>
          </div>
          
          <div className="service-status">
            <span className={`status-badge ${getStatusClass(zkStatus.status, zkStatus.online)}`}>
              {getStatusText(zkStatus.status, zkStatus.online)}
            </span>
            {zkStatus.pid && <span className="pid-badge">PID: {zkStatus.pid}</span>}
          </div>
          
          <div className="service-details">
            <div className="detail-item">
              <span className="detail-label">端口</span>
              <span className="detail-value">88 (HTTP) / 554 (RTSP) / 1935 (RTMP)</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">目录</span>
              <span className="detail-value">../ZKServer</span>
            </div>
          </div>

          <div className="service-actions">
            <button
              className="btn btn-start"
              onClick={() => handleAction(serviceApi.startZkServer, 'zkStart')}
              disabled={!managerAvailable || actionLoading.zkStart || zkStatus.online}
            >
              {actionLoading.zkStart ? '⏳' : '▶️'} 启动
            </button>
            <button
              className="btn btn-stop"
              onClick={() => handleAction(serviceApi.stopZkServer, 'zkStop')}
              disabled={!managerAvailable || actionLoading.zkStop}
            >
              {actionLoading.zkStop ? '⏳' : '⏹️'} 停止
            </button>
          </div>

          {/* 日志区域 */}
          {zkStatus.logs && zkStatus.logs.length > 0 && (
            <div className="service-logs">
              <div className="logs-header">📋 最近日志</div>
              <div className="logs-content">
                {zkStatus.logs.slice(-5).map((log, idx) => (
                  <div key={idx} className="log-line">{log}</div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 使用说明 */}
      <div className="usage-guide">
        <h4>🔧 使用说明</h4>
        <ol>
          <li>首先在终端运行 <code>node server-manager.cjs</code> 启动服务管理器</li>
          <li>点击"启动所有服务"快速启动 SIPServer 和 ZKServer</li>
          <li>或单独控制各个服务的启动/停止</li>
          <li>服务状态每 5 秒自动刷新</li>
        </ol>
      </div>
    </div>
  );
}

export default ServiceControlPanel;
