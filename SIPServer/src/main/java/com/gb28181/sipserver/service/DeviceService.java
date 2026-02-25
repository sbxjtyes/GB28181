package com.gb28181.sipserver.service;

import com.gb28181.sipserver.config.SipServerConfig;
import com.gb28181.sipserver.entity.DeviceInfo;
import com.gb28181.sipserver.repository.DeviceRepository;
import com.gb28181.sipserver.util.ConsoleHighlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * @param deviceId   设备ID
     * @param deviceInfo 设备信息
     */
    public void saveDeviceInfo(String deviceId, DeviceInfo deviceInfo) {
        try {
            deviceInfo.setDeviceId(deviceId);
            deviceRepository.save(deviceInfo);
            logger.debug("保存设备信息成功: {}", deviceId);
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
     * 分页获取设备列表
     *
     * @param pageable 分页参数
     * @return 分页设备列表
     */
    public Page<DeviceInfo> getDevicesPage(Pageable pageable) {
        return deviceRepository.findAll(pageable);
    }

    /**
     * 按在线状态分页获取设备列表
     *
     * @param online   在线状态
     * @param pageable 分页参数
     * @return 分页设备列表
     */
    public Page<DeviceInfo> getDevicesByOnlineStatus(Boolean online, Pageable pageable) {
        return deviceRepository.findByOnline(online, pageable);
    }

    /**
     * 更新设备心跳
     * 
     * @param deviceId 设备ID
     */
    public void updateDeviceHeartbeat(String deviceId) {
        try {
            int updated = deviceRepository.atomicUpdateHeartbeat(deviceId, System.currentTimeMillis());
            if (updated > 0) {
                logger.debug("更新设备心跳: {}", deviceId);
            }
        } catch (Exception e) {
            logger.error("更新设备心跳失败: {}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 设置设备在线状态
     * 
     * @param deviceId 设备ID
     * @param online   在线状态
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
     * @param live     推流状态
     */
    public void setDeviceLive(String deviceId, boolean live) {
        try {
            deviceRepository.updateLiveStatus(deviceId, live);
            logger.info("设备 {} 推流状态变更为: {}", deviceId, live ? "推流中" : "停止推流");
        } catch (Exception e) {
            logger.error("设置设备推流状态失败: {}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 更新设备推流信息
     *
     * @param deviceId 设备ID
     * @param callId   会话ID
     * @param fromInfo From信息
     * @param toInfo   To信息
     * @param ssrc     SSRC
     */
    public void updateDeviceLiveInfo(String deviceId, String callId, String fromInfo, String toInfo, String ssrc) {
        try {
            int updated = deviceRepository.atomicUpdateLiveInfo(deviceId, callId, fromInfo, toInfo, ssrc);
            if (updated > 0) {
                logger.info("更新设备推流信息: 设备ID={}, 会话ID={}, SSRC={}", deviceId, callId, ssrc);
            }
        } catch (Exception e) {
            logger.error("更新设备推流信息失败: {}, 错误: {}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 清除设备推流信息
     *
     * @param deviceId 设备ID
     */
    public void clearDeviceLiveInfo(String deviceId) {
        try {
            int updated = deviceRepository.atomicClearLiveInfo(deviceId);
            if (updated > 0) {
                logger.info("清除设备推流信息: 设备ID={}", deviceId);
            }
        } catch (Exception e) {
            logger.error("清除设备推流信息失败: {}, 错误: {}", deviceId, e.getMessage(), e);
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
            return new DeviceStatistics((int) totalCount, (int) onlineCount);
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
            int updated = deviceRepository.atomicMarkForceReregister(deviceId, System.currentTimeMillis());
            if (updated == 0) {
                logger.warn("设备不存在，无法强制重新注册: {}", deviceId);
                return false;
            }
            logger.info("设备已标记为强制重新注册（离线）: {}", deviceId);
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
            int updated = deviceRepository.atomicClearForceReregister(deviceId);
            if (updated > 0) {
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
     * 定时清理僵尸推流会话
     * 每30秒执行一次，清理发送了INVITE但超过30秒未收到200 OK的会话
     */
    @Scheduled(fixedRate = 30000) // 30秒
    public void cleanupStaleSessions() {
        try {
            // 仅查询有callId但live=false的设备，避免全表加载
            List<DeviceInfo> staleDevices = deviceRepository.findByLiveCallIDIsNotNullAndLive(false);
            long now = System.currentTimeMillis();
            int cleaned = 0;

            for (DeviceInfo device : staleDevices) {
                // 尝试从CallID中提取时间戳
                try {
                    String callId = device.getLiveCallID();
                    int underscoreIdx = callId.lastIndexOf('_');
                    if (underscoreIdx > 0) {
                        long timestamp = Long.parseLong(callId.substring(underscoreIdx + 1));
                        if (now - timestamp > 30000) { // 超过30秒
                            clearDeviceLiveInfo(device.getDeviceId());
                            cleaned++;
                            logger.info("清理僵尸推流会话: 设备ID={}, callId={}",
                                    device.getDeviceId(), callId);
                        }
                    }
                } catch (NumberFormatException e) {
                    // CallID格式不包含时间戳，直接清理
                    clearDeviceLiveInfo(device.getDeviceId());
                    cleaned++;
                }
            }

            if (cleaned > 0) {
                logger.info("本次清理僵尸推流会话数量: {}", cleaned);
            }
        } catch (Exception e) {
            logger.error("清理僵尸推流会话失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时清理超时设备
     * 每5分钟执行一次
     * 先设置超时设备为离线，再删除长时间超时的设备（避免数据竞争）
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupTimeoutDevices() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeoutThreshold = currentTime - (sipServerConfig.getHeartbeatTimeout() * 1000L);
            long cleanupThreshold = currentTime - (sipServerConfig.getHeartbeatTimeout() * 3 * 1000L);

            // 先设置超时设备为离线状态（超过心跳超时但未达到清理阈值）
            List<DeviceInfo> timeoutDevices = deviceRepository.findTimeoutDevices(timeoutThreshold);
            int offlineCount = 0;
            for (DeviceInfo device : timeoutDevices) {
                if (device.getLastHeartbeatTime() != null && device.getLastHeartbeatTime() >= cleanupThreshold) {
                    deviceRepository.updateOnlineStatus(device.getDeviceId(), false);
                    offlineCount++;
                    logger.info("设备超时，设置为离线: {}", device.getDeviceId());
                }
            }

            // 再删除长时间超时的设备
            int cleanupCount = deviceRepository.deleteTimeoutDevices(cleanupThreshold);

            if (offlineCount > 0 || cleanupCount > 0) {
                logger.info("设备清理完成: 设置离线 {}  个，删除 {} 个", offlineCount, cleanupCount);
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
