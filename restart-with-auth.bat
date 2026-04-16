@echo off
echo ==============================================
echo         VLStream Server Restart Script
echo ==============================================

echo.
echo 1. Stopping existing services...
taskkill /f /im java.exe 2>nul
timeout /t 3 /nobreak >nul

echo.
echo 2. Cleaning compilation files...
if exist target rmdir /s /q target
timeout /t 2 /nobreak >nul

echo.
echo 3. Compiling project...
call mvn clean compile -DskipTests
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo 4. Packaging project...
call mvn package -DskipTests
if %errorlevel% neq 0 (
    echo Packaging failed!
    pause
    exit /b 1
)

echo.
echo 5. Starting service...
java -jar target/vls-server-1.0.0.jar

echo.
echo Service startup completed!
echo Please check the logs to confirm the service started normally
pause 