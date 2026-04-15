@echo off
echo ==============================================
echo         VLStream Server 状态检查
echo ==============================================

echo.
echo 1. 检查Java进程...
tasklist /fi "imagename eq java.exe" /fo table
echo.

echo 2. 检查端口占用情况...
netstat -ano | findstr :18080
echo.

echo 3. 检查服务日志...
if exist logs\vlstream-server.log (
    echo 最新日志内容:
    powershell "Get-Content logs\vlstream-server.log -Tail 20"
) else (
    echo 日志文件不存在
)
echo.

echo 4. 检查服务是否响应...
curl -s http://localhost:18080/auth/health
echo.

echo 5. 检查网关路由（vls-server）...
curl -s http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/health
echo.

echo 6. 检查服务注册状态...
curl -s http://oort.oortcloudsmart.com:21410/bus/vls-server/
echo.

pause 