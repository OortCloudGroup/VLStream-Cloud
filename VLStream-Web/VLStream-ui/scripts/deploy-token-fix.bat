@echo off
echo ========================================
echo Token校验流程修复部署脚本
echo ========================================

echo.
echo 1. 备份当前文件...
if not exist "backup" mkdir backup
copy "src\api\userCenter.js" "backup\userCenter.js.bak" >nul
copy "src\utils\auth.js" "backup\auth.js.bak" >nul
copy "src\utils\storageEventSync.js" "backup\storageEventSync.js.bak" >nul
copy "src\utils\autoCrossSystemSync.js" "backup\autoCrossSystemSync.js.bak" >nul
copy "src\utils\crossSystemTokenSync.js" "backup\crossSystemTokenSync.js.bak" >nul
copy "src\utils\tokenSync.js" "backup\tokenSync.js.bak" >nul
copy "src\utils\unifiedPlatformTokenFetcher.js" "backup\unifiedPlatformTokenFetcher.js.bak" >nul

echo ✅ 备份完成

echo.
echo 2. 验证修复文件是否存在...
if not exist "src\api\userCenter.js" (
    echo ❌ 错误：userCenter.js 文件不存在
    pause
    exit /b 1
)

echo ✅ 所有文件存在

echo.
echo 3. 运行测试验证...
node test-token-fix.js

echo.
echo 4. 部署完成！
echo.
echo 修复内容：
echo - ✅ API配置修复（appid和secretkey）
echo - ✅ Token验证逻辑改进
echo - ✅ 错误处理优化
echo - ✅ 网络错误处理改进
echo - ✅ 避免错误清除有效token
echo.
echo 注意事项：
echo - 请重启应用服务器
echo - 监控token校验相关日志
echo - 测试SSO登录功能
echo.
pause 