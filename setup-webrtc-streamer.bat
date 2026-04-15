@echo off
echo ========================================
echo WebRTC-streamer 配置脚本
echo ========================================

set "WEBRTC_VERSION=v0.8.4"
set "WEBRTC_DIR=..\webrtc-streamer"
set "WEBRTC_PROJECT_DIR=F:\work\vls-tr\webrtc-streamer"
set "DOWNLOAD_URL=https://github.com/mpromonet/webrtc-streamer/releases/download/%WEBRTC_VERSION%/webrtc-streamer-%WEBRTC_VERSION%-Windows-AMD64.zip"
set "ZIP_FILE=webrtc-streamer-%WEBRTC_VERSION%-Windows-AMD64.zip"

echo 正在检查WebRTC-streamer安装...
echo.

:: 检查项目根目录的webrtc-streamer目录
if exist "%WEBRTC_PROJECT_DIR%" (
    echo 发现已存在的WebRTC-streamer目录: %WEBRTC_PROJECT_DIR%
    set "WEBRTC_DIR=%WEBRTC_PROJECT_DIR%"
    goto :check_executable
)

:: 检查当前目录的webrtc-streamer目录
if exist "%WEBRTC_DIR%" (
    echo 发现已存在的WebRTC-streamer目录: %WEBRTC_DIR%
    goto :check_executable
)

:: 如果都不存在，创建目录并下载
echo 未找到WebRTC-streamer，准备下载安装...
if not exist "%WEBRTC_DIR%" (
    echo 创建目录: %WEBRTC_DIR%
    mkdir "%WEBRTC_DIR%"
)

cd "%WEBRTC_DIR%"
goto :download_webrtc

:check_executable
echo 检查WebRTC-streamer可执行文件...
cd /d "%WEBRTC_DIR%"

:: 检查是否存在webrtc-streamer.exe
if exist "webrtc-streamer.exe" (
    echo 找到webrtc-streamer.exe，跳过下载
    goto :create_config
)

:: 检查是否存在压缩包文件
if exist "webrtc-streamer-*.exe" (
    echo 找到webrtc-streamer可执行文件
    ren webrtc-streamer-*.exe webrtc-streamer.exe
    goto :create_config
)

:: 如果目录存在但没有可执行文件，尝试下载
echo 目录存在但未找到可执行文件，尝试下载...
goto :download_webrtc

:download_webrtc
echo 正在下载WebRTC-streamer...
echo 下载地址: %DOWNLOAD_URL%
echo.

:: 使用PowerShell下载
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_FILE%' -UseBasicParsing } catch { Write-Host '下载失败，请检查网络连接或手动下载' -ForegroundColor Red; exit 1 }}"

if not exist "%ZIP_FILE%" (
    echo.
    echo 下载失败！请手动下载WebRTC-streamer
    echo 下载地址: %DOWNLOAD_URL%
    echo 解压到: %cd%
    echo.
    echo 手动下载步骤：
    echo 1. 访问 https://github.com/mpromonet/webrtc-streamer/releases
    echo 2. 下载 webrtc-streamer-%WEBRTC_VERSION%-Windows-AMD64.zip
    echo 3. 解压到当前目录: %cd%
    echo 4. 重新运行此脚本
    echo.
    pause
    exit /b 1
)

echo 下载完成，正在解压...
powershell -Command "try { Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '.' -Force } catch { Write-Host '解压失败' -ForegroundColor Red; exit 1 }"

:: 删除压缩包
if exist "%ZIP_FILE%" del "%ZIP_FILE%"

echo 解压完成

:create_config
echo 正在创建配置文件...
echo 当前目录: %cd%

:: 创建配置文件
echo {> config.json
echo   "webrtc-streamer": {>> config.json
echo     "http": "0.0.0.0:8000",>> config.json
echo     "verbose": "2",>> config.json
echo     "stunserver": "stun.l.google.com:19302",>> config.json
echo     "turnserver": "",>> config.json
echo     "recordpath": "./records",>> config.json
echo     "publishfilter": ".*",>> config.json
echo     "webroot": "./html">> config.json
echo   }>> config.json
echo }>> config.json

:: 创建启动脚本
echo @echo off> start.bat
echo echo ========================================>> start.bat
echo echo 启动WebRTC-streamer服务>> start.bat
echo echo ========================================>> start.bat
echo echo 服务地址: http://localhost:8000>> start.bat
echo echo 管理界面: http://localhost:8000/webrtcstreamer.html>> start.bat
echo echo 按Ctrl+C停止服务>> start.bat
echo echo ========================================>> start.bat
echo echo.>> start.bat
echo webrtc-streamer.exe -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2>> start.bat
echo echo.>> start.bat
echo echo 服务已停止>> start.bat
echo pause>> start.bat

