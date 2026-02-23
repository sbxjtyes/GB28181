package com.gb28181.sipserver.netty;

import com.gb28181.sipserver.config.SipServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * SIP UDP服务器
 * 
 * 基于Netty实现的UDP服务器，用于处理SIP信令消息，包括：
 * - 设备注册请求
 * - 心跳保活消息
 * - 推流控制消息
 * - 其他SIP协议消息
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Component
public class SipUdpServer {

    private static final Logger logger = LoggerFactory.getLogger(SipUdpServer.class);

    @Autowired
    private SipServerConfig sipServerConfig;

    @Autowired
    private SipUdpServerHandler sipUdpServerHandler;

    private EventLoopGroup group;
    private ChannelFuture channelFuture;

    /**
     * 启动SIP UDP服务器
     * 
     * 使用多线程 EventLoopGroup 提高 UDP 消息的 IO 接收能力
     */
    @PostConstruct
    public void start() {
        try {
            // 使用 CPU 核心数作为 IO 线程数，提高消息接收吞吐量
            int ioThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
            group = new NioEventLoopGroup(ioThreads);
            logger.info("Netty EventLoopGroup 初始化，IO线程数: {}", ioThreads);
            Bootstrap bootstrap = new Bootstrap();
            
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .option(ChannelOption.SO_RCVBUF, 1024 * 1024) // 1MB接收缓冲区
                    .option(ChannelOption.SO_SNDBUF, 1024 * 1024) // 1MB发送缓冲区
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(sipUdpServerHandler);
                        }
                    });

            // 绑定端口并启动服务器
            channelFuture = bootstrap.bind(sipServerConfig.getServerIp(), sipServerConfig.getServerPort()).sync();
            
            logger.info("SIP UDP服务器启动成功，监听地址: {}:{}", 
                    sipServerConfig.getServerIp(), sipServerConfig.getServerPort());
            
        } catch (Exception e) {
            logger.error("SIP UDP服务器启动失败: {}", e.getMessage(), e);
            throw new RuntimeException("SIP UDP服务器启动失败", e);
        }
    }

    /**
     * 停止SIP UDP服务器
     */
    @PreDestroy
    public void stop() {
        try {
            if (channelFuture != null && channelFuture.channel().isActive()) {
                channelFuture.channel().close().sync();
            }
            if (group != null) {
                group.shutdownGracefully().sync();
            }
            logger.info("SIP UDP服务器已停止");
        } catch (Exception e) {
            logger.error("停止SIP UDP服务器时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取服务器状态
     * 
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return channelFuture != null && channelFuture.channel().isActive();
    }

    /**
     * 获取监听端口
     * 
     * @return 监听端口
     */
    public int getListenPort() {
        return sipServerConfig.getServerPort();
    }

    /**
     * 获取监听IP
     *
     * @return 监听IP
     */
    public String getListenIp() {
        return sipServerConfig.getServerIp();
    }

    /**
     * 发送SIP消息到指定地址
     *
     * @param message SIP消息内容
     * @param targetIp 目标IP地址
     * @param targetPort 目标端口
     * @return 是否发送成功
     */
    public boolean sendMessage(String message, String targetIp, int targetPort) {
        try {
            if (channelFuture == null || !channelFuture.channel().isActive()) {
                logger.error("SIP UDP服务器未启动，无法发送消息");
                return false;
            }

            InetSocketAddress target = new InetSocketAddress(targetIp, targetPort);
            DatagramPacket packet = new DatagramPacket(
                    Unpooled.copiedBuffer(message, CharsetUtil.UTF_8), target);

            ChannelFuture writeFuture = channelFuture.channel().writeAndFlush(packet);
            boolean completed = writeFuture.await(3000); // 等待最多3秒
            
            if (!completed) {
                logger.warn("发送SIP消息超时: {}:{}", targetIp, targetPort);
                return false;
            }
            if (!writeFuture.isSuccess()) {
                logger.error("发送SIP消息失败: {}:{}, 原因: {}", targetIp, targetPort, 
                    writeFuture.cause() != null ? writeFuture.cause().getMessage() : "未知");
                return false;
            }

            logger.debug("发送SIP消息到 {}:{}\n{}", targetIp, targetPort, message.replace("\r\n", "\n"));
            return true;
        } catch (Exception e) {
            logger.error("发送SIP消息失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取Channel对象（用于其他组件发送消息）
     *
     * @return Channel对象
     */
    public DatagramChannel getChannel() {
        if (channelFuture != null && channelFuture.channel().isActive()) {
            return (DatagramChannel) channelFuture.channel();
        }
        return null;
    }
}
