@echo off
echo Checking and fixing video files...

set VIDEO_DIR=recordings\2025\07\16

if not exist "%VIDEO_DIR%" (
    echo Recording directory does not exist: %VIDEO_DIR%
    pause
    exit /b 1
)

echo Scanning video files...
for %%f in ("%VIDEO_DIR%\*.mp4") do (
    echo Processing file: %%f
    
    echo Checking file information...
    ffprobe -v quiet -print_format json -show_format -show_streams "%%f" > "%%f.info.json" 2>&1
    
    if errorlevel 1 (
        echo File corrupted, attempting to fix: %%f
        echo Re-encoding video...
        ffmpeg -i "%%f" -c copy -movflags +faststart "%%f.fixed.mp4" -y
        
        if errorlevel 0 (
            echo Fix successful: %%f.fixed.mp4
        ) else (
            echo Fix failed, attempting transcoding fix...
            ffmpeg -i "%%f" -c:v libx264 -c:a aac -movflags +faststart "%%f.transcoded.mp4" -y
            if errorlevel 0 (
                echo Transcoding fix successful: %%f.transcoded.mp4
            ) else (
                echo File cannot be fixed: %%f
            )
        )
    ) else (
        echo File is normal: %%f
    )
    echo ----------------------------------------
)

echo Check completed!
pause 