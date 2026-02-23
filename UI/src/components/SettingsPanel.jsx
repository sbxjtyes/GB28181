import { useState } from 'react';
import './SettingsPanel.css';

/**
 * 设置面板组件
 * 用于配置服务器地址和媒体服务器参数
 * 
 * @param {Object} props - 组件属性
 * @param {boolean} props.isOpen - 面板是否打开
 * @param {Function} props.onClose - 关闭面板回调
 * @param {Object} props.settings - 当前设置
 * @param {Function} props.onSave - 保存设置回调
 */
function SettingsPanel({ isOpen, onClose, settings, onSave }) {
  const [formData, setFormData] = useState(settings);

  /**
   * 处理输入变化
   * @param {Event} e - 输入事件
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'mediaServerPort' ? parseInt(value) || 0 : value
    }));
  };

  /**
   * 处理表单提交
   * @param {Event} e - 提交事件
   */
  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="settings-overlay" onClick={onClose}>
      <div className="settings-panel" onClick={e => e.stopPropagation()}>
        <div className="settings-header">
          <h2>⚙️ 设置</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>SIP服务器地址</label>
            <input
              type="text"
              name="baseUrl"
              value={formData.baseUrl}
              onChange={handleChange}
              placeholder="http://localhost:8080/gb28181"
            />
          </div>
          
          <div className="form-group">
            <label>媒体服务器IP</label>
            <input
              type="text"
              name="mediaServerIp"
              value={formData.mediaServerIp}
              onChange={handleChange}
              placeholder="192.168.0.15"
            />
            <span className="form-hint">摄像头推流目标地址，通常为本机局域网IP</span>
          </div>
          
          <div className="form-divider">ZLMediaKit 配置</div>
          
          <div className="form-group">
            <label>ZLMediaKit 地址</label>
            <input
              type="text"
              name="zlmBaseUrl"
              value={formData.zlmBaseUrl || ''}
              onChange={handleChange}
              placeholder="http://localhost:88"
            />
          </div>
          
          <div className="form-group">
            <label>ZLMediaKit 密钥</label>
            <input
              type="text"
              name="zlmSecret"
              value={formData.zlmSecret || ''}
              onChange={handleChange}
              placeholder="API密钥"
            />
          </div>
          
          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={onClose}>
              取消
            </button>
            <button type="submit" className="btn-save">
              保存
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SettingsPanel;
