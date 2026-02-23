/**
 * GB28181 SIP服务器API封装模块
 * 提供设备管理和推流控制的API调用方法
 */

// 从localStorage获取配置，提供默认值
const getConfig = () => {
  const saved = localStorage.getItem('sipConfig');
  if (saved) {
    return JSON.parse(saved);
  }
  return {
    baseUrl: 'http://localhost:8080/gb28181',
    mediaServerIp: '127.0.0.1',
    mediaServerPort: 30000,
    zlmBaseUrl: 'http://localhost:88',
    zlmSecret: ''
  };
};

/**
 * 保存配置到localStorage
 * @param {Object} config - 配置对象
 */
export const saveConfig = (config) => {
  localStorage.setItem('sipConfig', JSON.stringify(config));
};

/**
 * 获取当前配置
 * @returns {Object} 配置对象
 */
export const getSettings = () => getConfig();

/**
 * 通用请求方法
 * @param {string} endpoint - API端点
 * @param {Object} options - fetch选项
 * @returns {Promise<Object>} 响应数据
 */
const request = async (endpoint, options = {}) => {
  const config = getConfig();
  const url = `${config.baseUrl}${endpoint}`;
  
  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      ...options
    });
    
    const data = await response.json();
    return data;
  } catch (error) {
    console.error('API请求失败:', error);
    return { success: false, message: error.message };
  }
};

/**
 * 获取所有设备列表
 * @returns {Promise<Object>} 设备列表响应
 */
export const getDevices = () => request('/api/devices');

/**
 * 强制单个设备重新注册
 * @param {string} deviceId - 设备ID
 * @returns {Promise<Object>} 操作结果
 */
export const forceReregister = (deviceId) => 
  request(`/api/devices/${deviceId}/force-reregister`, { method: 'POST' });

/**
 * 批量强制设备重新注册
 * @param {string[]} deviceIds - 设备ID列表
 * @returns {Promise<Object>} 操作结果
 */
export const batchForceReregister = (deviceIds) => 
  request('/api/devices/batch/force-reregister', {
    method: 'POST',
    body: JSON.stringify({ deviceIds })
  });

/**
 * 使用自定义配置开始推流
 * @param {string} deviceId - 设备ID
 * @param {Object} streamConfig - 推流配置
 * @param {string} streamConfig.mediaServerIp - 媒体服务器IP
 * @param {number} streamConfig.mediaServerPort - 媒体服务器端口
 * @param {boolean} streamConfig.useTcp - 是否使用TCP
 * @returns {Promise<Object>} 操作结果
 */
export const startStreamWithConfig = (deviceId, streamConfig) => {
  return request('/api/stream/start', {
    method: 'POST',
    body: JSON.stringify({
      deviceId,
      mediaServerIp: streamConfig.mediaServerIp,
      mediaServerPort: streamConfig.mediaServerPort,
      useTcp: streamConfig.useTcp || false
    })
  });
};

/**
 * 停止推流
 * @param {string} deviceId - 设备ID
 * @returns {Promise<Object>} 操作结果
 */
export const stopStream = (deviceId) => 
  request('/api/stream/stop', {
    method: 'POST',
    body: JSON.stringify({ deviceId })
  });

/**
 * 测试服务器连接
 * @returns {Promise<boolean>} 是否连接成功
 */
export const testConnection = async () => {
  try {
    const result = await getDevices();
    return result.success === true;
  } catch {
    return false;
  }
};
