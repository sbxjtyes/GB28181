# API 接口文档

> **Base URL**: `http://localhost:8080/gb28181`
>
> **Swagger UI**: `http://localhost:8080/gb28181/swagger-ui/index.html`

---

## 通用说明

### 响应格式

所有 API 返回 JSON 格式，结构如下：

```json
{
  "success": true,
  "message": "操作描述",
  "...": "具体业务字段"
}
```

### 错误响应

```json
{
  "success": false,
  "message": "错误描述信息"
}
```

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 500 | 服务器内部错误 |

---

## 1. 设备管理 API

**路径前缀**: `/api/devices`

### 1.1 获取所有设备列表

**GET** `/api/devices`

**响应示例**:
```json
{
  "success": true,
  "message": "获取设备列表成功",
  "total": 2,
  "devices": [
    {
      "deviceId": "34020000001320000001",
      "ip": "192.168.1.100",
      "port": 5060,
      "localIp": "192.168.1.100",
      "localPort": 5060,
      "online": true,
      "live": false,
      "manufacturer": "Hikvision",
      "model": "DS-2CD2T47G2-L",
      "registerTime": 1740000000000,
      "lastHeartbeatTime": 1740000060000
    }
  ]
}
```

---

### 1.2 分页获取设备列表

**GET** `/api/devices/page`

**请求参数** (Query):

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | int | 否 | 0 | 页码（从0开始） |
| `size` | int | 否 | 50 | 每页数量（最大200） |
| `online` | boolean | 否 | - | 按在线状态筛选 |

**响应示例**:
```json
{
  "success": true,
  "devices": [...],
  "total": 100,
  "page": 0,
  "size": 50,
  "totalPages": 2
}
```

---

### 1.3 获取在线设备列表

**GET** `/api/devices/online`

**响应示例**:
```json
{
  "success": true,
  "message": "获取在线设备列表成功",
  "total": 5,
  "devices": [...]
}
```

---

### 1.4 获取设备详情

**GET** `/api/devices/{deviceId}`

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| `deviceId` | string | 设备ID（20位GB28181编码） |

**成功响应**:
```json
{
  "success": true,
  "message": "获取设备详情成功",
  "device": {
    "deviceId": "34020000001320000001",
    "ip": "192.168.1.100",
    "port": 5060,
    "localIp": "192.168.1.100",
    "localPort": 5060,
    "time": 1740000060000,
    "online": true,
    "live": false,
    "ssrc": null,
    "liveCallID": null,
    "liveFromInfo": null,
    "liveToInfo": null,
    "manufacturer": "Hikvision",
    "model": "DS-2CD2T47G2-L",
    "firmware": "V5.7.11",
    "registerTime": 1740000000000,
    "lastHeartbeatTime": 1740000060000,
    "expires": 3600,
    "forceReregister": false,
    "forceReregisterTime": null
  }
}
```

**设备不存在**:
```json
{
  "success": false,
  "message": "设备不存在: 34020000001320000099"
}
```

---

### 1.5 获取设备统计信息

**GET** `/api/devices/statistics`

**响应示例**:
```json
{
  "success": true,
  "message": "获取统计信息成功",
  "statistics": {
    "totalDevices": 10,
    "onlineDevices": 7,
    "offlineDevices": 3,
    "onlineRate": 70.0
  }
}
```

---

### 1.6 删除设备

**DELETE** `/api/devices/{deviceId}`

**响应示例**:
```json
{
  "success": true,
  "message": "设备删除成功",
  "deviceId": "34020000001320000001"
}
```

---

### 1.7 强制设备下线

**POST** `/api/devices/{deviceId}/offline`

**说明**: 将设备状态标记为离线。

**响应示例**:
```json
{
  "success": true,
  "message": "设备已强制下线",
  "deviceId": "34020000001320000001"
}
```

---

### 1.8 强制设备重新注册

**POST** `/api/devices/{deviceId}/force-reregister`

**说明**: 标记设备为需要强制重新注册，下次设备通信时将要求重新认证。

**响应示例**:
```json
{
  "success": true,
  "message": "设备已标记为强制重新注册，下次通信时将要求重新注册",
  "deviceId": "34020000001320000001"
}
```

---

### 1.9 批量强制重新注册

**POST** `/api/devices/batch/force-reregister`

**请求体**:
```json
{
  "deviceIds": [
    "34020000001320000001",
    "34020000001320000002"
  ]
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "批量强制重新注册完成，成功处理 2/2 个设备",
  "totalCount": 2,
  "successCount": 2,
  "failureCount": 0
}
```

---

### 1.10 全部设备强制重新注册

**POST** `/api/devices/all/force-reregister`

**响应示例**:
```json
{
  "success": true,
  "message": "强制所有在线设备重新注册完成，成功处理 5 个设备",
  "successCount": 5
}
```

---

## 2. 推流控制 API

**路径前缀**: `/api/stream`

### 2.1 开始推流

**POST** `/api/stream/start`

**请求体**:
```json
{
  "deviceId": "34020000001320000001",
  "mediaServerIp": "192.168.1.200",
  "mediaServerPort": 30000,
  "useTcp": false
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `deviceId` | string | 是 | - | 设备ID |
| `mediaServerIp` | string | 是 | - | 媒体服务器IP |
| `mediaServerPort` | int | 是 | - | 媒体服务器RTP端口 (1-65535) |
| `useTcp` | boolean | 否 | false | 是否使用TCP传输 |

**成功响应**:
```json
{
  "success": true,
  "message": "推流请求发送成功",
  "deviceId": "34020000001320000001",
  "mediaServerIp": "192.168.1.200",
  "mediaServerPort": 30000,
  "useTcp": false
}
```

**设备已在推流**:
```json
{
  "success": true,
  "message": "设备已在推流中",
  "alreadyStreaming": true,
  "deviceId": "34020000001320000001"
}
```

---

### 2.2 停止推流

**POST** `/api/stream/stop`

**请求体**:
```json
{
  "deviceId": "34020000001320000001"
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "停止推流请求发送成功",
  "deviceId": "34020000001320000001"
}
```

---

### 2.3 获取推流状态

**GET** `/api/stream/status/{deviceId}`

**响应示例**:
```json
{
  "success": true,
  "message": "获取推流状态成功",
  "deviceId": "34020000001320000001",
  "online": true,
  "streaming": true,
  "callId": "34020000001320000001-1740000000",
  "ssrc": "0340200001"
}
```

---

## 3. 系统管理端点

| 端点 | 说明 |
|------|------|
| GET `/actuator/health` | 健康检查 |
| GET `/actuator/info` | 应用信息 |
| GET `/actuator/metrics` | 应用指标 |
| GET `/actuator/prometheus` | Prometheus 指标 |
| GET `/actuator/env` | 环境配置 |
| GET `/actuator/threaddump` | 线程转储 |
| GET `/actuator/loggers` | 日志级别管理 |
