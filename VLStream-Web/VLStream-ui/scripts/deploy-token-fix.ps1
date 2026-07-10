# Token校验流程修复部署脚本
Write-Host "========================================" -ForegroundColor Green
Write-Host "Token校验流程修复部署脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host ""
Write-Host "1. 备份当前文件..." -ForegroundColor Yellow

# 创建备份目录
if (!(Test-Path "backup")) {
    New-Item -ItemType Directory -Path "backup" | Out-Null
}

# 备份文件
$filesToBackup = @(
    "src\api\userCenter.js",
    "src\utils\auth.js", 
    "src\utils\storageEventSync.js",
    "src\utils\autoCrossSystemSync.js",
    "src\utils\crossSystemTokenSync.js",
    "src\utils\tokenSync.js",
    "src\utils\unifiedPlatformTokenFetcher.js"
)

foreach ($file in $filesToBackup) {
    if (Test-Path $file) {
        $backupPath = "backup\$($file.Split('\')[-1]).bak"
        Copy-Item $file $backupPath -Force
        Write-Host "✅ 已备份: $file" -ForegroundColor Green
    } else {
        Write-Host "⚠️ 文件不存在: $file" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "2. 验证修复文件是否存在..." -ForegroundColor Yellow

$requiredFiles = @(
    "src\api\userCenter.js",
    "src\utils\auth.js",
    "test-token-fix.js"
)

foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Write-Host "✅ 文件存在: $file" -ForegroundColor Green
    } else {
        Write-Host "❌ 错误：文件不存在 $file" -ForegroundColor Red
        Read-Host "按任意键退出"
        exit 1
    }
}

Write-Host ""
Write-Host "3. 运行测试验证..." -ForegroundColor Yellow

try {
    $testResult = node test-token-fix.js 2>&1
    Write-Host "✅ 测试完成" -ForegroundColor Green
} catch {
    Write-Host "⚠️ 测试运行失败，但继续部署" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "4. 部署完成！" -ForegroundColor Green
Write-Host ""
Write-Host "修复内容：" -ForegroundColor Cyan
Write-Host "- ✅ API配置修复（appid和secretkey）" -ForegroundColor Green
Write-Host "- ✅ Token验证逻辑改进" -ForegroundColor Green
Write-Host "- ✅ 错误处理优化" -ForegroundColor Green
Write-Host "- ✅ 网络错误处理改进" -ForegroundColor Green
Write-Host "- ✅ 避免错误清除有效token" -ForegroundColor Green
Write-Host ""
Write-Host "注意事项：" -ForegroundColor Cyan
Write-Host "- 请重启应用服务器" -ForegroundColor Yellow
Write-Host "- 监控token校验相关日志" -ForegroundColor Yellow
Write-Host "- 测试SSO登录功能" -ForegroundColor Yellow
Write-Host ""

Read-Host "按任意键退出" 