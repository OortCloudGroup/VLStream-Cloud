<template>
  <el-dialog :model-value="visible" title="训练数据集生成检查" width="880px" @close="$emit('close')">
    <el-alert :closable="false" :type="report.status === 'READY' ? 'success' : 'warning'"
      :title="report.status === 'READY' ? '训练数据包已生成' : '发现无效样本，未生成新数据包'" />
    <p>已检查 {{ report.checkedImages }} 张图片；{{ report.corrections?.length || 0 }} 个框{{ report.status === 'READY' ? '已在训练包中按图片边界裁剪' : '可按图片边界裁剪' }}。原始标注保留。</p>
    <p v-if="report.repartitioned">训练/验证划分已按图片内容重新分组，相同图片不会跨集合；原划分已备份。</p>
    <p class="report-note">这是一次生成检查的记录；查看或修改图片不会自动更新清单，请重新生成获取最新检查结果。</p>
    <el-tabs v-model="activeTab">
      <el-tab-pane :label="`需要修正 (${report.errors?.length || 0})`" name="errors" />
      <el-tab-pane :label="`裁剪清单 (${report.corrections?.length || 0})`" name="corrections" />
    </el-tabs>
    <el-table class="generation-issues-table" :data="report[activeTab] || []" max-height="420" empty-text="没有此类问题">
      <el-table-column label="图片" width="88">
        <template #default="{ row }"><el-image v-if="row.previewUrl" :src="row.previewUrl" fit="contain" style="width:64px;height:56px" :preview-src-list="[row.previewUrl]" preview-teleported /></template>
      </el-table-column>
      <el-table-column prop="name" label="文件名" min-width="160" />
      <el-table-column prop="imageId" label="图片编号" min-width="180" />
      <el-table-column label="说明" min-width="220">
        <template #default="{ row }">
          <div>{{ row.reason }}</div>
          <div v-if="row.originalBox">原框：{{ formatBox(row.originalBox) }}</div>
          <div v-if="row.exportBox">训练框：{{ formatBox(row.exportBox) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="$emit('repair', row.imageId)">去修正</el-button></template></el-table-column>
    </el-table>
    <template #footer><el-button @click="$emit('close')">关闭</el-button></template>
  </el-dialog>
</template>
<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ visible: Boolean, report: { type: Object, default: () => ({}) } })
defineEmits(['close', 'repair'])
const activeTab = ref('errors')
watch(() => `${props.report.datasetId || ''}:${props.report.jobId || props.report.reference || props.report.status || ''}`, () => { activeTab.value = props.report.errors?.length ? 'errors' : 'corrections' }, { immediate: true })
const formatBox = box => box.map(value => Number(value).toFixed(2)).join(', ')
</script>
<style scoped>
.generation-issues-table :deep(.cell) { display: block; }
.report-note { color: var(--el-text-color-secondary); font-size: 13px; }
</style>
