<template>
  <el-drawer :model-value="modelValue" class="device-model-drawer locale-drawer model-selection-drawer" title="选择算法模型" direction="rtl" size="min(1440px, 94vw)"
    append-to-body :before-close="closeDrawer" :show-close="!submitting"
    :close-on-click-modal="false" :close-on-press-escape="!submitting" @update:model-value="emit('update:modelValue', $event)">
    <div class="picker-shell">
      <el-tabs v-model="source" class="source-tabs">
        <el-tab-pane :disabled="submitting" :label="`自主训练的模型${sourceTotal === null ? '' : ` (${sourceTotal})`}`" name="local" />
        <el-tab-pane :label="`Model Hub${hubTotal === null ? '' : ` (${hubTotal})`}`" name="hub" :disabled="submitting" />
      </el-tabs>
      <ModelHubCatalog v-if="source === 'hub' && modelValue" @count="hubTotal = $event" @busy="submitting = $event; emit('busy', $event)" />
      <template v-if="source === 'local'">
      <div class="picker-toolbar">
        <el-input v-model="keyword" placeholder="搜索模型或算法名称" :prefix-icon="Search" clearable
          :disabled="submitting" @input="scheduleSearch" @keyup.enter="searchNow" />
        <span class="target-device">下发至 {{ device?.deviceName || device?.deviceId }} · OM</span>
      </div>
      <div class="category-tabs" role="tablist" aria-label="模型分类">
        <button v-for="item in categories" :key="item.value" role="tab" :aria-selected="category === item.value"
          :class="{ active: category === item.value }" :disabled="submitting" @click="selectCategory(item.value)">{{ item.label }}</button>
      </div>
      <div class="picker-columns">
        <section class="model-catalog" aria-label="自主训练模型列表">
          <div v-if="loadError" class="catalog-error">
            <el-alert :title="loadError" type="error" :closable="false" show-icon />
            <el-button @click="loadModels">重新加载</el-button>
          </div>
          <div v-loading="loading" class="catalog-content">
            <el-empty v-if="!loading && !loadError && !models.length" :description="keyword || category ? '没有匹配的模型' : '暂无自主训练模型，请先完成训练并保存到算法模型'" />
            <div v-else class="model-grid">
              <button v-for="model in models" :key="model.id" type="button" class="model-card"
                :class="{ selected: isSelected(model.id), unavailable: !model.deployable }"
                :aria-label="`${model.modelName}，版本 ${model.version ?? '-'}${model.deployable ? '' : '，暂无可用 OM'}`"
                :aria-pressed="isSelected(model.id)" :disabled="submitting || !model.deployable || Boolean(results[model.id])"
                @click="toggleModel(model)">
                <div class="model-cover">
                  <el-image v-if="model.imageUrl" :src="model.imageUrl" fit="cover" loading="lazy">
                    <template #error><div class="cover-placeholder"><el-icon><Picture /></el-icon><span>{{ categoryLabel(model.category) }}</span></div></template>
                  </el-image>
                  <div v-else class="cover-placeholder"><el-icon><Picture /></el-icon><span>{{ categoryLabel(model.category) }}</span></div>
                  <span class="selection-dot"><el-icon v-if="isSelected(model.id)"><Check /></el-icon></span>
                  <span class="version-label">{{ model.version == null ? '未标版本' : `V${model.version}` }}</span>
                </div>
                <div class="model-card-body">
                  <h3 :title="model.modelName">{{ model.modelName || '未命名模型' }}</h3>
                  <p class="model-category">{{ categoryLabel(model.category) }}<span v-if="model.algorithmName"> · {{ model.algorithmName }}</span></p>
                  <p class="model-description">{{ model.description || '暂无模型描述' }}</p>
                  <span class="model-availability" :class="{ warning: !model.deployable }">{{ model.deployable ? 'OM 模型' : '暂无可用 OM 产物' }}</span>
                </div>
              </button>
            </div>
          </div>
          <el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total"
            :disabled="submitting || loading" layout="total, prev, pager, next" background @current-change="loadModels" />
        </section>
        <aside class="selection-panel" aria-label="已选取模型">
          <div class="selection-heading"><h3>已选取模型 <span>{{ selected.length }}</span></h3>
            <el-button v-if="selected.length && !hasResults" link :disabled="submitting" @click="selected = []">清空</el-button>
          </div>
          <p v-if="!selected.length" class="selection-empty">点击左侧模型卡片进行选择</p>
          <div v-for="model in selected" :key="model.id" class="selected-model">
            <div class="selected-title"><strong>{{ model.modelName }}</strong>
              <el-button v-if="!results[model.id]" link :icon="Close" :disabled="submitting" :aria-label="`移除 ${model.modelName}`" @click="toggleModel(model)" />
            </div>
            <span class="selected-version">{{ model.version == null ? '未标版本' : `V${model.version}` }} · OM</span>
            <p v-if="results[model.id]" class="submission-result" :class="results[model.id].state">{{ results[model.id].message }}</p>
          </div>
          <p v-if="hasResults" class="result-note">任务提交后由设备下载并部署，最终结果以设备回执为准。结果未确认的项目请先核实下发任务，避免重复提交。</p>
        </aside>
      </div>
      </template>
    </div>
    <template #footer>
      <div class="drawer-footer">
        <span>{{ source === 'hub' ? 'Model Hub · 公开模型' : submitting ? `正在提交 ${submittedIndex} / ${batchSize}` : `已选择 ${selected.length} 个模型` }}</span>
        <div><el-button :disabled="submitting" @click="closeDrawer()">{{ hasResults ? '关闭' : '取消' }}</el-button>
          <el-button v-if="source === 'local'" type="primary" :loading="submitting" :disabled="!pendingModels.length || !device?.online" @click="submitSelection">确定{{ pendingModels.length ? ` (${pendingModels.length})` : '' }}</el-button></div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, Picture, Search } from '@element-plus/icons-vue'
