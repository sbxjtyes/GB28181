package com.gb28181.sipserver.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * SIP消息解析服务
 * 
 * 负责解析SIP协议消息，包括：
 * - 请求消息解析（REGISTER、INVITE、MESSAGE等）
 * - 响应消息解析
 * - 消息头解析
 * - 消息体解析
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
@Service
public class SipMessageParser {

    private static final Logger logger = LoggerFactory.getLogger(SipMessageParser.class);

    /**
     * 解析SIP消息
     * 
     * @param message SIP消息字符串
     * @return 解析结果Map
     */
    public Map<String, String> parseSipMessage(String message) {
        Map<String, String> result = new HashMap<>();

        if (StringUtils.isEmpty(message)) {
            return result;
        }

        StringBuilder bodyBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new StringReader(message))) {

            String line;
            int lineCount = 0;
            boolean isMessageBody = false;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                lineCount++;

                // 空行表示消息头结束，消息体开始
                if (StringUtils.isEmpty(trimmed)) {
                    isMessageBody = true;
                    continue;
                }

                // 收集消息体行（保留原始行以保持XML结构完整）
                if (isMessageBody) {
                    bodyBuilder.append(line).append("\n");
                    continue;
                }

                // 解析第一行（请求行或状态行）
                if (lineCount == 1) {
                    parseFirstLine(trimmed, result);
                    continue;
                }

                // 解析消息头
                parseHeader(trimmed, result);
            }

        } catch (Exception e) {
            logger.error("解析SIP消息失败: {}", e.getMessage(), e);
        }

        // 使用DOM解析完整的XML消息体
        String bodyContent = bodyBuilder.toString().trim();
        if (StringUtils.isNotEmpty(bodyContent)) {
            parseMessageBodyXml(bodyContent, result);
        }

        // 处理设备ID的特殊情况
        processDeviceId(result);

        return result;
    }

    /**
     * 解析第一行（请求行或状态行）
     * 
     * @param line   第一行内容
     * @param result 解析结果
     */
    private void parseFirstLine(String line, Map<String, String> result) {
        if (line.endsWith("SIP/2.0")) {
            // 请求消息：METHOD sip:uri SIP/2.0
            String[] parts = line.split("\\s+");
            if (parts.length >= 3) {
                result.put("method", parts[0].trim());
                result.put("uri", parts[1].trim());
                result.put("messageType", "REQUEST");
            }
        } else if (line.startsWith("SIP/2.0")) {
            // 响应消息：SIP/2.0 statusCode reasonPhrase
            String[] parts = line.split("\\s+", 3);
            if (parts.length >= 2) {
                result.put("stateCode", parts[1].trim());
                if (parts.length >= 3) {
                    result.put("reasonPhrase", parts[2].trim());
                }
                result.put("messageType", "RESPONSE");
            }
        }
    }

    /**
     * 解析消息头
     * 
     * @param line   消息头行
     * @param result 解析结果
     */
    private void parseHeader(String line, Map<String, String> result) {
        if (!line.contains(":")) {
            return;
        }

        // 防御性检查：排除误入的SIP请求行/状态行
        if (line.startsWith("SIP/2.0") || line.endsWith("SIP/2.0")) {
            return;
        }

        // 兼容 "Header: value" 和 "Header:value" 两种格式
        String[] parts = line.split(":\\s*", 2);
        if (parts.length != 2) {
            return;
        }

        String headerName = parts[0].trim();
        String headerValue = parts[1].trim();

        result.put(headerName, headerValue);

        // 特殊处理某些头部
        switch (headerName) {
            case "From":
                parseFromHeader(headerValue, result);
                break;
            case "To":
                parseToHeader(headerValue, result);
                break;
            case "Contact":
                parseContactHeader(headerValue, result);
                break;
            case "Via":
                parseViaHeader(headerValue, result);
                break;
            case "Authorization":
                parseAuthorizationHeader(headerValue, result);
                break;
        }
    }

    /**
     * 解析From头部
     * 
     * @param fromValue From头部值
     * @param result    解析结果
     */
    private void parseFromHeader(String fromValue, Map<String, String> result) {
        if ("REQUEST".equals(result.get("messageType"))) {
            // 从From头部提取设备ID
            try {
                String deviceId = fromValue.split(";")[0];
                deviceId = deviceId.split(":")[1];
                deviceId = deviceId.replace(">", "");
                deviceId = deviceId.split("@")[0];
                result.put("deviceId", deviceId);
            } catch (Exception e) {
                logger.warn("解析From头部设备ID失败: {}", fromValue);
            }
        }
    }

    /**
     * 解析To头部
     * 
     * 注意：此方法在解析时不再依赖Via头部判断，因为Via可能在To之后才被解析。
     * 对于RESPONSE类型的消息，始终尝试解析deviceLocalIp和deviceLocalPort。
     * BYE响应的特殊处理会在后续的postProcessResponse中完成。
     * 
     * @param toValue To头部值
     * @param result  解析结果
     */
    private void parseToHeader(String toValue, Map<String, String> result) {
        if ("RESPONSE".equals(result.get("messageType"))) {
            try {
                // 尝试解析格式: "deviceId" <sip:deviceId@ip:port>
                String[] parts = toValue.split("\\s+");
                String deviceId = parts[0].replaceAll("\"", "");
                result.put("deviceId", deviceId);

                // 尝试解析设备本地IP和端口
                if (parts.length > 1) {
                    String deviceLocalInfo = parts[1].split(";")[0];
                    if (deviceLocalInfo.contains("@")) {
                        deviceLocalInfo = deviceLocalInfo.split("@")[1];
                        deviceLocalInfo = deviceLocalInfo.replace(">", "");
                        String[] ipPort = deviceLocalInfo.split(":");
                        if (ipPort.length == 2) {
                            result.put("deviceLocalIp", ipPort[0]);
                            result.put("deviceLocalPort", ipPort[1]);
                            logger.debug("从To头部解析到设备本地地址: ip={}, port={}", ipPort[0], ipPort[1]);
                        }
                    }
                }

                // 如果上面的解析方式失败，尝试另一种格式: <sip:deviceId@ip:port>
                if (!result.containsKey("deviceLocalIp") && toValue.contains("<sip:")) {
                    String sipUri = toValue;
                    int start = sipUri.indexOf("<sip:");
                    int end = sipUri.indexOf(">");
                    if (start >= 0 && end > start) {
                        sipUri = sipUri.substring(start + 5, end);
                        sipUri = sipUri.split(";")[0]; // 去掉参数
                        if (sipUri.contains("@")) {
                            String[] uriParts = sipUri.split("@");
                            if (uriParts.length == 2) {
                                // 更新deviceId
                                if (StringUtils.isEmpty(result.get("deviceId")) || result.get("deviceId").isEmpty()) {
                                    result.put("deviceId", uriParts[0]);
                                }
                                // 解析IP和端口
                                String[] ipPort = uriParts[1].split(":");
                                if (ipPort.length == 2) {
                                    result.put("deviceLocalIp", ipPort[0]);
                                    result.put("deviceLocalPort", ipPort[1]);
                                    logger.debug("从To头部（备用解析）解析到设备本地地址: ip={}, port={}", ipPort[0], ipPort[1]);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("解析To头部失败: {}, 错误: {}", toValue, e.getMessage());
            }
        }
    }

    /**
     * 解析Contact头部
     * 
     * @param contactValue Contact头部值
     * @param result       解析结果
     */
    private void parseContactHeader(String contactValue, Map<String, String> result) {
        try {
            String contact = contactValue.split("@")[1];
            contact = contact.replace(">", "");
            String[] ipPort = contact.split(":");
            if (ipPort.length == 2) {
                result.put("contactIp", ipPort[0]);
                result.put("contactPort", ipPort[1]);
            }
        } catch (Exception e) {
            logger.warn("解析Contact头部失败: {}", contactValue);
        }
    }

    /**
     * 解析Via头部
     * 
     * @param viaValue Via头部值
     * @param result   解析结果
     */
    private void parseViaHeader(String viaValue, Map<String, String> result) {
        // Via头部包含网络路径信息，通常在响应时需要原样返回
        result.put("viaProtocol", "UDP"); // 默认UDP

        try {
            String[] parts = viaValue.split("\\s+");
            if (parts.length > 1) {
                String address = parts[1].split(";")[0];
                String[] ipPort = address.split(":");
                if (ipPort.length == 2) {
                    result.put("viaIp", ipPort[0]);
                    result.put("viaPort", ipPort[1]);
                }
            }
        } catch (Exception e) {
            logger.warn("解析Via头部失败: {}", viaValue);
        }
    }

    /**
     * 解析Authorization头部
     * 
     * @param authValue Authorization头部值
     * @param result    解析结果
     */
    private void parseAuthorizationHeader(String authValue, Map<String, String> result) {
        if (!authValue.startsWith("Digest ")) {
            return;
        }

        String digestInfo = authValue.substring("Digest ".length());
        String[] params = digestInfo.split(",");

        for (String param : params) {
            String[] kv = param.trim().split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim().replaceAll("\"", "");
                result.put("auth_" + key, value);
            }
        }
    }

    /**
     * 使用DOM解析完整的XML消息体
     * 相比逐行正则匹配，DOM解析可以正确处理：
     * - 标签跨行的情况
     * - 带命名空间的标签
     * - 带属性的标签
     * - 嵌套结构
     *
     * @param xmlContent 完整的XML字符串
     * @param result     解析结果
     */
    private void parseMessageBodyXml(String xmlContent, Map<String, String> result) {
        // 需要提取的标签名到结果Key的映射
        String[][] tagMappings = {
                { "CmdType", "CmdType" },
                { "DeviceID", "bodyDeviceId" },
                { "Status", "Status" },
                { "Manufacturer", "Manufacturer" },
                { "Model", "Model" },
                { "Firmware", "Firmware" },
                { "Result", "Result" },
                { "SumNum", "SumNum" }
        };

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 安全配置：禁用外部实体解析，防止XXE攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            Element root = doc.getDocumentElement();
            for (String[] mapping : tagMappings) {
                String tagName = mapping[0];
                String resultKey = mapping[1];
                NodeList nodes = root.getElementsByTagName(tagName);
                if (nodes.getLength() > 0) {
                    String textContent = nodes.item(0).getTextContent();
                    if (StringUtils.isNotEmpty(textContent)) {
                        result.put(resultKey, textContent.trim());
                    }
                }
            }
        } catch (Exception e) {
            // DOM解析失败时，回退为正则提取（兼容非标准XML片段）
            logger.debug("DOM解析XML消息体失败，回退为正则提取: {}", e.getMessage());
            parseMessageBodyRegexFallback(xmlContent, result);
        }
    }

    /**
     * 正则回退方案：当XML格式不合法（如缺少根节点）时使用
     */
    private void parseMessageBodyRegexFallback(String content, Map<String, String> result) {
        String[][] tagMappings = {
                { "CmdType", "CmdType" },
                { "DeviceID", "bodyDeviceId" },
                { "Status", "Status" },
                { "Manufacturer", "Manufacturer" },
                { "Model", "Model" },
                { "Firmware", "Firmware" },
                { "Result", "Result" },
                { "SumNum", "SumNum" }
        };
        for (String[] mapping : tagMappings) {
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "<" + mapping[0] + "[^>]*>([^<]*)</" + mapping[0] + ">");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    result.put(mapping[1], matcher.group(1).trim());
                }
            } catch (Exception e) {
                logger.warn("正则提取XML标签 {} 失败", mapping[0]);
            }
        }
    }

    /**
     * 处理设备ID的特殊情况
     * 
     * @param result 解析结果
     */
    private void processDeviceId(Map<String, String> result) {
        String deviceId = result.get("deviceId");
        if (StringUtils.isNotEmpty(deviceId)) {
            // 处理设备ID包含空格的情况
            String[] parts = deviceId.split("\\s+");
            if (parts.length > 1) {
                result.put("deviceId", parts[1]);
            }
        }

        // 如果消息体中有设备ID，优先使用消息体中的
        String bodyDeviceId = result.get("bodyDeviceId");
        if (StringUtils.isNotEmpty(bodyDeviceId)) {
            result.put("deviceId", bodyDeviceId);
        }
    }
}
