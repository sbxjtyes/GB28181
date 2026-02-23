package com.gb28181.sipserver.service;

import com.gb28181.sipserver.config.SipServerConfig;
import com.gb28181.sipserver.entity.DeviceInfo;
import com.gb28181.sipserver.netty.SipUdpServer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 推流服务
 * 
 * 提供主动向摄像头发起推流请求的功能，包括：
 * - 构建INVITE推流请求
 * - 发送SDP消息
 * - 管理推流会话
 * - 处理推流状态
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Service
public class StreamService {

    private static final Logger logger = LoggerFactory.getLogger(StreamService.class);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SipMessageTemplate sipMessageTemplate;

    @Autowired
    private SipServerConfig sipServerConfig;

    @Autowired
    private SipUdpServer sipUdpServer;

    /**
     * 开始推流
     * 
     * @param deviceId 设备ID
     * @param mediaServerIp 媒体服务器IP
     * @param mediaServerPort 媒体服务器端口
     * @param useTcp 是否使用TCP传输
     * @return 推流请求结果
     */
    public boolean startStream(String deviceId, String mediaServerIp, int mediaServerPort, boolean useTcp) {
        logger.info("开始推流请求: 设备ID={}, 媒体服务器IP={}, 媒体服务器端口={}, 使用TCP={}", 
                   deviceId, mediaServerIp, mediaServerPort, useTcp);

        try {
            // 获取设备信息
            DeviceInfo deviceInfo = deviceService.getDeviceInfo(deviceId);
            if (deviceInfo == null) {
                logger.error("设备不存在: {}", deviceId);
                return false;
            }

            if (!Boolean.TRUE.equals(deviceInfo.getOnline())) {
                logger.error("设备不在线: {}", deviceId);
                return false;
            }

            if (Boolean.TRUE.equals(deviceInfo.getLive())) {
                logger.warn("设备已在推流中: {}", deviceId);
                return true;
            }

            // 生成推流会话信息
            String callId = generateCallId(deviceId);
            String ssrc = generateSsrc(deviceId);

            // 构建INVITE推流请求
            String inviteMessage = sipMessageTemplate.buildInviteRequest(
                    deviceId,
                    deviceInfo.getLocalIp() != null ? deviceInfo.getLocalIp() : deviceInfo.getIp(),
                    deviceInfo.getLocalPort() != null ? deviceInfo.getLocalPort() : String.valueOf(deviceInfo.getPort()),
                    callId,
                    sipServerConfig.getServerId(),
                    sipServerConfig.getServerIp(),
                    String.valueOf(sipServerConfig.getServerPort()),
                    ssrc,
                    mediaServerIp,
                    String.valueOf(mediaServerPort),
                    useTcp
            );

            // 发送INVITE请求
            boolean sent = sipUdpServer.sendMessage(inviteMessage, deviceInfo.getIp(), deviceInfo.getPort());

            if (sent) {
                // 更新设备推流信息
                deviceInfo.setLiveCallID(callId);
                deviceInfo.setSsrc(ssrc);
                deviceInfo.setLive(false); // 等待设备响应后再设置为true
                deviceService.saveDeviceInfo(deviceId, deviceInfo);

                logger.info("推流请求发送成功: 设备ID={}, 会话ID={}, SSRC={}", deviceId, callId, ssrc);
                return true;
            } else {
                logger.error("推流请求发送失败: 设备ID={}", deviceId);
                return false;
            }

        } catch (Exception e) {
            logger.error("推流请求异常: 设备ID={}, 错误={}", deviceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 停止推流
     * 
     * @param deviceId 设备ID
     * @return 停止推流结果
     */
    public boolean stopStream(String deviceId) {
        logger.info("===== 停止推流请求开始 =====");
        logger.info("停止推流请求: 设备ID={}", deviceId);

        try {
            // 获取设备信息
            DeviceInfo deviceInfo = deviceService.getDeviceInfo(deviceId);
            if (deviceInfo == null) {
                logger.error("设备不存在: {}", deviceId);
                return false;
            }

            // 检查设备状态和会话信息
            logger.info("设备状态: live={}, callId={}, from={}, to={}", 
                       deviceInfo.getLive(), 
                       deviceInfo.getLiveCallID(),
                       deviceInfo.getLiveFromInfo(),
                       deviceInfo.getLiveToInfo());

            // 改进：即使live状态不正确，只要有会话ID就尝试发送BYE
            String callId = deviceInfo.getLiveCallID();
            if (callId == null || callId.isEmpty()) {
                logger.warn("设备没有活动的推流会话: deviceId={}", deviceId);
                return true;
            }

            // 获取From和To信息，如果缺失则使用默认值
            String fromInfo = deviceInfo.getLiveFromInfo();
            String toInfo = deviceInfo.getLiveToInfo();
            
            if (fromInfo == null || fromInfo.isEmpty()) {
                fromInfo = "<sip:" + sipServerConfig.getServerId() + "@" + 
                          sipServerConfig.getServerIp() + ":" + sipServerConfig.getServerPort() + ">;tag=live";
                logger.warn("缺少From信息，使用默认值: {}", fromInfo);
            }
            
            if (toInfo == null || toInfo.isEmpty()) {
                String deviceLocalIp = deviceInfo.getLocalIp() != null ? deviceInfo.getLocalIp() : deviceInfo.getIp();
                String deviceLocalPort = deviceInfo.getLocalPort() != null ? deviceInfo.getLocalPort() : String.valueOf(deviceInfo.getPort());
                toInfo = "\"" + deviceId + "\" <sip:" + deviceId + "@" + deviceLocalIp + ":" + deviceLocalPort + ">";
                logger.warn("缺少To信息，使用默认值: {}", toInfo);
            }

            // 构建BYE断流请求
            String byeMessage = sipMessageTemplate.buildByeRequest(
                    deviceId,
                    deviceInfo.getLocalIp() != null ? deviceInfo.getLocalIp() : deviceInfo.getIp(),
                    deviceInfo.getLocalPort() != null ? deviceInfo.getLocalPort() : String.valueOf(deviceInfo.getPort()),
                    callId,
                    fromInfo,
                    toInfo,
                    sipServerConfig.getServerId(),
                    sipServerConfig.getServerIp(),
                    String.valueOf(sipServerConfig.getServerPort())
            );

            logger.info("BYE消息内容:\n{}", byeMessage);

            // 发送BYE请求
            boolean sent = sipUdpServer.sendMessage(byeMessage, deviceInfo.getIp(), deviceInfo.getPort());

            if (sent) {
                // 立即清除推流状态（不等待BYE响应）
                deviceService.clearDeviceLiveInfo(deviceId);
                logger.info("✓ 停止推流请求发送成功: 设备ID={}", deviceId);
                logger.info("===== 停止推流请求结束（成功）=====");
                return true;
            } else {
                logger.error("✗ 停止推流请求发送失败: 设备ID={}", deviceId);
                logger.info("===== 停止推流请求结束（失败）=====");
                return false;
            }

        } catch (Exception e) {
            logger.error("停止推流请求异常: 设备ID={}, 错误={}", deviceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取推流状态
     * 
     * @param deviceId 设备ID
     * @return 推流状态信息
     */
    public StreamStatus getStreamStatus(String deviceId) {
        DeviceInfo deviceInfo = deviceService.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            return new StreamStatus(false, false, null, null);
        }

        return new StreamStatus(
                Boolean.TRUE.equals(deviceInfo.getOnline()),
                Boolean.TRUE.equals(deviceInfo.getLive()),
                deviceInfo.getLiveCallID(),
                deviceInfo.getSsrc()
        );
    }

    /**
     * 生成Call-ID
     */
    private String generateCallId(String deviceId) {
        return deviceId + "_" + System.currentTimeMillis();
    }

    /**
     * 生成SSRC
     */
    private String generateSsrc(String deviceId) {
        if (StringUtils.isBlank(deviceId)) {
            return "01000000";
        }

        String numericPart = deviceId.replaceAll("\\D", "");
        if (numericPart.length() < 4) {
            numericPart = StringUtils.leftPad(numericPart, 4, '0');
        } else {
            numericPart = numericPart.substring(numericPart.length() - 4);
        }
        return "0100" + numericPart;
    }

    /**
     * 推流状态信息
     */
    public static class StreamStatus {
        private boolean online;
        private boolean streaming;
        private String callId;
        private String ssrc;

        public StreamStatus(boolean online, boolean streaming, String callId, String ssrc) {
            this.online = online;
            this.streaming = streaming;
            this.callId = callId;
            this.ssrc = ssrc;
        }

        // Getters
        public boolean isOnline() { return online; }
        public boolean isStreaming() { return streaming; }
        public String getCallId() { return callId; }
        public String getSsrc() { return ssrc; }
    }
}
