<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-03-25 18:11:12
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <el-table
    ref="tableRef"
    v-bind="mergedAttrs"
    :class="[customClass, $attrs.class]"
    :style="$attrs.style"
    highlight-current-row
    @row-click="handleRowClick"
    @select="handleSelect"
  >
    <!-- 透传所有插槽 -->
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps" />
    </template>
    <!-- 默认插槽（用于列） -->
    <slot />
  </el-table>
</template>
<script setup>
import { ref, useAttrs, computed, watch } from 'vue'

defineProps({
  // 可添加自定义属性
  customClass: {
    type: String,
    default: ''
  }
})

const selectedRows = ref([])
const currentRows = ref(null)

// 点击行事件处理
const handleRowClick = (row) => {
  currentRows.value = row
  // 直接切换该行的选中状态，由 element-plus 内部 select 事件来维护 selection
  if (tableRef.value) {
    tableRef.value.toggleRowSelection(row)
  }
}

// 保持勾选状态同步
const handleSelect = (selection) => {
  // selection 是当前所有已选中的行
  selectedRows.value = selection.slice()
}

// 获取未声明的原生属性
const $attrs = useAttrs()

// 过滤掉 class 和 style 避免重复
const mergedAttrs = computed(() => {
  const { class: _, style: __, ...rest } = $attrs
  return rest
})

// 暴露 table 实例
const tableRef = ref(null)
defineExpose({
  tableRef
})

watch(() => $attrs.data, (newData) => {
  if (!Array.isArray(newData)) return
  // 如果有current-row-key 则回显是恢复选中 使用current-row-key
  if (tableRef.value && currentRows.value) {
    let findRow = null
    if ($attrs['current-row-key']) {
      findRow = newData.find(item => item[$attrs['current-row-key']] === currentRows.value[$attrs['current-row-key']])
    } else {
      // 默认id
      if (newData.length > 0 && newData[0].id) {
        findRow = newData.find(item => item['id'] === currentRows.value['id'])
      }
    }
    setTimeout(() => {
      if (findRow) {
        tableRef.value.setCurrentRow(findRow)
        tableRef.value.toggleRowSelection(findRow)
      }
    }, 0)
  }
})

</script>

<style scoped>
</style>
