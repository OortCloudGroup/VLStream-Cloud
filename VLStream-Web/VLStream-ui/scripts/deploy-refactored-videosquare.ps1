# VideoSquare组件拆分部署脚本 (Windows PowerShell)
# 该脚本用于部署重构后的VideoSquare组件

Write-Host "🚀 开始部署重构后的VideoSquare组件..." -ForegroundColor Green

# 定义路径
$ORIGINAL_FILE = "VLStream-ui\src\views\VideoAggregation\VideoSquare.vue"
$REFACTORED_FILE = "VLStream-ui\src\views\VideoAggregation\VideoSquareRefactored.vue"
$BACKUP_FILE = "VLStream-ui\src\views\VideoAggregation\VideoSquare.vue.backup"
$COMPONENTS_DIR = "VLStream-ui\src\views\VideoAggregation\components"

# 1. 备份原始文件
Write-Host "📦 备份原始VideoSquare.vue文件..." -ForegroundColor Yellow
if (Test-Path $ORIGINAL_FILE) {
    Copy-Item $ORIGINAL_FILE $BACKUP_FILE
    Write-Host "✅ 原始文件已备份到: $BACKUP_FILE" -ForegroundColor Green
} else {
    Write-Host "⚠️  原始文件不存在，跳过备份" -ForegroundColor Yellow
}

# 2. 检查重构后的文件是否存在
if (-not (Test-Path $REFACTORED_FILE)) {
    Write-Host "❌ 重构后的文件不存在: $REFACTORED_FILE" -ForegroundColor Red
    exit 1
}

# 3. 检查组件目录是否存在
if (-not (Test-Path $COMPONENTS_DIR)) {
    Write-Host "❌ 组件目录不存在: $COMPONENTS_DIR" -ForegroundColor Red
    exit 1
}

# 4. 检查所有必需的组件文件
$REQUIRED_COMPONENTS = @(
    "VideoDialog.vue",
    "VideoLayoutControls.vue",
    "DeviceListPanel.vue", 
    "MapControls.vue",
    "DebugPanel.vue"
)

Write-Host "🔍 检查组件文件..." -ForegroundColor Yellow
foreach ($component in $REQUIRED_COMPONENTS) {
    $componentPath = Join-Path $COMPONENTS_DIR $component
    if (Test-Path $componentPath) {
        Write-Host "✅ $component - 存在" -ForegroundColor Green
    } else {
        Write-Host "❌ $component - 缺失" -ForegroundColor Red
        exit 1
    }
}

# 5. 替换主文件
Write-Host "🔄 替换主文件..." -ForegroundColor Yellow
Move-Item $REFACTORED_FILE $ORIGINAL_FILE -Force
Write-Host "✅ VideoSquareRefactored.vue 已重命名为 VideoSquare.vue" -ForegroundColor Green

# 6. 计算文件大小对比
if (Test-Path $BACKUP_FILE) {
    $originalLines = (Get-Content $BACKUP_FILE | Measure-Object -Line).Lines
    $newLines = (Get-Content $ORIGINAL_FILE | Measure-Object -Line).Lines
    $reduction = [math]::Round((1 - ($newLines / $originalLines)) * 100, 1)
    
    Write-Host ""
    Write-Host "📊 文件大小对比:" -ForegroundColor Cyan
    Write-Host "   原始文件: $originalLines 行" -ForegroundColor White
    Write-Host "   新文件:   $newLines 行" -ForegroundColor White
    Write-Host "   减少:     $reduction%" -ForegroundColor Green
}

# 7. 显示组件文件统计
Write-Host ""
Write-Host "📁 组件文件统计:" -ForegroundColor Cyan
foreach ($component in $REQUIRED_COMPONENTS) {
    $componentPath = Join-Path $COMPONENTS_DIR $component
    $lines = (Get-Content $componentPath | Measure-Object -Line).Lines
    Write-Host "   $component`: $lines 行" -ForegroundColor White
}

# 8. 创建组件索引文件（可选）
$INDEX_FILE = Join-Path $COMPONENTS_DIR "index.js"
Write-Host "📝 创建组件索引文件..." -ForegroundColor Yellow

$indexContent = @"
// VideoSquare组件拆分后的子组件导出
export { default as VideoDialog } from './VideoDialog.vue'
export { default as VideoLayoutControls } from './VideoLayoutControls.vue'
export { default as DeviceListPanel } from './DeviceListPanel.vue'
export { default as MapControls } from './MapControls.vue'
export { default as DebugPanel } from './DebugPanel.vue'
"@

Set-Content -Path $INDEX_FILE -Value $indexContent -Encoding UTF8
Write-Host "✅ 组件索引文件已创建: $INDEX_FILE" -ForegroundColor Green

Write-Host ""
Write-Host "🎉 VideoSquare组件拆分部署完成！" -ForegroundColor Green -BackgroundColor DarkBlue
Write-Host ""
Write-Host "📋 部署总结:" -ForegroundColor Cyan
Write-Host "   ✅ 原始文件已备份" -ForegroundColor Green
Write-Host "   ✅ 新文件已替换" -ForegroundColor Green
Write-Host "   ✅ 所有组件文件就绪" -ForegroundColor Green
Write-Host "   ✅ 组件索引文件已创建" -ForegroundColor Green
Write-Host ""
Write-Host "🔧 下一步操作:" -ForegroundColor Yellow
Write-Host "   1. 运行 npm run dev 测试应用" -ForegroundColor White
Write-Host "   2. 检查所有功能是否正常" -ForegroundColor White
Write-Host "   3. 如有问题，可从备份文件恢复" -ForegroundColor White
Write-Host ""
Write-Host "📂 备份文件位置: $BACKUP_FILE" -ForegroundColor Magenta
Write-Host "📂 组件文件位置: $COMPONENTS_DIR" -ForegroundColor Magenta

# 暂停以查看结果
Write-Host ""
Write-Host "按任意键继续..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") 