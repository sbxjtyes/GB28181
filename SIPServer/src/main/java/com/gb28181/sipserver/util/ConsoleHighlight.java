package com.gb28181.sipserver.util;

/**
 * 控制台高亮显示工具类
 * 
 * 提供ANSI颜色代码支持的控制台文本高亮功能，包括：
 * - 文本颜色设置（前景色）
 * - 背景颜色设置
 * - 文本样式设置（粗体、下划线等）
 * - 预定义的常用高亮样式
 * 
 * @author GB28181 Team
 * @version 1.0.0
 */
public class ConsoleHighlight {

    // ANSI颜色代码
    public static final String RESET = "\u001B[0m";  // 重置所有格式
    
    // 前景色（文字颜色）
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // 亮色前景色
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_PURPLE = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    
    // 背景色
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_PURPLE = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";
    
    // 文本样式
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String BLINK = "\u001B[5m";
    public static final String REVERSE = "\u001B[7m";
    public static final String STRIKETHROUGH = "\u001B[9m";

    /**
     * 将文本应用指定的颜色和样式
     *
     * @param text 要处理的文本
     * @param colorCode 颜色/样式代码
     * @return 带颜色格式的文本
     */
    public static String color(String text, String colorCode) {
        return colorCode + text + RESET;
    }

    /**
     * 将文本应用多个颜色和样式
     *
     * @param text 要处理的文本
     * @param colorCodes 颜色/样式代码数组
     * @return 带颜色格式的文本
     */
    public static String color(String text, String... colorCodes) {
        StringBuilder sb = new StringBuilder();
        for (String code : colorCodes) {
            sb.append(code);
        }
        sb.append(text).append(RESET);
        return sb.toString();
    }

    /**
     * 成功消息高亮（绿色粗体）
     *
     * @param text 要高亮的文本
     * @return 高亮后的文本
     */
    public static String success(String text) {
        return color(text, BRIGHT_GREEN, BOLD);
    }

    /**
     * 错误消息高亮（红色粗体）
     *
     * @param text 要高亮的文本
     * @return 高亮后的文本
     */
    public static String error(String text) {
        return color(text, BRIGHT_RED, BOLD);
    }

    /**
     * 警告消息高亮（黄色粗体）
     *
     * @param text 要高亮的文本
     * @return 高亮后的文本
     */
    public static String warning(String text) {
        return color(text, BRIGHT_YELLOW, BOLD);
    }

    /**
     * 信息消息高亮（蓝色）
     *
     * @param text 要高亮的文本
     * @return 高亮后的文本
     */
    public static String info(String text) {
        return color(text, BRIGHT_BLUE);
    }

    /**
     * 重要消息高亮（紫色粗体）
     *
     * @param text 要高亮的文本
     * @return 高亮后的文本
     */
    public static String important(String text) {
        return color(text, BRIGHT_PURPLE, BOLD);
    }

    /**
     * 设备注册成功高亮（绿色背景 + 白色粗体文字）
     *
     * @param deviceId 设备ID
     * @return 高亮后的消息
     */
    public static String registerSuccess(String deviceId) {
        return color(" ✓ 设备注册成功: " + deviceId + " ", BG_GREEN, BRIGHT_WHITE, BOLD);
    }

    /**
     * 设备注册成功高亮（简化版：绿色粗体 + 成功图标）
     *
     * @param deviceId 设备ID
     * @return 高亮后的消息
     */
    public static String registerSuccessSimple(String deviceId) {
        return color("✓ 设备注册成功: " + deviceId, BRIGHT_GREEN, BOLD);
    }

    /**
     * 检查控制台是否支持ANSI颜色代码
     * 
     * @return 如果支持颜色显示返回true，否则返回false
     */
    public static boolean supportsAnsiColors() {
        // 检查系统属性和环境变量
        String os = System.getProperty("os.name").toLowerCase();
        String term = System.getenv("TERM");
        String colorTerm = System.getenv("COLORTERM");
        
        // Windows 10 及以上版本支持ANSI颜色
        if (os.contains("windows")) {
            try {
                String version = System.getProperty("os.version");
                if (version != null) {
                    String[] parts = version.split("\\.");
                    if (parts.length >= 1) {
                        int majorVersion = Integer.parseInt(parts[0]);
                        return majorVersion >= 10; // Windows 10+
                    }
                }
            } catch (NumberFormatException e) {
                // 如果无法解析版本，假设支持
                return true;
            }
        }
        
        // Unix/Linux/Mac 通常支持ANSI颜色
        if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            return true;
        }
        
        // 检查终端环境变量
        if (term != null && (term.contains("color") || term.contains("xterm") || term.contains("256"))) {
            return true;
        }
        
        if (colorTerm != null) {
            return true;
        }
        
        // 默认支持
        return true;
    }

    /**
     * 安全的颜色文本方法，如果不支持颜色则返回原始文本
     *
     * @param text 要处理的文本
     * @param colorCode 颜色代码
     * @return 如果支持颜色返回带颜色的文本，否则返回原始文本
     */
    public static String safeColor(String text, String colorCode) {
        if (supportsAnsiColors()) {
            return color(text, colorCode);
        } else {
            return text;
        }
    }

    /**
     * 安全的设备注册成功高亮方法
     *
     * @param deviceId 设备ID
     * @return 高亮后的消息（如果支持颜色）或普通消息
     */
    public static String safeRegisterSuccess(String deviceId) {
        if (supportsAnsiColors()) {
            return registerSuccessSimple(deviceId);
        } else {
            return "✓ 设备注册成功: " + deviceId;
        }
    }
}
