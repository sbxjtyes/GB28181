package com.gb28181.sipserver.config;

import com.gb28181.sipserver.service.DeviceService;
import com.gb28181.sipserver.service.SystemMonitorService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 注册Micrometer指标
 */
@Configuration
public class MonitoringConfiguration {

    private final MeterRegistry meterRegistry;
    private final DeviceService deviceService;
    private final SystemMonitorService systemMonitorService;

    public MonitoringConfiguration(MeterRegistry meterRegistry,
                                   DeviceService deviceService,
                                   SystemMonitorService systemMonitorService) {
        this.meterRegistry = meterRegistry;
        this.deviceService = deviceService;
        this.systemMonitorService = systemMonitorService;
    }

    @PostConstruct
    public void bindMetrics() {
        Gauge.builder("sipserver_devices_total", deviceService,
                        ds -> ds.getDeviceStatistics().getTotalCount())
                .description("Total GB28181 devices registered")
                .register(meterRegistry);

        Gauge.builder("sipserver_devices_online", deviceService,
                        ds -> ds.getDeviceStatistics().getOnlineCount())
                .description("Online GB28181 devices")
                .register(meterRegistry);

        Gauge.builder("sipserver_memory_usage_percent", systemMonitorService,
                        svc -> svc.getSystemStatus().getMemoryUsagePercent())
                .description("JVM heap memory usage percent")
                .register(meterRegistry);

        Gauge.builder("sipserver_cpu_usage_percent", systemMonitorService,
                        svc -> svc.getSystemStatus().getCpuUsagePercent())
                .description("Process CPU usage percent")
                .register(meterRegistry);

        Gauge.builder("sipserver_threads_total", systemMonitorService,
                        svc -> svc.getSystemStatus().getThreadCount())
                .description("JVM thread count")
                .register(meterRegistry);
                
        // Log that metrics have been successfully registered
        org.slf4j.LoggerFactory.getLogger(MonitoringConfiguration.class)
            .info("Custom Micrometer metrics registered successfully.");
    }
}
