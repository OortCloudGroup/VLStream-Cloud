@echo off
echo ==============================================
echo         VLStream Server Status Check
echo ==============================================

echo.
echo 1. Checking Java processes...
tasklist /fi "imagename eq java.exe" /fo table
echo.

echo 2. Checking port usage...
netstat -ano | findstr :18080
echo.

echo 3. Checking service logs...
if exist logs\vlstream-server.log (
    echo Latest log content:
    powershell "Get-Content logs\vlstream-server.log -Tail 20"
) else (
    echo Log file does not exist
)
echo.

echo 4. Checking service response...
curl -s http://localhost:18080/auth/health
echo.

echo 5. Checking gateway route (vls-server)...
curl -s http://oort.oortcloudsmart.com:21410/bus/vls-server/auth/health
echo.

echo 6. Checking service registration status...
curl -s http://oort.oortcloudsmart.com:21410/bus/vls-server/
echo.

pause 