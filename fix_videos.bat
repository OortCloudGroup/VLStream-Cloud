@echo off
echo 正在检查和修复视频文件...

set VIDEO_DIR=recordings\2025\07\16

if not exist "%VIDEO_DIR%" (
    echo 录制目录不存在: %VIDEO_DIR%
    pause
    exit /b 1
)

echo 扫描视频文件...
for %%f in ("%VIDEO_DIR%\*.mp4") do (
    echo 处理文件: %%f
    
    echo 检查文件信息...
    ffprobe -v quiet -print_format json -show_format -show_streams "%%f" > "%%f.info.json" 2>&1
    
    if errorlevel 1 (
        echo 文件损坏，尝试修复: %%f
        echo 重新封装视频...
        ffmpeg -i "%%f" -c copy -movflags +faststart "%%f.fixed.mp4" -y
        
        if errorlevel 0 (
            echo 修复成功: %%f.fixed.mp4
        ) else (
            echo 修复失败，尝试转码修复...
            ffmpeg -i "%%f" -c:v libx264 -c:a aac -movflags +faststart "%%f.transcoded.mp4" -y
            if errorlevel 0 (
                echo 转码修复成功: %%f.transcoded.mp4
            ) else (
                echo 文件无法修复: %%f
            )
        )
    ) else (
        echo 文件正常: %%f
    )
    echo ----------------------------------------
)

echo 检查完成！
pause 