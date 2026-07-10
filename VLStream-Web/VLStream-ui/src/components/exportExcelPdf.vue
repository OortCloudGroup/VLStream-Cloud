<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-3-18 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="exportExcelPDF flexRowAC">
    <el-dropdown trigger="hover">
      <el-button class="exportBtn Btn2 flexRowAC" title="导出">
        <oort-svg-icon class="BtnImg export" name="export" />
        <div class="line" />
        <el-icon>
          <ArrowDown />
        </el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item :disabled="exportItem.isDisabledExcel" @click.stop="emits('handle', 'Excel')">
            <img class="exportImg" src="@/assets/img/exportExcel.png" alt="" />
            Excel
          </el-dropdown-item>
          <el-dropdown-item :disabled="exportItem.isDisabledPDF" @click.stop="emits('handle', 'PDF')">
            <img class="exportImg" src="@/assets/img/exportPDF.png" alt="" />
            PDF
          </el-dropdown-item>
          <el-dropdown-item :disabled="exportItem.isDisabledHTML" @click.stop="emits('handle', 'HTML')">
            <img class="exportImg" src="@/assets/img/exportHTM.png" alt="" />
            HTML
          </el-dropdown-item>
          <el-dropdown-item :disabled="exportItem.isDisabledXML" @click.stop="emits('handle', 'XML')">
            <img class="exportImg" src="@/assets/img/exportXML.png" alt="" />
            XML
          </el-dropdown-item>
          <el-dropdown-item :disabled="exportItem.isDisabledJSON" @click.stop="emits('handle', 'JSON')">
            <img class="exportImg" src="@/assets/img/exportJSON.png" alt="" />
            JSON
          </el-dropdown-item>
          <el-dropdown-item :disabled="exportItem.isDisabledCSV" @click.stop="emits('handle', 'CSV')">
            <img class="exportImg" src="@/assets/img/exportCSV.png" alt="" />
            CSV
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    default: () => ({})
  }
})
const emits = defineEmits(['handle'])

// 处理默认都为 true，只有传 false 才为 false
const exportItem = computed(() => ({
  isDisabledExcel: props.item.isDisabledExcel !== false,
  isDisabledPDF: props.item.isDisabledPDF !== false,
  isDisabledHTML: props.item.isDisabledHTML !== false,
  isDisabledXML: props.item.isDisabledXML !== false,
  isDisabledJSON: props.item.isDisabledJSON !== false,
  isDisabledCSV: props.item.isDisabledCSV !== false
})).value
</script>

<style lang="scss" scoped>

.exportImg{
  width: 20px;
  height: 20px;
  margin-right: 4px;
}
</style>