import { getDeviceModelCandidates, dispatchSelectedModel } from '@/api/deviceModelCatalog'
import ModelHubCatalog from './ModelHubCatalog.vue'

const props = defineProps({ modelValue: Boolean, device: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'busy', 'submitted'])
const source = ref('local')
const hubTotal = ref(null)
const categories = [
  { value: '', label: '全部' }, { value: 'personDetect', label: '行人检测' },
  { value: 'detect', label: '目标检测' }, { value: 'segment', label: '实例分割' },
  { value: 'semanticSeg', label: '语义分割' }, { value: 'classify', label: '图像分类' },
  { value: 'pose', label: '关键点检测' }, { value: 'obb', label: '旋转目标检测' }, { value: 'faceDetect', label: '人脸识别' }
]
const categoryLabel = value => categories.find(item => item.value === value && value)?.label || '其他模型'
const keyword = ref('')
const category = ref('')
const page = ref(1)
const pageSize = 12
const models = ref([])
const total = ref(0)
const sourceTotal = ref(null)
const loading = ref(false)
const loadError = ref('')
const selected = ref([])
const results = ref({})
const submitting = ref(false)
const submittedIndex = ref(0)
const batchSize = ref(0)
const hasResults = computed(() => Object.keys(results.value).length > 0)
const pendingModels = computed(() => selected.value.filter(model => !results.value[model.id]))
const isSelected = id => selected.value.some(model => model.id === id)
let searchTimer
let requestSequence = 0
let session = 0

function toggleModel(model) {
  if (submitting.value || !model.deployable || results.value[model.id]) return
  selected.value = isSelected(model.id) ? selected.value.filter(item => item.id !== model.id) : [...selected.value, model]
}

