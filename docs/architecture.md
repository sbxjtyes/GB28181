# 架构设计文档

## 1. 系统架构总览

GB28181 SIP 服务器系统采用 **三模块分离架构**：

```
┌─────────────────────────────────────────────────────────────────┐
│                       浏览器 / Web 界面                          │
│                      (React + Vite)                             │
└───────────────┬─────────────────────────────┬───────────────────┘
                │ HTTP REST API               │ HTTP API
                ▼                             ▼
┌───────────────────────────┐   ┌─────────────────────────────────┐
│      SIPServer            │   │        ZKServer                 │
│   (Spring Boot + Netty)   │   │      (ZLMediaKit)               │
│                           │   │                                 │
│  ┌─────────────────────┐  │   │  ┌───────────────────────────┐  │
│  │ HTTP API (8080)      │  │   │  │ HTTP API (88)             │  │
│  │ - DeviceController   │  │   │  │ - RTP端口管理              │  │
│  │ - StreamController   │  │   │  │ - 流状态查询               │  │
│  └─────────────────────┘  │   │  └───────────────────────────┘  │
│                           │   │                                 │
│  ┌─────────────────────┐  │   │  ┌───────────────────────────┐  │
│  │ SIP/UDP (5060)       │  │   │  │ Media Ports               │  │
│  │ - Netty SipUdpServer │◄─┼───┼─►│ - RTP (30000-35000)       │  │
│  │ - REGISTER/MESSAGE   │  │   │  │ - RTSP (554)              │  │
│  │ - INVITE/BYE/ACK     │  │   │  │ - RTMP (1935)             │  │
│  └─────────────────────┘  │   │  └───────────────────────────┘  │
│                           │   │                                 │
│  ┌─────────────────────┐  │   └─────────────────────────────────┘
│  │ MySQL Database       │  │
│  │ - device_info 表     │  │
│  └─────────────────────┘  │
└──────────┬────────────────┘
           │ SIP/UDP
           ▼
┌─────────────────────────┐
│   摄像头 / IPC 设备       │
│   (GB28181 协议)          │
└─────────────────────────┘
```

## 2. SIPServer 分层架构

```
┌────────────────────────────────────────────────────────────────┐
│  Presentation Layer（展示层）                                    │
│  ├── DeviceController.java      REST API - 设备管理             │
│  └── StreamController.java      REST API - 推流控制             │
├────────────────────────────────────────────────────────────────┤
│  Business Layer（业务层）                                        │
│  ├── DeviceService.java         设备管理逻辑、超时清理            │
│  ├── StreamService.java         推流/断流 SIP 消息构建           │
│  ├── SipMessageParser.java      SIP 消息解析                    │
│  ├── SipMessageTemplate.java    SIP 消息模板生成                 │
│  └── SystemMonitorService.java  系统监控                        │
├────────────────────────────────────────────────────────────────┤
│  Data Access Layer（数据层）                                     │
│  ├── DeviceRepository.java      JPA 数据访问                    │
│  └── DeviceInfo.java            JPA 实体                       │
├────────────────────────────────────────────────────────────────┤
│  Network Layer（网络层）                                         │
│  ├── SipUdpServer.java          Netty UDP 服务器启停             │
│  └── SipUdpServerHandler.java   SIP 消息路由与处理               │
├────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer（基础设施层）                               │
│  ├── SipServerConfig.java       SIP 配置属性                    │
│  ├── ThreadPoolConfig.java      业务线程池配置                    │
│  ├── WebConfig.java             CORS / 编码配置                  │
│  ├── OpenApiConfig.java         Swagger 配置                    │
│  ├── MonitoringConfiguration.java  Prometheus 监控               │
│  └── GlobalExceptionHandler.java   全局异常处理                   │
└────────────────────────────────────────────────────────────────┘
```

## 3. 核心信令流程

### 3.1 设备注册流程

```
摄像头(IPC)                    SIPServer
    │                             │
    │──── REGISTER ──────────────►│  1. 收到注册请求
    │                             │  2. 检查是否需要认证
    │◄─── 401 Unauthorized ──────│  3. 要求 Digest 认证
    │                             │
    │──── REGISTER (with auth) ──►│  4. 验证认证信息
    │                             │  5. 保存设备信息到 MySQL
    │◄─── 200 OK ────────────────│  6. 注册成功
    │                             │
```

### 3.2 心跳保活流程

```
摄像头(IPC)                    SIPServer
    │                             │
    │──── MESSAGE (Keepalive) ───►│  1. 解析心跳 XML
    │                             │  2. 更新设备心跳时间
    │◄─── 200 OK ────────────────│  3. 确认收到
    │                             │
    │  (每隔 N 秒重复)             │
    │                             │
    │       (超时未收到心跳)        │  4. 定时任务检测超时
    │                             │  5. 标记设备离线
```

