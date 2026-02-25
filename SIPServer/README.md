# GB28181 SIP 服务器

基于GB28181协议的SIP信令服务器，支持摄像头设备注册、认证、心跳保活、会话建立和推流控制。

## 项目概述

这是一个企业级的GB28181 SIP服务器实现，采用Java Spring Boot框架开发，集成了Netty网络库和MySQL数据库。系统支持标准的GB28181协议，实现了设备管理、实时流媒体控制等核心功能。

### 主要特性

- **设备管理**：支持设备注册、认证、心跳保活、状态监控
- **SIP信令**：基于Netty实现高性能SIP服务器
- **流媒体控制**：支持推流启动、停止、状态查询
- **REST API**：提供完整的HTTP API接口
- **数据库存储**：使用MySQL持久化设备信息
- **系统监控**：内置系统状态监控、Prometheus 指标和健康检查
- **异步处理**：支持异步任务处理，提高系统性能
- **定时任务**：自动清理超时设备，维护系统健康

## 技术栈

### 后端技术
- **Java 17**：核心编程语言
- **Spring Boot 2.7.18**：应用框架
- **Spring Data JPA**：数据访问层
- **Netty 4.1.38**：网络通信框架
- **MySQL 8.0**：关系型数据库

### 开发工具
- **Maven 3.8+**：项目构建管理
- **JUnit 4.13**：单元测试
- **Mockito 3.12**：测试模拟框架
- **JaCoCo**：代码覆盖率分析

### 主要依赖
- Spring Boot Web：REST API支持
- Spring Boot Data JPA：数据库访问
- Spring Boot Validation：数据验证
- Spring Boot Actuator：运行时监控
- Micrometer Prometheus Registry：指标导出
- Apache Commons Lang3：工具类
- Commons Codec：编码解码工具

## 系统架构

### 项目结构
```
src/main/java/com/gb28181/sipserver/
├── config/                 # 配置类
│   ├── SipServerConfig.java       # SIP服务器配置
│   ├── ThreadPoolConfig.java      # 线程池配置
│   └── WebConfig.java            # Web配置
├── controller/            # REST控制器
│   ├── DeviceController.java     # 设备管理API
│   └── StreamController.java     # 推流控制API
├── entity/               # 实体类
│   └── DeviceInfo.java           # 设备信息实体
├── netty/                # Netty网络层
│   ├── SipUdpServer.java         # SIP UDP服务器
│   └── SipUdpServerHandler.java  # SIP消息处理器
├── repository/           # 数据访问层
│   └── DeviceRepository.java     # 设备数据仓库
├── service/              # 业务逻辑层
│   ├── DeviceService.java        # 设备管理服务
│   ├── SipMessageParser.java     # SIP消息解析器
│   ├── SipMessageTemplate.java   # SIP消息模板
│   ├── StreamService.java        # 推流服务
│   └── SystemMonitorService.java # 系统监控服务
└── util/                 # 工具类
    ├── NetworkUtils.java         # 网络工具
    ├── SchemaGenerator.java      # 数据库架构生成
    └── SipUtils.java            # SIP工具
```

### 架构设计

#### 1. 网络层 (Netty)
- 基于UDP协议的SIP服务器
- 高性能异步I/O处理
- 支持并发连接管理
- 消息编解码处理

#### 2. 业务层 (Service)
- 设备生命周期管理
- SIP消息处理
- 推流控制逻辑
- 系统监控统计

#### 3. 数据层 (Repository)
- JPA实体映射
- 数据库CRUD操作
- 自定义查询方法
- 事务管理

#### 4. API层 (Controller)
- RESTful API设计
- 请求参数验证
- 统一响应格式
- 异常处理

## 功能特性

### 1. 设备管理
- **设备注册**：支持GB28181设备注册流程
- **身份认证**：设备认证和授权
- **心跳保活**：设备在线状态维护
- **状态监控**：实时设备状态查询
- **超时清理**：自动清理离线设备

### 2. SIP信令
- **协议支持**：完整GB28181 SIP协议实现
- **消息解析**：REGISTER、INVITE、ACK、BYE等消息处理
- **会话管理**：SIP会话建立、维护、释放
- **错误处理**：SIP响应码处理和重试机制