async function loadModels() {
  const sequence = ++requestSequence
  loading.value = true
  loadError.value = ''
  models.value = []
  try {
    const response = await getDeviceModelCandidates({ current: page.value, size: pageSize, keyword: keyword.value.trim(), category: category.value })
    if (sequence !== requestSequence || !props.modelValue) return
    if (!Array.isArray(response?.data?.records)) throw new Error('模型列表响应格式不正确')
    models.value = response.data.records.map(model => ({ ...model, id: String(model.id) }))
    total.value = Number(response.data.total || 0)
    if (!keyword.value.trim() && !category.value) sourceTotal.value = total.value
  } catch (error) {
    if (sequence === requestSequence && props.modelValue) {
      total.value = 0
      loadError.value = error?.response?.status === 404 ? '当前后端尚未提供自主训练模型选择接口，请更新 VLS 后端后重试' : error?.response?.data?.msg || error?.message || '加载模型失败'
    }
  } finally { if (sequence === requestSequence) loading.value = false }
}

function searchNow() { clearTimeout(searchTimer); page.value = 1; loadModels() }
function scheduleSearch() {
  clearTimeout(searchTimer)
  // Invalidate an older request immediately, before the debounce delay.
  requestSequence++
  searchTimer = setTimeout(searchNow, 300)
}
function selectCategory(value) { category.value = value; searchNow() }
function closeDrawer(done) {
  if (submitting.value) return
  if (typeof done === 'function') done()
  emit('update:modelValue', false)
}

async function submitSelection() {
  if (submitting.value || !props.device?.online || !pendingModels.value.length) return
  const targetDevice = props.device.deviceId
  const currentSession = session
  const batch = [...pendingModels.value]
  batchSize.value = batch.length
  submittedIndex.value = 0
  submitting.value = true
  emit('busy', true)
  let submitted = 0
  try {
    for (const model of batch) {
      if (currentSession !== session) break
      submittedIndex.value++
      results.value[model.id] = { state: 'pending', message: '正在提交…' }
      try {
        const response = await dispatchSelectedModel(model.id, targetDevice)
        if (response?.code !== 200 || !response?.data) throw new Error(response?.msg || '未收到下发任务编号')
        if (currentSession !== session) break
        results.value[model.id] = { state: 'success', message: '任务已提交，等待设备部署', requestId: response.data }
        submitted++
      } catch (error) {
        if (currentSession !== session) break
        results.value[model.id] = { state: 'uncertain', message: error?.response?.data?.msg || error?.message || '提交结果未确认，请核实任务' }
      }
    }
    if (submitted) { ElMessage.success(`已提交 ${submitted} 个模型下发任务`); emit('submitted') }
  } finally { submitting.value = false; emit('busy', false) }
}

watch(() => props.modelValue, visible => {
  session++
  requestSequence++
  clearTimeout(searchTimer)
  if (visible) {
    keyword.value = ''; category.value = ''; page.value = 1; selected.value = []; results.value = {}; sourceTotal.value = null
    loadModels()
  }
})
onBeforeUnmount(() => { session++; requestSequence++; clearTimeout(searchTimer) })
</script>