### 3.3 推流流程

```
UI              SIPServer              摄像头(IPC)           ZKServer
│                  │                      │                    │
│─ POST /start ──►│                      │                    │
│                  │                      │                    │
│                  │── INVITE (SDP) ─────►│                    │
│                  │                      │                    │
│                  │◄─ 200 OK (SDP) ─────│                    │
│                  │                      │                    │
│                  │── ACK ──────────────►│                    │
│                  │                      │── RTP Stream ────►│
│◄─ 200 OK ──────│                      │                    │
│                  │                      │                    │
│─ POST /stop ───►│                      │                    │
│                  │── BYE ─────────────►│                    │
│                  │◄─ 200 OK ──────────│  (停止推流)          │
│◄─ 200 OK ──────│                      │                    │
```

## 4. 线程模型

### Netty IO 线程与业务线程池分离

```
                    ┌──────────────────────────────────┐
                    │  Netty EventLoopGroup (IO 线程)    │
                    │  - 接收 UDP 数据包                  │
                    │  - 解码 SIP 消息文本                │
                    │  - 发送 SIP 响应                    │
                    └──────────┬───────────────────────┘
                               │ 提交到业务线程池
                               ▼
                    ┌──────────────────────────────────┐
                    │  sipMessageExecutor (业务线程池)    │
                    │  core: 10, max: 40, queue: 1000  │
                    │  - 数据库读写                       │
                    │  - 认证验证                         │
                    │  - 设备状态更新                      │
                    └──────────────────────────────────┘
```

**设计原则**：`SipUdpServerHandler.channelRead0()` 仅做最基本的消息接收和解码，所有涉及数据库操作的业务逻辑均通过 `sipMessageExecutor.execute()` 提交到业务线程池异步执行，避免阻塞 Netty IO 线程。

### 线程池配置

| 线程池 | 用途 | 核心数 | 最大数 | 队列 |
|--------|------|--------|--------|------|
| `asyncExecutor` | 通用异步任务 | 10 | 50 | 1000 |
| `sipMessageExecutor` | SIP 消息处理 | 10 | 40 | 1000 |
| `mediaExecutor` | 媒体相关操作 | 8 | 32 | 2000 |
| `pushExecutor` | 推流操作 | 4 | 16 | 100 |

## 5. 数据模型

### device_info 表

| 字段 | 类型 | 说明 |
|------|------|------|
| `device_id` | VARCHAR(20) PK | GB28181 设备编码 |
| `ip` | VARCHAR(45) | 设备外网IP |
| `port` | INT | 设备外网端口 |
| `local_ip` | VARCHAR(45) | 设备内网IP |
| `local_port` | INT | 设备内网端口 |
| `time` | BIGINT | 最后通信时间戳 |
| `online` | BIT | 在线状态 |
| `live` | BIT | 推流状态 |
| `ssrc` | VARCHAR(20) | 同步源标识符 |
| `live_call_id` | VARCHAR(100) | 推流会话ID |
| `live_from_info` | VARCHAR(500) | 推流 From 头 |
| `live_to_info` | VARCHAR(500) | 推流 To 头 |
| `manufacturer` | VARCHAR(100) | 设备厂商 |
| `model` | VARCHAR(100) | 设备型号 |
| `firmware` | VARCHAR(50) | 固件版本 |
| `register_time` | BIGINT | 注册时间 |
| `last_heartbeat_time` | BIGINT | 最后心跳时间 |
| `expires` | INT | 注册有效期 |
| `force_reregister` | BIT | 强制重新注册标记 |
| `force_reregister_time` | BIGINT | 强制重新注册时间 |

**索引**：`online`, `live`, `last_heartbeat_time`, `register_time`, `force_reregister`, `force_reregister_time`

## 6. 定时任务

| 任务 | 间隔 | 说明 |
|------|------|------|
| 超时设备清理 | 5 分钟 | 检测心跳超时设备，标记离线或删除长时间离线设备 |
| 僵尸会话清理 | 30 秒 | 清理发送 INVITE 后超过 30 秒未收到 200 OK 的会话 |

## 7. 技术选型理由

| 技术 | 理由 |
|------|------|
| **Netty** | 高性能异步 NIO 框架，适合处理大量 UDP 并发消息 |
| **Spring Boot 2.7** | 成熟稳定的应用框架，丰富的生态系统 |
| **MySQL + JPA** | 满足设备信息持久化需求，JPA 简化数据访问 |
| **ZLMediaKit** | 高性能 C++ 流媒体服务器，支持多种协议转换 |
| **React + Vite** | 现代前端技术栈，开发体验好，构建速度快 |
