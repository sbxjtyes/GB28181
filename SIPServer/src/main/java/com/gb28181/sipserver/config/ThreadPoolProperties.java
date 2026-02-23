package com.gb28181.sipserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池参数配置
 */
@Component
@ConfigurationProperties(prefix = "app.thread-pool")
public class ThreadPoolProperties {

    private ExecutorProperties async = new ExecutorProperties(10, 50, 1000, 60);
    private ExecutorProperties sipMessage = new ExecutorProperties(5, 20, 500, 60);
    private ExecutorProperties media = new ExecutorProperties(8, 32, 2000, 60);
    private ExecutorProperties push = new ExecutorProperties(4, 16, 100, 300);

    public ExecutorProperties getAsync() {
        return async;
    }

    public void setAsync(ExecutorProperties async) {
        this.async = async;
    }

    public ExecutorProperties getSipMessage() {
        return sipMessage;
    }

    public void setSipMessage(ExecutorProperties sipMessage) {
        this.sipMessage = sipMessage;
    }

    public ExecutorProperties getMedia() {
        return media;
    }

    public void setMedia(ExecutorProperties media) {
        this.media = media;
    }

    public ExecutorProperties getPush() {
        return push;
    }

    public void setPush(ExecutorProperties push) {
        this.push = push;
    }

    public static class ExecutorProperties {
        private int coreSize;
        private int maxSize;
        private int queueCapacity;
        private int keepAliveSeconds;

        public ExecutorProperties() {
        }

        public ExecutorProperties(int coreSize, int maxSize, int queueCapacity, int keepAliveSeconds) {
            this.coreSize = coreSize;
            this.maxSize = maxSize;
            this.queueCapacity = queueCapacity;
            this.keepAliveSeconds = keepAliveSeconds;
        }

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }
}
