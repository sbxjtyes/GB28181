@echo off
chcp 65001 > nul
title GB28181 SIP Server

echo.
echo ===============================================
echo    GB28181 SIP Server 启动脚本
echo    版本: 1.0.0
echo    Java 版本要求: 17+
echo ===============================================
echo.

:: 检查Java环境
echo [INFO] 检查Java环境...
java -version > nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到Java环境，请确保已安装Java 17+并配置环境变量
    echo [ERROR] 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

:: 获取Java版本
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%
echo [INFO] 当前Java版本: %JAVA_VERSION%

:: 检查Java版本是否为17+
for /f "delims=." %%a in ("%JAVA_VERSION%") do set JAVA_MAJOR=%%a
if %JAVA_MAJOR% lss 17 (
    echo [WARNING] 建议使用Java 17+，当前版本可能不兼容
)

:: 检查项目是否已编译
echo [INFO] 检查项目编译状态...
if not exist "target\classes\com\gb28181\sipserver\GB28181SipServerApplication.class" (
    echo [WARNING] 项目未编译或编译不完整
    echo [INFO] 正在执行Maven编译...
    
    :: 检查Maven环境
    mvn -version > nul 2>&1
    if %errorlevel% neq 0 (
        echo [ERROR] 未找到Maven环境，请先手动编译项目
        echo [ERROR] 执行命令: mvn clean compile
        pause
        exit /b 1
    )
    
    :: 执行Maven编译
    echo [INFO] 执行: mvn clean compile
    mvn clean compile -q
    if %errorlevel% neq 0 (
        echo [ERROR] Maven编译失败，请检查代码和依赖
        pause
        exit /b 1
    )
    echo [INFO] 编译完成
)

:: 设置应用程序参数
set APP_NAME=GB28181 SIP Server
set MAIN_CLASS=com.gb28181.sipserver.GB28181SipServerApplication
set LOG_DIR=logs
set LOG_FILE=%LOG_DIR%\gb28181-sip-server.log

:: 创建日志目录
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: 设置类路径
set CLASSPATH=target\classes
for %%f in (target\dependency\*.jar) do (
    set CLASSPATH=!CLASSPATH!;%%f
)

:: 如果没有dependency目录，使用Maven运行
if not exist "target\dependency" (
    echo [INFO] 使用Maven方式启动应用...
    echo [INFO] 启动命令: mvn spring-boot:run
    echo [INFO] 应用将在 http://localhost:8080/gb28181 启动
    echo [INFO] 按 Ctrl+C 停止应用
    echo.
    mvn spring-boot:run
    goto :end
)

:: 设置JVM参数（从pom.xml配置中获取）
set JAVA_OPTS=-Dfile.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dsun.jnu.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dconsole.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dstdout.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dstderr.encoding=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Duser.language=zh
set JAVA_OPTS=%JAVA_OPTS% -Duser.country=CN
set JAVA_OPTS=%JAVA_OPTS% -Djava.awt.headless=true
set JAVA_OPTS=%JAVA_OPTS% -Dspring.http.encoding.charset=UTF-8
set JAVA_OPTS=%JAVA_OPTS% -Dspring.http.encoding.enabled=true
set JAVA_OPTS=%JAVA_OPTS% -Dspring.http.encoding.force=true

:: 内存配置
set JAVA_OPTS=%JAVA_OPTS% -Xms512m
set JAVA_OPTS=%JAVA_OPTS% -Xmx1024m

:: GC配置
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseStringDeduplication

:: 可选：启用GC日志（调试用）
:: set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCDetails
:: set JAVA_OPTS=%JAVA_OPTS% -XX:+PrintGCTimeStamps

echo [INFO] 启动参数:
echo [INFO] 主类: %MAIN_CLASS%
echo [INFO] JVM参数: %JAVA_OPTS%
echo [INFO] 日志文件: %LOG_FILE%
echo.

:: 显示重要信息
echo ===============================================
echo 服务信息:
echo   - Web端口: 8080
echo   - 访问地址: http://localhost:8080/gb28181
echo   - SIP端口: 5060 (UDP)
echo   - 数据库: MySQL (localhost:31306)
echo   - 日志文件: %LOG_FILE%
echo.
echo 停止服务: 按 Ctrl+C
echo ===============================================
echo.

:: 启动应用程序
echo [INFO] 正在启动 %APP_NAME%...
java %JAVA_OPTS% -cp "%CLASSPATH%" %MAIN_CLASS%

:end
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] 应用程序异常退出，错误码: %errorlevel%
    echo [ERROR] 请检查日志文件: %LOG_FILE%
    echo [ERROR] 或查看上面的错误信息
)

echo.
echo [INFO] %APP_NAME% 已停止
pause
