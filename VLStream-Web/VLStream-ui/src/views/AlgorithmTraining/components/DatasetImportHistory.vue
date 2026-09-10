<template>
  <section class="import-history">
    <div class="history-heading"><h4>导入记录</h4><el-button link type="primary" @click="refresh">刷新</el-button></div>
    <el-table :data="jobs" max-height="300" empty-text="暂无导入任务">
      <el-table-column prop="filename" label="文件 / 目录" min-width="160" show-overflow-tooltip />
      <el-table-column label="任务" width="100"><template #default="{ row }">{{ row.importType === 'video-frames' ? '视频切图' : '数据导入' }}</template></el-table-column>
      <el-table-column prop="sourceName" label="来源" width="100" show-overflow-tooltip />
      <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="stateColor(row.jobState)">{{ states[row.jobState] || row.jobState }}</el-tag></template></el-table-column>
      <el-table-column label="进度与结果" min-width="190"><template #default="{ row }"><span v-if="row.jobState === 'PROCESSING' && row.progressMessage">{{ row.progressMessage }}</span><span v-else>成功 {{ row.importedFiles || 0 }} / 跳过 {{ row.skippedFiles || 0 }} / 失败 {{ row.failedFiles || 0 }}</span></template></el-table-column>
      <el-table-column label="操作" width="145"><template #default="{ row }"><el-button link @click="details = row">详情</el-button><el-button v-if="['FAILED', 'PARTIAL'].includes(row.jobState)" link type="primary" @click="retry(row)">重试</el-button><el-button v-if="['UPLOADING', 'QUEUED'].includes(row.jobState)" link type="danger" @click="cancel(row)">取消</el-button></template></el-table-column>
    </el-table>
    <p class="history-tip">上传未完成的任务保留 7 天。在本地导入中重新选择原文件，点击“开始 / 继续上传”可续传；上传完成后后台继续解析，关闭页面不影响已排队的导入。</p>
    <el-dialog :model-value="Boolean(details)" title="导入任务详情" width="700px" append-to-body @close="details = null">
      <template v-if="details"><p>文件：{{ details.filename }} · {{ states[details.jobState] }}</p><el-alert v-if="details.errorMessage" :title="details.errorMessage" type="warning" :closable="false" /><el-table :data="results(details)" max-height="350"><el-table-column prop="filename" label="文件" /><el-table-column label="结果"><template #default="{ row }">{{ row.success ? row.duplicate ? '重复，已跳过' : '成功' : row.message }}</template></el-table-column></el-table><p class="history-tip">详情最多显示 500 条，汇总数量以导入记录为准。</p></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDatasetImports, retryDatasetImport, cancelDatasetImport } from '@/api/datasetImport'
const props = defineProps({ datasetId: { type: [String, Number], default: undefined } })
const emit = defineEmits(['completed'])
const jobs = ref([]), details = ref(null)
const states = { UPLOADING: '等待上传完成', COMPLETING: '合并中', QUEUED: '等待导入', PROCESSING: '解析 / 导入中', COMPLETED: '导入完成', PARTIAL: '部分成功', FAILED: '失败', CANCELLED: '已取消', EXPIRED: '暂存已过期' }
const stateColor = state => ({ COMPLETED: 'success', FAILED: 'danger', PARTIAL: 'warning' })[state] || 'info'
const results = job => { try { return JSON.parse(job.resultJson || '[]') } catch { return [] } }
let timer, fetching = false, disposed = false
const refresh = async () => {
  if (fetching || disposed) return
  fetching = true
  try {
    const next = await listDatasetImports(props.datasetId)
    if (disposed) return
    if (next.some(job => ['COMPLETED', 'PARTIAL'].includes(job.jobState) && jobs.value.some(old => old.id === job.id && old.jobState !== job.jobState))) emit('completed')
    jobs.value = next
  } catch (error) { if (!timer) ElMessage.error(error.message) } finally { fetching = false }
}
const retry = async row => { try { await retryDatasetImport(row.id); await refresh() } catch (e) { ElMessage.error(e.message) } }
const cancel = async row => { try { await ElMessageBox.confirm('取消此上传或排队任务并清理暂存文件？已经导入的样本会保留。', '取消任务'); await cancelDatasetImport(row.id); await refresh() } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message) } }
watch(() => props.datasetId, () => { jobs.value = []; refresh() })
onMounted(() => { refresh(); timer = setInterval(refresh, 3000) })
onUnmounted(() => { disposed = true; clearInterval(timer) })
defineExpose({ refresh })
</script>
<style scoped>
.import-history { margin-top: 24px; } .history-heading { display: flex; align-items: center; justify-content: space-between; } h4 { margin: 0 0 12px; } .history-tip { font-size: 12px; color: #909399; line-height: 1.8; }
</style>
