# ActionButtonGroup 操作按钮组组件

## 概述

ActionButtonGroup 是一个公共的操作按钮组组件，提供了统一的新增、编辑、删除按钮样式和功能。该组件遵循 UI 设计规范，支持无缝连接的圆角设计。

## 特性

- ✅ 统一的UI设计规范（82px×36px，18px圆角）
- ✅ 无缝连接的编辑删除按钮组设计
- ✅ 自动计算禁用状态（基于选中行数）
- ✅ 支持批量删除文本切换
- ✅ 可配置的按钮显示/隐藏
- ✅ 额外按钮插槽支持
- ✅ 完整的事件系统

## 安装和导入

```vue
<script setup>
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
</script>
```

## 基础用法

```vue
<template>
  <ActionButtonGroup 
    :selected-count="selectedRows.length"
    @add="handleAdd"
    @edit="handleEdit"
    @delete="handleDelete"
  />
</template>
```

## API

### Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| selectedCount | Number | 0 | 选中的行数，用于控制编辑删除按钮的禁用状态 |
| showAdd | Boolean | true | 是否显示新增按钮 |
| showEditDelete | Boolean | true | 是否显示编辑删除按钮组 |
| addText | String | '新增' | 新增按钮文本 |
| editText | String | '编辑' | 编辑按钮文本 |
| deleteText | String | '删除' | 删除按钮文本 |
| batchDeleteText | String | '批量删除' | 批量删除按钮文本 |
| addDisabled | Boolean | false | 自定义新增按钮禁用状态 |
| editDisabled | Boolean | undefined | 自定义编辑按钮禁用状态（undefined时自动计算） |
| deleteDisabled | Boolean | undefined | 自定义删除按钮禁用状态（undefined时自动计算） |

### Events

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| add | 点击新增按钮时触发 | - |
| edit | 点击编辑按钮时触发 | - |
| delete | 点击删除按钮时触发 | - |

### Slots

| 名称 | 说明 |
|------|------|
| extra-buttons | 额外按钮插槽，用于添加自定义按钮 |

## 完整示例

```vue
<template>
  <div class="page-container">
    <!-- 操作按钮区域 -->
    <div class="toolbar-section">
      <div class="toolbar-left">
        <ActionButtonGroup 
          :selected-count="selectedRows.length"
          @add="handleAdd"
          @edit="handleEdit"
          @delete="handleDelete"
        >
          <template #extra-buttons>
            <el-button @click="handleImport">
              <el-icon><Upload /></el-icon>
              导入
            </el-button>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </template>
        </ActionButtonGroup>
      </div>
    </div>

    <!-- 表格区域 -->
    <el-table 
      :data="tableData"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <!-- 其他列... -->
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ActionButtonGroup from '@/components/ActionButtonGroup.vue'
import { Upload, Download } from '@element-plus/icons-vue'

const selectedRows = ref([])
const tableData = ref([
  // 表格数据...
])

// 事件处理
const handleAdd = () => {
  console.log('新增操作')
}

const handleEdit = () => {
  console.log('编辑操作', selectedRows.value[0])
}

const handleDelete = () => {
  if (selectedRows.value.length === 1) {
    console.log('删除单个项目', selectedRows.value[0])
  } else {
    console.log('批量删除', selectedRows.value)
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleImport = () => {
  console.log('导入操作')
}

const handleExport = () => {
  console.log('导出操作')
}
</script>
```

## 禁用逻辑

组件内置了智能的禁用逻辑：

- **编辑按钮**：当且仅当选中1行时启用
- **删除按钮**：选中至少1行时启用
- **批量删除文本**：选中多行时自动显示"批量删除"

如需自定义禁用逻辑，可传入对应的 disabled props：

```vue
<ActionButtonGroup 
  :selected-count="selectedRows.length"
  :edit-disabled="customEditDisabled"
  :delete-disabled="customDeleteDisabled"
  @add="handleAdd"
  @edit="handleEdit"
  @delete="handleDelete"
/>
```

## 样式规范

- **新增按钮**：82px×36px，18px圆角，#1A53FF背景
- **编辑删除按钮组**：无缝连接设计
  - 编辑按钮：左圆角（18px 0 0 18px）
  - 删除按钮：右圆角（0 18px 18px 0），红色字体

## 已应用页面

- ✅ 算法标注页面（AlgorithmStandard.vue）
- ✅ 设备管理页面（DeviceManagement.vue）

## 注意事项

1. 使用该组件时，页面中原有的按钮样式代码可以删除
2. 确保传入正确的 selectedCount 以获得最佳的用户体验
3. 额外按钮建议使用Element Plus的默认样式以保持一致性 