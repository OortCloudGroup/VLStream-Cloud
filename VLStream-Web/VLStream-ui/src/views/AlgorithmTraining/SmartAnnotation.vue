<template>
  <div class="smart-page" v-loading="loading">
    <header class="smart-heading"><div><el-button v-if="detail" link @click="leave">← 返回任务列表</el-button><h2>{{ detail ? detail.task.taskName : '智能标注' }}</h2><p>自动生成分类、目标框或像素掩膜，确认后加入数据集。</p></div><div class="actions"><el-button @click="refresh">刷新</el-button><el-button v-if="!detail" type="primary" @click="openCreate">创建任务</el-button><el-button v-else @click="openDataset">查看数据集</el-button></div></header>
    <template v-if="!detail">
      <div class="mode-cards"><div><h3>主动学习</h3><p>少量人工标注 → 训练标注模型 → 优先检查难例 → 继续下一轮</p></div><div><h3>指定模型</h3><p>选择已有模型 → 批量预标注 → 修正并确认结果</p></div></div>
      <el-alert title="预标注结果独立保存；确认后才计入已标注图片。不会覆盖已有人工标注。" type="info" :closable="false" />
      <el-table :data="tasks" empty-text="暂无智能标注任务，创建任务开始预标注">
        <el-table-column prop="taskName" label="任务名称" min-width="160" /><el-table-column label="模式" width="110"><template #default="{ row }">{{ modeName(row.mode) }}</template></el-table-column>
        <el-table-column label="标注类型" width="110"><template #default="{ row }">{{ typeNames[row.annotationType] }}</template></el-table-column>
        <el-table-column prop="modelName" label="模型来源" min-width="170" /><el-table-column label="轮次" width="75"><template #default="{ row }">{{ row.roundNumber }}</template></el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="stateType(row.taskState)">{{ states[row.taskState] || row.taskState }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" /><el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="enter(row.id)">查看任务</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="taskPage" :page-size="20" :total="taskTotal" layout="total, prev, pager, next" @current-change="refresh" />
    </template>
    <template v-else>
      <div class="task-summary"><el-tag :type="stateType(detail.task.taskState)">{{ states[detail.task.taskState] }}</el-tag><span>{{ modeName(detail.task.mode) }} · 第 {{ detail.task.roundNumber }} 轮</span><span>已预测 {{ detail.round.predictedCount }} 张</span><span>已确认 {{ detail.counts.ACCEPTED || 0 }} 张</span><span>待确认 {{ detail.counts.PENDING || 0 }} 张</span><span>已跳过 {{ detail.counts.SKIPPED || 0 }} 张</span></div>
      <el-alert v-if="detail.task.errorMessage" :title="detail.task.errorMessage" type="error" :closable="false" />
      <el-alert v-if="detail.task.bulkError" :title="detail.task.bulkError" type="warning" :closable="false" />
      <div v-if="detail.task.taskState === 'CONFIRMING' || detail.task.bulkTotal" class="bulk-summary"><span>批量确认：{{ detail.task.bulkProcessed || 0 }} / {{ detail.task.bulkTotal || 0 }}，采用 {{ detail.task.bulkAccepted || 0 }} 张，{{ detail.task.bulkConflicts || 0 }} 张需人工检查</span><el-progress v-if="detail.task.taskState === 'CONFIRMING'" :percentage="Math.min(100, Math.round((detail.task.bulkProcessed || 0) * 100 / Math.max(1, detail.task.bulkTotal || 1)))" /></div>
      <div v-if="active && detail.task.taskState !== 'CONFIRMING' && detail.round.progressTotal" class="bulk-summary"><span>{{ detail.round.progressStage === 'TRAINING' ? '本轮模型训练' : '生成预标注' }}：{{ detail.round.progressCurrent || 0 }} / {{ detail.round.progressTotal }}</span><el-progress :percentage="Math.min(100, Math.round((detail.round.progressCurrent || 0) * 100 / detail.round.progressTotal))" /></div>
      <el-alert v-if="active" :title="detail.task.taskState === 'QUEUED' ? '已进入 GPU 队列，等待前序训练或智能标注任务结束。' : '正在处理本轮数据。可以离开页面，后台将继续运行。'" type="info" :closable="false" />
      <div class="actions task-actions"><el-button @click="showLogs">查看日志</el-button><el-button v-if="detail.task.taskState === 'FAILED'" :disabled="busy" type="primary" @click="perform(retrySmartTask)">重试本轮</el-button>
        <template v-if="detail.task.taskState === 'REVIEW'"><el-button v-if="detail.task.mode === 'active'" :disabled="busy || detail.pendingHard > 0 || detail.task.roundNumber >= detail.maxRounds || !detail.counts.ACCEPTED" @click="next">继续下一轮</el-button><el-button type="primary" :disabled="busy" @click="finish">完成任务</el-button></template>
        <el-button v-if="!['COMPLETED', 'CANCELLED', 'CANCEL_REQUESTED'].includes(detail.task.taskState)" :disabled="busy" type="danger" plain @click="cancel">取消任务</el-button>
      </div>
      <template v-if="['REVIEW', 'COMPLETED'].includes(detail.task.taskState)">
        <div class="filters"><el-select v-model="query.state" @change="loadPredictions"><el-option label="待确认" value="PENDING" /><el-option label="已确认" value="ACCEPTED" /><el-option label="已跳过" value="SKIPPED" /><el-option label="全部结果" value="" /></el-select><el-checkbox v-if="detail.task.mode === 'active'" v-model="query.hardOnly" @change="loadPredictions">仅看本轮难例（待处理 {{ detail.pendingHard }}）</el-checkbox><el-button v-if="detail.task.taskState === 'REVIEW'" :disabled="busy || !confirmable.length" @click="batchConfirm">确认本页可用结果（{{ confirmable.length }}）</el-button><el-button v-if="detail.task.taskState === 'REVIEW'" type="primary" :disabled="busy || !detail.counts.PENDING" @click="confirmAll">一键确认剩余预测</el-button></div>
        <p class="hint">难例按预测置信度排序，优先检查漏检、低置信度和类别不匹配的图片。模型类别未匹配时，需手动选择标签。</p>
        <div class="prediction-grid"><article v-for="row in predictions" :key="row.candidate.id" class="prediction-card" @click="openReview(row)"><div class="image"><img :src="row.previewUrl" :alt="row.imageName" loading="lazy" /><el-tag v-if="row.candidate.hardExample" class="hard-tag" type="warning" size="small">难例</el-tag></div><div class="prediction-description"><strong>{{ row.imageName }}</strong><span>{{ boxCount(row) }} {{ countUnits[detail.task.annotationType] }} · {{ reviewStates[row.candidate.reviewState] }}</span><small v-if="row.candidate.reviewError">{{ row.candidate.reviewError }}</small><el-button v-if="row.candidate.reviewState === 'PENDING' && detail.task.taskState === 'REVIEW'" link type="primary">检查并确认</el-button></div></article></div>
        <el-empty v-if="!predictions.length" description="当前筛选下没有结果" /><el-pagination v-model:current-page="query.page" :page-size="20" :total="predictionTotal" layout="total, prev, pager, next" @current-change="loadPredictions" />
      </template>
      <details class="round-history"><summary>轮次与模型记录</summary><el-table :data="detail.rounds"><el-table-column prop="roundNumber" label="轮次" width="70" /><el-table-column prop="versionId" label="输入数据集版本 ID" width="200" /><el-table-column prop="predictedCount" label="预测图片" width="100" /><el-table-column prop="modelSha256" label="模型校验值" min-width="220" /><el-table-column prop="roundState" label="状态" width="100" /></el-table></details>
    </template>

    <el-dialog v-model="creating" title="创建智能标注任务" width="660px" :close-on-click-modal="false">
      <el-form label-width="110px"><el-form-item label="数据集"><el-select v-model="form.datasetId" filterable remote :remote-method="searchDatasets" placeholder="搜索数据集" style="width:100%" @change="loadOptions"><el-option v-for="dataset in datasets" :key="dataset.id" :label="`${dataset.annotationName} · ${typeNames[dataset.annotationType]}`" :value="String(dataset.id)" /></el-select></el-form-item>
        <el-form-item label="任务名称"><el-input v-model="form.taskName" maxlength="80" /></el-form-item><el-form-item label="标注方式"><el-radio-group v-model="form.mode" @change="sourceKey = form.mode === 'active' ? 'system:0' : ''"><el-radio value="active">主动学习</el-radio><el-radio value="model">指定模型</el-radio></el-radio-group></el-form-item>
        <el-form-item label="模型来源"><el-select v-model="sourceKey" placeholder="请选择模型" style="width:100%" filterable><el-option v-for="source in sources" :key="`${source.type}:${source.id}`" :label="source.name" :value="`${source.type}:${source.id}`" /></el-select></el-form-item>
        <el-form-item label="预测阈值"><el-slider v-model="form.confidence" :min="0.05" :max="0.95" :step="0.05" show-input /></el-form-item>
        <template v-if="form.mode === 'active'"><el-form-item label="每轮训练次数"><el-input-number v-model="form.epochs" :min="1" :max="100" /></el-form-item><el-form-item label="每轮难例数量"><el-input-number v-model="form.reviewSize" :min="1" :max="500" /></el-form-item></template>
      </el-form>
      <template v-if="options"><el-alert :title="`${typeNames[options.annotationType]}：当前有 ${options.unlabeled} 张未标注图片。${form.mode === 'active' ? `首次主动学习要求至少 ${options.minUnlabeledImages} 张未标图片，每类至少 ${options.minBoxesPerLabel}${options.countUnit}已确认标注，最多 ${options.maxRounds} 轮。` : '指定模型会预标注当前所有可用的未标注图片。'}`" :type="ready ? 'info' : 'warning'" :closable="false" /><div class="label-counts"><el-tag v-for="label in options.labelCounts" :key="label.name">{{ label.name }}：{{ label.count }} {{ options.countUnit }}</el-tag></div></template>
      <template #footer><el-button :disabled="busy" @click="creating = false">取消</el-button><el-button type="primary" :loading="busy" :disabled="!ready" @click="create">创建并入队</el-button></template>
    </el-dialog>
    <SmartAnnotationReview :item="reviewItem" :annotation-type="detail?.task.annotationType || 'object_detection'" :labels="detail?.labels || []" :saving="busy" @close="reviewItem = null" @save="saveReview" />
    <el-dialog v-model="logsOpen" title="本轮运行日志" width="85vw"><el-button :disabled="busy" @click="showLogs">刷新日志</el-button><pre class="logs">{{ logs }}</pre></el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDataProjects } from '@/api/dataManagement'
