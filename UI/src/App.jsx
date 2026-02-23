import { useState, useEffect, useCallback } from 'react';
import Toolbar from './components/Toolbar';
import DeviceCard from './components/DeviceCard';
import SettingsPanel from './components/SettingsPanel';
import MediaServerPanel from './components/MediaServerPanel';
import StreamConfigDialog from './components/StreamConfigDialog';
import ServiceControlPanel from './components/ServiceControlPanel';
import * as api from './api/sipApi';
import * as zlmApi from './api/zlmediaApi';
import './App.css';

/**
 * 主应用组件
 * GB28181 SIP服务器设备管理界面
 */
function App() {
  // 状态定义
  const [devices, setDevices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [settings, setSettings] = useState(api.getSettings());
  const [message, setMessage] = useState(null);
  const [activeTab, setActiveTab] = useState('devices'); // 标签页状态
  const [streamConfigOpen, setStreamConfigOpen] = useState(false); // 推流配置对话框
  const [selectedDevice, setSelectedDevice] = useState(null); // 选中的设备

  /**
   * 显示提示消息
   * @param {string} text - 消息内容
   * @param {string} type - 消息类型 (success/error/info)
   */
  const showMessage = useCallback((text, type = 'info') => {
    setMessage({ text, type });
    setTimeout(() => setMessage(null), 3000);
  }, []);

  /**
   * 刷新设备列表
   */
  const refreshDevices = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.getDevices();
      if (result.success) {
        setDevices(result.devices || []);
        setConnected(true);
        showMessage(`获取到 ${result.devices?.length || 0} 个设备`, 'success');
      } else {
        setConnected(false);
        showMessage('获取设备列表失败: ' + (result.message || '未知错误'), 'error');
      }
    } catch (err) {
      setConnected(false);
      showMessage('连接服务器失败', 'error');
    } finally {
      setLoading(false);
    }
  }, [showMessage]);

  // 初始加载
  useEffect(() => {
    refreshDevices();
  }, [refreshDevices]);

  /**
   * 处理单个设备重新注册
   * @param {string} deviceId - 设备ID
   */
  const handleReregister = async (deviceId) => {
    const result = await api.forceReregister(deviceId);
    if (result.success) {
      showMessage(`${deviceId} 注册请求已发送`, 'success');
    } else {
      showMessage(`注册失败: ${result.message}`, 'error');
    }
  };

  /**
   * 处理开始推流 - 打开配置对话框
   * @param {string} deviceId - 设备ID
   */
  const handleStartStream = (deviceId) => {
    const device = devices.find(d => d.deviceId === deviceId);
    if (device) {
      setSelectedDevice(device);
      setStreamConfigOpen(true);
    }
  };

  /**
   * 确认推流 - 执行实际推流操作
   * 流程：1.开启ZKServer RTP端口 → 2.发送SIP INVITE请求
   * @param {Object} config - 推流配置
   */
  const handleConfirmStream = async (config) => {
    const { deviceId, port, streamId, useTcp } = config;
    
    // 步骤1：先在ZKServer开启RTP收流端口
    showMessage(`正在开启RTP端口 ${port}...`, 'info');
    const rtpResult = await zlmApi.openRtpServer(port, streamId, useTcp ? 1 : 0);
    
    if (!rtpResult.success) {
      showMessage(`开启RTP端口失败: ${rtpResult.message}`, 'error');
      return;
    }
    
    const actualPort = rtpResult.data?.port || port;
    showMessage(`RTP端口 ${actualPort} 已开启，正在发送INVITE...`, 'info');
    
    // 步骤2：调用SIP服务器发送INVITE请求
    const serverSettings = api.getSettings();
    const result = await api.startStreamWithConfig(deviceId, {
      mediaServerIp: serverSettings.mediaServerIp,
      mediaServerPort: actualPort,
      useTcp
    });
    
    if (result.success) {
      showMessage(`${deviceId} 推流启动成功`, 'success');
      setStreamConfigOpen(false);
      setSelectedDevice(null);
    } else {
      // 推流失败时关闭RTP端口
      await zlmApi.closeRtpServer(streamId);
      showMessage(`推流失败: ${result.message}`, 'error');
    }
  };

  /**
   * 处理停止推流
   * @param {string} deviceId - 设备ID
   */
  const handleStopStream = async (deviceId) => {
    const result = await api.stopStream(deviceId);
    if (result.success) {
      showMessage(`${deviceId} 推流已停止`, 'success');
    } else {
      showMessage(`停止失败: ${result.message}`, 'error');
    }
  };

  /**
   * 批量强制注册所有离线设备
   */
  const handleBatchRegister = async () => {
    const offlineDevices = devices.filter(d => !d.online);
    if (offlineDevices.length === 0) {
      showMessage('没有离线设备需要注册', 'info');
      return;
    }
    
    const deviceIds = offlineDevices.map(d => d.deviceId);
    setLoading(true);
    try {
      const result = await api.batchForceReregister(deviceIds);
      if (result.success) {
        showMessage(`批量注册完成: 成功 ${result.successCount || 0} 个`, 'success');
      } else {
        showMessage(`批量注册失败: ${result.message}`, 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 批量推流所有在线设备
   */
  const handleBatchStream = async () => {
    const onlineDevices = devices.filter(d => d.online);
    if (onlineDevices.length === 0) {
      showMessage('没有在线设备可以推流', 'info');
      return;
    }
    
    const deviceIds = onlineDevices.map(d => d.deviceId);
    setLoading(true);
    try {
      const results = await api.batchStartStream(deviceIds);
      const successCount = results.filter(r => r.success).length;
      showMessage(`批量推流完成: ${successCount}/${deviceIds.length} 成功`, 'success');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 保存设置
   * @param {Object} newSettings - 新设置
   */
  const handleSaveSettings = (newSettings) => {
    api.saveConfig(newSettings);
    setSettings(newSettings);
    showMessage('设置已保存', 'success');
    refreshDevices();
  };

  // 统计数据
  const onlineCount = devices.filter(d => d.online).length;

  return (
    <div className="app">
      <Toolbar
        connected={connected}
        deviceCount={devices.length}
        onlineCount={onlineCount}
        loading={loading}
        onRefresh={refreshDevices}
        onBatchRegister={handleBatchRegister}
        onBatchStream={handleBatchStream}
        onOpenSettings={() => setSettingsOpen(true)}
      />

      {/* 标签页导航 */}
      <div className="tab-nav">
        <button 
          className={`tab-btn ${activeTab === 'services' ? 'active' : ''}`}
          onClick={() => setActiveTab('services')}
        >
          🚀 服务控制
        </button>
        <button 
          className={`tab-btn ${activeTab === 'devices' ? 'active' : ''}`}
          onClick={() => setActiveTab('devices')}
        >
          📹 设备管理
        </button>
        <button 
          className={`tab-btn ${activeTab === 'media' ? 'active' : ''}`}
          onClick={() => setActiveTab('media')}
        >
          🎥 媒体服务器
        </button>
      </div>

      {/* 提示消息 */}
      {message && (
        <div className={`message ${message.type}`}>
          {message.text}
        </div>
      )}

      {/* 设备管理标签页 */}
      {activeTab === 'devices' && (
        <div className="device-grid">
          {devices.length === 0 && !loading && (
            <div className="empty-state">
              {connected ? '暂无设备' : '点击刷新按钮连接服务器'}
            </div>
          )}
          
          {devices.map(device => (
            <DeviceCard
              key={device.deviceId}
              device={device}
              onReregister={handleReregister}
              onStartStream={handleStartStream}
              onStopStream={handleStopStream}
            />
          ))}
        </div>
      )}

      {/* 媒体服务器标签页 */}
      {activeTab === 'media' && (
        <MediaServerPanel showMessage={showMessage} />
      )}

      {/* 服务控制标签页 */}
      {activeTab === 'services' && (
        <ServiceControlPanel showMessage={showMessage} />
      )}

      {/* 设置面板 */}
      <SettingsPanel
        isOpen={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        settings={settings}
        onSave={handleSaveSettings}
      />

      {/* 推流配置对话框 */}
      <StreamConfigDialog
        isOpen={streamConfigOpen}
        device={selectedDevice}
        onConfirm={handleConfirmStream}
        onCancel={() => {
          setStreamConfigOpen(false);
          setSelectedDevice(null);
        }}
      />
    </div>
  );
}

export default App;
