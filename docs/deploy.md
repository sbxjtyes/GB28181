# 部署指南

## 1. 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Java (JDK) | 17 | 17 LTS |
| Maven | 3.6 | 3.9+ |
| MySQL | 8.0 | 8.0+ |
| Node.js | 18 | 20 LTS |
| 操作系统 | Windows 10 / Linux | Windows Server / Ubuntu 22.04 |

## 2. 端口清单

| 端口 | 协议 | 服务 | 说明 |
|------|------|------|------|
| 5060 | UDP | SIPServer | SIP 信令端口 |
| 8080 | TCP | SIPServer | HTTP REST API |
| 88 | TCP | ZKServer | HTTP API & Web |
| 443 | TCP | ZKServer | HTTPS |
| 554 | TCP | ZKServer | RTSP |
| 1935 | TCP | ZKServer | RTMP |
| 8000 | TCP/UDP | ZKServer | WebRTC |
| 9000 | TCP | ZKServer | Shell / SRT |
| 10000 | UDP | ZKServer | RTP 代理端口 |
| 30000-35000 | UDP | ZKServer | RTP 接收端口范围 |
| 5173 | TCP | UI (dev) | Vite 开发服务器 |

> ⚠️ 请确保防火墙放行以上端口。

## 3. 环境变量清单

### SIPServer 环境变量

| 变量名 | 默认值 | 必填 | 说明 |
|--------|--------|------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:31306/gb28181_sip_server?...` | 否 | MySQL 连接 URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | 否 | MySQL 用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `changeme` | **是** | MySQL 密码（生产环境必须修改） |
| `GB28181_SERVER_ID` | `34020000002000000001` | 否 | SIP 服务器 ID（20位） |
| `GB28181_SERVER_IP` | `0.0.0.0` | 否 | SIP 监听 IP |
| `GB28181_SERVER_PORT` | `5060` | 否 | SIP 监听端口 |
| `GB28181_SIP_IP` | (自动检测) | 生产推荐 | SIP 消息中的 IP 地址 |
| `GB28181_SIP_DOMAIN` | `3402000000` | 否 | SIP 域 |
| `GB28181_DEVICE_PASSWORD` | `changeme` | **是** | 设备认证密码（生产环境必须修改） |
| `GB28181_HEARTBEAT_TIMEOUT` | `60` | 否 | 心跳超时时间（秒） |
| `GB28181_REGISTER_EXPIRES` | `3600` | 否 | 注册有效期（秒） |
| `GB28181_SIP_CHARSET` | `UTF-8` | 否 | 字符集（老旧设备改为 `GBK`） |

> ⚠️ **安全提醒**: `SPRING_DATASOURCE_PASSWORD` 和 `GB28181_DEVICE_PASSWORD` 在生产环境中必须修改为强密码，绝对不能使用默认值。

## 4. 部署步骤

### 4.1 数据库部署

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 创建数据库
CREATE DATABASE IF NOT EXISTS gb28181_sip_server
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

# 3. 创建专用用户（推荐）
CREATE USER 'gb28181'@'%' IDENTIFIED BY '<strong_password>';
GRANT ALL PRIVILEGES ON gb28181_sip_server.* TO 'gb28181'@'%';
FLUSH PRIVILEGES;

# 4. 执行建表脚本
USE gb28181_sip_server;
SOURCE SIPServer/sql/create_tables.sql;

# 5. 验证
DESCRIBE device_info;
SHOW INDEX FROM device_info;
```

### 4.2 SIPServer 部署

```bash
# 1. 编译打包
cd SIPServer
mvn clean package -DskipTests -Dfile.encoding=UTF-8

# 2. 运行 JAR
java -Dfile.encoding=UTF-8 \
     -Dsun.jnu.encoding=UTF-8 \
     -Xms512m -Xmx1024m \
     -XX:+UseG1GC \
     -jar target/gb28181-sip-server-1.0.0.jar

# 或使用 Windows 启动脚本
start.bat
```