import { listSmartTasks, smartOptions, createSmartTask, getSmartTask, smartPredictions, getSmartPrediction, reviewSmartPrediction, confirmSmartBatch, confirmAllSmartPredictions, nextSmartRound, finishSmartTask, retrySmartTask, cancelSmartTask, smartTaskLogs } from '@/api/smartAnnotation'
import SmartAnnotationReview from './components/SmartAnnotationReview.vue'

const route = useRoute(); const router = useRouter()
const tasks = ref([]); const taskPage = ref(1); const taskTotal = ref(0); const detail = ref(null)
const loading = ref(false); const busy = ref(false); const creating = ref(false)
const datasets = ref([]); const options = ref(null); const sourceKey = ref('')
const predictions = ref([]); const predictionTotal = ref(0); const reviewItem = ref(null)
const logsOpen = ref(false); const logs = ref('')
const form = reactive({ datasetId: '', taskName: '', mode: 'active', confidence: 0.25, epochs: 10, reviewSize: 50 })
const query = reactive({ page: 1, state: 'PENDING', hardOnly: false })
const states = { QUEUED: '等待 GPU', PREPARING: '准备数据', RUNNING: '计算中', REVIEW: '待确认', CONFIRMING: '批量确认中', COMPLETED: '已完成', FAILED: '失败', CANCEL_REQUESTED: '正在取消', CANCELLED: '已取消' }
const typeNames = { image_classification: '图像分类', object_detection: '物体检测', instance_segmentation: '实例分割', semantic_segmentation: '语义分割' }
const countUnits = { image_classification: '个类别', object_detection: '个预测框', instance_segmentation: '个实例', semantic_segmentation: '类区域' }
const reviewStates = { PENDING: '待确认', ACCEPTED: '已确认', SKIPPED: '已跳过' }
const modeName = mode => mode === 'active' ? '主动学习' : '指定模型'
const stateType = state => ({ COMPLETED: 'success', FAILED: 'danger', REVIEW: 'warning' }[state] || 'info')
const active = computed(() => detail.value && ['QUEUED', 'PREPARING', 'RUNNING', 'CONFIRMING', 'CANCEL_REQUESTED'].includes(detail.value.task.taskState))
const sources = computed(() => (options.value?.sources || []).filter(source => form.mode === 'active' || source.type !== 'algorithm'))
const ready = computed(() => form.datasetId && form.taskName.trim() && sourceKey.value && options.value?.unlabeled > 0 && (form.mode !== 'active' || (options.value.unlabeled >= options.value.minUnlabeledImages && options.value.labelCounts.every(label => label.count >= options.value.minBoxesPerLabel))))
const boxCount = row => JSON.parse(row.candidate.boxesJson || '[]').length
const confirmable = computed(() => predictions.value.filter(row => {
  const boxes = JSON.parse(row.candidate.boxesJson || '[]')
  return row.candidate.reviewState === 'PENDING' && boxes.length && boxes.every(box => box.labelId && box.confidence >= detail.value.task.confidence)
}))
const handleError = error => { if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '操作失败') }
const run = async operation => { if (busy.value) return; busy.value = true; try { await operation() } catch (error) { handleError(error) } finally { busy.value = false } }
const loadPredictions = async () => {
  try { const result = await smartPredictions(detail.value.task.id, query); predictions.value = result.records; predictionTotal.value = result.total } catch (error) { handleError(error) }
}
const refresh = async () => {
  if (loading.value) return
  loading.value = true
  try {
    if (detail.value) { detail.value = await getSmartTask(detail.value.task.id); if (['REVIEW', 'COMPLETED'].includes(detail.value.task.taskState)) await loadPredictions() }
    else { const result = await listSmartTasks({ page: taskPage.value, datasetId: route.query.dataset }); tasks.value = result.records; taskTotal.value = result.total }
  } catch (error) { handleError(error) } finally { loading.value = false }
}
const enter = id => run(async () => { detail.value = await getSmartTask(id); query.page = 1; query.state = 'PENDING'; query.hardOnly = detail.value.task.mode === 'active'; await router.replace({ query: { ...route.query, task: String(id) } }); if (['REVIEW', 'COMPLETED'].includes(detail.value.task.taskState)) await loadPredictions() })
const leave = async () => { detail.value = null; reviewItem.value = null; await router.replace({ query: route.query.dataset ? { dataset: route.query.dataset } : {} }); await refresh() }
const openDataset = () => router.push({ path: '/data-management', query: { dataset: detail.value.task.datasetId } })
const searchDatasets = async keyword => { try { const result = await listDataProjects({ keyword, size: 100 }); datasets.value = result.records.filter(dataset => typeNames[dataset.annotationType]) } catch (error) { handleError(error) } }
let optionSequence = 0
const loadOptions = async () => { const sequence = ++optionSequence; options.value = null; sourceKey.value = ''; try { const result = await smartOptions(form.datasetId); if (sequence === optionSequence) { options.value = result; if (form.mode === 'active') sourceKey.value = 'system:0'; if (!form.taskName) form.taskName = `${datasets.value.find(dataset => String(dataset.id) === form.datasetId)?.annotationName || '数据集'}智能标注` } } catch (error) { handleError(error) } }
const openCreate = async () => { creating.value = true; await searchDatasets(''); if (route.query.dataset) { form.datasetId = String(route.query.dataset); await loadOptions() } }
const create = () => run(async () => { const [sourceType, sourceId] = sourceKey.value.split(':'); const result = await createSmartTask({ ...form, taskName: form.taskName.trim(), sourceType, sourceId }); creating.value = false; detail.value = await getSmartTask(result.id); await router.replace({ query: { ...route.query, task: String(result.id) } }); ElMessage.success('智能标注任务已入队') })
const openReview = row => run(async () => {
  if (row.candidate.reviewState !== 'PENDING' || detail.value.task.taskState !== 'REVIEW') return
  const result = await getSmartPrediction(detail.value.task.id, row.candidate.id)
  busy.value = false
  reviewItem.value = result
})
const saveReview = request => run(async () => { await reviewSmartPrediction(detail.value.task.id, reviewItem.value.candidate.id, request); reviewItem.value = null; await refresh(); ElMessage.success(request.skip ? '已跳过' : '标注已确认并保存') })
const batchConfirm = () => run(async () => { await ElMessageBox.confirm(`将本页 ${confirmable.value.length} 张图片的全部预测框保存为正式标注。请先检查预测效果。`, '批量确认', { type: 'warning' }); await confirmSmartBatch(detail.value.task.id, confirmable.value.map(row => row.candidate.id)); await refresh() })
const confirmAll = () => run(async () => { await ElMessageBox.confirm(`检查效果满意后，可采用本轮剩余 ${detail.value.counts.PENDING || 0} 张图片的预测。低置信度、标签未匹配或存在冲突的图片会留待人工处理。`, '一键确认剩余预测', { type: 'warning', confirmButtonText: '采用剩余预测' }); await confirmAllSmartPredictions(detail.value.task.id); await refresh() })
const perform = operation => run(async () => { await operation(detail.value.task.id); await refresh() })
const next = () => run(async () => { await ElMessageBox.confirm('使用当前已确认标注训练下一轮模型，并重新筛选剩余图片。未确认结果不会用于训练。', '继续主动学习'); await nextSmartRound(detail.value.task.id); predictions.value = []; await refresh() })
const finish = () => run(async () => { await ElMessageBox.confirm('完成任务并保存数据集版本。未确认结果会保留在任务记录中，不会自动写入正式标注。', '完成智能标注'); await finishSmartTask(detail.value.task.id); await refresh() })
const cancel = () => run(async () => { await ElMessageBox.confirm('取消当前智能标注任务，已确认标注保留。运行中的本任务容器将停止。', '取消智能标注', { type: 'warning' }); await cancelSmartTask(detail.value.task.id); await refresh() })
const showLogs = () => run(async () => { logsOpen.value = true; logs.value = await smartTaskLogs(detail.value.task.id) })
let timer
onMounted(async () => { if (route.query.task) await enter(String(route.query.task)); else await refresh(); timer = setInterval(() => { if (active.value && !busy.value && !reviewItem.value) refresh() }, 5000) })
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.smart-page{padding:24px;max-width:1600px;margin:auto}.smart-heading,.actions,.task-summary,.filters{display:flex;align-items:center;gap:14px;flex-wrap:wrap}.smart-heading{justify-content:space-between;margin-bottom:24px}.smart-heading h2{margin:8px 0}.smart-heading p,.hint{color:var(--el-text-color-secondary);font-size:13px}.mode-cards{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px}.mode-cards>div{padding:18px 22px;border:1px solid var(--el-border-color);border-radius:10px;background:var(--el-bg-color)}.mode-cards h3{margin-top:0}.mode-cards p{line-height:1.7;color:var(--el-text-color-secondary)}.el-table{margin-top:20px}.el-pagination{margin-top:20px;justify-content:flex-end}.task-summary{padding:18px;border:1px solid var(--el-border-color);border-radius:10px;margin-bottom:20px}.task-actions{margin:20px 0}.filters .el-select{width:160px}.prediction-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:18px}.prediction-card{overflow:hidden;border:1px solid var(--el-border-color);border-radius:9px;cursor:pointer;background:var(--el-bg-color)}.prediction-card .image{height:170px;position:relative;background:#142333}.prediction-card img{width:100%;height:100%;object-fit:contain}.hard-tag{position:absolute;left:8px;top:8px}.prediction-description{padding:12px;display:flex;flex-direction:column;gap:8px}.prediction-description strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.prediction-description span{font-size:12px;color:var(--el-text-color-secondary)}.round-history{margin-top:28px}.label-counts{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}.logs{white-space:pre-wrap;word-break:break-word;background:#102032;color:#d1e2f4;padding:16px;max-height:60vh;overflow:auto;font-size:12px}@media(max-width:700px){.smart-page{padding:14px}.mode-cards{grid-template-columns:1fr}.prediction-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
