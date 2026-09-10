<template>
  <el-dialog :model-value="modelValue" title="导入数据" width="min(1000px, 94vw)" :close-on-click-modal="false" :before-close="close" @closed="emit('update:modelValue', false)">
    <el-form label-width="110px" class="import-config">
      <el-form-item label="数据标注状态"><el-radio-group v-model="annotated" :disabled="uploading"><el-radio :label="false">无标注信息</el-radio><el-radio :label="true">有标注信息</el-radio></el-radio-group></el-form-item>
      <el-form-item v-if="annotated" label="标注格式"><el-select v-model="annotationFormat" :disabled="uploading"><el-option label="VLS 样本归档（vls-samples.json）" value="vls" /><el-option v-if="annotationType === 'object_detection'" label="YOLO 检测框 ZIP" value="yolo" /></el-select></el-form-item>
      <el-form-item label="导入方式"><el-select v-model="method" :disabled="uploading" @change="loadSources"><el-option label="本地导入" value="local" /><el-option label="S3 对象存储" value="s3" /><el-option label="公开文件链接" value="public" /></el-select></el-form-item>
    </el-form>
    <template v-if="method === 'local'">
      <el-alert title="支持单文件 ≤4 GiB 断点续传；更大的文件会继续尝试，受存储容量和文件内容限制。" type="info" :closable="false" />
      <p class="import-tip">支持 JPG/JPEG、PNG、BMP、MP4、MOV、ZIP。图片保持 4000 万像素上限。ZIP 支持 ZIP64，默认解压容量 32 GiB、最多 10000 个样本；文件上传成功后仍需完成格式和标注校验。</p>
      <el-upload v-model:file-list="files" drag :auto-upload="false" multiple :limit="500" :disabled="uploading" :accept="annotated ? '.zip' : '.jpg,.jpeg,.png,.bmp,.mp4,.mov,.zip'"><p>拖入文件或点击选择文件</p><p class="import-tip">恢复上传时重新选择同一原文件即可</p></el-upload>
      <input ref="directoryInput" type="file" webkitdirectory multiple hidden @change="selectDirectory" />
      <el-button v-if="!annotated" :disabled="uploading" @click="directoryInput.click()">选择本地文件夹</el-button>
      <div v-if="phase" class="transfer-progress"><span>{{ phase }}</span><el-progress :percentage="Math.min(100, Math.round(progress))" /><span class="import-tip">{{ currentName }}</span></div>
      <div class="transfer-actions"><el-button v-if="uploading" @click="pause">暂停上传</el-button><el-button v-else type="primary" :disabled="!files.length" @click="start">开始 / 继续上传</el-button></div>
    </template>
    <template v-else-if="['s3', 'public'].includes(method)">
      <el-form label-width="110px"><el-form-item label="数据来源"><el-select v-model="sourceId" filterable placeholder="选择已配置来源" @change="sourceChanged"><el-option v-for="source in sources" :key="source.id" :value="source.id" :label="source.name" /></el-select><el-button link type="primary" @click="emit('sources')">管理数据来源</el-button><el-button link @click="loadSources">刷新</el-button></el-form-item>
        <template v-if="method === 's3'"><el-form-item label="对象 / Prefix"><el-input v-model="remotePath" placeholder="填写来源 Prefix 内的对象键或目录前缀" /></el-form-item><el-form-item label="导入范围"><el-radio-group v-model="directory"><el-radio :label="false">单个文件 / ZIP</el-radio><el-radio v-if="!annotated" :label="true">目录内全部兼容文件</el-radio></el-radio-group><el-button link type="primary" :disabled="!sourceId" @click="browse">查看文件</el-button></el-form-item><el-table v-if="remoteFiles.length" :data="remoteFiles" max-height="200" @row-click="row => { remotePath = row.key; directory = false }"><el-table-column prop="key" label="对象键（点击选择）" /><el-table-column label="大小" width="110"><template #default="{ row }">{{ size(row.size) }}</template></el-table-column></el-table><el-button v-if="nextToken" link @click="browse(true)">下一页</el-button></template>
      </el-form>
      <p class="import-tip">公开来源支持可直接下载的兼容图片、视频和 ZIP；GitHub blob、Hugging Face blob、Gitee blob 链接会转换为文件地址。登录/受限数据、Parquet/TAR、任意仓库首页暂不支持。</p>
      <el-button type="primary" :disabled="!sourceId" :loading="submitting" @click="remoteImport">创建导入任务</el-button>
    </template>
    <DatasetImportHistory ref="history" :dataset-id="datasetId" @completed="emit('completed')" />
    <template #footer><el-button @click="close">{{ uploading ? '暂停并关闭' : '关闭' }}</el-button></template>
  </el-dialog>
