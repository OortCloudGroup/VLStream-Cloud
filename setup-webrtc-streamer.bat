@echo off
echo ========================================
echo WebRTC-streamer Configuration Script
echo ========================================

set "WEBRTC_VERSION=v0.8.4"
set "WEBRTC_DIR=..\webrtc-streamer"
set "WEBRTC_PROJECT_DIR=F:\work\vls-tr\webrtc-streamer"
set "DOWNLOAD_URL=https://github.com/mpromonet/webrtc-streamer/releases/download/%WEBRTC_VERSION%/webrtc-streamer-%WEBRTC_VERSION%-Windows-AMD64.zip"
set "ZIP_FILE=webrtc-streamer-%WEBRTC_VERSION%-Windows-AMD64.zip"

echo Checking WebRTC-streamer installation...
echo.

:: Check webrtc-streamer directory in project root
if exist "%WEBRTC_PROJECT_DIR%" (
    echo Found existing WebRTC-streamer directory: %WEBRTC_PROJECT_DIR%
    set "WEBRTC_DIR=%WEBRTC_PROJECT_DIR%"
    goto :check_executable
)

:: Check webrtc-streamer directory in current directory
if exist "%WEBRTC_DIR%" (
    echo Found existing WebRTC-streamer directory: %WEBRTC_DIR%
    goto :check_executable
)

:: If neither exists, create directory and download
echo WebRTC-streamer not found, preparing to download and install...
if not exist "%WEBRTC_DIR%" (
    echo Creating directory: %WEBRTC_DIR%
    mkdir "%WEBRTC_DIR%"
)

cd "%WEBRTC_DIR%"
goto :download_webrtc

:check_executable
echo Checking WebRTC-streamer executable...
cd /d "%WEBRTC_DIR%"

:: Check if webrtc-streamer.exe exists
if exist "webrtc-streamer.exe" (
    echo Found webrtc-streamer.exe, skipping download
    goto :create_config
)

:: Check if webrtc-streamer executable exists
if exist "webrtc-streamer-*.exe" (
    echo Found webrtc-streamer executable
    ren webrtc-streamer-*.exe webrtc-streamer.exe
    goto :create_config
)

:: If directory exists but no executable, try to download
echo Directory exists but no executable found, attempting to download...
goto :download_webrtc

:download_webrtc
echo Downloading WebRTC-streamer...
echo Download URL: %DOWNLOAD_URL%
echo.

:: Use PowerShell to download
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_FILE%' -UseBasicParsing } catch { Write-Host 'Download failed, please check network connection or download manually' -ForegroundColor Red; exit 1 }}"

if not exist "%ZIP_FILE%" (
    echo.
    echo Download failed! Please download WebRTC-streamer manually
    echo Download URL: %DOWNLOAD_URL%
    echo Extract to: %cd%
    echo.
    echo Manual download steps:
    echo 1. Visit https://github.com/mpromonet/webrtc-streamer/releases
    echo 2. Download webrtc-streamer-%WEBRTC_VERSION%-Windows-AMD64.zip
    echo 3. Extract to current directory: %cd%
    echo 4. Rerun this script
    echo.
    pause
    exit /b 1
)

echo Download completed, extracting...
powershell -Command "try { Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '.' -Force } catch { Write-Host 'Extraction failed' -ForegroundColor Red; exit 1 }"

:: Delete zip file
if exist "%ZIP_FILE%" del "%ZIP_FILE%"

echo Extraction completed

:create_config
echo Creating configuration files...
echo Current directory: %cd%

:: Create configuration file
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

:: Create start script
echo @echo off> start.bat
echo echo ========================================>> start.bat
echo echo Starting WebRTC-streamer service>> start.bat
echo echo ========================================>> start.bat
echo echo Service address: http://localhost:8000>> start.bat
echo echo Management interface: http://localhost:8000/webrtcstreamer.html>> start.bat
echo echo Press Ctrl+C to stop service>> start.bat
echo echo ========================================>> start.bat
echo echo.>> start.bat
echo webrtc-streamer.exe -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2>> start.bat
echo echo.>> start.bat
echo echo Service stopped>> start.bat
echo pause>> start.bat

