import './StreamCard.css';

/**
 * 流卡片组件
 * 显示单个媒体流的信息和操作按钮
 * 
 * @param {Object} props - 组件属性
 * @param {Object} props.stream - 流信息
 * @param {Object} props.playUrls - 播放地址
 * @param {Function} props.onClose - 关闭流回调
 * @param {Function} props.onCopyUrl - 复制地址回调
 */
function StreamCard({ stream, playUrls, onClose, onCopyUrl }) {
  const { app, stream: streamName, schema, vhost, originType, totalReaderCount } = stream;

  const fallbackCopyText = (text) => {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.setAttribute('readonly', '');
    textArea.style.position = 'fixed';
    textArea.style.top = '-9999px';
    textArea.style.left = '-9999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    let copied = false;
    try {
      copied = document.execCommand('copy');
    } finally {
      document.body.removeChild(textArea);
    }

    return copied;
  };

  /**
   * 获取流来源类型标签
   * @param {number} type - 来源类型
   * @returns {string} 类型标签
   */
  const getOriginLabel = (type) => {
    const origins = {
      0: '未知',
      1: 'RTMP推流',
      2: 'RTSP推流',
      3: 'RTP推流',
      4: '拉流代理',
      5: 'FFmpeg推流',
      6: 'MP4点播',
      7: 'TS点播'
    };
    return origins[type] || '未知';
  };

  /**
   * 复制地址到剪贴板
   * @param {string} url - 地址
   * @param {string} type - 协议类型
   */
  const handleCopy = async (url, type) => {
    if (!url) {
      onCopyUrl && onCopyUrl(type, false, '地址为空，无法复制');
      return;
    }

    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(url);
      } else {
        const copied = fallbackCopyText(url);
        if (!copied) {
          throw new Error('浏览器不支持复制或复制被拦截');
        }
      }
      onCopyUrl && onCopyUrl(type, true);
    } catch (err) {
      console.error('复制失败:', err);
      onCopyUrl && onCopyUrl(type, false, err.message || '复制失败');
    }
  };

  return (
    <div className="stream-card">
      <div className="stream-header">
        <div className="stream-name">
          <span className="stream-icon">🎬</span>
          <span className="stream-title">{app}/{streamName}</span>
        </div>
        <span className={`origin-badge origin-${originType}`}>
          {getOriginLabel(originType)}
        </span>
      </div>
      
      <div className="stream-stats">
        <div className="stat-item">
          <span className="stat-label">总观看</span>
          <span className="stat-value">{totalReaderCount || 0}</span>
        </div>
      </div>

      <div className="play-urls">
        <div className="url-item" title={playUrls.rtsp}>
          <span className="url-protocol">RTSP</span>
          <button className="copy-btn" onClick={() => handleCopy(playUrls.rtsp, 'RTSP')}>
            📋 复制
          </button>
        </div>
        <div className="url-item" title={playUrls.rtmp}>
          <span className="url-protocol">RTMP</span>
          <button className="copy-btn" onClick={() => handleCopy(playUrls.rtmp, 'RTMP')}>
            📋 复制
          </button>
        </div>
        <div className="url-item" title={playUrls.flv}>
          <span className="url-protocol">HTTP-FLV</span>
          <button className="copy-btn" onClick={() => handleCopy(playUrls.flv, 'FLV')}>
            📋 复制
          </button>
        </div>
        <div className="url-item" title={playUrls.hls}>
          <span className="url-protocol">HLS</span>
          <button className="copy-btn" onClick={() => handleCopy(playUrls.hls, 'HLS')}>
            📋 复制
          </button>
        </div>
      </div>

      <div className="stream-actions">
        <button 
          className="action-btn play-btn"
          onClick={() => handleCopy(playUrls.flv, 'HTTP-FLV')}
          title="复制HTTP-FLV播放地址，可用VLC或浏览器插件播放"
        >
          📋 复制播放地址
        </button>
        <button 
          className="action-btn close-btn"
          onClick={() => onClose(schema, app, streamName, vhost)}
        >
          ⏹️ 关闭
        </button>
      </div>
    </div>
  );
}

export default StreamCard;