</template>
<script setup>
import { onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import CryptoJS from 'crypto-js'
import * as api from '@/api/datasetImport'
import HashWorker from '@/utils/datasetHash.worker.js?worker'
import DatasetImportHistory from './DatasetImportHistory.vue'
const props = defineProps({ modelValue: Boolean, annotationType: { type: String, default: 'object_detection' }, datasetId: { type: [String, Number], required: true }, initialSource: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'completed', 'sources'])
const annotated = ref(false), annotationFormat = ref('vls'), method = ref('local'), files = ref([]), directoryInput = ref(null), uploading = ref(false), progress = ref(0), phase = ref(''), currentName = ref(''), history = ref(null)
const sources = ref([]), sourceId = ref(''), remotePath = ref(''), directory = ref(false), remoteFiles = ref([]), nextToken = ref(''), submitting = ref(false)
let controller, worker, rejectHash, stopped = false
const size = bytes => `${(Number(bytes || 0) / 1024 / 1024).toFixed(1)} MB`
const hash = file => new Promise((resolve, reject) => {
  worker = new HashWorker(); rejectHash = reject
  worker.onmessage = ({ data }) => { if (data.error) { worker.terminate(); reject(new Error(data.error)) } else if (data.sha256) { worker.terminate(); worker = null; rejectHash = null; resolve(data.sha256) } else progress.value = data.progress * 100 }
  worker.onerror = () => { worker.terminate(); reject(new Error('文件校验失败，请重试')) }; worker.postMessage(file)
})
const pause = () => { stopped = true; controller?.abort(); worker?.terminate(); rejectHash?.(new Error('PAUSED')); worker = null; rejectHash = null; phase.value = '已暂停，已完成的分片保留，可继续上传' }
const close = () => { if (uploading.value) pause(); emit('update:modelValue', false) }
const selectDirectory = event => {
  const selected = Array.from(event.target.files || []).filter(file => /\.(jpg|jpeg|png|bmp|mp4|mov|zip)$/i.test(file.name))
  if (selected.length > 500) return ElMessage.warning('单次请选择不超过 500 个文件，可分批继续')
  files.value = selected.map((file, index) => ({ name: file.webkitRelativePath || file.name, raw: file, uid: Date.now() + index })); event.target.value = ''
}
const start = async () => {
  if (uploading.value) return
  uploading.value = true; stopped = false
  try {
    const limits = await api.importCapabilities()
    for (const item of files.value) {
      if (stopped) break
      const file = item.raw; currentName.value = item.name
      if (!file || file.size > Number(limits.maxFileBytes)) throw new Error('文件超过当前服务器允许的最大容量')
      if (annotated.value && !file.name.toLowerCase().endsWith('.zip')) throw new Error('有标注数据请使用同时包含媒体与标注的 ZIP 归档')
      phase.value = '校验文件，以便识别并恢复之前的上传'; progress.value = 0
      const sha256 = await hash(file)
      if (stopped) break
      const state = await api.beginDatasetUpload({ datasetId: props.datasetId, filename: file.name, fileSize: file.size, sha256, annotationFormat: annotated.value ? annotationFormat.value : 'none', sourceName: 'local' })
      if (['QUEUED', 'PROCESSING', 'COMPLETED', 'PARTIAL'].includes(state.job.jobState)) { phase.value = '该文件已有导入任务，请查看记录'; continue }
      if (state.job.jobState === 'FAILED') { await api.retryDatasetImport(state.job.id); continue }
      let confirmed = Number(state.uploadedBytes || 0); const chunkSize = Number(state.job.chunkSize); const existing = new Set(state.parts.map(part => Number(part.partNumber)))
      phase.value = '上传中，断网后可继续'; progress.value = confirmed / file.size * 100
      for (let offset = 0, number = 1; offset < file.size; offset += chunkSize, number++) {
        if (stopped) break
        if (existing.has(number)) continue
        const chunk = file.slice(offset, offset + chunkSize)
        const partHash = CryptoJS.SHA256(CryptoJS.lib.WordArray.create(new Uint8Array(await chunk.arrayBuffer()))).toString()
        let success = false
        for (let attempt = 0; attempt < 3 && !stopped; attempt++) {
          controller = new AbortController()
          try { await api.uploadDatasetPart(state.job.id, number, chunk, partHash, controller.signal, event => { progress.value = (confirmed + Math.min(event.loaded, chunk.size)) / file.size * 100 }); success = true; break }
          catch (error) { if (stopped) break; if (attempt === 2) throw error }
        }
        if (!success) break
        confirmed += chunk.size; progress.value = confirmed / file.size * 100
      }
      if (!stopped) { phase.value = '上传完成，正在提交后台导入'; await api.completeDatasetUpload(state.job.id); history.value?.refresh() }
    }
    if (!stopped) { phase.value = '上传已提交，后台继续解析，请查看导入记录'; ElMessage.success('已提交导入任务') }
  } catch (error) { if (!stopped && error.message !== 'PAUSED') { phase.value = '上传中断，重新点击可继续'; ElMessage.error(error.message || '上传失败') } }
  finally { uploading.value = false; controller = null; history.value?.refresh() }
}
const loadSources = async () => {
  try {
    if (!['s3', 'public'].includes(method.value)) return
    const selectedMethod = method.value
    const all = []
    let page = 1, total = 0
    do {
      const result = await api.listDatasetSources({ page })
      all.push(...result.records); total = Number(result.total); page++
      if (!result.records.length) break
    } while (all.length < total)
    if (method.value !== selectedMethod) return
    sources.value = all.filter(source => source.sourceType === selectedMethod)
    sourceId.value = ''; remoteFiles.value = []
  } catch (error) { ElMessage.error(error.message) }
}
const sourceChanged = () => { remotePath.value = sources.value.find(source => source.id === sourceId.value)?.keyPrefix || ''; remoteFiles.value = []; nextToken.value = '' }
const browse = async (next = false) => { try { const result = await api.browseDatasetSource(sourceId.value, remotePath.value, next === true ? nextToken.value : undefined); remoteFiles.value = result.files; nextToken.value = result.nextToken } catch (error) { ElMessage.error(error.message) } }
const remoteImport = async () => { submitting.value = true; try { await api.importRemoteDataset({ datasetId: props.datasetId, sourceId: sourceId.value, path: remotePath.value, directory: method.value === 's3' && directory.value, annotationFormat: annotated.value ? annotationFormat.value : 'none' }); ElMessage.success('已创建后台导入任务'); history.value?.refresh() } catch (error) { ElMessage.error(error.message) } finally { submitting.value = false } }
watch(() => props.modelValue, async visible => { if (visible) { if (props.initialSource) method.value = props.initialSource.sourceType; await loadSources(); if (props.initialSource) { if (!sources.value.some(item => item.id === props.initialSource.id)) sources.value.push(props.initialSource); sourceId.value = props.initialSource.id; sourceChanged() } } }, { immediate: true })
watch(annotated, value => { if (value) directory.value = false })
onUnmounted(pause)
</script>
<style scoped>
.import-config .el-select { width: 300px; } .import-tip { color: #7c8594; font-size: 12px; line-height: 1.8; } .transfer-progress { margin: 18px 0; } .transfer-actions { margin: 16px 0; } .el-select { min-width: 240px; }
</style>
