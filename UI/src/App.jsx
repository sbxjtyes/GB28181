import { useState, useEffect, useCallback, useRef } from 'react';
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
  const [messages, setMessages] = useState([]); // 消息队列（支持多条叠显）
  const [activeTab, setActiveTab] = useState('devices'); // 标签页状态
  const [streamConfigOpen, setStreamConfigOpen] = useState(false); // 推流配置对话框
  const [selectedDevice, setSelectedDevice] = useState(null); // 选中的设备

  // 消息ID计数器
  const messageIdRef = useRef(0);

  /**
   * 显示提示消息（追加到队列，支持多条同时显示）
   * @param {string} text - 消息内容
   * @param {string} type - 消息类型 (success/error/info)
   */
  const showMessage = useCallback((text, type = 'info') => {
    const id = ++messageIdRef.current;
    setMessages(prev => {
      // 最多保留3条，超出则移除最旧的
      const next = prev.length >= 3 ? prev.slice(1) : prev;
      return [...next, { id, text, type }];
    });
    // 3秒后自动移除该条消息
    setTimeout(() => {
      setMessages(prev => prev.filter(m => m.id !== id));
    }, 3000);
  }, []);

  /**
   * 刷新设备列表
   * @param {boolean} silent - 是否静默刷新（不弹toast）
   */
  const refreshDevices = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const result = await api.getDevices();
      if (result.success) {
        setDevices(result.devices || []);
        setConnected(true);
        if (!silent) {
          showMessage(`获取到 ${result.devices?.length || 0} 个设备`, 'success');
        }
      } else {
        setConnected(false);
        if (!silent) {
          showMessage('获取设备列表失败: ' + (result.message || '未知错误'), 'error');
        }
      }
    } catch (err) {
      setConnected(false);
      if (!silent) {
        showMessage('连接服务器失败', 'error');
      }
    } finally {
      if (!silent) setLoading(false);
    }
  }, [showMessage]);

  // 初始加载 + 自动刷新（10秒轮询，静默模式）
  useEffect(() => {
    refreshDevices();
    const interval = setInterval(() => {
      refreshDevices(true);
    }, 10000);
    return () => clearInterval(interval);
  }, [refreshDevices]);

  /**
   * 处理单个设备重新注册
   * @param {string} deviceId - 设备ID
   */
  const handleReregister = async (deviceId) => {
    try {
      const result = await api.forceReregister(deviceId);
      if (result.success) {
        showMessage(`${deviceId} 注册请求已发送`, 'success');
      } else {
        showMessage(`注册失败: ${result.message}`, 'error');
      }
    } catch (err) {
      showMessage(`注册请求异常: ${err.message}`, 'error');
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
    const { deviceId, port, useTcp } = config;
    // 使用确定性streamId，便于停流时关闭对应RTP端口
    const rtpStreamId = `rtp_${deviceId}`;

    try {
      // 步骤1：先在ZKServer开启RTP收流端口
      const portLabel = port === 0 ? '自动分配' : port;
      showMessage(`正在开启RTP端口 ${portLabel}...`, 'info');
      const rtpResult = await zlmApi.openRtpServer(port, rtpStreamId, useTcp ? 1 : 0);

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
        if (result.alreadyStreaming) {
          // 设备已在推流，关闭多余的RTP端口
          await zlmApi.closeRtpServer(rtpStreamId);
          showMessage(`${deviceId} 已在推流中`, 'info');
        } else {
          showMessage(`${deviceId} 推流启动成功`, 'success');
        }
        setStreamConfigOpen(false);
        setSelectedDevice(null);
        refreshDevices(true);
      } else {
        // 推流失败时关闭RTP端口
        await zlmApi.closeRtpServer(rtpStreamId);
        showMessage(`推流失败: ${result.message}`, 'error');
      }
    } catch (err) {
      // 异常时也需要关闭已开启的RTP端口，避免泄漏
      await zlmApi.closeRtpServer(rtpStreamId).catch(() => { });
      showMessage(`推流异常: ${err.message}`, 'error');
    }
  };

  /**
   * 处理停止推流
   * @param {string} deviceId - 设备ID
   */
  const handleStopStream = async (deviceId) => {
    try {
      const result = await api.stopStream(deviceId);
      if (result.success) {
        // 同时关闭ZLM上对应的RTP收流端口，避免资源泄漏
        const rtpStreamId = `rtp_${deviceId}`;
        await zlmApi.closeRtpServer(rtpStreamId).catch(() => { });
        showMessage(`${deviceId} 推流已停止`, 'success');
        refreshDevices(true);
      } else {
        showMessage(`停止失败: ${result.message}`, 'error');
      }
    } catch (err) {
      showMessage(`停止推流异常: ${err.message}`, 'error');
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
    } catch (err) {
      showMessage(`批量注册异常: ${err.message}`, 'error');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 批量推流所有在线设备
   * 复用完整推流流程：开启RTP端口 → 发送INVITE
   */
  const handleBatchStream = async () => {
    const onlineDevices = devices.filter(d => d.online && !d.live);
    if (onlineDevices.length === 0) {
      showMessage('没有可推流的在线设备', 'info');
      return;
    }

    setLoading(true);
    let successCount = 0;
    const serverSettings = api.getSettings();
    const BATCH_SIZE = 5; // 每批并发处理5台设备，平衡速度和服务器压力

    /**
     * 单个设备的推流任务（开RTP端口 → 发INVITE）
     * @param {Object} device - 设备信息
     * @returns {boolean} 是否成功
     */
    const streamOneDevice = async (device) => {
      const rtpStreamId = `rtp_${device.deviceId}`;
      try {
        // 步骤1：开启RTP端口（port=0 让ZLM自动分配，避免端口冲突）
        const rtpResult = await zlmApi.openRtpServer(0, rtpStreamId, 0);
        if (!rtpResult.success) {
          console.warn(`设备 ${device.deviceId} 开启RTP端口失败:`, rtpResult.message);
          return false;
        }
        const actualPort = rtpResult.data?.port;
        if (!actualPort) {
          console.warn(`设备 ${device.deviceId} 未返回有效端口`);
          await zlmApi.closeRtpServer(rtpStreamId);
          return false;
        }

        // 步骤2：发送INVITE
        const result = await api.startStreamWithConfig(device.deviceId, {
          mediaServerIp: serverSettings.mediaServerIp,
          mediaServerPort: actualPort,
          useTcp: false
        });

        if (result.success) {
          if (result.alreadyStreaming) {
            await zlmApi.closeRtpServer(rtpStreamId);
          }
          return true;
        } else {
          await zlmApi.closeRtpServer(rtpStreamId);
          return false;
        }
      } catch (err) {
        await zlmApi.closeRtpServer(rtpStreamId).catch(() => { });
        console.warn(`设备 ${device.deviceId} 推流失败:`, err);
        return false;
      }
    };

    try {
      // 分批并发执行，每批 BATCH_SIZE 个设备同时推流
      for (let i = 0; i < onlineDevices.length; i += BATCH_SIZE) {
        const batch = onlineDevices.slice(i, i + BATCH_SIZE);
        const results = await Promise.allSettled(batch.map(streamOneDevice));
        for (const r of results) {
          if (r.status === 'fulfilled' && r.value) successCount++;
        }
      }
      showMessage(`批量推流完成: ${successCount}/${onlineDevices.length} 成功`, 'success');
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

      {/* 提示消息队列 */}
      {messages.length > 0 && (
        <div className="message-stack">
          {messages.map(msg => (
            <div key={msg.id} className={`message ${msg.type}`}>
              {msg.text}
            </div>
          ))}
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
