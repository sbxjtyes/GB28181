package com.gb28181.sipserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * GB28181 SIP服务器主应用程序
 *
 * 功能特性：
 * - SIP信令服务器，支持摄像头注册、认证、心跳保活
 * - 会话建立和管理
 * - 推流请求处理和停止推流操作
 * - 使用MySQL数据库存储设备信息
 *
 * @author GB28181 Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class GB28181SipServerApplication {

    public static void main(String[] args) {
        try {
            // 设置系统编码属性 - 必须在应用启动前设置
            setupSystemEncoding();
            
            // 设置控制台编码为UTF-8
            setupConsoleEncoding();

            // 启动Spring Boot应用
            SpringApplication app = new SpringApplication(GB28181SipServerApplication.class);
            app.run(args);

            // 使用正确编码输出启动成功信息
            printStartupMessage();
            
        } catch (Exception e) {
            System.err.println("应用启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 设置系统编码属性
     */
    private static void setupSystemEncoding() {
        // 设置文件编码
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        
        // 设置控制台编码
        System.setProperty("console.encoding", "UTF-8");
        System.setProperty("stdout.encoding", "UTF-8");
        System.setProperty("stderr.encoding", "UTF-8");
        
        // 设置Spring编码
        System.setProperty("spring.http.encoding.charset", "UTF-8");
        System.setProperty("spring.http.encoding.enabled", "true");
        System.setProperty("spring.http.encoding.force", "true");
        
        // 设置JVM编码
        System.setProperty("user.language", "zh");
        System.setProperty("user.country", "CN");
        System.setProperty("user.variant", "");
        
        // 设置默认区域
        Locale.setDefault(new Locale("zh", "CN"));
    }

    /**
     * 设置控制台编码（简化版）
     * 解决Windows环境下的中文乱码问题
     */
    private static void setupConsoleEncoding() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            
            // 在Windows上简单设置控制台代码页
            if (osName.contains("windows")) {
                try {
                    // 使用简单的cmd命令设置代码页
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "chcp 65001 >nul");
                    Process p = pb.start();
                    // 设置较短的超时时间，避免阻塞
                    boolean finished = p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
                    if (!finished) {
                        p.destroyForcibly(); // 强制终止超时的进程
                    }
                } catch (Exception winEx) {
                    // 忽略Windows特定设置失败，不影响应用启动
                    System.out.println("Windows控制台编码设置跳过: " + winEx.getMessage());
                }
            }

            // 尝试设置控制台输出流编码（更安全的方式）
            try {
                System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8));
                System.setErr(new java.io.PrintStream(System.err, true, StandardCharsets.UTF_8));
            } catch (Exception streamEx) {
                // 如果设置失败，使用默认编码继续
                System.out.println("控制台流编码设置失败，使用默认编码: " + streamEx.getMessage());
            }
            
            // 验证编码设置
            String testString = "测试中文编码 - Test Chinese Encoding";
            System.out.println("编码测试: " + testString);
            
        } catch (Exception e) {
            // 编码设置失败不应该影响应用启动，只输出警告
            System.out.println("警告: 控制台编码设置失败，使用默认编码: " + e.getMessage());
        }
    }

    /**
     * 输出启动成功信息
     */
    private static void printStartupMessage() {
        try {
            System.out.println("=================================================");
            System.out.println("     GB28181 SIP服务器启动成功！     ");
            System.out.println("     Web服务: http://localhost:8080/gb28181     ");
            System.out.println("     API文档: http://localhost:8080/gb28181/swagger-ui/index.html");
            System.out.println("     监控指标: http://localhost:8080/gb28181/actuator");
            System.out.println("     SIP服务: 0.0.0.0:5060 (UDP)              ");
            System.out.println("=================================================");
        } catch (Exception e) {
            // 如果UTF-8输出失败，使用ASCII输出
            System.out.println("=================================================");
            System.out.println("   GB28181 SIP Server Started Successfully!   ");
            System.out.println("   Web Service: http://localhost:8080/gb28181  ");
            System.out.println("   API Docs:    http://localhost:8080/gb28181/swagger-ui/index.html");
            System.out.println("   Actuator:    http://localhost:8080/gb28181/actuator");
            System.out.println("   SIP Service: 0.0.0.0:5060 (UDP)            ");
            System.out.println("=================================================");
        }
    }
}
