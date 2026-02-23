package com.gb28181.sipserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * SIP服务器配置类
 * 
 * 配置SIP服务器的基本参数，包括：
 * - 服务器ID和网络配置
 * - 数据库配置
 * - 设备认证配置
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Validated
@Component
@ConfigurationProperties(prefix = "gb28181.sip")
public class SipServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(SipServerConfig.class);

    /**
     * SIP服务器ID，符合GB28181标准的20位编码
     */
    @NotEmpty(message = "serverId 不能为空")
    private String serverId;

    /**
     * SIP服务器监听端口，默认5060
     */
    @NotNull(message = "serverPort 不能为空")
    private Integer serverPort = 5060;

    /**
     * SIP服务器IP地址，默认监听所有接口
     */
    private String serverIp = "0.0.0.0";

    /**
     * SIP消息中使用的服务器IP（用于SIP头部的From/Via/Contact）
     * 如果serverIp为0.0.0.0，则自动检测本机实际IP
     */
    private String sipIp;



    /**
     * 设备信息存储的数据库表前缀标识
     */
    @NotEmpty(message = "sipDeviceKey 不能为空")
    private String sipDeviceKey;

    /**
     * SIP域，用于设备认证
     */
    private String sipDomain = "3402000000";

    /**
     * 设备认证密码，默认为changeme
     */
    private String devicePassword = "changeme";

    /**
     * 心跳超时时间（秒），默认60秒
     */
    private Integer heartbeatTimeout = 60;

    /**
     * 设备注册有效期（秒），默认3600秒
     */
    private Integer registerExpires = 3600;

    /**
     * 初始化时检测实际IP，避免0.0.0.0出现在SIP消息头中
     */
    @PostConstruct
    public void init() {
        if (sipIp != null && !sipIp.isEmpty()) {
            // 用户显式配置了sipIp，直接使用
            logger.info("SIP消息使用配置的IP: {}", sipIp);
        } else if ("0.0.0.0".equals(serverIp) || serverIp == null) {
            sipIp = detectLocalIp();
            logger.info("SIP服务器监听地址: 0.0.0.0，SIP消息使用自动检测的IP: {}", sipIp);
        } else {
            sipIp = serverIp;
        }
    }

    /**
     * 检测本机第一个非回环网卡IP
     */
    private String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("自动检测本机IP失败: {}", e.getMessage());
        }
        return "127.0.0.1";
    }

    // Getter and Setter methods

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * 获取监听地址（用于 Netty 绑定）
     */
    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    /**
     * 获取SIP消息中使用的IP地址（用于From/Via/Contact头部）
     * 如果配置为0.0.0.0，返回自动检测的本机IP
     */
    public String getSipIp() {
        return sipIp;
    }

    public void setSipIp(String sipIp) {
        this.sipIp = sipIp;
    }

    public String getSipDeviceKey() {
        return sipDeviceKey;
    }

    public void setSipDeviceKey(String sipDeviceKey) {
        this.sipDeviceKey = sipDeviceKey;
    }



    public String getSipDomain() {
        return sipDomain;
    }

    public void setSipDomain(String sipDomain) {
        this.sipDomain = sipDomain;
    }

    public String getDevicePassword() {
        return devicePassword;
    }

    public void setDevicePassword(String devicePassword) {
        this.devicePassword = devicePassword;
    }

    public Integer getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(Integer heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public Integer getRegisterExpires() {
        return registerExpires;
    }

    public void setRegisterExpires(Integer registerExpires) {
        this.registerExpires = registerExpires;
    }



    @Override
    public String toString() {
        return "SipServerConfig{" +
                "serverId='" + serverId + '\'' +
                ", serverPort=" + serverPort +
                ", serverIp='" + serverIp + '\'' +
                ", sipDeviceKey='" + sipDeviceKey + '\'' +
                ", sipDomain='" + sipDomain + '\'' +
                ", heartbeatTimeout=" + heartbeatTimeout +
                ", registerExpires=" + registerExpires +
                '}';
    }
}
