package com.gb28181.sipserver.netty;

import com.gb28181.sipserver.config.SipServerConfig;
import com.gb28181.sipserver.entity.DeviceInfo;
import com.gb28181.sipserver.service.DeviceService;
import com.gb28181.sipserver.service.SipMessageParser;
import com.gb28181.sipserver.service.SipMessageTemplate;
import com.gb28181.sipserver.util.ConsoleHighlight;
import com.gb28181.sipserver.util.SipUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * SIP UDP服务器消息处理器
 * 
 * 处理接收到的SIP消息，包括：
 * - REGISTER注册请求处理
 * - MESSAGE心跳保活处理
 * - INVITE响应处理
 * - BYE响应处理
 * 
 * 【并发优化】所有包含数据库操作的业务逻辑均提交到 sipMessageExecutor 线程池异步执行，
 * 避免阻塞 Netty IO 线程，确保多设备消息可以并发处理。
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Component
@ChannelHandler.Sharable
public class SipUdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(SipUdpServerHandler.class);

    @Autowired
    private SipServerConfig sipServerConfig;

    @Autowired
    private SipMessageParser sipMessageParser;

    @Autowired
    private SipMessageTemplate sipMessageTemplate;

    @Autowired
    private DeviceService deviceService;

    /**
     * 注入 SIP 消息处理线程池，用于异步处理耗时的业务逻辑（如数据库操作）
     * 避免阻塞 Netty IO 线程，从而解决多设备并发时互相阻塞的问题
     */
    @Autowired
    @Qualifier("sipMessageExecutor")
    private Executor sipMessageExecutor;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        try {
            // 在 IO 线程中快速提取消息内容和发送方地址
            // 注意：DatagramPacket 的 ByteBuf 在 channelRead0 返回后会被自动释放，
            // 因此必须在此处提取为 String，不能在异步任务中访问 ByteBuf
            InetSocketAddress sender = packet.sender();
            String senderIp = sender.getAddress().getHostAddress();
            int senderPort = sender.getPort();
            String message = packet.content().toString(sipServerConfig.getCharset());

            // 将耗时的业务处理提交到线程池异步执行，立即释放 IO 线程
            sipMessageExecutor.execute(() -> {
                try {
                    processMessage(ctx, message, senderIp, senderPort);
                } catch (Exception e) {
                    logger.error("异步处理SIP消息时发生错误: {}", e.getMessage(), e);
                }
            });

        } catch (Exception e) {
            logger.error("接收SIP消息时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理 SIP 消息的核心逻辑（在线程池中异步执行）
     * 
     * @param ctx        Netty 通道上下文（线程安全，可跨线程使用）
     * @param message    SIP 消息原始文本
     * @param senderIp   发送方 IP
     * @param senderPort 发送方端口
     */
    private void processMessage(ChannelHandlerContext ctx, String message, String senderIp, int senderPort) {
        Map<String, String> sipMessage = sipMessageParser.parseSipMessage(message);
        if (sipMessage.isEmpty()) {
            return;
        }

        // 根据消息类型处理
        String messageType = sipMessage.get("messageType");
        if ("REQUEST".equals(messageType)) {
            handleRequest(ctx, sipMessage, senderIp, senderPort);
        } else if ("RESPONSE".equals(messageType)) {
            handleResponse(ctx, sipMessage, senderIp, senderPort);
        }
    }

    /**
     * 处理SIP请求消息
     */
    private void handleRequest(ChannelHandlerContext ctx, Map<String, String> sipMessage,
            String senderIp, int senderPort) {
        String method = sipMessage.get("method");
        String deviceId = sipMessage.get("deviceId");

        if (method == null) {
            logger.warn("SIP请求缺少方法名, from={}:{}", senderIp, senderPort);
            return;
        }

        switch (method) {
            case "REGISTER":
                logger.info("← 收到SIP请求: method=REGISTER, deviceId={}, from={}:{}",
                        deviceId, senderIp, senderPort);
                handleRegisterRequest(ctx, sipMessage, senderIp, senderPort);
                break;
            case "MESSAGE":
                logger.debug("← 收到SIP请求: method=MESSAGE, deviceId={}, from={}:{}",
                        deviceId, senderIp, senderPort);
                handleMessageRequest(ctx, sipMessage, senderIp, senderPort);
                break;
            default:
                logger.info("收到未处理的SIP请求方法: {}, deviceId={}", method, deviceId);
                break;
        }
    }

    /**
     * 处理SIP响应消息
     * 使用CSeq头部中的方法名区分INVITE/BYE响应（符合RFC 3261）
     */
    private void handleResponse(ChannelHandlerContext ctx, Map<String, String> sipMessage,
            String senderIp, int senderPort) {
        String stateCode = sipMessage.get("stateCode");
        String cseq = sipMessage.get("CSeq");

        // 记录SIP临时响应（100 Trying / 180 Ringing），正常推流流程中每次都会产生
        if ("100".equals(stateCode) || "180".equals(stateCode)) {
            logger.debug("← 收到SIP临时响应: {} {}, CSeq={}, from={}:{}",
                    stateCode, sipMessage.get("reasonPhrase"), cseq, senderIp, senderPort);
            return;
        }

        if (StringUtils.isEmpty(cseq)) {
            return;
        }

        if ("200".equals(stateCode)) {
            if (cseq.toUpperCase().contains("INVITE")) {
                handleInviteOkResponse(ctx, sipMessage, senderIp, senderPort);
            } else if (cseq.toUpperCase().contains("BYE")) {
                handleByeOkResponse(ctx, sipMessage, senderIp, senderPort);
            }
        } else if (cseq.toUpperCase().contains("INVITE")) {
            // INVITE的错误响应（4xx/5xx/6xx）：记录日志并清理僵尸会话
            String deviceId = sipMessage.get("deviceId");
            logger.warn("收到INVITE错误响应: stateCode={}, deviceId={}, CSeq={}, from={}:{}",
                    stateCode, deviceId, cseq, senderIp, senderPort);
            if (StringUtils.isNotEmpty(deviceId)) {
                deviceService.clearDeviceLiveInfo(deviceId);
            }
        }
    }

    /**
     * 处理REGISTER注册请求
     */
    private void handleRegisterRequest(ChannelHandlerContext ctx, Map<String, String> sipMessage,
            String senderIp, int senderPort) {
        String deviceId = sipMessage.get("deviceId");
        String callId = StringUtils.defaultString(sipMessage.get("Call-ID"));
        String from = StringUtils.defaultString(sipMessage.get("From"));
        String to = StringUtils.defaultString(sipMessage.get("To"));
        String via = StringUtils.defaultString(sipMessage.get("Via"));
        String authorization = sipMessage.get("Authorization");

        if (StringUtils.isEmpty(deviceId)) {
            logger.warn("REGISTER请求中缺少设备ID");
            return;
        }

        // 检查是否包含认证信息
        if (StringUtils.isEmpty(authorization)) {
            // 发送401未授权响应（CSeq从请求透传，符合RFC 3261）
            String cseq = sipMessage.get("CSeq");
            if (StringUtils.isEmpty(cseq)) {
                cseq = "1 REGISTER";
            }
            String nonce = SipUtils.generateNonce(callId, deviceId);
            String response = sipMessageTemplate.build401Unauthorized(
                    cseq, callId, from, to, via, sipServerConfig.getSipDomain(), nonce);
            sendResponse(ctx, response, senderIp, senderPort);
            logger.info("向设备发送401未授权响应（等待认证）: {}", deviceId);
            return;
        }

        // 验证认证信息
        String username = sipMessage.get("auth_username");
        String realm = sipMessage.get("auth_realm");
        String nonce = sipMessage.get("auth_nonce");
        String uri = sipMessage.get("auth_uri");
        String response = sipMessage.get("auth_response");

        if (SipUtils.verifyAuthResponse(username, realm, sipServerConfig.getDevicePassword(),
                nonce, "REGISTER", uri, response)) {
            // 认证成功，注册设备
            registerDevice(deviceId, sipMessage, senderIp, senderPort);

            // 清除强制重新注册标记
            deviceService.handleDeviceRegistered(deviceId);

            // 发送200 OK响应
            String cseq = StringUtils.defaultString(sipMessage.get("CSeq"));
            String expires = String.valueOf(sipServerConfig.getRegisterExpires());
            String okResponse = sipMessageTemplate.build200OkRegister(
                    cseq, callId, from, to, via, expires);
            sendResponse(ctx, okResponse, senderIp, senderPort);

            // 使用高亮显示设备注册成功信息
            logger.info(ConsoleHighlight.safeRegisterSuccess(deviceId));
        } else {
            logger.warn("设备认证失败: {}", deviceId);
        }
    }

    /**
     * 处理MESSAGE心跳保活请求
     */
    private void handleMessageRequest(ChannelHandlerContext ctx, Map<String, String> sipMessage,
            String senderIp, int senderPort) {
        String deviceId = sipMessage.get("deviceId");
        String cmdType = sipMessage.get("CmdType");

        if (StringUtils.isEmpty(deviceId)) {
            logger.warn("MESSAGE请求中缺少设备ID");
            return;
        }

        // 检查设备是否需要强制重新注册
        boolean needForceReregister = deviceService.isDeviceNeedForceReregister(deviceId);
        logger.debug("检查设备强制重注册状态: deviceId={}, needForceReregister={}", deviceId, needForceReregister);

        if (needForceReregister) {
            // 强制设备重新注册：发送401未授权响应
            String callId = StringUtils.defaultString(sipMessage.get("Call-ID"));
            String cseq = sipMessage.get("CSeq");
            if (StringUtils.isEmpty(cseq)) {
                cseq = "1 MESSAGE";
            }
            String from = StringUtils.defaultString(sipMessage.get("From"));
            String to = StringUtils.defaultString(sipMessage.get("To"));
            String via = StringUtils.defaultString(sipMessage.get("Via"));
            String nonce = SipUtils.generateNonce(callId, deviceId);

            String response = sipMessageTemplate.build401Unauthorized(
                    cseq, callId, from, to, via, sipServerConfig.getSipDomain(), nonce);
            sendResponse(ctx, response, senderIp, senderPort);

            logger.warn("设备心跳被拒绝，等待重新注册: deviceId={}, forceReregister=true", deviceId);
            return;
        }

        if ("Keepalive".equals(cmdType)) {
            // 更新设备心跳
            deviceService.updateDeviceHeartbeat(deviceId);

            // 发送200 OK响应
            String cseq = StringUtils.defaultString(sipMessage.get("CSeq"));
            String callId = StringUtils.defaultString(sipMessage.get("Call-ID"));
            String from = StringUtils.defaultString(sipMessage.get("From"));
            String to = StringUtils.defaultString(sipMessage.get("To"));
            String via = StringUtils.defaultString(sipMessage.get("Via"));

            String response = sipMessageTemplate.build200OkKeepalive(
                    cseq, callId, from, to, via);
            sendResponse(ctx, response, senderIp, senderPort);

            logger.debug("处理设备心跳: {}", deviceId);
        }
    }

    /**
     * 处理INVITE推流请求的200 OK响应
     * 
     * 收到设备的200 OK响应后，需要发送ACK确认消息，设备收到ACK后才会开始推流。
     */
    private void handleInviteOkResponse(ChannelHandlerContext ctx, Map<String, String> sipMessage,
            String senderIp, int senderPort) {
        String deviceId = sipMessage.get("deviceId");
        logger.info("收到设备INVITE 200 OK响应: deviceId={}, from={}:{}", deviceId, senderIp, senderPort);

        if (StringUtils.isEmpty(deviceId)) {
            logger.warn("无法从200 OK响应中解析设备ID，跳过ACK发送");
            logger.debug("解析到的sipMessage内容: {}", sipMessage);
            return;
        }

        // 发送ACK确认消息
        String callId = StringUtils.defaultString(sipMessage.get("Call-ID"));
        String from = StringUtils.defaultString(sipMessage.get("From"));
        String to = StringUtils.defaultString(sipMessage.get("To"));
        String deviceLocalIp = StringUtils.defaultString(sipMessage.get("deviceLocalIp"));
        String deviceLocalPort = StringUtils.defaultString(sipMessage.get("deviceLocalPort"));

        logger.debug("解析INVITE响应: callId={}, from={}, to={}", callId, from, to);
        logger.debug("设备本地地址: deviceLocalIp={}, deviceLocalPort={}", deviceLocalIp, deviceLocalPort);

        if (StringUtils.isNotEmpty(deviceLocalIp) && StringUtils.isNotEmpty(deviceLocalPort)) {
            String ackMessage = sipMessageTemplate.buildAckMessage(
                    deviceId, deviceLocalIp, deviceLocalPort, callId,
                    sipServerConfig.getSipIp(), String.valueOf(sipServerConfig.getServerPort()),
                    from, to);

            logger.debug("发送ACK确认消息到 {}:{}", senderIp, senderPort);
            sendResponse(ctx, ackMessage, senderIp, senderPort);

            // 更新设备推流状态
            // 从Subject提取SSRC；如果200 OK不含Subject，保留startStream时已存储的SSRC
            String ssrc = extractSsrcFromSubject(sipMessage.get("Subject"));
            if (ssrc == null) {
                DeviceInfo existingDevice = deviceService.getDeviceInfo(deviceId);
                if (existingDevice != null) {
                    ssrc = existingDevice.getSsrc();
                }
            }
            deviceService.updateDeviceLiveInfo(deviceId, callId, from, to, ssrc);

            logger.info("✓ 设备开始推流: deviceId={}, callId={}, ssrc={}", deviceId, callId, ssrc);
        } else {
            logger.error("✗ 无法发送ACK: 缺少设备本地地址信息, deviceLocalIp={}, deviceLocalPort={}",
                    deviceLocalIp, deviceLocalPort);
            logger.debug("完整的sipMessage内容: {}", sipMessage);
        }
    }

    /**
     * 处理BYE断流请求的200 OK响应
     */
    private void handleByeOkResponse(ChannelHandlerContext ctx, Map<String, String> sipMessage,
            String senderIp, int senderPort) {
        String deviceId = sipMessage.get("deviceId");
        if (StringUtils.isNotEmpty(deviceId)) {
            logger.info("收到设备BYE响应: {}", deviceId);

            // 清除设备推流信息
            deviceService.clearDeviceLiveInfo(deviceId);
        }
    }

    /**
     * 从Subject头部提取SSRC
     * Subject格式: deviceId:ssrc,serverId:0
     * 兼容不同厂商可能的格式差异，直接提取冒号后的完整SSRC字符串
     */
    private String extractSsrcFromSubject(String subject) {
        if (StringUtils.isEmpty(subject)) {
            return null;
        }

        try {
            // 解析Subject: deviceId:ssrc,serverId:0
            String[] parts = subject.split(",");
            if (parts.length > 0) {
                String devicePart = parts[0].trim();
                int colonIndex = devicePart.indexOf(":");
                if (colonIndex > 0 && colonIndex < devicePart.length() - 1) {
                    // 直接取冒号后面的全部内容作为SSRC，不硬编码偏移
                    return devicePart.substring(colonIndex + 1).trim();
                }
            }
        } catch (Exception e) {
            logger.warn("从 Subject 解析 SSRC 失败: {}", subject, e);
        }

        return null;
    }

    /**
     * 注册设备信息
     */
    private void registerDevice(String deviceId, Map<String, String> sipMessage,
            String senderIp, int senderPort) {
        // 校验设备ID格式（GB28181标准为20位数字）
        if (!SipUtils.isValidDeviceId(deviceId)) {
            logger.warn("设备ID格式无效（非20位数字），拒绝注册: {}", deviceId);
            return;
        }

        DeviceInfo deviceInfo = deviceService.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            deviceInfo = new DeviceInfo(deviceId);
        }

        // 更新设备信息
        deviceInfo.setIp(senderIp);
        deviceInfo.setPort(senderPort);
        deviceInfo.setOnline(true);
        deviceInfo.setRegisterTime(System.currentTimeMillis());
        deviceInfo.setExpires(sipServerConfig.getRegisterExpires());

        // 更新心跳时间，避免注册后立即被标记为超时离线
        deviceInfo.updateHeartbeat();

        // 从Contact头部获取设备本地地址
        String contactIp = sipMessage.get("contactIp");
        String contactPort = sipMessage.get("contactPort");
        if (StringUtils.isNotEmpty(contactIp)) {
            deviceInfo.setLocalIp(contactIp);
        }
        if (StringUtils.isNotEmpty(contactPort)) {
            try {
                deviceInfo.setLocalPort(Integer.parseInt(contactPort));
            } catch (NumberFormatException e) {
                logger.warn("解析Contact端口失败: {}", contactPort);
            }
        }

        deviceService.saveDeviceInfo(deviceId, deviceInfo);
    }

    /**
     * 发送响应消息
     * 
     * 注意：ChannelHandlerContext 是线程安全的，可以在线程池中安全调用
     */
    private void sendResponse(ChannelHandlerContext ctx, String response, String targetIp, int targetPort) {
        try {
            InetSocketAddress target = new InetSocketAddress(targetIp, targetPort);
            DatagramPacket packet = new DatagramPacket(
                    Unpooled.copiedBuffer(response, CharsetUtil.UTF_8), target);
            ctx.writeAndFlush(packet);

            logger.debug("发送SIP响应到 {}:{}\n{}", targetIp, targetPort, response.replace("\r\n", "\n"));
        } catch (Exception e) {
            logger.error("发送SIP响应失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("SIP UDP服务器处理异常: {}", cause.getMessage(), cause);
    }
}
