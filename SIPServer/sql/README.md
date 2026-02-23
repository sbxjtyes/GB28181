# 数据库初始化说明

## 1. 创建数据库

在MySQL中执行以下命令创建数据库：

```sql
-- 连接到MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE IF NOT EXISTS gb28181_sip_server 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选）
CREATE USER 'gb28181'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON gb28181_sip_server.* TO 'gb28181'@'localhost';
FLUSH PRIVILEGES;
```

## 2. 执行建表脚本

```bash
# 方法1：使用MySQL命令行
mysql -u root -p gb28181_sip_server < sql/create_tables.sql

# 方法2：在MySQL客户端中执行
USE gb28181_sip_server;
SOURCE sql/create_tables.sql;
```

## 3. 验证表结构

```sql
-- 查看表结构
DESCRIBE device_info;

-- 查看索引
SHOW INDEX FROM device_info;
```

## 4. 配置应用

修改 `application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gb28181_sip_server?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root  # 或者你创建的用户名
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 5. 启动应用

```bash
# Windows
start.bat

# Linux/Mac
chmod +x start.sh
./start.sh

# 或者直接使用Maven
mvn spring-boot:run -Dfile.encoding=UTF-8
```

## 注意事项

1. **字符编码**：确保数据库、表和连接都使用 UTF-8 编码
2. **时区设置**：MySQL连接URL中包含时区参数
3. **权限配置**：确保数据库用户有足够的权限
4. **防火墙**：确保MySQL端口（默认3306）可访问

## 故障排除

### 1. 中文乱码问题
- 使用提供的启动脚本 `start.bat` 或 `start.sh`
- 确保IDE和终端使用UTF-8编码
- 检查MySQL字符集设置

### 2. 数据库连接问题
- 检查MySQL服务是否启动
- 验证用户名和密码
- 确认数据库名称正确
- 检查网络连接和防火墙设置

### 3. 表不存在问题
- 确保已执行建表脚本
- 检查数据库名称是否正确
- 验证用户权限
