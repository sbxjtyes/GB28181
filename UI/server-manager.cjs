/**
 * GB28181 服务管理器
 * 用于启动/停止 SIPServer 和 ZKServer (ZLMediaKit)
 * 
 * 使用方法：node server-manager.cjs
 */

const { spawn, exec } = require('child_process');
const http = require('http');
const path = require('path');

// 服务配置
const CONFIG = {
  // 管理服务器端口
  PORT: 3001,
  
  // SIPServer 配置
  SIP_SERVER: {
    name: 'SIPServer',
    cwd: path.join(__dirname, '..', 'SIPServer'),
    command: 'mvn',
    args: ['spring-boot:run'],
    checkUrl: 'http://localhost:8080/gb28181/api/devices',
    checkInterval: 3000
  },
  
  // ZLMediaKit (ZKServer) 配置
  ZK_SERVER: {
    name: 'ZKServer',
    cwd: path.join(__dirname, '..', 'ZKServer'),
    command: 'MediaServer.exe',
    args: ['-c', 'config.ini'],
    checkUrl: 'http://localhost:88/index/api/getServerConfig?secret=FpvUDVyWlqohv98EEE5Of3UcFhc18Ipt',
    checkInterval: 2000
  }
};

// 服务状态
const services = {
  sipServer: { process: null, status: 'stopped', pid: null, logs: [] },
  zkServer: { process: null, status: 'stopped', pid: null, logs: [] }
};

// 最大日志条数
const MAX_LOGS = 100;

/**
 * 添加日志
 * @param {string} serviceName - 服务名称
 * @param {string} message - 日志消息
 */
function addLog(serviceName, message) {
  const timestamp = new Date().toLocaleTimeString('zh-CN');
  const logEntry = `[${timestamp}] ${message}`;
  
  if (services[serviceName]) {
    services[serviceName].logs.push(logEntry);
    if (services[serviceName].logs.length > MAX_LOGS) {
      services[serviceName].logs.shift();
    }
  }
  
  console.log(`[${serviceName}] ${logEntry}`);
}

/**
 * 检查服务是否在线
 * @param {string} url - 检查URL
 * @returns {Promise<boolean>}
 */
function checkServiceHealth(url) {
  return new Promise((resolve) => {
    const timeout = setTimeout(() => resolve(false), 5000);
    
    http.get(url, (res) => {
      clearTimeout(timeout);
      resolve(res.statusCode === 200);
    }).on('error', () => {
      clearTimeout(timeout);
      resolve(false);
    });
  });
}

/**
 * 启动 SIPServer
 */
async function startSipServer() {
  if (services.sipServer.status === 'running') {
    return { success: false, message: 'SIPServer 已在运行中' };
  }
  
  const config = CONFIG.SIP_SERVER;
  addLog('sipServer', `正在启动 ${config.name}...`);
  services.sipServer.status = 'starting';
  
  try {
    const proc = spawn(config.command, config.args, {
      cwd: config.cwd,
      shell: true,
      stdio: ['ignore', 'pipe', 'pipe']
    });
    
    services.sipServer.process = proc;
    services.sipServer.pid = proc.pid;
    
    proc.stdout.on('data', (data) => {
      addLog('sipServer', data.toString().trim());
    });
    
    proc.stderr.on('data', (data) => {
      addLog('sipServer', `[ERROR] ${data.toString().trim()}`);
    });
    
    proc.on('close', (code) => {
      addLog('sipServer', `进程退出，代码: ${code}`);
      services.sipServer.status = 'stopped';
      services.sipServer.process = null;
      services.sipServer.pid = null;
    });
    
    proc.on('error', (err) => {
      addLog('sipServer', `启动失败: ${err.message}`);
      services.sipServer.status = 'error';
    });
    
    // 等待服务启动
    await new Promise(resolve => setTimeout(resolve, 8000));
    
    // 检查服务是否成功启动
    const isRunning = await checkServiceHealth(config.checkUrl);
    if (isRunning) {
      services.sipServer.status = 'running';
      addLog('sipServer', '启动成功！');
      return { success: true, message: 'SIPServer 启动成功' };
    } else {
      services.sipServer.status = 'starting';
      addLog('sipServer', '正在启动中，请稍候...');
      return { success: true, message: 'SIPServer 正在启动中' };
    }
  } catch (err) {
    addLog('sipServer', `启动异常: ${err.message}`);
    services.sipServer.status = 'error';
    return { success: false, message: err.message };
  }
}

