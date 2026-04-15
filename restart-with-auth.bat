@echo off
echo ==============================================
echo         VLStream Server 重启脚本
echo ==============================================

echo.
echo 1. 停止现有服务...
taskkill /f /im java.exe 2>nul
timeout /t 3 /nobreak >nul

echo.
echo 2. 清理编译文件...
if exist target rmdir /s /q target
timeout /t 2 /nobreak >nul

echo.
echo 3. 编译项目...
call mvn clean compile -DskipTests
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo 4. 打包项目...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo 打包失败！
    pause
    exit /b 1
)

echo.
echo 5. 启动服务...
java -jar target/vls-server-1.0.0.jar

echo.
echo 服务启动完成！
echo 请检查日志确认服务是否正常启动
pause 