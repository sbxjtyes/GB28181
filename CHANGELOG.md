# Changelog

本项目的所有版本变更记录。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

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
  - 预编译 MediaServer.exe
  - RTP 端口范围配置（30000-35000）
  - 支持 RTSP / RTMP / HLS / HTTP-FLV / WebRTC 播放

### Fixed
- 统一 Handler SIP 响应编码为配置字符集，解决中文乱码问题
- 清理 CharsetUtil 残留引用
- 修复 12 项协议兼容性、性能和 Bug 问题

---

## [Unreleased]

### Added
- 项目文档体系建设
  - 新增 README.md 项目说明文档
  - 新增 CHANGELOG.md 版本变更记录
  - 新增 .gitignore 根目录 Git 忽略规则
  - 新增 docs/api.md API 接口详细文档
  - 新增 docs/architecture.md 架构设计文档
  - 新增 docs/deploy.md 部署指南
