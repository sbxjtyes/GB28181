package com.gb28181.sipserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 网络工具类
 * 
 * 提供网络相关的工具方法，包括：
 * - IP地址获取和验证
 * - 端口可用性检测
 * - 网络连通性测试
 * - 网络接口信息获取
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
public class NetworkUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * 获取本机IP地址
     * 
     * @return 本机IP地址
     */
    public static String getLocalIpAddress() {
        try {
            // 优先获取非回环地址
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // 只返回IPv4地址，且不是回环地址
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            
            // 如果没有找到合适的地址，返回localhost
            return InetAddress.getLocalHost().getHostAddress();
            
        } catch (Exception e) {
            logger.error("获取本机IP地址失败: {}", e.getMessage(), e);
            return "127.0.0.1";
        }
    }

    /**
     * 获取所有本机IP地址
     * 
     * @return IP地址列表
     */
    public static List<String> getAllLocalIpAddresses() {
        List<String> ipList = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // 只添加IPv4地址
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        ipList.add(address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取所有本机IP地址失败: {}", e.getMessage(), e);
        }
        
        return ipList;
    }

    /**
     * 检查端口是否可用
     * 
     * @param port 端口号
     * @return 是否可用
     */
    public static boolean isPortAvailable(int port) {
        return isPortAvailable("localhost", port);
    }

    /**
     * 检查指定主机的端口是否可用
     * 
     * @param host 主机地址
     * @param port 端口号
     * @return 是否可用
     */
    public static boolean isPortAvailable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return false; // 能连接说明端口被占用
        } catch (Exception e) {
            return true; // 连接失败说明端口可用
        }
    }

    /**
     * 获取可用的端口
     * 
     * @param startPort 起始端口
     * @param endPort 结束端口
     * @return 可用端口，如果没有可用端口返回-1
     */
    public static int getAvailablePort(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1;
    }

    /**
     * 获取随机可用端口
     * 
     * @return 可用端口
     */
    public static int getRandomAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            logger.error("获取随机可用端口失败: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 验证IP地址格式
     * 
     * @param ip IP地址字符串
     * @return 是否为有效的IP地址
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address instanceof Inet4Address;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证端口号范围
     * 
     * @param port 端口号
     * @return 是否为有效的端口号
     */
    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    /**
     * 测试网络连通性
     * 
     * @param host 目标主机
     * @param port 目标端口
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否连通
     */
    public static boolean testConnection(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            logger.info("连接测试失败 {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * Ping测试
     * 
     * @param host 目标主机
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否可达
     */
    public static boolean ping(String host, int timeoutMs) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeoutMs);
        } catch (Exception e) {
            logger.info("Ping测试失败 {} - {}", host, e.getMessage());
            return false;
        }
    }

    /**
     * 解析主机名为IP地址
     * 
     * @param hostname 主机名
     * @return IP地址，解析失败返回null
     */
    public static String resolveHostname(String hostname) {
        try {
            InetAddress address = InetAddress.getByName(hostname);
            return address.getHostAddress();
        } catch (Exception e) {
            logger.error("解析主机名失败: {} - {}", hostname, e.getMessage());
            return null;
        }
    }

    /**
     * 获取网络接口信息
     * 
     * @return 网络接口信息列表
     */
    public static List<NetworkInterfaceInfo> getNetworkInterfaces() {
        List<NetworkInterfaceInfo> interfaceList = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                NetworkInterfaceInfo info = new NetworkInterfaceInfo();
                info.setName(networkInterface.getName());
                info.setDisplayName(networkInterface.getDisplayName());
                info.setLoopback(networkInterface.isLoopback());
                info.setUp(networkInterface.isUp());
                
                List<String> addresses = new ArrayList<>();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    if (address instanceof Inet4Address) {
                        addresses.add(address.getHostAddress());
                    }
                }
                info.setIpAddresses(addresses);
                
                interfaceList.add(info);
            }
        } catch (Exception e) {
            logger.error("获取网络接口信息失败: {}", e.getMessage(), e);
        }
        
        return interfaceList;
    }

    /**
     * 网络接口信息类
     */
    public static class NetworkInterfaceInfo {
        private String name;
        private String displayName;
        private boolean isLoopback;
        private boolean isUp;
        private List<String> ipAddresses;

        // Getter and Setter methods
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isLoopback() {
            return isLoopback;
        }

        public void setLoopback(boolean loopback) {
            isLoopback = loopback;
        }

        public boolean isUp() {
            return isUp;
        }

        public void setUp(boolean up) {
            isUp = up;
        }

        public List<String> getIpAddresses() {
            return ipAddresses;
        }

        public void setIpAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
        }

        @Override
        public String toString() {
            return "NetworkInterfaceInfo{" +
                    "name='" + name + '\'' +
                    ", displayName='" + displayName + '\'' +
                    ", isLoopback=" + isLoopback +
                    ", isUp=" + isUp +
                    ", ipAddresses=" + ipAddresses +
                    '}';
        }
    }
}
