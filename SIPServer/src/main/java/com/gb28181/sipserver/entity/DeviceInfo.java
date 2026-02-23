package com.gb28181.sipserver.entity;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 设备信息实体类
 *
 * 存储GB28181设备的基本信息和状态，包括：
 * - 设备网络信息（IP、端口）
 * - 设备状态信息（在线状态、推流状态）
 * - 推流相关信息（SSRC、会话信息）
 *
 * @author GB28181 Team
 * @version 1.0.0
 */
@Entity
@Table(name = "device_info")
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID，符合GB28181标准的20位编码
     */
    @Id
    @Column(name = "device_id", length = 20)
    private String deviceId;

    /**
     * 设备外网IP地址
     */
    @Column(name = "ip", length = 45)
    private String ip;

    /**
     * 设备外网端口
     */
    @Column(name = "port")
    private Integer port;

    /**
     * 设备内网IP地址
     */
    @Column(name = "local_ip", length = 45)
    private String localIp;

    /**
     * 设备内网端口
     */
    @Column(name = "local_port")
    private Integer localPort;

    /**
     * 最后通信时间戳
     */
    @Column(name = "time")
    private Long time;

    /**
     * 设备是否在线
     */
    @Column(name = "online")
    private Boolean online = false;

    /**
     * 设备是否正在推流
     */
    @Column(name = "live")
    private Boolean live = false;

    /**
     * 同步源标识符（SSRC）
     */
    @Column(name = "ssrc", length = 20)
    private String ssrc;

    /**
     * 推流会话ID
     */
    @Column(name = "live_call_id", length = 100)
    private String liveCallID;

    /**
     * 推流From信息
     */
    @Column(name = "live_from_info", length = 500)
    private String liveFromInfo;

    /**
     * 推流To信息
     */
    @Column(name = "live_to_info", length = 500)
    private String liveToInfo;

    /**
     * 设备厂商信息
     */
    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    /**
     * 设备型号
     */
    @Column(name = "model", length = 100)
    private String model;

    /**
     * 设备固件版本
     */
    @Column(name = "firmware", length = 50)
    private String firmware;

    /**
     * 设备注册时间
     */
    @Column(name = "register_time")
    private Long registerTime;

    /**
     * 设备最后心跳时间
     */
    @Column(name = "last_heartbeat_time")
    private Long lastHeartbeatTime;

    /**
     * 设备注册有效期
     */
    @Column(name = "expires")
    private Integer expires;

    /**
     * 是否需要强制重新注册
     */
    @Column(name = "force_reregister")
    private Boolean forceReregister = false;

    /**
     * 强制重新注册时间
     */
    @Column(name = "force_reregister_time")
    private Long forceReregisterTime;

    // 构造函数
    public DeviceInfo() {
        this.time = System.currentTimeMillis();
        this.registerTime = System.currentTimeMillis();
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public DeviceInfo(String deviceId) {
        this();
        this.deviceId = deviceId;
    }

    // Getter and Setter methods

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Boolean getLive() {
        return live;
    }

    public void setLive(Boolean live) {
        this.live = live;
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getLiveCallID() {
        return liveCallID;
    }

    public void setLiveCallID(String liveCallID) {
        this.liveCallID = liveCallID;
    }

    public String getLiveFromInfo() {
        return liveFromInfo;
    }

    public void setLiveFromInfo(String liveFromInfo) {
        this.liveFromInfo = liveFromInfo;
    }

    public String getLiveToInfo() {
        return liveToInfo;
    }

    public void setLiveToInfo(String liveToInfo) {
        this.liveToInfo = liveToInfo;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFirmware() {
        return firmware;
    }

    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }

    public Long getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Long registerTime) {
        this.registerTime = registerTime;
    }

    public Long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(Long lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    public Integer getExpires() {
        return expires;
    }

    public void setExpires(Integer expires) {
        this.expires = expires;
    }

    public Boolean getForceReregister() {
        return forceReregister;
    }

    public void setForceReregister(Boolean forceReregister) {
        this.forceReregister = forceReregister;
    }

    public Long getForceReregisterTime() {
        return forceReregisterTime;
    }

    public void setForceReregisterTime(Long forceReregisterTime) {
        this.forceReregisterTime = forceReregisterTime;
    }

    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
        this.time = this.lastHeartbeatTime;
        this.online = true;
    }

    /**
     * 检查设备是否超时
     * 
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否超时
     */
    public boolean isTimeout(int timeoutSeconds) {
        if (lastHeartbeatTime == null) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastHeartbeatTime) > (timeoutSeconds * 1000L);
    }

    /**
     * 标记设备需要强制重新注册
     */
    public void markForceReregister() {
        this.forceReregister = true;
        this.forceReregisterTime = System.currentTimeMillis();
        this.online = false;  // 标记为离线，强制重新注册
    }

    /**
     * 清除强制重新注册标记（注册成功后调用）
     */
    public void clearForceReregister() {
        this.forceReregister = false;
        this.forceReregisterTime = null;
    }

    /**
     * 检查是否需要强制重新注册
     * 
     * @return 是否需要强制重新注册
     */
    public boolean needForceReregister() {
        return Boolean.TRUE.equals(forceReregister);
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", localIp='" + localIp + '\'' +
                ", localPort=" + localPort +
                ", online=" + online +
                ", live=" + live +
                ", ssrc='" + ssrc + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", model='" + model + '\'' +
                ", registerTime=" + registerTime +
                ", lastHeartbeatTime=" + lastHeartbeatTime +
                ", forceReregister=" + forceReregister +
                ", forceReregisterTime=" + forceReregisterTime +
                '}';
    }
}