<style scoped>
.picker-shell { display: flex; flex-direction: column; height: 100%; min-height: 0; }
.source-tabs { flex: none; }
.picker-toolbar { display: flex; gap: 20px; justify-content: space-between; align-items: center; margin: 4px 0 18px; }
.picker-toolbar .el-input { width: 340px; max-width: 55%; }
.picker-toolbar :deep(.el-input__wrapper) { border-radius: 20px; }
.target-device { font-size: 12px; color: #909399; }
.category-tabs { display: flex; gap: 22px; flex-wrap: wrap; margin-bottom: 22px; }
.category-tabs button { border: 0; background: transparent; padding: 6px 0 10px; cursor: pointer; color: #8b919b; font-size: 14px; position: relative; }
.category-tabs button.active { color: #307bff; font-weight: 600; }
.category-tabs button.active::after { content: ''; position: absolute; bottom: 0; left: 0; width: 24px; height: 3px; background: #307bff; border-radius: 2px; }
.picker-columns { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 32px; flex: 1; min-height: 0; }
.model-catalog { min-width: 0; display: flex; flex-direction: column; min-height: 0; }
.catalog-content { flex: 1; min-height: 140px; overflow-y: auto; padding: 2px 4px 12px 2px; }
.model-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.model-card { padding: 0; border: 1px solid #e9edf3; border-radius: 8px; background: #fff; text-align: left; cursor: pointer; overflow: hidden; color: #303846; transition: border-color .15s, box-shadow .15s; font: inherit; }
.model-card:hover:not(:disabled), .model-card.selected { border-color: #307bff; box-shadow: 0 2px 10px #307bff15; }
.model-card:focus-visible { outline: 2px solid #307bff; outline-offset: 2px; }
.model-card.unavailable { opacity: .62; cursor: not-allowed; }
.model-cover { height: 130px; position: relative; background: #edf3fc; }
.model-cover .el-image { width: 100%; height: 100%; }
.cover-placeholder { height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 10px; color: #7b9cc4; background: linear-gradient(135deg, #e5effb, #f3f6fc); font-size: 13px; }
.cover-placeholder .el-icon { font-size: 32px; }
.selection-dot { position: absolute; top: 12px; left: 12px; width: 18px; height: 18px; border: 1px solid #aebed2; border-radius: 50%; background: #ffffffd9; display: flex; align-items: center; justify-content: center; }
.selected .selection-dot { background: #307bff; color: white; border-color: #307bff; }
.version-label { position: absolute; bottom: 8px; right: 8px; background: #ffffffed; color: #65738b; font-size: 11px; padding: 3px 7px; border-radius: 4px; }
.model-card-body { padding: 13px; }
.model-card h3 { font-size: 14px; font-weight: 500; margin: 0 0 8px; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; }
.model-category { font-size: 11px; color: #9098a5; margin: 0 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.model-description { font-size: 12px; color: #7c8594; line-height: 1.7; margin: 0 0 10px; height: 40px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.model-availability { color: #5f82b0; font-size: 11px; }
.model-availability.warning { color: #b58342; }
.selection-panel { overflow-y: auto; padding: 0 2px; }
.selection-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.selection-heading h3 { font-size: 14px; font-weight: 500; margin: 0; }
.selection-heading h3 span { color: #9098a5; font-size: 12px; margin-left: 5px; }
.selection-empty { color: #a2a9b3; font-size: 13px; padding-top: 25px; text-align: center; }
.selected-model { background: white; padding: 12px 15px; margin-bottom: 10px; border-radius: 4px; border: 1px solid #edf0f5; }
.selected-title { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.selected-title strong { font-weight: 400; font-size: 13px; overflow-wrap: anywhere; }
.selected-version { font-size: 11px; color: #9098a5; }
.submission-result { font-size: 12px; line-height: 1.5; margin: 8px 0 0; overflow-wrap: anywhere; }
.submission-result.success { color: #389768; }
.submission-result.uncertain { color: #ba792c; }
.result-note { font-size: 12px; color: #9098a5; line-height: 1.7; }
.drawer-footer { display: flex; align-items: center; justify-content: space-between; }
.drawer-footer > span { color: #9098a5; font-size: 13px; }
.drawer-footer .el-button { min-width: 100px; }
.catalog-error { margin-bottom: 16px; display: flex; flex-direction: column; gap: 12px; align-items: flex-start; }
.el-pagination { justify-content: center; padding-top: 16px; }
@media (max-width: 1200px) { .model-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .picker-columns { grid-template-columns: minmax(0, 1fr) 250px; gap: 20px; } }
@media (max-width: 760px) { .picker-columns { display: flex; flex-direction: column; overflow-y: auto; } .model-catalog { flex: none; } .selection-panel { flex: none; max-height: 250px; } .target-device { display: none; } .picker-toolbar .el-input { max-width: 100%; width: 100%; } .category-tabs { gap: 12px; } }
</style>

<style>
.device-model-drawer { background: #f2f5fa; }
.device-model-drawer .el-drawer__header { margin-bottom: 8px; padding: 22px 28px 10px; color: #303846; font-size: 15px; }
.device-model-drawer .el-drawer__body { padding: 0 28px 20px; min-height: 0; }
.device-model-drawer .el-drawer__footer { padding: 16px 28px 24px; border-top: 1px solid #e7ecf3; }
</style>
