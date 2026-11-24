@echo off
setlocal
chcp 65001 >nul
set "VLSTREAM_PWSH=pwsh.exe"
where pwsh.exe >nul 2>nul
if not errorlevel 1 goto :run
set "VLSTREAM_PWSH=%ProgramW6432%\PowerShell\7\pwsh.exe"
if not exist "%VLSTREAM_PWSH%" goto :missing

:run
if /i "%~1"=="stop" goto :stop
"%VLSTREAM_PWSH%" -NoLogo -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0server\Install-VLStreamServer.ps1" -ReleaseDirectory "%~dp0release" -DataDirectory "%LOCALAPPDATA%\VLStream\ServerData"
goto :result

:stop
"%VLSTREAM_PWSH%" -NoLogo -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0server\Install-VLStreamServer.ps1" -DataDirectory "%LOCALAPPDATA%\VLStream\ServerData" -Stop

:result
set "VLSTREAM_SERVER_EXIT=%ERRORLEVEL%"
if "%VLSTREAM_SERVER_EXIT%"=="0" exit /b 0

echo.
echo The VLStream server command failed. Review the message above before retrying.
pause
exit /b %VLSTREAM_SERVER_EXIT%

:missing
echo PowerShell 7 is required to deploy or stop the VLStream services.
echo https://learn.microsoft.com/powershell/scripting/install/installing-powershell-on-windows
pause
exit /b 1
