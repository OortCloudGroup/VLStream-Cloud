<template>
  <div class="annotation-page" v-loading="loading">
    <header><div><el-button link @click="back">← 返回数据集</el-button><h2>{{ project?.annotationName || '图片标注' }}</h2><p>{{ names[project?.annotationType] }} · {{ descriptions[project?.annotationType] }}</p></div><div class="actions"><el-button @click="addLabel">添加标签</el-button><el-button type="primary" @click="router.push({ path: '/smart-annotation', query: { dataset: datasetId } })">智能标注</el-button></div></header>
    <div class="labels"><el-tag v-for="label in labels" :key="label.id">{{ label.name }}</el-tag><span v-if="!labels.length">请先添加需要识别的标签。</span></div>
    <div class="filters"><el-select v-model="query.annotationStatus" clearable placeholder="全部标注状态" @change="search"><el-option label="未标注" value="unannotated" /><el-option label="已标注" value="annotated" /></el-select><el-input v-model="query.keyword" clearable placeholder="图片名称" @keyup.enter="search" @clear="search" /><el-button @click="search">查询</el-button></div>
    <div class="sample-grid"><article v-for="sample in samples" :key="sample.id"><div class="sample-image" @click="edit(sample)"><img :src="sample.previewUrl" :alt="sample.imageName" loading="lazy" /></div><div class="sample-caption"><strong>{{ sample.imageName }}</strong><span>{{ sample.annotationCount ? '已标注' : '未标注' }}</span><div><el-button link type="primary" :disabled="busy" @click="edit(sample)">编辑标注</el-button><el-button v-if="sample.annotationCount" link type="danger" :disabled="busy" @click="clear(sample)">清除本图标注</el-button></div></div></article></div>
    <el-empty v-if="!samples.length" description="暂无匹配图片，可返回数据集导入图片或先对视频切图" />
    <el-pagination v-model:current-page="query.page" :page-size="20" :total="total" layout="total, prev, pager, next" @current-change="refresh" />
    <SmartAnnotationReview :item="item" :annotation-type="project?.annotationType || 'object_detection'" :labels="labels" :saving="busy" title="编辑标注" :allow-skip="false" @close="item = null" @save="save" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDataProject, listDataSamples } from '@/api/dataManagement'
import { createAnnotationLabel, getAnnotationLabels } from '@/api/annotationLabel'
import { getDatasetAnnotation, saveDatasetAnnotation } from '@/api/datasetAnnotation'
import SmartAnnotationReview from './components/SmartAnnotationReview.vue'

const route = useRoute(); const router = useRouter(); const datasetId = String(route.query.dataset || '')
const project = ref(null); const labels = ref([]); const samples = ref([]); const item = ref(null); const total = ref(0); const loading = ref(false); const busy = ref(false)
const query = reactive({ page: 1, size: 20, mediaType: 'image', keyword: '', annotationStatus: '' })
const names = { image_classification: '图像分类', object_detection: '物体检测', instance_segmentation: '实例分割', semantic_segmentation: '语义分割' }
const descriptions = { image_classification: '每张图片选择一个类别', object_detection: '标记目标的位置和类别', instance_segmentation: '为每个目标标注独立的像素区域', semantic_segmentation: '为图片中的每个像素指定类别' }
const fail = error => { if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '操作失败') }
const run = async action => { if (busy.value) return; busy.value = true; try { await action() } catch (error) { fail(error) } finally { busy.value = false } }
const refresh = async () => { loading.value = true; try { const result = await listDataSamples(datasetId, query); samples.value = result.records; total.value = result.total } catch (error) { fail(error) } finally { loading.value = false } }
const loadLabels = async () => { const response = await getAnnotationLabels(datasetId); if (response.code !== 200 || response.success === false) throw new Error(response.msg || '标签加载失败'); labels.value = response.data || [] }
const search = () => { query.page = 1; refresh() }
const back = () => router.push({ path: '/data-management', query: { dataset: datasetId } })
const addLabel = () => run(async () => {
  const answer = await ElMessageBox.prompt('请输入标签名称', '添加标签', { inputValidator: value => Boolean(value?.trim()) && value.trim().length <= 40 || '请输入1至40字的标签名称' })
  const response = await createAnnotationLabel(datasetId, { name: answer.value.trim(), color: '#409eff' })
  if (response.code !== 200 || response.success === false) throw new Error(response.msg || '创建标签失败')
  await loadLabels()
})
const edit = sample => run(async () => {
  const result = await getDatasetAnnotation(datasetId, sample.id)
  if (result.legacyError) await ElMessageBox.confirm(`${result.legacyError}。保存前会备份历史标注版本，是否重新标注这张图片？`, '确认历史标注格式', { type: 'warning' })
  labels.value = result.labels
  busy.value = false
  item.value = { ...result, candidate: { boxesJson: JSON.stringify(result.regions) } }
})
const save = request => run(async () => {
  await saveDatasetAnnotation(datasetId, item.value.imageId, { ...request, revision: item.value.revision, replaceLegacy: Boolean(item.value.legacyError) })
  item.value = null; await refresh(); ElMessage.success('标注已保存')
})
const clear = sample => run(async () => {
  await ElMessageBox.confirm(`清除“${sample.imageName}”的全部标注，原图片保留。`, '清除本图标注', { type: 'warning' })
  const current = await getDatasetAnnotation(datasetId, sample.id)
  await saveDatasetAnnotation(datasetId, sample.id, { boxes: [], revision: current.revision, replaceLegacy: Boolean(current.legacyError) })
  await refresh()
})
onMounted(async () => { try { if (!/^\d+$/.test(datasetId)) throw new Error('请从数据集进入标注'); project.value = await getDataProject(datasetId); await loadLabels(); await refresh() } catch (error) { fail(error) } })
</script>

<style scoped>
.annotation-page{padding:24px;max-width:1600px;margin:auto}.annotation-page header{display:flex;justify-content:space-between;align-items:center;gap:16px}.annotation-page header p{color:var(--el-text-color-secondary)}.actions,.labels,.filters{display:flex;gap:12px;align-items:center;flex-wrap:wrap}.labels{margin:16px 0}.filters{margin-bottom:20px}.filters .el-select,.filters .el-input{width:220px}.sample-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:18px}.sample-grid article{border:1px solid var(--el-border-color);border-radius:9px;overflow:hidden}.sample-image{height:180px;background:#162536;cursor:pointer}.sample-image img{width:100%;height:100%;object-fit:contain}.sample-caption{padding:12px;display:flex;gap:8px;flex-direction:column}.sample-caption strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.sample-caption span{font-size:12px;color:var(--el-text-color-secondary)}.el-pagination{justify-content:flex-end;margin-top:20px}
</style>