### 4.3 ZKServer 部署

```bash
# 1. 编辑配置文件
# 修改 ZKServer/config.ini 中的关键配置:
#   [api] secret       → 修改 API 密钥
#   [rtp_proxy] port_range → RTP 端口范围

# 2. 启动
cd ZKServer
MediaServer.exe     # Windows
./MediaServer &     # Linux
```

#### ZKServer 关键配置项（config.ini）

| 配置节 | 参数 | 默认值 | 说明 |
|--------|------|--------|------|
| `[general]` | `mergeWriteMS` | `300` | 帧合并写入间隔（毫秒）。设为 `0` 会导致多客户端拉流时 I/O 竞争，仅一台能正常播放。推荐 `300` |
| `[rtsp]` | `directProxy` | `0` | RTSP 直接代理模式。设为 `1` 会绕过帧分发管线，导致多客户端无法同时播放。必须设为 `0` |
| `[rtmp]` | `directProxy` | `0` | RTMP 直接代理模式。同上，必须设为 `0` |
| `[http]` | `sendBufSize` | `262144` | HTTP 发送缓冲区大小（字节）。过小会导致多客户端 HTTP-FLV/HLS 播放阻塞。推荐 `262144`（256KB） |
| `[rtp_proxy]` | `port_range` | `30000-35000` | RTP 接收端口范围，需在防火墙中开放 |
| `[rtp_proxy]` | `gop_cache` | `1` | GOP 缓存，确保新客户端快速出画面 |
| `[api]` | `secret` | — | API 密钥，生产环境必须修改 |

### 4.4 UI 部署

#### 开发模式
```bash
cd UI
npm install
npm run dev
```

#### 生产构建
```bash
cd UI
npm install
npm run build
# 产出在 UI/dist/ 目录，部署到静态文件服务器（Nginx 等）
```

## 5. 健康检查

部署完成后，验证各服务是否正常运行：

```bash
# SIPServer 健康检查
curl http://localhost:8080/gb28181/actuator/health

# SIPServer API 测试
curl http://localhost:8080/gb28181/api/devices

# ZKServer 健康检查
curl http://localhost:88/index/api/getServerConfig
```

## 6. 日志位置

| 服务 | 日志路径 | 说明 |
|------|---------|------|
| SIPServer | `SIPServer/logs/gb28181-sip-server.log` | 应用日志 |
| SIPServer | `SIPServer/logs/gc.log` | GC 日志 |
| ZKServer | `ZKServer/log/` | 流媒体服务器日志 |

## 7. 常见问题

### 7.1 中文乱码
- 使用 `start.bat` 启动，其中包含完整的 UTF-8 编码配置
- 确保 MySQL 数据库使用 `utf8mb4` 字符集
- 如摄像头设备名显示乱码，尝试将 `GB28181_SIP_CHARSET` 设为 `GBK`

### 7.2 SIP 信令不通
- 检查 UDP 5060 端口是否被防火墙阻止
- 确认摄像头配置的 SIP 服务器 IP 和端口正确
- 确认 `GB28181_SIP_IP` 在多网卡环境中配置正确

### 7.3 推流失败
- 确认 ZKServer 已启动且 RTP 端口范围已开放
- 检查摄像头与 ZKServer 之间的网络连通性
- 查看 SIPServer 日志，确认 INVITE 消息已正确发送

### 7.4 多个播放器同时拉流时只有一台能正常播放
- 检查 `config.ini` 中 `mergeWriteMS` 是否为 `0`，必须设为 `300`
- 检查 `[rtsp]` 和 `[rtmp]` 下的 `directProxy` 是否为 `1`，必须设为 `0`
- 检查 `[http]` 下 `sendBufSize` 是否过小，推荐 `262144`
- 修改配置后需重启 `MediaServer.exe` 生效
