package com.gb28181.sipserver.service;

import com.gb28181.sipserver.config.SipServerConfig;
import com.gb28181.sipserver.entity.DeviceInfo;
import com.gb28181.sipserver.repository.DeviceRepository;
import com.gb28181.sipserver.util.ConsoleHighlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 设备管理服务
 * 
 * 负责设备信息的管理，包括：
 * - 设备信息的存储和获取
 * - 设备在线状态管理
 * - 设备超时检测和清理
 * - 设备统计信息
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Service
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    @Autowired
    private SipServerConfig sipServerConfig;

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 保存设备信息
     *
     * @param deviceId 设备ID
     * @param deviceInfo 设备信息
     */
    public void saveDeviceInfo(String deviceId, DeviceInfo deviceInfo) {
        try {
            deviceInfo.setDeviceId(deviceId);
            deviceRepository.save(deviceInfo);
            logger.info("保存设备信息成功: {}", deviceId);
        } catch (Exception e) {
            logger.error("保存设备信息失败: {}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 获取设备信息
     *
     * @param deviceId 设备ID
     * @return 设备信息
     */
    public DeviceInfo getDeviceInfo(String deviceId) {
        try {
            Optional<DeviceInfo> deviceInfo = deviceRepository.findById(deviceId);
            return deviceInfo.orElse(null);
        } catch (Exception e) {
            logger.error("获取设备信息失败: {}, 错误: {}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除设备信息
     *
     * @param deviceId 设备ID
     */
    public void removeDeviceInfo(String deviceId) {
        try {
            deviceRepository.deleteById(deviceId);
            logger.info("删除设备信息: {}", deviceId);
        } catch (Exception e) {
            logger.error("删除设备信息失败: {}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 获取所有设备信息
     *
     * @return 设备信息列表
     */
    public List<DeviceInfo> getAllDevices() {
        try {
            return deviceRepository.findAll();
        } catch (Exception e) {
            logger.error("获取所有设备信息失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取在线设备列表
     *
     * @return 在线设备列表
     */
    public List<DeviceInfo> getOnlineDevices() {
        try {
            return deviceRepository.findByOnline(true);
        } catch (Exception e) {
            logger.error("获取在线设备列表失败: {}", e.getMessage(), e);
            return List.of();
        }
    }



    /**
     * 更新设备心跳
     * 
     * @param deviceId 设备ID
     */
    public void updateDeviceHeartbeat(String deviceId) {
        DeviceInfo deviceInfo = getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            deviceInfo.updateHeartbeat();
            saveDeviceInfo(deviceId, deviceInfo);
            logger.info("更新设备心跳: {}", deviceId);
        }
    }

    /**
     * 设置设备在线状态
     * 
     * @param deviceId 设备ID
     * @param online 在线状态
     */
    public void setDeviceOnline(String deviceId, boolean online) {
        DeviceInfo deviceInfo = getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            deviceInfo.setOnline(online);
            if (online) {
                deviceInfo.updateHeartbeat();
            }
            saveDeviceInfo(deviceId, deviceInfo);
            logger.info("设备 {} 状态变更为: {}", deviceId, online ? "在线" : "离线");
        }
    }

    /**
     * 设置设备推流状态
     *
     * @param deviceId 设备ID
     * @param live 推流状态
     */
    public void setDeviceLive(String deviceId, boolean live) {
        DeviceInfo deviceInfo = getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            deviceInfo.setLive(live);
            saveDeviceInfo(deviceId, deviceInfo);
            logger.info("设备 {} 推流状态变更为: {}", deviceId, live ? "推流中" : "停止推流");
        }
    }

    /**
     * 更新设备推流信息
     *
     * @param deviceId 设备ID
     * @param callId 会话ID
     * @param fromInfo From信息
     * @param toInfo To信息
     * @param ssrc SSRC
     */
    public void updateDeviceLiveInfo(String deviceId, String callId, String fromInfo, String toInfo, String ssrc) {
        DeviceInfo deviceInfo = getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            deviceInfo.setLiveCallID(callId);
            deviceInfo.setLiveFromInfo(fromInfo);
            deviceInfo.setLiveToInfo(toInfo);
            deviceInfo.setSsrc(ssrc);
            deviceInfo.setLive(true);
            saveDeviceInfo(deviceId, deviceInfo);
            logger.info("更新设备推流信息: 设备ID={}, 会话ID={}, SSRC={}", deviceId, callId, ssrc);
        }
    }

    /**
     * 清除设备推流信息
     *
     * @param deviceId 设备ID
     */
    public void clearDeviceLiveInfo(String deviceId) {
        DeviceInfo deviceInfo = getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            deviceInfo.setLiveCallID(null);
            deviceInfo.setLiveFromInfo(null);
            deviceInfo.setLiveToInfo(null);
            deviceInfo.setSsrc(null);
            deviceInfo.setLive(false);
            saveDeviceInfo(deviceId, deviceInfo);
            logger.info("清除设备推流信息: 设备ID={}", deviceId);
        }
    }

    /**
     * 获取设备统计信息
     *
     * @return 统计信息Map
     */
    public DeviceStatistics getDeviceStatistics() {
        try {
            long totalCount = deviceRepository.count();
            long onlineCount = deviceRepository.countOnlineDevices();
            return new DeviceStatistics((int)totalCount, (int)onlineCount);
        } catch (Exception e) {
            logger.error("获取设备统计信息失败: {}", e.getMessage(), e);
            return new DeviceStatistics(0, 0);
        }
    }

    /**
     * 强制设备重新注册
     *
     * @param deviceId 设备ID
     * @return 操作是否成功
     */
    public boolean forceDeviceReregister(String deviceId) {
        try {
            DeviceInfo deviceInfo = getDeviceInfo(deviceId);
            if (deviceInfo == null) {
                logger.warn("设备不存在，无法强制重新注册: {}", deviceId);
                return false;
            }

            // 标记设备需要强制重新注册，同时标记为离线
            deviceInfo.markForceReregister();
            deviceInfo.setOnline(false);  // 同时标记为离线，确保设备需要重新注册
            saveDeviceInfo(deviceId, deviceInfo);
            
            logger.info("设备已标记为强制重新注册（离线）: {}, forceReregister={}", 
                deviceId, deviceInfo.needForceReregister());
            return true;
        } catch (Exception e) {
            logger.error("强制设备重新注册失败: {}, 错误: {}", deviceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量强制设备重新注册
     *
     * @param deviceIds 设备ID列表
     * @return 成功处理的设备数量
     */
    public int batchForceDeviceReregister(List<String> deviceIds) {
        int successCount = 0;
        for (String deviceId : deviceIds) {
            if (forceDeviceReregister(deviceId)) {
                successCount++;
            }
        }
        logger.info("批量强制重新注册完成: 成功 {}/{} 个设备", successCount, deviceIds.size());
        return successCount;
    }

    /**
     * 强制所有在线设备重新注册
     *
     * @return 成功处理的设备数量
     */
    public int forceAllOnlineDevicesReregister() {
        try {
            List<DeviceInfo> onlineDevices = getOnlineDevices();
            List<String> deviceIds = onlineDevices.stream()
                    .map(DeviceInfo::getDeviceId)
                    .collect(Collectors.toList());
            
            int successCount = batchForceDeviceReregister(deviceIds);
            logger.info("强制所有在线设备重新注册完成: 成功 {}/{} 个设备", successCount, deviceIds.size());
            return successCount;
        } catch (Exception e) {
            logger.error("强制所有在线设备重新注册失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 处理设备注册成功（清除强制重新注册标记）
     *
     * @param deviceId 设备ID
     */
    public void handleDeviceRegistered(String deviceId) {
        try {
            DeviceInfo deviceInfo = getDeviceInfo(deviceId);
            if (deviceInfo != null && deviceInfo.needForceReregister()) {
                deviceInfo.clearForceReregister();
                saveDeviceInfo(deviceId, deviceInfo);
                // 使用高亮显示设备注册成功信息
                logger.info(ConsoleHighlight.safeColor("设备注册成功，清除强制重新注册标记: " + deviceId, 
                    ConsoleHighlight.BRIGHT_GREEN));
            }
        } catch (Exception e) {
            logger.error("处理设备注册成功失败: {}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 检查设备是否需要强制重新注册
     *
     * @param deviceId 设备ID
     * @return 是否需要强制重新注册
     */
    public boolean isDeviceNeedForceReregister(String deviceId) {
        try {
            DeviceInfo deviceInfo = getDeviceInfo(deviceId);
            return deviceInfo != null && deviceInfo.needForceReregister();
        } catch (Exception e) {
            logger.error("检查设备强制重新注册状态失败: {}, 错误: {}", deviceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 定时清理超时设备
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupTimeoutDevices() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeoutThreshold = currentTime - (sipServerConfig.getHeartbeatTimeout() * 1000L);
            long cleanupThreshold = currentTime - (sipServerConfig.getHeartbeatTimeout() * 3 * 1000L);

            // 删除超时设备
            int cleanupCount = deviceRepository.deleteTimeoutDevices(cleanupThreshold);

            // 设置超时设备为离线状态
            List<DeviceInfo> timeoutDevices = deviceRepository.findTimeoutDevices(timeoutThreshold);
            for (DeviceInfo device : timeoutDevices) {
                if (device.getLastHeartbeatTime() >= cleanupThreshold) { // 未达到清理阈值
                    deviceRepository.updateOnlineStatus(device.getDeviceId(), false);
                    logger.info("设备超时，设置为离线: {}", device.getDeviceId());
                }
            }

            if (cleanupCount > 0) {
                logger.info("本次清理超时设备数量: {}", cleanupCount);
            }
        } catch (Exception e) {
            logger.error("清理超时设备失败: {}", e.getMessage(), e);
        }
    }



    /**
     * 设备统计信息内部类
     */
    public static class DeviceStatistics {
        private final int totalCount;
        private final int onlineCount;

        public DeviceStatistics(int totalCount, int onlineCount) {
            this.totalCount = totalCount;
            this.onlineCount = onlineCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getOnlineCount() {
            return onlineCount;
        }

        public int getOfflineCount() {
            return totalCount - onlineCount;
        }

        @Override
        public String toString() {
            return "DeviceStatistics{" +
                    "totalCount=" + totalCount +
                    ", onlineCount=" + onlineCount +
                    ", offlineCount=" + getOfflineCount() +
                    '}';
        }
    }
}
