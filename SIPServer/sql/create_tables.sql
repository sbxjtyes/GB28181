-- GB28181 SIP服务器数据库表结构
-- 创建时间: 2025-07-14
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS gb28181_sip_server 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE gb28181_sip_server;

-- 设备信息表
CREATE TABLE IF NOT EXISTS device_info (
    device_id VARCHAR(20) NOT NULL COMMENT '设备ID，符合GB28181标准的20位编码',
    ip VARCHAR(45) COMMENT '设备外网IP地址',
    port INT COMMENT '设备外网端口',
    local_ip VARCHAR(45) COMMENT '设备内网IP地址',
    local_port VARCHAR(10) COMMENT '设备内网端口',
    time BIGINT COMMENT '最后通信时间戳',
    online BIT DEFAULT 0 COMMENT '设备是否在线',
    live BIT DEFAULT 0 COMMENT '设备是否正在推流',
    ssrc VARCHAR(20) COMMENT '同步源标识符（SSRC）',
    live_call_id VARCHAR(100) COMMENT '推流会话ID',
    live_from_info VARCHAR(500) COMMENT '推流From信息',
    live_to_info VARCHAR(500) COMMENT '推流To信息',
    manufacturer VARCHAR(100) COMMENT '设备厂商信息',
    model VARCHAR(100) COMMENT '设备型号',
    firmware VARCHAR(50) COMMENT '设备固件版本',
    register_time BIGINT COMMENT '设备注册时间',
    last_heartbeat_time BIGINT COMMENT '设备最后心跳时间',
    expires INT COMMENT '设备注册有效期',
    force_reregister BIT DEFAULT 0 COMMENT '是否需要强制重新注册',
    force_reregister_time BIGINT COMMENT '强制重新注册时间',
    PRIMARY KEY (device_id)
) ENGINE=InnoDB 
DEFAULT CHARSET=utf8mb4 
COLLATE=utf8mb4_unicode_ci 
COMMENT='GB28181设备信息表';

-- 创建索引
CREATE INDEX idx_device_online ON device_info(online);
CREATE INDEX idx_device_live ON device_info(live);
CREATE INDEX idx_device_heartbeat ON device_info(last_heartbeat_time);
CREATE INDEX idx_device_register_time ON device_info(register_time);
CREATE INDEX idx_device_force_reregister ON device_info(force_reregister);
CREATE INDEX idx_device_force_reregister_time ON device_info(force_reregister_time);

-- 插入示例数据（可选）
-- INSERT INTO device_info (device_id, ip, port, local_ip, local_port, online, live, register_time, last_heartbeat_time) 
-- VALUES ('34020000001320000001', '192.168.1.100', 5060, '192.168.1.100', '5060', 1, 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

COMMIT;
