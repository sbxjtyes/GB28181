package com.gb28181.sipserver.service;


import com.gb28181.sipserver.netty.SipUdpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

/**
 * 系统监控服务
 * 
 * 监控系统运行状态，包括：
 * - 内存使用情况
 * - CPU使用情况
 * - 线程状态
 * - 服务运行状态
 * - 设备连接统计
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Service
public class SystemMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(SystemMonitorService.class);

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SipUdpServer sipUdpServer;



    // JMX Bean
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /**
     * 定时监控系统状态
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void monitorSystemStatus() {
        try {
            SystemStatus status = getSystemStatus();
            logger.info("系统状态监控: {}", status);
            
            // 检查内存使用率
            if (status.getMemoryUsagePercent() > 80) {
                logger.warn("内存使用率过高: {}%", status.getMemoryUsagePercent());
            }
            
            // 检查线程数
            if (status.getThreadCount() > 200) {
                logger.warn("线程数量过多: {}", status.getThreadCount());
            }
            
        } catch (Exception e) {
            logger.error("系统状态监控失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时清理资源
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupResources() {
        try {
            logger.info("开始系统资源清理...");
            
            // 清理超时设备
            deviceService.cleanupTimeoutDevices();
            
            // 建议垃圾回收
            System.gc();
            
            logger.info("系统资源清理完成");
            
        } catch (Exception e) {
            logger.error("系统资源清理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取系统状态
     * 
     * @return 系统状态对象
     */
    public SystemStatus getSystemStatus() {
        SystemStatus status = new SystemStatus();
        
        // 内存信息
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        status.setUsedMemory(usedMemory);
        status.setMaxMemory(maxMemory);
        status.setMemoryUsagePercent((double) usedMemory / maxMemory * 100);
        
        // CPU信息
        double cpuUsage = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            cpuUsage = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
        }
        status.setCpuUsagePercent(cpuUsage);
        status.setAvailableProcessors(osBean.getAvailableProcessors());
        
        // 线程信息
        status.setThreadCount(threadBean.getThreadCount());
        status.setDaemonThreadCount(threadBean.getDaemonThreadCount());
        
        // 服务状态
        status.setSipServerRunning(sipUdpServer.isRunning());
        
        // 设备统计
        DeviceService.DeviceStatistics deviceStats = deviceService.getDeviceStatistics();
        status.setTotalDevices(deviceStats.getTotalCount());
        status.setOnlineDevices(deviceStats.getOnlineCount());
        

        
        return status;
    }

    /**
     * 获取详细的内存信息
     * 
     * @return 内存信息对象
     */
    public MemoryInfo getMemoryInfo() {
        MemoryInfo memoryInfo = new MemoryInfo();
        
        // 堆内存
        memoryInfo.setHeapUsed(memoryBean.getHeapMemoryUsage().getUsed());
        memoryInfo.setHeapMax(memoryBean.getHeapMemoryUsage().getMax());
        memoryInfo.setHeapCommitted(memoryBean.getHeapMemoryUsage().getCommitted());
        
        // 非堆内存
        memoryInfo.setNonHeapUsed(memoryBean.getNonHeapMemoryUsage().getUsed());
        memoryInfo.setNonHeapMax(memoryBean.getNonHeapMemoryUsage().getMax());
        memoryInfo.setNonHeapCommitted(memoryBean.getNonHeapMemoryUsage().getCommitted());
        
        return memoryInfo;
    }

    /**
     * 系统状态信息类
     */
    public static class SystemStatus {
        private long usedMemory;
        private long maxMemory;
        private double memoryUsagePercent;
        private double cpuUsagePercent;
        private int availableProcessors;
        private int threadCount;
        private int daemonThreadCount;
        private boolean sipServerRunning;
        private int totalDevices;
        private int onlineDevices;


        // Getter and Setter methods
        public long getUsedMemory() {
            return usedMemory;
        }

        public void setUsedMemory(long usedMemory) {
            this.usedMemory = usedMemory;
        }

        public long getMaxMemory() {
            return maxMemory;
        }

        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }

        public double getMemoryUsagePercent() {
            return memoryUsagePercent;
        }

        public void setMemoryUsagePercent(double memoryUsagePercent) {
            this.memoryUsagePercent = memoryUsagePercent;
        }

        public double getCpuUsagePercent() {
            return cpuUsagePercent;
        }

        public void setCpuUsagePercent(double cpuUsagePercent) {
            this.cpuUsagePercent = cpuUsagePercent;
        }

        public int getAvailableProcessors() {
            return availableProcessors;
        }

        public void setAvailableProcessors(int availableProcessors) {
            this.availableProcessors = availableProcessors;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public int getDaemonThreadCount() {
            return daemonThreadCount;
        }

        public void setDaemonThreadCount(int daemonThreadCount) {
            this.daemonThreadCount = daemonThreadCount;
        }

        public boolean isSipServerRunning() {
            return sipServerRunning;
        }

        public void setSipServerRunning(boolean sipServerRunning) {
            this.sipServerRunning = sipServerRunning;
        }

        public int getTotalDevices() {
            return totalDevices;
        }

        public void setTotalDevices(int totalDevices) {
            this.totalDevices = totalDevices;
        }

        public int getOnlineDevices() {
            return onlineDevices;
        }

        public void setOnlineDevices(int onlineDevices) {
            this.onlineDevices = onlineDevices;
        }



        @Override
        public String toString() {
            return String.format("SystemStatus{内存使用率=%.1f%%, CPU使用率=%.1f%%, 线程数=%d, " +
                            "SIP服务器=%s, 设备总数=%d, 在线设备=%d}",
                    memoryUsagePercent, cpuUsagePercent, threadCount,
                    sipServerRunning ? "运行中" : "已停止", totalDevices, onlineDevices);
        }
    }

    /**
     * 内存信息类
     */
    public static class MemoryInfo {
        private long heapUsed;
        private long heapMax;
        private long heapCommitted;
        private long nonHeapUsed;
        private long nonHeapMax;
        private long nonHeapCommitted;

        // Getter and Setter methods
        public long getHeapUsed() {
            return heapUsed;
        }

        public void setHeapUsed(long heapUsed) {
            this.heapUsed = heapUsed;
        }

        public long getHeapMax() {
            return heapMax;
        }

        public void setHeapMax(long heapMax) {
            this.heapMax = heapMax;
        }

        public long getHeapCommitted() {
            return heapCommitted;
        }

        public void setHeapCommitted(long heapCommitted) {
            this.heapCommitted = heapCommitted;
        }

        public long getNonHeapUsed() {
            return nonHeapUsed;
        }

        public void setNonHeapUsed(long nonHeapUsed) {
            this.nonHeapUsed = nonHeapUsed;
        }

        public long getNonHeapMax() {
            return nonHeapMax;
        }

        public void setNonHeapMax(long nonHeapMax) {
            this.nonHeapMax = nonHeapMax;
        }

        public long getNonHeapCommitted() {
            return nonHeapCommitted;
        }

        public void setNonHeapCommitted(long nonHeapCommitted) {
            this.nonHeapCommitted = nonHeapCommitted;
        }
    }
}
