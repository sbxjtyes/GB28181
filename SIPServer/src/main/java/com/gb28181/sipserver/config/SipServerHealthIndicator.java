package com.gb28181.sipserver.config;

import com.gb28181.sipserver.netty.SipUdpServer;
import com.gb28181.sipserver.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义SIP服务器健康检查
 */
@Component
public class SipServerHealthIndicator implements HealthIndicator {

    @Autowired
    private SipUdpServer sipUdpServer;

    @Autowired
    private DeviceService deviceService;

    @Override
    public Health health() {
        DeviceService.DeviceStatistics stats = deviceService.getDeviceStatistics();
        boolean running = sipUdpServer.isRunning();
        Health.Builder builder = running ? Health.up() : Health.down();
        return builder
                .withDetail("sipServerRunning", running)
                .withDetail("listenIp", sipUdpServer.getListenIp())
                .withDetail("listenPort", sipUdpServer.getListenPort())
                .withDetail("totalDevices", stats.getTotalCount())
                .withDetail("onlineDevices", stats.getOnlineCount())
                .build();
    }
}
