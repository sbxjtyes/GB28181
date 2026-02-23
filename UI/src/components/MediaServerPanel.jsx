import { useState, useEffect, useCallback } from 'react';
import StreamCard from './StreamCard';
import * as zlmApi from '../api/zlmediaApi';
import './MediaServerPanel.css';

/**
 * 媒体服务器控制面板组件
 * 管理ZLMediaKit媒体服务器的流列表和RTP服务器
 * 
 * @param {Object} props - 组件属性
 * @param {Function} props.showMessage - 显示消息回调
 */
function MediaServerPanel({ showMessage }) {
  // 状态定义
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [streams, setStreams] = useState([]);
  const [rtpServers, setRtpServers] = useState([]);

  /**
   * 刷新流列表
   * @param {boolean} silent - 是否静默刷新（不弹toast）
   */
  const refreshStreams = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const result = await zlmApi.getMediaList();
      if (result.success) {
        // 按 app + stream 去重，ZLMediaKit会为同一个流返回多个协议版本
        const allStreams = result.data.data || [];
        const uniqueMap = new Map();
        allStreams.forEach(stream => {
          const key = `${stream.app}/${stream.stream}`;
          if (!uniqueMap.has(key)) {
            uniqueMap.set(key, stream);
          }
        });
        const uniqueStreams = Array.from(uniqueMap.values());
        
        setStreams(uniqueStreams);
        setConnected(true);
        if (!silent) {
          showMessage(`获取到 ${uniqueStreams.length} 个流`, 'success');
        }
      } else {
        setConnected(false);
        if (!silent) {
          showMessage('获取流列表失败: ' + result.message, 'error');
        }
      }
    } catch (err) {
      setConnected(false);
      if (!silent) {
        showMessage('连接媒体服务器失败', 'error');
      }
    } finally {
      if (!silent) setLoading(false);
    }
  }, [showMessage]);

  /**
   * 刷新RTP服务器列表
   */
  const refreshRtpServers = useCallback(async () => {
    try {
      const result = await zlmApi.listRtpServer();
      if (result.success) {
        setRtpServers(result.data.data || []);
      }
    } catch (err) {
      console.error('获取RTP服务器列表失败:', err);
    }
  }, []);

  /**
   * 测试连接
   */
  const testConnection = useCallback(async () => {
    const isConnected = await zlmApi.testConnection();
    setConnected(isConnected);
    if (isConnected) {
      refreshStreams(true);
      refreshRtpServers();
    }
  }, [refreshStreams, refreshRtpServers]);

  // 初始加载 + 自动刷新
  useEffect(() => {
    testConnection();
    const interval = setInterval(() => {
      if (connected) {
        refreshStreams(true);
        refreshRtpServers();
      }
    }, 15000);
    return () => clearInterval(interval);
  }, [testConnection, connected, refreshStreams, refreshRtpServers]);

  /**
   * 关闭流
   * @param {string} schema - 协议类型 (如 rtsp/rtmp 等)
   * @param {string} app - 应用名
   * @param {string} stream - 流名
   * @param {string} vhost - 虚拟主机名
   */
  const handleCloseStream = async (schema, app, stream, vhost) => {
    try {
      // 不传schema，关闭该流的所有协议版本
      const result = await zlmApi.closeStream(null, app, stream, true, vhost || '__defaultVhost__');
      if (result.success) {
        showMessage(`流 ${app}/${stream} 已关闭`, 'success');
        refreshStreams(true);
        refreshRtpServers();
      } else {
        showMessage(`关闭失败: ${result.message}`, 'error');
      }
    } catch (err) {
      showMessage(`关闭流异常: ${err.message}`, 'error');
    }
  };

  /**
   * 复制地址回调
   * @param {string} type - 协议类型
   */
  const handleCopyUrl = (type, success = true, errorMessage = '') => {
    if (success) {
      showMessage(`${type} 地址已复制`, 'success');
      return;
    }
    showMessage(`${type} 地址复制失败${errorMessage ? `: ${errorMessage}` : ''}`, 'error');
  };

  /**
   * 关闭RTP服务器
   * @param {string} streamId - 流ID
   */
  const handleCloseRtpServer = async (streamId) => {
    try {
      const result = await zlmApi.closeRtpServer(streamId);
      if (result.success) {
        showMessage(`RTP服务器 ${streamId} 已关闭`, 'success');
        refreshRtpServers();
      } else {
        showMessage(`关闭失败: ${result.message}`, 'error');
      }
    } catch (err) {
      showMessage(`关闭RTP服务器异常: ${err.message}`, 'error');
    }
  };

  return (
    <div className="media-server-panel">
      {/* 服务器状态栏 */}
      <div className="server-status-bar">
        <div className="status-info">
          <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
          <span className="status-text">
            ZLMediaKit {connected ? '已连接' : '未连接'}
          </span>
          {connected && (
            <span className="stream-count">
              流数量: {streams.length}
            </span>
          )}
        </div>
        <div className="status-actions">
          <button 
            className="status-btn refresh"
            onClick={refreshStreams}
            disabled={loading}
          >
            {loading ? '⏳' : '🔄'} 刷新
          </button>
        </div>
      </div>

      {/* RTP服务器管理 - 仅展示，推流时自动开启端口 */}
      <div className="rtp-server-section">
        <h3 className="section-title">📡 活跃的RTP收流端口</h3>
        <p className="section-hint">推流时自动开启端口，此处仅展示当前活跃的收流端口</p>
        
        {rtpServers.length > 0 ? (
          <div className="rtp-server-list">
            {rtpServers.map((server, index) => (
              <div key={index} className="rtp-server-item">
                <span className="rtp-port">端口: {server.port}</span>
                <span className="rtp-stream-id">流ID: {server.stream_id}</span>
                <button 
                  className="rtp-close-btn"
                  onClick={() => handleCloseRtpServer(server.stream_id)}
                >
                  关闭
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-rtp">暂无活跃的RTP收流端口</div>
        )}
      </div>

      {/* 流列表 */}
      <div className="streams-section">
        <h3 className="section-title">🎥 在线流列表</h3>
        <div className="stream-grid">
          {streams.length === 0 && !loading && (
            <div className="empty-streams">
              {connected ? '暂无在线流' : '点击刷新按钮连接服务器'}
            </div>
          )}
          
          {streams.map((stream, index) => (
            <StreamCard
              key={`${stream.app}-${stream.stream}-${index}`}
              stream={stream}
              playUrls={zlmApi.generatePlayUrls(stream)}
              onClose={handleCloseStream}
              onCopyUrl={handleCopyUrl}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

export default MediaServerPanel;
