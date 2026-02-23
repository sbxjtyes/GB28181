package com.gb28181.sipserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * SIP服务器配置类
 * 
 * 配置SIP服务器的基本参数，包括：
 * - 服务器ID和网络配置
 * - Redis配置
 * - 设备认证配置
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Validated
@Component
@ConfigurationProperties(prefix = "gb28181.sip")
public class SipServerConfig {

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
     * 设备信息存储的Redis Key前缀
     */
    @NotEmpty(message = "sipDeviceKey 不能为空")
    private String sipDeviceKey;

    /**
     * SIP域，用于设备认证
     */
    private String sipDomain = "3402000000";

    /**
     * 设备认证密码，默认为123456
     */
    private String devicePassword = "123456";

    /**
     * 心跳超时时间（秒），默认60秒
     */
    private Integer heartbeatTimeout = 60;

    /**
     * 设备注册有效期（秒），默认3600秒
     */
    private Integer registerExpires = 3600;

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

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
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