### 3. 推流控制
- **推流启动**：向设备发送推流请求
- **推流停止**：停止设备推流
- **状态查询**：获取推流状态信息
- **参数配置**：支持TCP/UDP传输协议选择

### 4. 系统监控
- **设备统计**：在线/离线设备数量统计
- **性能指标**：系统资源使用情况
- **日志记录**：详细操作日志
- **健康检查**：系统健康状态监控

## 安装部署

### 环境要求
- **Java**：JDK 17或更高版本
- **Maven**：3.8.0或更高版本
- **MySQL**：8.0或更高版本
- **操作系统**：Windows 10+/Linux/MacOS

### 数据库配置

1. **创建数据库**
```sql
CREATE DATABASE IF NOT EXISTS gb28181_sip_server
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

2. **执行建表脚本**
```bash
mysql -u root -p gb28181_sip_server < sql/create_tables.sql
```

### 应用配置

修改 `src/main/resources/application.yml` 文件：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gb28181_sip_server?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password

# GB28181配置
gb28181:
  sip:
    server-id: 34020000002000000001    # SIP服务器ID
    server-ip: 0.0.0.0                 # 监听IP
    server-port: 5060                  # 监听端口
    sip-domain: 3402000000             # SIP域
    device-password: your_password     # 设备认证密码
```

### 启动应用

#### Windows
```bash
# 使用启动脚本
start.bat

# 或直接使用Maven
mvn spring-boot:run -Dfile.encoding=UTF-8
```

#### Linux/Mac
```bash
# 确保脚本可执行
chmod +x start.sh
./start.sh

# 或直接使用Maven
mvn spring-boot:run -Dfile.encoding=UTF-8
```

## API接口

### 设备管理API

#### 获取所有设备
```http
GET /gb28181/api/devices
```

#### 获取在线设备
```http
GET /gb28181/api/devices/online
```

#### 获取设备详情
```http
GET /gb28181/api/devices/{deviceId}
```

#### 获取设备统计
```http
GET /gb28181/api/devices/statistics
```

#### 删除设备
```http
DELETE /gb28181/api/devices/{deviceId}
```

#### 强制设备下线
```http
POST /gb28181/api/devices/{deviceId}/offline
```

### 推流控制API

#### 开始推流
```http
POST /gb28181/api/stream/start
Content-Type: application/json

{
  "deviceId": "34020000001320000001",
  "mediaServerIp": "192.168.1.100",
  "mediaServerPort": 30000,
  "useTcp": false
}
```

### 环境变量优先级

生产环境中建议通过环境变量注入敏感信息：

| 变量 | 说明 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | 数据库 JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `GB28181_DEVICE_PASSWORD` | 设备认证口令 |
| `GB28181_SERVER_IP/PORT` | SIP 监听地址 |
| `GB28181_HEARTBEAT_TIMEOUT` | 心跳超时 |

未设置时会采用 `application.yml` 中的安全默认值。

#### 停止推流
```http
POST /gb28181/api/stream/stop
Content-Type: application/json

{
  "deviceId": "34020000001320000001"
}
```

#### 获取推流状态
```http
GET /gb28181/api/stream/status/{deviceId}
```

## 配置说明

### SIP服务器配置
```yaml
gb28181:
  sip:
    server-id: 34020000002000000001    # 服务器ID（20位GB28181编码）
    server-ip: 0.0.0.0                 # 监听IP地址
    server-port: 5060                  # SIP监听端口
    sip-domain: 3402000000             # SIP域
    device-password: Yf.31306           # 设备认证密码
    heartbeat-timeout: 60              # 心跳超时时间（秒）
    register-expires: 3600             # 设备注册有效期（秒）
```

### 媒体处理配置
```yaml
app:
  media:
    rtp-cache-size: 60                  # RTP包缓存大小
    udp-cache-frame-length: 5           # UDP缓存帧数
    tcp-cache-frame-length: 2           # TCP缓存帧数
    stream-timeout: 30                  # 推流超时时间（秒）
    default-server-ip: 192.168.97.12   # 默认媒体服务器IP
    default-server-port: 30000          # 默认媒体服务器端口
    default-use-tcp: false              # 默认使用TCP推流
```