:: 创建后台启动脚本
echo @echo off> start-background.bat
echo echo 启动WebRTC-streamer后台服务...>> start-background.bat
echo start "" /B webrtc-streamer.exe -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2>> start-background.bat
echo echo WebRTC-streamer服务已在后台启动>> start-background.bat
echo echo 服务地址: http://localhost:8000>> start-background.bat
echo echo 管理界面: http://localhost:8000/webrtcstreamer.html>> start-background.bat
echo echo 运行 stop.bat 停止服务>> start-background.bat
echo pause>> start-background.bat

:: 创建测试脚本
echo @echo off> test.bat
echo echo 测试WebRTC-streamer服务...>> test.bat
echo echo.>> test.bat
echo curl -s -m 5 http://localhost:8000/api/getIceServers>> test.bat
echo if %%ERRORLEVEL%% EQU 0 (>> test.bat
echo   echo.>> test.bat
echo   echo [成功] WebRTC-streamer服务运行正常>> test.bat
echo   echo 服务地址: http://localhost:8000>> test.bat
echo   echo 管理界面: http://localhost:8000/webrtcstreamer.html>> test.bat
echo ) else (>> test.bat
echo   echo.>> test.bat
echo   echo [失败] WebRTC-streamer服务未运行或不可访问>> test.bat
echo   echo 请先运行 start.bat 启动服务>> test.bat
echo )>> test.bat
echo echo.>> test.bat
echo pause>> test.bat

:: 创建停止脚本
echo @echo off> stop.bat
echo echo 停止WebRTC-streamer服务...>> stop.bat
echo taskkill /F /IM webrtc-streamer.exe 2^>nul>> stop.bat
echo if %%ERRORLEVEL%% EQU 0 (>> stop.bat
echo   echo [成功] WebRTC-streamer服务已停止>> stop.bat
echo ) else (>> stop.bat
echo   echo [信息] WebRTC-streamer服务未运行>> stop.bat
echo )>> stop.bat
echo echo.>> stop.bat
echo pause>> stop.bat

:: 创建README
echo # WebRTC-streamer 使用说明> README.md
echo.>> README.md
echo ## 文件说明>> README.md
echo - `webrtc-streamer.exe`: WebRTC-streamer主程序>> README.md
echo - `config.json`: 配置文件>> README.md
echo - `start.bat`: 启动服务（前台运行）>> README.md
echo - `start-background.bat`: 启动服务（后台运行）>> README.md
echo - `test.bat`: 测试服务状态>> README.md
echo - `stop.bat`: 停止服务>> README.md
echo.>> README.md
echo ## 使用步骤>> README.md
echo 1. 双击 `start.bat` 启动服务（前台运行，可看到日志）>> README.md
echo 2. 或双击 `start-background.bat` 启动服务（后台运行）>> README.md
echo 3. 双击 `test.bat` 测试服务是否正常>> README.md
echo 4. 双击 `stop.bat` 停止服务>> README.md
echo.>> README.md
echo ## 访问地址>> README.md
echo - 服务地址: http://localhost:8000>> README.md
echo - 管理界面: http://localhost:8000/webrtcstreamer.html>> README.md
echo - API文档: http://localhost:8000/help>> README.md
echo.>> README.md
echo ## 配置说明>> README.md
echo - 服务端口: 8000>> README.md
echo - STUN服务器: stun.l.google.com:19302>> README.md
echo - 日志级别: 2 (详细)>> README.md
echo - 录制路径: ./records>> README.md

echo.
echo ========================================
echo 配置完成！
echo ========================================
echo.
echo 安装位置: %cd%
echo.
echo 文件检查:
if exist "webrtc-streamer.exe" (
    echo   [√] webrtc-streamer.exe - 主程序
) else (
    echo   [×] webrtc-streamer.exe - 主程序 ^(缺失^)
)
if exist "config.json" (
    echo   [√] config.json - 配置文件
) else (
    echo   [×] config.json - 配置文件 ^(缺失^)
)
if exist "start.bat" (
    echo   [√] start.bat - 启动脚本
) else (
    echo   [×] start.bat - 启动脚本 ^(缺失^)
)
echo.
echo 使用说明:
echo   1. 双击 start.bat 启动服务
echo   2. 双击 test.bat 测试服务
echo   3. 双击 stop.bat 停止服务
echo.
echo 访问地址:
echo   - 服务: http://localhost:8000
echo   - 管理: http://localhost:8000/webrtcstreamer.html
echo.
echo 现在启动服务吗？(Y/N)
set /p choice=
if /i "%choice%"=="Y" (
    echo.
    echo 启动WebRTC-streamer服务...
    start start.bat
)

echo.
echo 配置完成！请查看 README.md 了解详细使用方法。
pause 