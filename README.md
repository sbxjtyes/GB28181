# GB28181 SIP 服务器

基于 GB28181-2016 协议标准的 SIP 信令服务器系统，支持摄像头设备注册、心跳保活、会话建立与推流管理。

---

## 📌 项目概述

本项目实现了一个完整的 GB28181 视频监控联网系统，包含三个核心模块：

| 模块 | 说明 |
|------|------|
| **SIPServer** | Java 后端 SIP 信令服务器，处理设备注册、心跳、INVITE 推流 |
| **UI** | React Web 管理界面，设备管理与推流控制 |
| **ZKServer** | ZLMediaKit 流媒体服务器配置（需自行下载二进制文件，详见[部署说明](#4-启动-zkserver流媒体服务器)） |

> ⚠️ **注意**: 本项目的流媒体服务器模块使用了 [ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit)（MIT License）。
> ZLMediaKit 的二进制文件（`MediaServer.exe`、`.dll` 等）**未包含在本仓库中**，请前往其 [Releases 页面](https://github.com/ZLMediaKit/ZLMediaKit/releases) 下载并放置到 `ZKServer/` 目录。

## 🏗️ 架构概览

```
摄像头(IPC) ←→ [SIP UDP:5060] SIPServer [HTTP:8080] ←→ UI
                                    ↕
                              ZKServer (ZLMediaKit)
                              [RTP:30000-35000]
                              [HTTP:88] [RTSP:554] [RTMP:1935]
```

**信令流程**：
1. 摄像头通过 SIP/UDP 向 SIPServer 发送 `REGISTER` 注册
2. SIPServer 进行设备认证（Digest Auth），存储设备信息到 MySQL
3. 定时接收设备 `MESSAGE` 心跳保活
4. 推流时 SIPServer 向设备发送 `INVITE`，设备向 ZKServer 推 RTP 流
5. 通过 ZKServer 的 HTTP/RTSP/RTMP 播放实时视频

## 🛠️ 技术栈

### SIPServer（后端）
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 2.7.18 | 应用框架 |
| Netty | 4.1.110.Final | SIP/UDP 网络通信 |
| MySQL | 8.0+ | 设备信息持久化 |
| Spring Data JPA | - | 数据访问层 |
| springdoc-openapi | 1.7.0 | API 文档（Swagger） |
| Micrometer + Prometheus | - | 监控指标 |

### UI（前端）
| 技术 | 版本 | 用途 |
|------|------|------|
| React | 19.2 | UI 框架 |
| Vite | 7.2 | 构建工具 |
| ESLint | 9.39 | 代码检查 |

### ZKServer（流媒体）
| 技术 | 说明 |
|------|------|
| [ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit) | 高性能流媒体服务器（第三方开源项目，MIT License） |
| 支持协议 | RTSP / RTMP / HLS / HTTP-FLV / WebRTC |

## 📁 目录结构

```
GB28181/
├── SIPServer/                  # SIP 信令服务器（Java）
│   ├── src/main/java/com/gb28181/sipserver/
│   │   ├── GB28181SipServerApplication.java  # 启动类
│   │   ├── common/             # 通用类（ApiResponse）
│   │   ├── config/             # 配置类（SIP、线程池、Web、Swagger）
│   │   ├── controller/         # REST API 控制器
│   │   │   ├── DeviceController.java   # 设备管理 API
│   │   │   └── StreamController.java   # 推流控制 API
│   │   ├── entity/             # JPA 实体类
│   │   ├── exception/          # 全局异常处理
│   │   ├── netty/              # Netty SIP 消息处理
│   │   │   ├── SipUdpServer.java       # UDP 服务器
│   │   │   └── SipUdpServerHandler.java # 消息处理器
│   │   ├── repository/         # 数据访问层
│   │   ├── service/            # 业务逻辑层
│   │   │   ├── DeviceService.java      # 设备管理
│   │   │   ├── StreamService.java      # 推流管理
│   │   │   ├── SipMessageParser.java   # SIP 消息解析
│   │   │   └── SipMessageTemplate.java # SIP 消息模板
│   │   └── util/               # 工具类
│   ├── src/main/resources/
│   │   └── application.yml     # 应用配置
│   ├── sql/                    # 数据库脚本
│   ├── pom.xml                 # Maven 配置
│   └── start.bat               # Windows 启动脚本
├── UI/                         # Web 管理界面（React）
│   ├── src/
│   │   ├── App.jsx             # 主应用组件
│   │   ├── api/                # API 调用层
│   │   └── components/         # UI 组件
│   ├── package.json
│   └── vite.config.js
├── ZKServer/                   # ZLMediaKit 流媒体服务器配置
│   └── config.ini              # 配置文件（二进制文件需自行下载）
├── docs/                       # 项目文档
│   ├── api.md                  # API 接口详细文档
│   ├── architecture.md         # 架构设计文档
│   └── deploy.md               # 部署指南
├── LICENSE                     # MIT 许可证
├── CHANGELOG.md                # 版本变更记录
└── README.md                   # 本文件
```

## 🚀 快速启动

### 前置要求

- **Java** 17+
- **Maven** 3.6+
- **MySQL** 8.0+
- **Node.js** 18+（如需运行前端）
- **ZLMediaKit** 二进制文件（[下载地址](https://github.com/ZLMediaKit/ZLMediaKit/releases)）

### 1. 初始化数据库

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS gb28181_sip_server
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

```bash
# 执行建表脚本
mysql -u root -p gb28181_sip_server < SIPServer/sql/create_tables.sql
```

### 2. 配置应用

编辑 `SIPServer/src/main/resources/application.yml`，或通过环境变量配置：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:31306/gb28181_sip_server?...` | 数据库连接 URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `changeme` | 数据库密码 |
| `GB28181_SERVER_ID` | `34020000002000000001` | SIP 服务器 ID（20位） |
| `GB28181_SERVER_IP` | `0.0.0.0` | SIP 服务器监听 IP |
| `GB28181_SERVER_PORT` | `5060` | SIP 服务器监听端口 |
| `GB28181_SIP_IP` | (自动检测) | SIP 消息中使用的 IP |
| `GB28181_SIP_DOMAIN` | `3402000000` | SIP 域 |
| `GB28181_DEVICE_PASSWORD` | `changeme` | 设备认证密码 |
| `GB28181_HEARTBEAT_TIMEOUT` | `60` | 心跳超时时间（秒） |
| `GB28181_REGISTER_EXPIRES` | `3600` | 注册有效期（秒） |
| `GB28181_SIP_CHARSET` | `UTF-8` | SIP 协议字符集 |

### 3. 启动 SIPServer

```bash
# 方式一：使用启动脚本（Windows）
cd SIPServer
start.bat

# 方式二：使用 Maven
cd SIPServer
mvn spring-boot:run -Dfile.encoding=UTF-8
```

### 4. 启动 ZKServer（流媒体服务器）

> 📥 首次使用需从 [ZLMediaKit Releases](https://github.com/ZLMediaKit/ZLMediaKit/releases) 下载预编译文件，将 `MediaServer.exe` 及相关 `.dll` 文件放入 `ZKServer/` 目录。

```bash
cd ZKServer
MediaServer.exe
```

### 5. 启动 UI（开发模式）

```bash
cd UI
npm install
npm run dev
```

### 6. 访问服务

| 服务 | 地址 |
|------|------|
| Web 管理界面 | http://localhost:5173 |
| SIPServer REST API | http://localhost:8080/gb28181 |
| Swagger API 文档 | http://localhost:8080/gb28181/swagger-ui/index.html |
| 监控端点 | http://localhost:8080/gb28181/actuator |
| ZLMediaKit 控制台 | http://localhost:88 |

## 📡 API 概要

### 设备管理 `/api/devices`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/devices` | 获取所有设备列表 |
| GET | `/api/devices/page?page=0&size=50&online=true` | 分页获取设备列表 |
| GET | `/api/devices/online` | 获取在线设备列表 |
| GET | `/api/devices/{deviceId}` | 获取设备详情 |
| GET | `/api/devices/statistics` | 获取设备统计信息 |
| DELETE | `/api/devices/{deviceId}` | 删除设备 |
| POST | `/api/devices/{deviceId}/offline` | 强制设备下线 |
| POST | `/api/devices/{deviceId}/force-reregister` | 强制设备重新注册 |
| POST | `/api/devices/batch/force-reregister` | 批量强制重新注册 |
| POST | `/api/devices/all/force-reregister` | 全部设备重新注册 |

### 推流控制 `/api/stream`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/stream/start` | 开始推流 |
| POST | `/api/stream/stop` | 停止推流 |
| GET | `/api/stream/status/{deviceId}` | 获取推流状态 |

> 📖 完整 API 文档请参阅 [docs/api.md](docs/api.md) 或访问 Swagger UI。

## 📋 更多文档

- [API 接口详细文档](docs/api.md)
- [架构设计文档](docs/architecture.md)
- [部署指南](docs/deploy.md)
- [数据库说明](SIPServer/sql/README.md)
- [版本变更记录](CHANGELOG.md)

## 🙏 致谢

本项目使用了以下开源项目：

| 项目 | 说明 | 许可证 |
|------|------|--------|
| [ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit) | 高性能流媒体服务器，用于 RTP 接收与协议转换 | MIT |
| [Spring Boot](https://spring.io/projects/spring-boot) | Java 应用框架 | Apache 2.0 |
| [Netty](https://netty.io/) | 异步网络通信框架 | Apache 2.0 |
| [React](https://react.dev/) | 前端 UI 框架 | MIT |
| [Vite](https://vitejs.dev/) | 前端构建工具 | MIT |

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

> **注意**: 本仓库不包含 ZLMediaKit 的二进制文件。ZLMediaKit 是独立的开源项目，拥有自己的 [MIT 许可证](https://github.com/ZLMediaKit/ZLMediaKit/blob/master/LICENSE)。
