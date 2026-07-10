<template>
  <div class="system-page">
    <!-- 工具栏插槽，包含搜索表单、新增/编辑/批量删除按钮等 -->
    <div v-if="$slots.toolbar" class="system-toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 数据主体展示区域，通常为一个 el-table 或 el-tree-table -->
    <div class="system-content">
      <slot />
    </div>

    <!-- 底部分页插槽 -->
    <div v-if="$slots.pagination" class="system-pagination">
      <slot name="pagination" />
    </div>
  </div>
</template>

<script setup>
// SystemPageShell 是一个简单的布局外壳组件，用来保持所有系统管理页面一致的高宽比例、背景色和边距样式。
</script>

<style scoped>
.system-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

.system-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.system-content {
  flex: 1;
  min-height: 0;
  background: #fff;
  padding: 20px;
  overflow: hidden;
}

.system-pagination {
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-radius: 0 0 8px 8px;
  padding: 0 20px 20px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.el-button--primary) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-button--primary:hover) {
  background-color: #4A72FF;
  border-color: #4A72FF;
}
</style>