/**
 * 停止 SIPServer
 */
async function stopSipServer() {
  if (!services.sipServer.process) {
    // 尝试通过端口杀死进程
    return new Promise((resolve) => {
      exec('netstat -ano | findstr :8080', (err, stdout) => {
        if (stdout) {
          const lines = stdout.trim().split('\n');
          const pids = new Set();
          lines.forEach(line => {
            const parts = line.trim().split(/\s+/);
            if (parts.length >= 5) {
              pids.add(parts[4]);
            }
          });
          pids.forEach(pid => {
            exec(`taskkill /F /PID ${pid}`, () => {});
          });
          addLog('sipServer', '已停止');
          services.sipServer.status = 'stopped';
          resolve({ success: true, message: 'SIPServer 已停止' });
        } else {
          resolve({ success: false, message: 'SIPServer 未在运行' });
        }
      });
    });
  }
  
  addLog('sipServer', '正在停止...');
  
  // Windows 下杀死进程树
  exec(`taskkill /F /T /PID ${services.sipServer.pid}`, (err) => {
    if (err) {
      addLog('sipServer', `停止失败: ${err.message}`);
    }
  });
  
  services.sipServer.process = null;
  services.sipServer.pid = null;
  services.sipServer.status = 'stopped';
  addLog('sipServer', '已停止');
  
  return { success: true, message: 'SIPServer 已停止' };
}

/**
 * 启动 ZKServer (ZLMediaKit)
 */
async function startZkServer() {
  if (services.zkServer.status === 'running') {
    return { success: false, message: 'ZKServer 已在运行中' };
  }
  
  const config = CONFIG.ZK_SERVER;
  addLog('zkServer', `正在启动 ${config.name}...`);
  services.zkServer.status = 'starting';
  
  try {
    const proc = spawn(config.command, config.args, {
      cwd: config.cwd,
      shell: true,
      stdio: ['ignore', 'pipe', 'pipe'],
      detached: false
    });
    
    services.zkServer.process = proc;
    services.zkServer.pid = proc.pid;
    
    proc.stdout.on('data', (data) => {
      addLog('zkServer', data.toString().trim());
    });
    
    proc.stderr.on('data', (data) => {
      addLog('zkServer', `[ERROR] ${data.toString().trim()}`);
    });
    
    proc.on('close', (code) => {
      addLog('zkServer', `进程退出，代码: ${code}`);
      services.zkServer.status = 'stopped';
      services.zkServer.process = null;
      services.zkServer.pid = null;
    });
    
    proc.on('error', (err) => {
      addLog('zkServer', `启动失败: ${err.message}`);
      services.zkServer.status = 'error';
    });
    
    // 等待服务启动
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // 检查服务是否成功启动
    const isRunning = await checkServiceHealth(config.checkUrl);
    if (isRunning) {
      services.zkServer.status = 'running';
      addLog('zkServer', '启动成功！');
      return { success: true, message: 'ZKServer 启动成功' };
    } else {
      addLog('zkServer', '启动可能失败，请检查');
      return { success: false, message: 'ZKServer 启动超时' };
    }
  } catch (err) {
    addLog('zkServer', `启动异常: ${err.message}`);
    services.zkServer.status = 'error';
    return { success: false, message: err.message };
  }
}

/**
 * 停止 ZKServer
 */
