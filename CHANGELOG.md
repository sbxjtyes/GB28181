# Changelog

本项目的所有版本变更记录。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

---

## [1.1.0] - 2026-03-03

### Added
- 项目文档体系建设
  - 新增 README.md 项目说明文档（含致谢、许可证说明）
  - 新增 CHANGELOG.md 版本变更记录
  - 新增 LICENSE（MIT）
  - 新增 .gitignore 根目录 Git 忽略规则
  - 新增 docs/api.md API 接口详细文档
  - 新增 docs/architecture.md 架构设计文档
  - 新增 docs/deploy.md 部署指南

### Fixed
- 修复 CORS 跨域配置仅允许内网 IP，导致公网访问时 UI 无法连接 SIP 服务器的问题
- 修复多设备并发推流时第二路流卡住的问题，根因是 INVITE 请求的 `From` 头部 `tag` 参数硬编码为 `live`，违反 RFC 3261 对 SIP 对话唯一标识（Call-ID + From-tag + To-tag）的要求，改为每次推流会话生成唯一 tag
- 修复多个播放器客户端同时拉流时只有一台能正常播放、其他卡住的问题，调整 ZLMediaKit 配置：
  - `mergeWriteMS` 从 `0` 改为 `300`：启用帧合并写入，避免多客户端 I/O 竞争
  - `directProxy` 从 `1` 改为 `0`（RTSP/RTMP）：关闭直接代理，经过完整帧分发管线支持多客户端扇出
  - `sendBufSize` 从 `65536` 改为 `262144`：扩大 HTTP 发送缓冲区

### Removed
- 清理误提交的 `SIPServer/target/` 编译产物
- 清理 IDE 配置文件（`.idea/`、`.vscode/`、`.cursorrules`）
- 清理运行时日志文件（`SIPServer/logs/`、`ZKServer/log/`）
- 移除 ZLMediaKit 二进制文件（`MediaServer.exe`、`.dll`），改由用户自行下载
- 移除调试用 API 测试脚本（`api.py`、`api_config.json`）
- 清理 ZLMediaKit 媒体缓存文件（`ZKServer/www/rtp/`）

---

## [1.0.0] - 2026-02-25

### Added
- SIP 信令服务器核心功能
  - 设备 REGISTER 注册处理（支持 Digest 认证）
  - 设备 MESSAGE 心跳保活处理
  - INVITE 推流请求与 BYE 断流处理
  - 设备超时自动清理机制
  - 僵尸推流会话定时清理
- 设备管理 REST API
  - 设备列表查询（全量 + 分页 + 按状态筛选）
  - 设备详情查询
  - 设备统计信息（在线数、离线数、在线率）
  - 设备删除与强制下线
  - 强制重新注册（单个 / 批量 / 全部）
- 推流控制 REST API
  - 开始推流（INVITE）
  - 停止推流（BYE）
  - 推流状态查询
- Web 管理界面（React + Vite）
  - 设备列表展示与状态监控
  - 推流配置对话框
  - 批量操作功能
  - 媒体服务器管理面板
  - 服务控制面板
  - 系统设置面板
- 基础设施
  - 基于 Netty 的高性能 SIP/UDP 通信
  - MySQL 设备信息持久化（JPA）
  - SIP 消息业务逻辑线程池异步执行，避免阻塞 Netty IO 线程
  - Swagger/OpenAPI 在线 API 文档
  - Prometheus 监控指标集成
  - Spring Boot Actuator 健康检查
  - 全局异常处理器
  - UTF-8 全链路编码支持（含 Windows 控制台兼容）
- ZLMediaKit 流媒体服务器集成
  - RTP 端口范围配置（30000-35000）
  - 支持 RTSP / RTMP / HLS / HTTP-FLV / WebRTC 播放

### Fixed
- 统一 Handler SIP 响应编码为配置字符集，解决中文乱码问题
- 清理 CharsetUtil 残留引用
- 修复 12 项协议兼容性、性能和 Bug 问题
