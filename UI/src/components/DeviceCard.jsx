import { useState } from 'react';
import './DeviceCard.css';

/**
 * 设备卡片组件
 * 显示单个设备信息和操作按钮
 * 
 * @param {Object} props - 组件属性
 * @param {Object} props.device - 设备信息
 * @param {Function} props.onReregister - 重新注册回调
 * @param {Function} props.onStartStream - 开始推流回调
 * @param {Function} props.onStopStream - 停止推流回调
 */
function DeviceCard({ device, onReregister, onStartStream, onStopStream }) {
  const [loading, setLoading] = useState(null);

  /**
   * 处理操作按钮点击
   * @param {string} action - 操作类型
   * @param {Function} handler - 操作处理函数
   */
  const handleAction = async (action, handler) => {
    setLoading(action);
    try {
      await handler(device.deviceId);
    } finally {
      setLoading(null);
    }
  };

  return (
    <div className={`device-card ${device.online ? 'online' : 'offline'}`}>
      <div className="device-header">
        <span className={`status-dot ${device.online ? 'online' : 'offline'}`}></span>
        <span className="device-status">{device.online ? '在线' : '离线'}</span>
      </div>
      
      <div className="device-id" title={device.deviceId}>
        {device.deviceId}
      </div>
      
      <div className="device-actions">
        <button 
          className="btn btn-register"
          onClick={() => handleAction('register', onReregister)}
          disabled={loading !== null}
        >
          {loading === 'register' ? '...' : '🔄 注册'}
        </button>
        
        <button 
          className="btn btn-play"
          onClick={() => handleAction('play', onStartStream)}
          disabled={loading !== null || !device.online}
        >
          {loading === 'play' ? '...' : '▶️ 推流'}
        </button>
        
        <button 
          className="btn btn-stop"
          onClick={() => handleAction('stop', onStopStream)}
          disabled={loading !== null}
        >
          {loading === 'stop' ? '...' : '⏹️ 停止'}
        </button>
      </div>
    </div>
  );
}

export default DeviceCard;