:: Create background start script
echo @echo off> start-background.bat
echo echo Starting WebRTC-streamer background service...>> start-background.bat
echo start "" /B webrtc-streamer.exe -H 0.0.0.0:8000 -S stun.l.google.com:19302 -v 2>> start-background.bat
echo echo WebRTC-streamer service started in background>> start-background.bat
echo echo Service address: http://localhost:8000>> start-background.bat
echo echo Management interface: http://localhost:8000/webrtcstreamer.html>> start-background.bat
echo echo Run stop.bat to stop service>> start-background.bat
echo pause>> start-background.bat

:: Create test script
echo @echo off> test.bat
echo echo Testing WebRTC-streamer service...>> test.bat
echo echo.>> test.bat
echo curl -s -m 5 http://localhost:8000/api/getIceServers>> test.bat
echo if %%ERRORLEVEL%% EQU 0 (>> test.bat
echo   echo.>> test.bat
echo   echo [Success] WebRTC-streamer service is running normally>> test.bat
echo   echo Service address: http://localhost:8000>> test.bat
echo   echo Management interface: http://localhost:8000/webrtcstreamer.html>> test.bat
echo ) else (>> test.bat
echo   echo.>> test.bat
echo   echo [Failure] WebRTC-streamer service is not running or inaccessible>> test.bat
echo   echo Please run start.bat to start service first>> test.bat
echo )>> test.bat
echo echo.>> test.bat
echo pause>> test.bat

:: Create stop script
echo @echo off> stop.bat
echo echo Stopping WebRTC-streamer service...>> stop.bat
echo taskkill /F /IM webrtc-streamer.exe 2^>nul>> stop.bat
echo if %%ERRORLEVEL%% EQU 0 (>> stop.bat
echo   echo [Success] WebRTC-streamer service stopped>> stop.bat
echo ) else (>> stop.bat
echo   echo [Info] WebRTC-streamer service is not running>> stop.bat
echo )>> stop.bat
echo echo.>> stop.bat
echo pause>> stop.bat

:: Create README
echo # WebRTC-streamer Usage Instructions> README.md
echo.>> README.md
echo ## File Description>> README.md
echo - `webrtc-streamer.exe`: WebRTC-streamer main program>> README.md
echo - `config.json`: Configuration file>> README.md
echo - `start.bat`: Start service (foreground)>> README.md
echo - `start-background.bat`: Start service (background)>> README.md
echo - `test.bat`: Test service status>> README.md
echo - `stop.bat`: Stop service>> README.md
echo.>> README.md
echo ## Usage Steps>> README.md
echo 1. Double-click `start.bat` to start service (foreground, logs visible)>> README.md
echo 2. Or double-click `start-background.bat` to start service (background)>> README.md
echo 3. Double-click `test.bat` to test if service is normal>> README.md
echo 4. Double-click `stop.bat` to stop service>> README.md
echo.>> README.md
echo ## Access Addresses>> README.md
echo - Service address: http://localhost:8000>> README.md
echo - Management interface: http://localhost:8000/webrtcstreamer.html>> README.md
echo - API documentation: http://localhost:8000/help>> README.md
echo.>> README.md
echo ## Configuration Description>> README.md
echo - Service port: 8000>> README.md
echo - STUN server: stun.l.google.com:19302>> README.md
echo - Log level: 2 (detailed)>> README.md
echo - Recording path: ./records>> README.md

echo.
echo ========================================
echo Configuration completed!
echo ========================================
echo.
echo Installation location: %cd%
echo.
echo File check:
if exist "webrtc-streamer.exe" (
    echo   [√] webrtc-streamer.exe - Main program
) else (
    echo   [×] webrtc-streamer.exe - Main program ^(missing^)
)
if exist "config.json" (
    echo   [√] config.json - Configuration file
) else (
    echo   [×] config.json - Configuration file ^(missing^)
)
if exist "start.bat" (
    echo   [√] start.bat - Start script
) else (
    echo   [×] start.bat - Start script ^(missing^)
)
echo.
echo Usage instructions:
echo   1. Double-click start.bat to start service
echo   2. Double-click test.bat to test service
echo   3. Double-click stop.bat to stop service
echo.
echo Access addresses:
echo   - Service: http://localhost:8000
echo   - Management: http://localhost:8000/webrtcstreamer.html
echo.
echo Start service now? (Y/N)
set /p choice=
if /i "%choice%"=="Y" (
    echo.
    echo Starting WebRTC-streamer service...
    start start.bat
)

echo.
echo Configuration completed! Please check README.md for detailed usage instructions.
pause 