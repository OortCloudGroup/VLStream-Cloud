<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<!--
 * @Created by:
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-03-25 18:11:12
 * @Last Modified by:
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
    <!-- all -->
    <template v-for="(_, slotName) in $slots" #[slotName]="slotProps">
      <slot :name="slotName" v-bind="slotProps" />
    </template>
    <!-- ( ) -->
    <slot />
  </el-table>
</template>
<script setup>
import { ref, useAttrs, computed, watch } from 'vue'

defineProps({
  // Customproperty
  customClass: {
    type: String,
    default: ''
  }
})

const selectedRows = ref([])
const currentRows = ref(null)

// eventProcess
const handleRowClick = (row) => {
  currentRows.value = row
  // in , element-plus select event selection
  if (tableRef.value) {
    tableRef.value.toggleRowSelection(row)
  }
}

//
const handleSelect = (selection) => {
  // selection is current all already in
  selectedRows.value = selection.slice()
}

// Get not property
const $attrs = useAttrs()

// class and style
const mergedAttrs = computed(() => {
  const { class: _, style: __, ...rest } = $attrs
  return rest
})

// table instance
const tableRef = ref(null)
defineExpose({
  tableRef
})

watch(() => $attrs.data, (newData) => {
  if (!Array.isArray(newData)) return
  // if current-row-key is in current-row-key
  if (tableRef.value && currentRows.value) {
    let findRow = null
    if ($attrs['current-row-key']) {
      findRow = newData.find(item => item[$attrs['current-row-key']] === currentRows.value[$attrs['current-row-key']])
    } else {
      // id
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
