#!/bin/bash

# VideoSquare组件拆分部署脚本
# 该脚本用于部署重构后的VideoSquare组件

echo "🚀 开始部署重构后的VideoSquare组件..."

# 定义路径
ORIGINAL_FILE="VLStream-ui/src/views/VideoAggregation/VideoSquare.vue"
REFACTORED_FILE="VLStream-ui/src/views/VideoAggregation/VideoSquareRefactored.vue"
BACKUP_FILE="VLStream-ui/src/views/VideoAggregation/VideoSquare.vue.backup"
COMPONENTS_DIR="VLStream-ui/src/views/VideoAggregation/components"

# 1. 备份原始文件
echo "📦 备份原始VideoSquare.vue文件..."
if [ -f "$ORIGINAL_FILE" ]; then
    cp "$ORIGINAL_FILE" "$BACKUP_FILE"
    echo "✅ 原始文件已备份到: $BACKUP_FILE"
else
    echo "⚠️  原始文件不存在，跳过备份"
fi

# 2. 检查重构后的文件是否存在
if [ ! -f "$REFACTORED_FILE" ]; then
    echo "❌ 重构后的文件不存在: $REFACTORED_FILE"
    exit 1
fi

# 3. 检查组件目录是否存在
if [ ! -d "$COMPONENTS_DIR" ]; then
    echo "❌ 组件目录不存在: $COMPONENTS_DIR"
    exit 1
fi

# 4. 检查所有必需的组件文件
REQUIRED_COMPONENTS=(
    "VideoDialog.vue"
    "VideoLayoutControls.vue" 
    "DeviceListPanel.vue"
    "MapControls.vue"
    "DebugPanel.vue"
)

echo "🔍 检查组件文件..."
for component in "${REQUIRED_COMPONENTS[@]}"; do
    if [ -f "$COMPONENTS_DIR/$component" ]; then
        echo "✅ $component - 存在"
    else
        echo "❌ $component - 缺失"
        exit 1
    fi
done

# 5. 替换主文件
echo "🔄 替换主文件..."
mv "$REFACTORED_FILE" "$ORIGINAL_FILE"
echo "✅ VideoSquareRefactored.vue 已重命名为 VideoSquare.vue"

# 6. 计算文件大小对比
if [ -f "$BACKUP_FILE" ]; then
    ORIGINAL_LINES=$(wc -l < "$BACKUP_FILE")
    NEW_LINES=$(wc -l < "$ORIGINAL_FILE")
    REDUCTION=$((100 - (NEW_LINES * 100 / ORIGINAL_LINES)))
    
    echo ""
    echo "📊 文件大小对比:"
    echo "   原始文件: $ORIGINAL_LINES 行"
    echo "   新文件:   $NEW_LINES 行"
    echo "   减少:     $REDUCTION%"
fi

# 7. 显示组件文件统计
echo ""
echo "📁 组件文件统计:"
for component in "${REQUIRED_COMPONENTS[@]}"; do
    lines=$(wc -l < "$COMPONENTS_DIR/$component")
    echo "   $component: $lines 行"
done

# 8. 创建组件索引文件（可选）
INDEX_FILE="$COMPONENTS_DIR/index.js"
echo "📝 创建组件索引文件..."
cat > "$INDEX_FILE" << 'EOF'
// VideoSquare组件拆分后的子组件导出
export { default as VideoDialog } from './VideoDialog.vue'
export { default as VideoLayoutControls } from './VideoLayoutControls.vue'
export { default as DeviceListPanel } from './DeviceListPanel.vue'
export { default as MapControls } from './MapControls.vue'
export { default as DebugPanel } from './DebugPanel.vue'
EOF
echo "✅ 组件索引文件已创建: $INDEX_FILE"

echo ""
echo "🎉 VideoSquare组件拆分部署完成！"
echo ""
echo "📋 部署总结:"
echo "   ✅ 原始文件已备份"
echo "   ✅ 新文件已替换"
echo "   ✅ 所有组件文件就绪"
echo "   ✅ 组件索引文件已创建"
echo ""
echo "🔧 下一步操作:"
echo "   1. 运行 npm run dev 测试应用"
echo "   2. 检查所有功能是否正常"
echo "   3. 如有问题，可从备份文件恢复"
echo ""
echo "📂 备份文件位置: $BACKUP_FILE"
echo "📂 组件文件位置: $COMPONENTS_DIR" 