### 监控和运维

Actuator 已公开 `/gb28181/actuator` 路径，支持健康检查、线程栈、环境变量等端点。

#### 健康检查
```http
GET /gb28181/actuator/health
```

内含自定义 `SipServerHealthIndicator`，会返回 SIP 监听地址、设备统计以及 Netty 运行状态。

#### Prometheus 指标
```http
GET /gb28181/actuator/prometheus
```

内置指标：

| 指标名称 | 说明 |
| --- | --- |
| `sipserver_devices_total` | 注册设备总数 |
| `sipserver_devices_online` | 在线设备数 |
| `sipserver_memory_usage_percent` | JVM 堆使用率 |
| `sipserver_cpu_usage_percent` | 进程 CPU 用量 |
| `sipserver_threads_total` | JVM 线程数 |

### 系统信息
```http
GET /gb28181/actuator/info
```

### 应用指标
```http
GET /gb28181/actuator/metrics
```

### 系统信息
```http
GET /gb28181/actuator/info
```

### 日志配置
- 日志文件：`logs/gb28181-sip-server.log`
- 最大文件大小：100MB
- 保留历史：30天

## 开发指南

### 开发环境设置
1. 安装JDK 17
2. 安装Maven
3. 安装MySQL 8.0
4. 配置IDE编码为UTF-8

### 代码规范
- 使用Java 17语法特性
- 遵循Spring Boot最佳实践
- 添加完整的函数级注释
- 编写单元测试

### 测试
```bash
# 运行单元测试
mvn test

# 生成测试覆盖率报告
mvn test jacoco:report

# 运行特定测试类
mvn test -Dtest=DeviceServiceTest
```

### 构建部署
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包应用
mvn clean package

# 运行打包后的应用
java -jar target/gb28181-sip-server-1.0.0.jar
```

## 常见问题

### 中文乱码问题
确保系统使用UTF-8编码：
- 使用提供的启动脚本
- IDE设置为UTF-8
- 数据库字符集为utf8mb4

### 数据库连接问题
- 检查MySQL服务状态
- 验证用户名和密码
- 确认数据库名称正确
- 检查网络连接和防火墙

### 设备注册失败
- 检查SIP服务器配置
- 验证设备密码
- 确认网络连通性
- 查看服务器日志

### 性能优化
- 调整线程池配置
- 优化数据库连接池
- 启用缓存机制
- 使用异步处理

#### 操作系统 UDP 缓冲区调优

当接入设备超过 500 台时，大量并发心跳可能导致 OS 层面 UDP 丢包。建议按如下方式调大系统 UDP 缓冲区：

**Linux**
```bash
# 查看当前设置
sysctl net.core.rmem_max
sysctl net.core.wmem_max

# 临时调整（重启后失效）
sudo sysctl -w net.core.rmem_max=4194304   # 接收缓冲区 4MB
sudo sysctl -w net.core.wmem_max=4194304   # 发送缓冲区 4MB

# 永久生效：编辑 /etc/sysctl.conf 追加
net.core.rmem_max=4194304
net.core.wmem_max=4194304
# 然后执行 sudo sysctl -p
```

**Windows**
```
暂无系统级 UDP Buffer 调整需求，Netty 已配置 1MB SO_RCVBUF/SO_SNDBUF。
若有百万级并发需求，建议部署于 Linux 系统。
```

### SIP 字符集配置

GB28181-2016 标准默认字符集为 GB2312。如果接入的老旧设备（部分海康/大华旧版固件）中文字段出现乱码，可在 `application.yml` 中修改：

```yaml
gb28181:
  sip:
    sip-charset: GBK   # 兼容 GB2312 编码，默认值为 UTF-8
```

也可以通过环境变量设置：`GB28181_SIP_CHARSET=GBK`

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 贡献指南

欢迎提交Issue和Pull Request来改进项目。

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 发起Pull Request

## 版本历史

### v1.0.0
- 初始版本发布
- 实现基础的GB28181 SIP服务器功能
- 支持设备注册、认证、心跳保活
- 提供REST API接口
- 集成MySQL数据库存储

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交GitHub Issue
- 发送邮件至项目维护者

---

**GB28181 Team** © 2024. 保留所有权利。
