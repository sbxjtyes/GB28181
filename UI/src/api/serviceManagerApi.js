/**
 * 服务管理 API 模块
 * 用于与本地服务管理器通信，控制 SIPServer 和 ZKServer
 */

// 服务管理器地址配置
const STORAGE_KEY = 'gb28181_manager_url';
let managerUrl = localStorage.getItem(STORAGE_KEY) || 'http://localhost:3001';

/**
 * 获取服务管理器地址
 * @returns {string} 当前地址
 */
export const getManagerUrl = () => managerUrl;

/**
 * 设置服务管理器地址
 * @param {string} url - 新地址
 */
export const setManagerUrl = (url) => {
  // 确保URL格式正确（去掉末尾斜杠）
  managerUrl = url.replace(/\/$/, '');
  localStorage.setItem(STORAGE_KEY, managerUrl);
};

/**
 * 发送请求到服务管理器
 * @param {string} endpoint - API 端点
 * @param {string} method - HTTP 方法
 * @returns {Promise<Object>} 响应数据
 */
const request = async (endpoint, method = 'GET') => {
  try {
    const response = await fetch(`${managerUrl}${endpoint}`, { method });
    const data = await response.json();
    return data;
  } catch (error) {
    console.error('服务管理器请求失败:', error);
    return { 
      success: false, 
      message: `无法连接服务管理器 (${managerUrl})，请确保地址正确且服务正在运行`,
      error: error.message 
    };
  }
};

/**
 * 获取所有服务状态
 * @returns {Promise<Object>} 服务状态
 */
export const getServicesStatus = () => request('/api/status');

/**
 * 启动 SIPServer
 * @returns {Promise<Object>} 操作结果
 */
export const startSipServer = () => request('/api/sip/start', 'POST');

/**
 * 停止 SIPServer
 * @returns {Promise<Object>} 操作结果
 */
export const stopSipServer = () => request('/api/sip/stop', 'POST');

/**
 * 启动 ZKServer (ZLMediaKit)
 * @returns {Promise<Object>} 操作结果
 */
export const startZkServer = () => request('/api/zk/start', 'POST');

/**
 * 停止 ZKServer
 * @returns {Promise<Object>} 操作结果
 */
export const stopZkServer = () => request('/api/zk/stop', 'POST');

/**
 * 启动所有服务
 * @returns {Promise<Object>} 操作结果
 */
export const startAllServices = () => request('/api/all/start', 'POST');

/**
 * 停止所有服务
 * @returns {Promise<Object>} 操作结果
 */
export const stopAllServices = () => request('/api/all/stop', 'POST');

/**
 * 检查服务管理器是否可用
 * @returns {Promise<boolean>} 是否可用
 */
export const checkManagerAvailable = async () => {
  try {
    const result = await getServicesStatus();
    return result.success === true;
  } catch {
    return false;
  }
};
