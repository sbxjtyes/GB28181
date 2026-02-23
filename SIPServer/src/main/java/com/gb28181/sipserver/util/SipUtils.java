package com.gb28181.sipserver.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

/**
 * SIP协议工具类
 * 
 * 提供SIP协议相关的工具方法，包括：
 * - SSRC生成
 * - 时间格式转换
 * - 认证信息生成
 * - 字符串替换工具
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
public class SipUtils {

    private static final Random RANDOM = new Random();
    
    /**
     * 生成SSRC（同步源标识符）
     * 
     * @param prefix SSRC前缀
     * @return 生成的SSRC字符串
     */
    public static String getSsrc(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            prefix = "010000";
        }
        
        // 确保前缀长度为6位
        if (prefix.length() < 6) {
            prefix = StringUtils.leftPad(prefix, 6, '0');
        } else if (prefix.length() > 6) {
            prefix = prefix.substring(0, 6);
        }
        
        // 生成4位随机数
        int randomNum = RANDOM.nextInt(10000);
        String suffix = String.format("%04d", randomNum);
        
        return prefix + suffix;
    }

    /**
     * 生成Call-ID
     * 
     * @param serverIp 服务器IP
     * @return 生成的Call-ID
     */
    public static String getCallID(String serverIp) {
        return UUID.randomUUID().toString().replace("-", "") + "@" + serverIp;
    }

    /**
     * 生成Branch ID
     * 
     * @return 生成的Branch ID
     */
    public static String getBranchId() {
        return "z9hG4bK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 获取GMT时间字符串
     * 
     * @return GMT时间字符串
     */
    public static String getGMT() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    /**
     * 生成SIP认证响应
     * 
     * @param username 用户名
     * @param realm 域
     * @param password 密码
     * @param nonce 随机数
     * @param method 方法
     * @param uri URI
     * @return 认证响应
     */
    public static String generateAuthResponse(String username, String realm, String password, 
                                            String nonce, String method, String uri) {
        // HA1 = MD5(username:realm:password)
        String ha1 = DigestUtils.md5Hex(username + ":" + realm + ":" + password);
        
        // HA2 = MD5(method:uri)
        String ha2 = DigestUtils.md5Hex(method + ":" + uri);
        
        // Response = MD5(HA1:nonce:HA2)
        return DigestUtils.md5Hex(ha1 + ":" + nonce + ":" + ha2);
    }

    /**
     * 验证SIP认证响应
     * 
     * @param username 用户名
     * @param realm 域
     * @param password 密码
     * @param nonce 随机数
     * @param method 方法
     * @param uri URI
     * @param response 客户端响应
     * @return 验证结果
     */
    public static boolean verifyAuthResponse(String username, String realm, String password,
                                           String nonce, String method, String uri, String response) {
        String expectedResponse = generateAuthResponse(username, realm, password, nonce, method, uri);
        return StringUtils.equals(expectedResponse, response);
    }

    /**
     * 生成随机Nonce
     * 
     * @param callId Call-ID
     * @param deviceId 设备ID
     * @return 生成的Nonce
     */
    public static String generateNonce(String callId, String deviceId) {
        return DigestUtils.md5Hex(callId + deviceId + System.currentTimeMillis());
    }

    /**
     * 替换字符串中的占位符
     * 
     * @param template 模板字符串
     * @param placeholder 占位符
     * @param replacement 替换值
     * @return 替换后的字符串
     */
    public static String replaceAll(String template, String placeholder, String replacement) {
        if (StringUtils.isEmpty(template) || StringUtils.isEmpty(placeholder)) {
            return template;
        }
        
        if (replacement == null) {
            replacement = "";
        }
        
        // 转义特殊字符
        String escapedPlaceholder = placeholder;
        if (placeholder.contains("{")) {
            escapedPlaceholder = placeholder.replace("{", "\\{");
        }
        if (placeholder.contains("}")) {
            escapedPlaceholder = escapedPlaceholder.replace("}", "\\}");
        }
        
        return template.replaceAll(escapedPlaceholder, replacement);
    }

    /**
     * 从设备ID生成SSRC后缀
     * 
     * @param deviceId 设备ID
     * @return SSRC后缀
     */
    public static String getSsrcSuffix(String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return "0000";
        }
        
        if (deviceId.length() >= 4) {
            return deviceId.substring(deviceId.length() - 4);
        } else {
            return StringUtils.leftPad(deviceId, 4, '0');
        }
    }

    /**
     * 验证设备ID格式
     * 
     * @param deviceId 设备ID
     * @return 是否有效
     */
    public static boolean isValidDeviceId(String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return false;
        }
        
        // GB28181设备ID应该是20位数字
        return deviceId.matches("\\d{20}");
    }

    /**
     * 验证服务器ID格式
     * 
     * @param serverId 服务器ID
     * @return 是否有效
     */
    public static boolean isValidServerId(String serverId) {
        if (StringUtils.isEmpty(serverId)) {
            return false;
        }
        
        // GB28181服务器ID应该是20位数字
        return serverId.matches("\\d{20}");
    }

    /**
     * 计算内容长度
     * 
     * @param content 内容
     * @return 字节长度
     */
    public static int getContentLength(String content) {
        if (StringUtils.isEmpty(content)) {
            return 0;
        }
        return content.getBytes().length;
    }

    /**
     * 生成Tag
     * 
     * @return 生成的Tag
     */
    public static String generateTag() {
        return String.valueOf(Math.abs(RANDOM.nextLong()));
    }

    /**
     * 解析Via头中的received参数
     * 
     * @param via Via头内容
     * @return received IP地址
     */
    public static String parseReceivedFromVia(String via) {
        if (StringUtils.isEmpty(via)) {
            return null;
        }
        
        String[] parts = via.split(";");
        for (String part : parts) {
            if (part.trim().startsWith("received=")) {
                return part.trim().substring("received=".length());
            }
        }
        return null;
    }

    /**
     * 解析Via头中的rport参数
     * 
     * @param via Via头内容
     * @return rport端口号
     */
    public static Integer parseRportFromVia(String via) {
        if (StringUtils.isEmpty(via)) {
            return null;
        }
        
        String[] parts = via.split(";");
        for (String part : parts) {
            if (part.trim().startsWith("rport=")) {
                try {
                    return Integer.parseInt(part.trim().substring("rport=".length()));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
