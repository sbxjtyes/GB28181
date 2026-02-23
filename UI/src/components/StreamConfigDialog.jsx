import { useState, useEffect } from 'react';
import './StreamConfigDialog.css';

/**
 * 推流配置对话框组件
 * 用于在开始推流前配置具体参数
 * 
 * @param {Object} props - 组件属性
 * @param {boolean} props.isOpen - 是否打开对话框
 * @param {Object} props.device - 设备信息
 * @param {Function} props.onConfirm - 确认推流回调
 * @param {Function} props.onCancel - 取消回调
 */
function StreamConfigDialog({ isOpen, device, onConfirm, onCancel }) {
  // 配置状态
  const [port, setPort] = useState(30000);
  const [autoPort, setAutoPort] = useState(true);
  const [useTcp, setUseTcp] = useState(false);
  const [loading, setLoading] = useState(false);

  /**
   * 对话框打开时重置配置
   */
  useEffect(() => {
    if (isOpen && device) {
      setPort(30000);
      setAutoPort(true);
      setUseTcp(false);
      setLoading(false);
    }
  }, [isOpen, device]);

  /**
   * 处理确认推流
   */
  const handleConfirm = async () => {
    setLoading(true);
    try {
      await onConfirm({
        deviceId: device.deviceId,
        port: autoPort ? 0 : (parseInt(port) || 30000),
        useTcp
      });
    } finally {
      setLoading(false);
    }
  };

  /**
   * 处理背景点击关闭
   */
  const handleBackdropClick = (e) => {
    if (e.target.className === 'dialog-backdrop' && !loading) {
      onCancel();
    }
  };

  if (!isOpen || !device) return null;

  return (
    <div className="dialog-backdrop" onClick={handleBackdropClick}>
      <div className="stream-config-dialog">
        <div className="dialog-header">
          <h3>📹 推流配置</h3>
          <button className="close-btn" onClick={onCancel} disabled={loading}>×</button>
        </div>

        <div className="dialog-body">
          <div className="device-info">
            <span className="label">设备ID:</span>
            <span className="value">{device.deviceId}</span>
          </div>

          <div className="config-item checkbox-item">
            <label>
              <input
                type="checkbox"
                checked={autoPort}
                onChange={e => setAutoPort(e.target.checked)}
                disabled={loading}
              />
              自动分配端口
            </label>
            <span className="hint">由ZLMediaKit自动分配可用端口，避免冲突</span>
          </div>

          {!autoPort && (
            <div className="config-item">
              <label htmlFor="port">RTP端口</label>
              <input
                id="port"
                type="number"
                value={port}
                onChange={e => setPort(e.target.value)}
                placeholder="30000"
                min="1024"
                max="65535"
                disabled={loading}
              />
              <span className="hint">接收RTP流的端口 (1024-65535)</span>
            </div>
          )}

          <div className="config-item">
            <label>流ID</label>
            <input
              type="text"
              value={`rtp_${device.deviceId}`}
              disabled
            />
            <span className="hint">自动生成，用于标识流和RTP端口</span>
          </div>

          <div className="config-item checkbox-item">
            <label>
              <input
                type="checkbox"
                checked={useTcp}
                onChange={e => setUseTcp(e.target.checked)}
                disabled={loading}
              />
              使用TCP传输
            </label>
            <span className="hint">默认使用UDP，勾选后使用TCP</span>
          </div>
        </div>

        <div className="dialog-footer">
          <button 
            className="btn btn-cancel" 
            onClick={onCancel}
            disabled={loading}
          >
            取消
          </button>
          <button 
            className="btn btn-confirm" 
            onClick={handleConfirm}
            disabled={loading}
          >
            {loading ? '推流中...' : '▶️ 开始推流'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default StreamConfigDialog;
