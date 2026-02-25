package com.gb28181.sipserver.controller;

import com.gb28181.sipserver.entity.DeviceInfo;
import com.gb28181.sipserver.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备管理REST接口
 * 
 * 提供设备管理的HTTP API接口，包括：
 * - 获取设备列表
 * - 获取设备详情
 * - 获取设备统计信息
 * - 设备状态管理
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    @Autowired
    private DeviceService deviceService;

    /**
     * 获取所有设备列表
     * 
     * @return 设备列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDevices() {
        logger.debug("获取所有设备列表");

        Map<String, Object> response = new HashMap<>();

        try {
            List<DeviceInfo> devices = deviceService.getAllDevices();

            response.put("success", true);
            response.put("message", "获取设备列表成功");
            response.put("total", devices.size());
            response.put("devices", devices);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取设备列表异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 分页获取设备列表（适用于设备数量较大的场景）
     * 
     * @param page   页码（从0开始）
     * @param size   每页数量（默认50，最大200）
     * @param online 可选，筛选在线状态（true/false）
     * @return 分页设备列表
     */
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getDevicesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Boolean online) {
        logger.debug("分页获取设备列表: page={}, size={}, online={}", page, size, online);

        Map<String, Object> response = new HashMap<>();
        try {
            // 限制每页最大200条，防止恶意请求
            size = Math.min(size, 200);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastHeartbeatTime"));

            Page<DeviceInfo> devicePage;
            if (online != null) {
                devicePage = deviceService.getDevicesByOnlineStatus(online, pageable);
            } else {
                devicePage = deviceService.getDevicesPage(pageable);
            }

            response.put("success", true);
            response.put("devices", devicePage.getContent());
            response.put("total", devicePage.getTotalElements());
            response.put("page", devicePage.getNumber());
            response.put("size", devicePage.getSize());
            response.put("totalPages", devicePage.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("分页获取设备列表异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取在线设备列表
     *
     * @return 在线设备列表
     */
    @GetMapping("/online")
    public ResponseEntity<Map<String, Object>> getOnlineDevices() {
        logger.debug("获取在线设备列表");

        Map<String, Object> response = new HashMap<>();

        try {
            List<DeviceInfo> onlineDevices = deviceService.getOnlineDevices();

            response.put("success", true);
            response.put("message", "获取在线设备列表成功");
            response.put("total", onlineDevices.size());
            response.put("devices", onlineDevices);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取在线设备列表异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取指定设备详情
     * 
     * @param deviceId 设备ID
     * @return 设备详情
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> getDevice(@PathVariable String deviceId) {
        logger.debug("获取设备详情: deviceId={}", deviceId);

        Map<String, Object> response = new HashMap<>();

        try {
            DeviceInfo device = deviceService.getDeviceInfo(deviceId);

            if (device == null) {
                response.put("success", false);
                response.put("message", "设备不存在: " + deviceId);
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("message", "获取设备详情成功");
            response.put("device", device);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取设备详情异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取设备统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDeviceStatistics() {
        logger.debug("获取设备统计信息");

        Map<String, Object> response = new HashMap<>();

        try {
            // 使用数据库COUNT查询，避免全表加载到内存
            DeviceService.DeviceStatistics stats = deviceService.getDeviceStatistics();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalDevices", stats.getTotalCount());
            statistics.put("onlineDevices", stats.getOnlineCount());
            statistics.put("offlineDevices", stats.getOfflineCount());
            statistics.put("onlineRate",
                    stats.getTotalCount() > 0 ? (double) stats.getOnlineCount() / stats.getTotalCount() * 100 : 0);

            response.put("success", true);
            response.put("message", "获取统计信息成功");
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取设备统计信息异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除设备
     * 
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> deleteDevice(@PathVariable String deviceId) {
        logger.info("删除设备: deviceId={}", deviceId);

        Map<String, Object> response = new HashMap<>();

        try {
            DeviceInfo device = deviceService.getDeviceInfo(deviceId);

            if (device == null) {
                response.put("success", false);
                response.put("message", "设备不存在: " + deviceId);
                return ResponseEntity.badRequest().body(response);
            }

            deviceService.removeDeviceInfo(deviceId);

            response.put("success", true);
            response.put("message", "设备删除成功");
            response.put("deviceId", deviceId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("删除设备异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 强制设备下线
     *
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @PostMapping("/{deviceId}/offline")
    public ResponseEntity<Map<String, Object>> forceOffline(@PathVariable String deviceId) {
        logger.info("强制设备下线: deviceId={}", deviceId);

        Map<String, Object> response = new HashMap<>();

        try {
            DeviceInfo device = deviceService.getDeviceInfo(deviceId);

            if (device == null) {
                response.put("success", false);
                response.put("message", "设备不存在: " + deviceId);
                return ResponseEntity.badRequest().body(response);
            }

            // 设置设备离线
            deviceService.setDeviceOnline(deviceId, false);

            response.put("success", true);
            response.put("message", "设备已强制下线");
            response.put("deviceId", deviceId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("强制设备下线异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 强制设备重新注册
     *
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @PostMapping("/{deviceId}/force-reregister")
    public ResponseEntity<Map<String, Object>> forceReregister(@PathVariable String deviceId) {
        logger.info("强制设备重新注册: deviceId={}", deviceId);

        Map<String, Object> response = new HashMap<>();

        try {
            DeviceInfo device = deviceService.getDeviceInfo(deviceId);

            if (device == null) {
                response.put("success", false);
                response.put("message", "设备不存在: " + deviceId);
                return ResponseEntity.badRequest().body(response);
            }

            // 强制设备重新注册
            boolean success = deviceService.forceDeviceReregister(deviceId);

            if (success) {
                response.put("success", true);
                response.put("message", "设备已标记为强制重新注册，下次通信时将要求重新注册");
                response.put("deviceId", deviceId);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "强制重新注册失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("强制设备重新注册异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量强制设备重新注册
     *
     * @param request 批量操作请求体
     * @return 操作结果
     */
    @PostMapping("/batch/force-reregister")
    public ResponseEntity<Map<String, Object>> batchForceReregister(
            @RequestBody BatchForceReregisterRequest request) {
        logger.info("批量强制设备重新注册: deviceIds={}", request.getDeviceIds());

        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getDeviceIds() == null || request.getDeviceIds().isEmpty()) {
                response.put("success", false);
                response.put("message", "设备ID列表不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            int successCount = deviceService.batchForceDeviceReregister(request.getDeviceIds());

            response.put("success", true);
            response.put("message", String.format("批量强制重新注册完成，成功处理 %d/%d 个设备",
                    successCount, request.getDeviceIds().size()));
            response.put("totalCount", request.getDeviceIds().size());
            response.put("successCount", successCount);
            response.put("failureCount", request.getDeviceIds().size() - successCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("批量强制设备重新注册异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 强制所有在线设备重新注册
     *
     * @return 操作结果
     */
    @PostMapping("/all/force-reregister")
    public ResponseEntity<Map<String, Object>> forceAllReregister() {
        logger.info("强制所有在线设备重新注册");

        Map<String, Object> response = new HashMap<>();

        try {
            int successCount = deviceService.forceAllOnlineDevicesReregister();

            response.put("success", true);
            response.put("message", String.format("强制所有在线设备重新注册完成，成功处理 %d 个设备", successCount));
            response.put("successCount", successCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("强制所有在线设备重新注册异常: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量强制重新注册请求类
     */
    public static class BatchForceReregisterRequest {
        private List<String> deviceIds;

        public List<String> getDeviceIds() {
            return deviceIds;
        }

        public void setDeviceIds(List<String> deviceIds) {
            this.deviceIds = deviceIds;
        }
    }
}
