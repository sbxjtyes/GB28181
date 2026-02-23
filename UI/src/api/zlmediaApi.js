/**
 * ZLMediaKit 媒体服务器 API 封装模块
 * 提供与 ZLMediaKit 交互的 API 调用方法
 */

/**
 * 获取 ZLMediaKit 配置
 * @returns {Object} 配置对象
 */
const getZLMConfig = () => {
  const saved = localStorage.getItem('sipConfig');
  if (saved) {
    const config = JSON.parse(saved);
    return {
      baseUrl: config.zlmBaseUrl || 'http://localhost:88',
      secret: config.zlmSecret || ''
    };
  }
  return {
    baseUrl: 'http://localhost:88',
    secret: ''
  };
};

/**
 * 通用请求方法
 * @param {string} endpoint - API端点
 * @param {Object} params - 请求参数
 * @returns {Promise<Object>} 响应数据
 */
const request = async (endpoint, params = {}) => {
  const config = getZLMConfig();
  const queryParams = new URLSearchParams({
    secret: config.secret,
    ...params
  });
  const url = `${config.baseUrl}/index/api/${endpoint}?${queryParams}`;
  
  try {
    const response = await fetch(url);
    const data = await response.json();
    return {
      success: data.code === 0,
      data,
      message: data.msg || ''
    };
  } catch (error) {
    console.error('ZLMediaKit API请求失败:', error);
    return { success: false, message: error.message, data: null };
  }
};

/**
 * 获取服务器配置
 * @returns {Promise<Object>} 服务器配置
 */
export const getServerConfig = () => request('getServerConfig');

/**
 * 获取流列表
 * @param {string} schema - 协议类型 (rtsp/rtmp/hls 等，可选)
 * @returns {Promise<Object>} 流列表
 */
export const getMediaList = (schema = '') => {
  const params = schema ? { schema } : {};
  return request('getMediaList', params);
};

/**
 * 获取指定流的详细信息
 * @param {string} app - 应用名
 * @param {string} stream - 流名
 * @param {string} schema - 协议 (可选)
 * @returns {Promise<Object>} 流信息
 */
export const getMediaInfo = (app, stream, schema = '') => {
  const params = { app, stream };
  if (schema) params.schema = schema;
  return request('getMediaInfo', params);
};

/**
 * 关闭指定流
 * @param {string} schema - 协议类型 (如 rtsp/rtmp/hls 等)
 * @param {string} app - 应用名
 * @param {string} stream - 流名
 * @param {boolean} force - 是否强制关闭
 * @param {string} vhost - 虚拟主机名 (默认 __defaultVhost__)
 * @returns {Promise<Object>} 操作结果
 */
export const closeStream = (schema, app, stream, force = false, vhost = '__defaultVhost__') => {
  // 不传schema参数，让ZLM关闭该流的所有协议版本，避免去重后残留其他schema
  const params = { vhost, app, stream, force: force ? 1 : 0 };
  if (schema) params.schema = schema;
  return request('close_streams', params);
};

/**
 * 获取 RTP 服务器列表
 * @returns {Promise<Object>} RTP服务器列表
 */
export const listRtpServer = () => request('listRtpServer');

/**
 * 开启 RTP 收流端口
 * @param {number} port - 端口号 (0表示随机)
 * @param {string} streamId - 流ID
 * @param {number} tcpMode - TCP模式 (0:禁用, 1:启用, 2:同时启用)
 * @returns {Promise<Object>} 操作结果
 */
export const openRtpServer = (port = 0, streamId = '', tcpMode = 0) => {
  return request('openRtpServer', {
    port,
    stream_id: streamId || `stream_${Date.now()}`,
    tcp_mode: tcpMode
  });
};

/**
 * 关闭 RTP 收流端口
 * @param {string} streamId - 流ID
 * @returns {Promise<Object>} 操作结果
 */
export const closeRtpServer = (streamId) => {
  return request('closeRtpServer', { stream_id: streamId });
};

/**
 * 获取系统统计信息
 * @returns {Promise<Object>} 统计信息
 */
export const getStatistic = () => request('getStatistic');

/**
 * 测试 ZLMediaKit 服务器连接
 * @returns {Promise<boolean>} 是否连接成功
 */
export const testConnection = async () => {
  try {
    const result = await getServerConfig();
    return result.success;
  } catch {
    return false;
  }
};

/**
 * 生成播放地址
 * @param {Object} streamInfo - 流信息
 * @returns {Object} 各协议播放地址
 */
/**
 * 获取ZLMediaKit端口配置
 * @returns {Object} 端口配置
 */
const getPortConfig = () => {
  const saved = localStorage.getItem('sipConfig');
  if (saved) {
    const config = JSON.parse(saved);
    return {
      rtspPort: config.zlmRtspPort || 554,
      rtmpPort: config.zlmRtmpPort || 1935
    };
  }
  return { rtspPort: 554, rtmpPort: 1935 };
};

export const generatePlayUrls = (streamInfo) => {
  const config = getZLMConfig();
  const ports = getPortConfig();
  const baseUrl = config.baseUrl.replace(/^https?:\/\//, '');
  const host = baseUrl.split(':')[0];
  const { app, stream } = streamInfo;
  
  return {
    rtsp: `rtsp://${host}:${ports.rtspPort}/${app}/${stream}`,
    rtmp: `rtmp://${host}:${ports.rtmpPort}/${app}/${stream}`,
    flv: `http://${baseUrl}/${app}/${stream}.live.flv`,
    hls: `http://${baseUrl}/${app}/${stream}/hls.m3u8`,
    webrtc: `${config.baseUrl}/index/api/webrtc?app=${app}&stream=${stream}&type=play`
  };
};
