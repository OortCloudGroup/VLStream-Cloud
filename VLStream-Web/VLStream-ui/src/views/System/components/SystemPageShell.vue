<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="system-page">
    <!-- , form、Add / /Batch delete button etc. -->
    <div v-if="$slots.toolbar" class="system-toolbar">
      <slot name="toolbar" />
    </div>

    <!-- data main , to el-table el-tree-table -->
    <div class="system-content">
      <slot />
    </div>

    <!--  -->
    <div v-if="$slots.pagination" class="system-pagination">
      <slot name="pagination" />
    </div>
  </div>
</template>

<script setup>
// SystemPageShell is component, all page 、 and .
</script>

<style scoped>
.system-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  margin: 0;
  padding: 20px;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  overflow: hidden;
  box-sizing: border-box;
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