async function stopZkServer() {
  if (!services.zkServer.process) {
    // 尝试通过进程名杀死
    return new Promise((resolve) => {
      exec('taskkill /F /IM MediaServer.exe', (err) => {
        if (!err) {
          addLog('zkServer', '已停止');
          services.zkServer.status = 'stopped';
          resolve({ success: true, message: 'ZKServer 已停止' });
        } else {
          resolve({ success: false, message: 'ZKServer 未在运行' });
        }
      });
    });
  }
  
  addLog('zkServer', '正在停止...');
  
  exec(`taskkill /F /T /PID ${services.zkServer.pid}`, (err) => {
    if (err) {
      addLog('zkServer', `停止失败: ${err.message}`);
    }
  });
  
  services.zkServer.process = null;
  services.zkServer.pid = null;
  services.zkServer.status = 'stopped';
  addLog('zkServer', '已停止');
  
  return { success: true, message: 'ZKServer 已停止' };
}

/**
 * 获取服务状态
 */
async function getStatus() {
  // 检查实际运行状态
  const sipHealth = await checkServiceHealth(CONFIG.SIP_SERVER.checkUrl);
  const zkHealth = await checkServiceHealth(CONFIG.ZK_SERVER.checkUrl);
  
  // 更新状态
  if (sipHealth && services.sipServer.status !== 'running') {
    services.sipServer.status = 'running';
  } else if (!sipHealth && services.sipServer.status === 'running') {
    services.sipServer.status = 'stopped';
  }
  
  if (zkHealth && services.zkServer.status !== 'running') {
    services.zkServer.status = 'running';
  } else if (!zkHealth && services.zkServer.status === 'running') {
    services.zkServer.status = 'stopped';
  }
  
  return {
    sipServer: {
      status: services.sipServer.status,
      pid: services.sipServer.pid,
      online: sipHealth,
      logs: services.sipServer.logs.slice(-20)
    },
    zkServer: {
      status: services.zkServer.status,
      pid: services.zkServer.pid,
      online: zkHealth,
      logs: services.zkServer.logs.slice(-20)
    }
  };
}

/**
 * 处理 HTTP 请求
 */
async function handleRequest(req, res) {
  // 设置 CORS 头
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  
  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }
  
  const url = req.url;
  let result = { success: false, message: '未知请求' };
  
  try {
    switch (url) {
      case '/api/status':
        result = await getStatus();
        result.success = true;
        break;
        
      case '/api/sip/start':
        result = await startSipServer();
        break;
        
      case '/api/sip/stop':
        result = await stopSipServer();
        break;
        
      case '/api/zk/start':
        result = await startZkServer();
        break;
        
      case '/api/zk/stop':
        result = await stopZkServer();
        break;
        
      case '/api/all/start':
        const zkResult = await startZkServer();
        const sipResult = await startSipServer();
        result = {
          success: zkResult.success && sipResult.success,
          sipServer: sipResult,
          zkServer: zkResult
        };
        break;
        
      case '/api/all/stop':
        const sipStopResult = await stopSipServer();
        const zkStopResult = await stopZkServer();
        result = {
          success: sipStopResult.success && zkStopResult.success,
          sipServer: sipStopResult,
          zkServer: zkStopResult
        };
        break;
        
      default:
        result = { success: false, message: `未知路径: ${url}` };
    }
  } catch (err) {
    result = { success: false, message: err.message };
  }
  
  res.writeHead(200);
  res.end(JSON.stringify(result, null, 2));
}

// 创建 HTTP 服务器
const server = http.createServer(handleRequest);

server.listen(CONFIG.PORT, () => {
  console.log('='.repeat(50));
  console.log('  GB28181 服务管理器');
  console.log('='.repeat(50));
  console.log(`  管理API: http://localhost:${CONFIG.PORT}`);
  console.log('');
  console.log('  可用接口:');
  console.log('    GET  /api/status     - 获取服务状态');
  console.log('    POST /api/sip/start  - 启动 SIPServer');
  console.log('    POST /api/sip/stop   - 停止 SIPServer');
  console.log('    POST /api/zk/start   - 启动 ZKServer');
  console.log('    POST /api/zk/stop    - 停止 ZKServer');
  console.log('    POST /api/all/start  - 启动所有服务');
  console.log('    POST /api/all/stop   - 停止所有服务');
  console.log('='.repeat(50));
});

// 优雅退出
process.on('SIGINT', async () => {
  console.log('\n正在停止所有服务...');
  await stopSipServer();
  await stopZkServer();
  process.exit(0);
});
