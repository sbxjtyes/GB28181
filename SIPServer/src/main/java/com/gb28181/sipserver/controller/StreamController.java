package com.gb28181.sipserver.controller;

import com.gb28181.sipserver.service.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 推流控制REST接口
 * 
 * 提供推流管理的HTTP API接口，包括：
 * - 开始推流
 * - 停止推流
 * - 获取推流状态
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);

    @Autowired
    private StreamService streamService;

    /**
     * 开始推流
     * 
     * @param request 推流请求参数
     * @return 推流结果
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startStream(@RequestBody StreamStartRequest request) {
        logger.info("Received start stream request: deviceId={}, mediaServerIp={}, mediaServerPort={}", 
                   request.getDeviceId(), request.getMediaServerIp(), request.getMediaServerPort());

        Map<String, Object> response = new HashMap<>();

        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "设备ID不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getMediaServerIp() == null || request.getMediaServerIp().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "媒体服务器IP不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getMediaServerPort() <= 0 || request.getMediaServerPort() > 65535) {
                response.put("success", false);
                response.put("message", "媒体服务器端口无效");
                return ResponseEntity.badRequest().body(response);
            }

            // 检查设备是否已在推流
            StreamService.StreamStatus currentStatus = streamService.getStreamStatus(request.getDeviceId());
            if (currentStatus.isStreaming()) {
                response.put("success", true);
                response.put("message", "设备已在推流中");
                response.put("alreadyStreaming", true);
                response.put("deviceId", request.getDeviceId());
                return ResponseEntity.ok(response);
            }

            // 调用推流服务
            boolean success = streamService.startStream(
                    request.getDeviceId(),
                    request.getMediaServerIp(),
                    request.getMediaServerPort(),
                    request.isUseTcp()
            );

            if (success) {
                response.put("success", true);
                response.put("message", "推流请求发送成功");
                response.put("deviceId", request.getDeviceId());
                response.put("mediaServerIp", request.getMediaServerIp());
                response.put("mediaServerPort", request.getMediaServerPort());
                response.put("useTcp", request.isUseTcp());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "推流请求发送失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Start stream exception: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 停止推流
     * 
     * @param request 停止推流请求参数
     * @return 停止推流结果
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopStream(@RequestBody StreamStopRequest request) {
        logger.info("Received stop stream request: deviceId={}", request.getDeviceId());

        Map<String, Object> response = new HashMap<>();

        try {
            // 参数验证
            if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "设备ID不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 调用推流服务
            boolean success = streamService.stopStream(request.getDeviceId());

            if (success) {
                response.put("success", true);
                response.put("message", "停止推流请求发送成功");
                response.put("deviceId", request.getDeviceId());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "停止推流请求发送失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Stop stream exception: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取推流状态
     * 
     * @param deviceId 设备ID
     * @return 推流状态
     */
    @GetMapping("/status/{deviceId}")
    public ResponseEntity<Map<String, Object>> getStreamStatus(@PathVariable String deviceId) {
        logger.debug("Get stream status: deviceId={}", deviceId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 参数验证
            if (deviceId == null || deviceId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "设备ID不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 获取推流状态
            StreamService.StreamStatus status = streamService.getStreamStatus(deviceId);

            response.put("success", true);
            response.put("message", "获取推流状态成功");
            response.put("deviceId", deviceId);
            response.put("online", status.isOnline());
            response.put("streaming", status.isStreaming());
            response.put("callId", status.getCallId());
            response.put("ssrc", status.getSsrc());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Get stream status exception: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 开始推流请求参数
     */
    public static class StreamStartRequest {
        private String deviceId;
        private String mediaServerIp;
        private int mediaServerPort;
        private boolean useTcp = false; // 默认使用UDP

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

        public String getMediaServerIp() { return mediaServerIp; }
        public void setMediaServerIp(String mediaServerIp) { this.mediaServerIp = mediaServerIp; }

        public int getMediaServerPort() { return mediaServerPort; }
        public void setMediaServerPort(int mediaServerPort) { this.mediaServerPort = mediaServerPort; }

        public boolean isUseTcp() { return useTcp; }
        public void setUseTcp(boolean useTcp) { this.useTcp = useTcp; }
    }

    /**
     * 停止推流请求参数
     */
    public static class StreamStopRequest {
        private String deviceId;

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }
}
