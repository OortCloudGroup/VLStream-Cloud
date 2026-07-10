@echo off
chcp 65001 >nul
title VideoSquare组件拆分部署工具

echo.
echo 🚀 开始部署重构后的VideoSquare组件...
echo.

:: 定义路径
set "ORIGINAL_FILE=VLStream-ui\src\views\VideoAggregation\VideoSquare.vue"
set "REFACTORED_FILE=VLStream-ui\src\views\VideoAggregation\VideoSquareRefactored.vue"
set "BACKUP_FILE=VLStream-ui\src\views\VideoAggregation\VideoSquare.vue.backup"
set "COMPONENTS_DIR=VLStream-ui\src\views\VideoAggregation\components"

:: 1. 备份原始文件
echo 📦 备份原始VideoSquare.vue文件...
if exist "%ORIGINAL_FILE%" (
    copy "%ORIGINAL_FILE%" "%BACKUP_FILE%" >nul
    echo ✅ 原始文件已备份到: %BACKUP_FILE%
) else (
    echo ⚠️  原始文件不存在，跳过备份
)
echo.

:: 2. 检查重构后的文件是否存在
if not exist "%REFACTORED_FILE%" (
    echo ❌ 重构后的文件不存在: %REFACTORED_FILE%
    echo.
    pause
    exit /b 1
)

:: 3. 检查组件目录是否存在
if not exist "%COMPONENTS_DIR%" (
    echo ❌ 组件目录不存在: %COMPONENTS_DIR%
    echo.
    pause
    exit /b 1
)

:: 4. 检查所有必需的组件文件
echo 🔍 检查组件文件...
set "MISSING_FILES="

if exist "%COMPONENTS_DIR%\VideoDialog.vue" (
    echo ✅ VideoDialog.vue - 存在
) else (
    echo ❌ VideoDialog.vue - 缺失
    set "MISSING_FILES=1"
)

if exist "%COMPONENTS_DIR%\VideoLayoutControls.vue" (
    echo ✅ VideoLayoutControls.vue - 存在
) else (
    echo ❌ VideoLayoutControls.vue - 缺失
    set "MISSING_FILES=1"
)

if exist "%COMPONENTS_DIR%\DeviceListPanel.vue" (
    echo ✅ DeviceListPanel.vue - 存在
) else (
    echo ❌ DeviceListPanel.vue - 缺失
    set "MISSING_FILES=1"
)

if exist "%COMPONENTS_DIR%\MapControls.vue" (
    echo ✅ MapControls.vue - 存在
) else (
    echo ❌ MapControls.vue - 缺失
    set "MISSING_FILES=1"
)

if exist "%COMPONENTS_DIR%\DebugPanel.vue" (
    echo ✅ DebugPanel.vue - 存在
) else (
    echo ❌ DebugPanel.vue - 缺失
    set "MISSING_FILES=1"
)

if defined MISSING_FILES (
    echo.
    echo ❌ 有组件文件缺失，无法继续部署
    pause
    exit /b 1
)
echo.

:: 5. 替换主文件
echo 🔄 替换主文件...
move "%REFACTORED_FILE%" "%ORIGINAL_FILE%" >nul
echo ✅ VideoSquareRefactored.vue 已重命名为 VideoSquare.vue
echo.

:: 6. 显示组件文件统计
echo 📁 组件文件统计:
for %%f in ("%COMPONENTS_DIR%\*.vue") do (
    for /f %%a in ('find /c /v "" ^< "%%f"') do echo    %%~nxf: %%a 行
)
echo.

:: 7. 创建组件索引文件
echo 📝 创建组件索引文件...
set "INDEX_FILE=%COMPONENTS_DIR%\index.js"

(
echo // VideoSquare组件拆分后的子组件导出
echo export { default as VideoDialog } from './VideoDialog.vue'
echo export { default as VideoLayoutControls } from './VideoLayoutControls.vue'
echo export { default as DeviceListPanel } from './DeviceListPanel.vue'
echo export { default as MapControls } from './MapControls.vue'
echo export { default as DebugPanel } from './DebugPanel.vue'
) > "%INDEX_FILE%"

echo ✅ 组件索引文件已创建: %INDEX_FILE%
echo.

:: 8. 显示完成信息
echo ========================================
echo 🎉 VideoSquare组件拆分部署完成！
echo ========================================
echo.
echo 📋 部署总结:
echo    ✅ 原始文件已备份
echo    ✅ 新文件已替换
echo    ✅ 所有组件文件就绪
echo    ✅ 组件索引文件已创建
echo.
echo 🔧 下一步操作:
echo    1. 运行 npm run dev 测试应用
echo    2. 检查所有功能是否正常
echo    3. 如有问题，可从备份文件恢复
echo.
echo 📂 备份文件位置: %BACKUP_FILE%
echo 📂 组件文件位置: %COMPONENTS_DIR%
echo.

:: 9. 询问是否启动开发服务器
set /p "START_DEV=是否现在启动开发服务器? (y/n): "
if /i "%START_DEV%"=="y" (
    echo.
    echo 🚀 启动开发服务器...
    npm run dev
) else (
    echo.
    echo 👋 部署完成，您可以手动运行 npm run dev 启动开发服务器
)

echo.
pause 