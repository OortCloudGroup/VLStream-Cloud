<template>
  <el-drawer :model-value="modelValue" class="device-model-drawer locale-drawer model-selection-drawer" :class="{ 'device-model-drawer--hub': source === 'hub' }" :title="$tp('选择算法模型')" direction="rtl" size="min(1480px, calc(100vw - 32px))"
    append-to-body :before-close="closeDrawer" :show-close="!submitting"
    modal-class="device-model-mask" :close-on-click-modal="!submitting" :close-on-press-escape="!submitting" @update:model-value="emit('update:modelValue', $event)">
    <div class="picker-shell">
      <ModelHubCatalog v-if="source === 'hub' && modelValue" @count="hubTotal = $event" @busy="submitting = $event; emit('busy', $event)">
        <template #tabs>
          <el-tabs v-model="source" class="source-tabs">
            <el-tab-pane :disabled="submitting" :label="$tp('自主训练的模型{value0}', { value0: sourceTotal === null ? '' : ` (${sourceTotal})` })" name="local" />
            <el-tab-pane :label="`Model Hub${hubTotal === null ? '' : ` (${hubTotal})`}`" name="hub" :disabled="submitting" />
          </el-tabs>
        </template>
      </ModelHubCatalog>
      <template v-if="source === 'local'">
      <div class="picker-header">
        <el-tabs v-model="source" class="source-tabs">
          <el-tab-pane :disabled="submitting" :label="$tp('自主训练的模型{value0}', { value0: sourceTotal === null ? '' : ` (${sourceTotal})` })" name="local" />
          <el-tab-pane :label="`Model Hub${hubTotal === null ? '' : ` (${hubTotal})`}`" name="hub" :disabled="submitting" />
        </el-tabs>
        <div class="picker-toolbar">
          <el-input v-model="keyword" :placeholder="$tp('搜索模型或算法名称')" :prefix-icon="Search" clearable
            :disabled="submitting" @input="scheduleSearch" @keyup.enter="searchNow" />
          <span class="target-device" :title="targetNames">{{ $tp('下发至') }} {{ targetDevices.length > 1 ? `${targetDevices.length} 台设备` : targetNames }} · OM</span>
        </div>
      </div>
      <div class="category-tabs" role="tablist" :aria-label="$tp('模型分类')">
        <button v-for="item in categories" :key="item.value" role="tab" :aria-selected="category === item.value"
          :class="{ active: category === item.value }" :disabled="submitting" @click="selectCategory(item.value)">{{ item.label }}</button>
      </div>
      <div class="picker-columns">
        <section class="model-catalog" :aria-label="$tp('自主训练模型列表')">
          <div v-if="loadError" class="catalog-error">
            <el-alert :title="loadError" type="error" :closable="false" show-icon />
            <el-button @click="loadModels">{{ $tp('重新加载') }}</el-button>
          </div>
          <div v-loading="loading" class="catalog-content">
            <el-empty v-if="!loading && !loadError && !models.length" :description="keyword || category ? $tp('没有匹配的模型') : $tp('暂无自主训练模型，请先完成训练并保存到算法模型')" />
            <div v-else class="model-grid">
              <button v-for="model in models" :key="model.id" type="button" class="model-card"
                :class="{ selected: isSelected(model.id), unavailable: !model.deployable }"
                :aria-label="$tp('{value0}，版本 {value1}{value2}', { value0: model.modelName, value1: model.version ?? '-', value2: model.deployable ? '' : $tp('，暂无可用 OM') })"
                :aria-pressed="isSelected(model.id)" :disabled="submitting || !model.deployable || modelHasResults(model.id)"
                @click="toggleModel(model)">
                <div class="model-cover">
                  <el-image v-if="model.imageUrl" :src="model.imageUrl" fit="cover" loading="lazy">
                    <template #error><div class="cover-placeholder"><el-icon><Picture /></el-icon><span>{{ categoryLabel(model.category) }}</span></div></template>
                  </el-image>
                  <div v-else class="cover-placeholder"><el-icon><Picture /></el-icon><span>{{ categoryLabel(model.category) }}</span></div>
                  <span class="selection-dot"><el-icon v-if="isSelected(model.id)"><Check /></el-icon></span>
                  <span class="version-label">{{ model.version == null ? $tp('未标版本') : `V${model.version}` }}</span>
                </div>
                <div class="model-card-body">
                  <h3 :title="model.modelName">{{ model.modelName || $tp('未命名模型') }}</h3>
                  <p class="model-category">{{ categoryLabel(model.category) }}<span v-if="model.algorithmName"> · {{ model.algorithmName }}</span></p>
                  <p class="model-description" :title="model.description">{{ model.description || $tp('暂无模型描述') }}</p>
                  <span class="model-availability" :class="{ warning: !model.deployable }">{{ model.deployable ? $tp('OM 模型') : $tp('暂无可用 OM 产物') }}</span>
                </div>
              </button>
            </div>
          </div>
        </section>
        <aside class="selection-panel" :aria-label="$tp('已选取模型')">
          <div class="selection-heading"><h3>{{ $tp('已选取模型') }} <span>{{ selected.length }}</span></h3>
            <el-button v-if="selected.length && !hasResults" link :disabled="submitting" @click="selected = []">{{ $tp('清空') }}</el-button>
          </div>
          <p v-if="!selected.length" class="selection-empty">{{ $tp('点击左侧模型卡片进行选择') }}</p>
          <div v-for="model in selected" :key="model.id" class="selected-model">
            <div class="selected-title"><strong>{{ model.modelName }}</strong>
              <el-button v-if="!modelHasResults(model.id)" link :icon="Close" :disabled="submitting" :aria-label="$tp('移除 {value0}', { value0: model.modelName })" @click="toggleModel(model)" />
            </div>
            <span class="selected-version">{{ model.version == null ? $tp('未标版本') : `V${model.version}` }} · OM</span>
            <p v-for="item in modelResults(model.id)" :key="item.deviceId" class="submission-result" :class="item.state">
              <span v-if="targetDevices.length > 1" class="result-device">{{ item.deviceName }} · {{ item.deviceId }}<br /></span>{{ $tp(item.message) }}
            </p>
          </div>
          <p v-if="hasResults" class="result-note">{{ $tp('任务提交后由设备下载并部署，最终结果以设备回执为准。结果未确认的项目请先核实下发任务，避免重复提交。') }}</p>
          <el-button v-if="retryablePairs.length" link type="primary" :disabled="submitting" @click="retryFailed">{{ $tp('重试失败项') }} ({{ retryablePairs.length }})</el-button>
          <el-button v-if="uncertainPairs.length" link type="primary" :disabled="submitting" @click="recoverUncertain">{{ $tp('核实提交结果') }} ({{ uncertainPairs.length }})</el-button>
        </aside>
      </div>
      </template>
    </div>
    <template v-if="source === 'local'" #footer>
      <div class="picker-footer">
        <div v-if="total > pageSize" class="catalog-pagination">
          <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" :pager-count="5"
            :disabled="submitting || loading" layout="total, prev, pager, next" background @current-change="loadModels" />
        </div>
        <div class="drawer-footer">
          <span>{{ submitting ? $tp('正在提交 {value0} / {value1}', { value0: submittedIndex, value1: batchSize }) : $tp('已选择 {value0} 个模型', { value0: selected.length }) }}<template v-if="targetDevices.length > 1"> · {{ targetDevices.length }} 台设备 · {{ selected.length * targetDevices.length }} 个下发任务</template></span>
          <div class="drawer-footer-actions">
            <el-button :disabled="submitting" @click="closeDrawer()">{{ hasResults ? $tp('关闭') : $tp('取消') }}</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!pendingModels.length || !targetDevices.length || targetDevices.length > 20 || (targetDevices.length === 1 && !targetDevices[0].online)" @click="submitSelection">{{ $tp('确定') }}{{ pendingModels.length ? ` (${pendingModels.length})` : '' }}</el-button>
          </div>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Close, Picture, Search } from '@element-plus/icons-vue'
import { getDeviceModelCandidates, submitSavedModelAttempt, getSavedModelAttempt } from '@/api/deviceModelCatalog'
import { pairKey, attemptResult } from '@/utils/savedModelDispatch.mjs'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import ModelHubCatalog from './ModelHubCatalog.vue'

const props = defineProps({ modelValue: Boolean, device: { type: Object, default: null }, devices: { type: Array, default: null } })
const emit = defineEmits(['update:modelValue', 'busy', 'submitted'])
const source = ref('local')
const hubTotal = ref(null)
const categories = [
  { value: '', label: '全部' }, { value: 'personDetect', label: '行人检测' },
  { value: 'detect', label: '目标检测' }, { value: 'segment', label: '实例分割' },
  { value: 'semanticSeg', label: '语义分割' }, { value: 'classify', label: '图像分类' },
  { value: 'pose', label: '关键点检测' }, { value: 'obb', label: '旋转目标检测' }, { value: 'faceDetect', label: '人脸识别' }
]
const categoryLabel = value => categories.find(item => item.value === value && value)?.label || uiText('其他模型')
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
const targetDevices = ref([])
const targetNames = computed(() => targetDevices.value.map(device => device.deviceName || device.deviceId).join('、'))
const hasResults = computed(() => Object.keys(results.value).length > 0)
const modelHasResults = id => Object.values(results.value).some(item => item.modelId === String(id))
const modelResults = id => Object.values(results.value).filter(item => item.modelId === String(id))
const pendingModels = computed(() => selected.value.filter(model => !modelHasResults(model.id)))
const retryablePairs = computed(() => Object.values(results.value).filter(item => item.retryable))
const uncertainPairs = computed(() => Object.values(results.value).filter(item => item.state === 'uncertain'))
const isSelected = id => selected.value.some(model => model.id === id)
let searchTimer
let requestSequence = 0
let session = 0
let identityTimer, drawerIdentity, resultTimer, polling = false, activeTargets = ''

function toggleModel(model) {
  if (submitting.value || !model.deployable || modelHasResults(model.id)) return
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
  try { drawerIdentity?.assertCurrent() } catch { onDeviceModelIdentityChanged(); return }
  if (submitting.value || !pendingModels.value.length || !targetDevices.value.length || targetDevices.value.length > 20) return
  const operationId = crypto.randomUUID()
  const batch = pendingModels.value.flatMap(model => targetDevices.value.map(device => ({
    modelId: String(model.id), deviceId: device.deviceId, deviceName: device.deviceName || device.deviceId, operationId
  })))
  await submitPairs(batch)
}

async function retryFailed() {
  if (submitting.value) return
  const operationId = crypto.randomUUID()
  await submitPairs(retryablePairs.value.map(item => ({ ...item, operationId })))
}

async function recoverUncertain() {
  if (!submitting.value) await submitPairs(uncertainPairs.value.map(item => ({ ...item })))
}

async function submitPairs(batch) {
  if (submitting.value || !batch.length) return
  const currentSession = session
  const identity = captureComputeIdentity()
  batchSize.value = batch.length
  submittedIndex.value = 0
  submitting.value = true
  emit('busy', true)
  let submitted = 0
  try {
    for (const item of batch) {
      if (currentSession !== session) break
      submittedIndex.value++
      const key = pairKey(item.modelId, item.deviceId)
      results.value[key] = { ...item, state: 'pending', retryable: false, message: '正在提交…' }
      try {
        identity.assertCurrent()
        const response = await submitSavedModelAttempt(item.modelId, item.deviceId, item.operationId)
        if (currentSession !== session) break
        identity.assertCurrent()
        if (response?.code !== 200) throw new Error(response?.msg || '提交结果未确认')
        results.value[key] = { ...item, ...attemptResult(response.data) }
        if (results.value[key].state !== 'failed' && results.value[key].state !== 'uncertain') submitted++
      } catch (error) {
        if (currentSession !== session) break
        try { identity.assertCurrent() } catch { onDeviceModelIdentityChanged(); break }
        if (error?.code === 'COMPUTE_IDENTITY_CHANGED') { onDeviceModelIdentityChanged(); break }
        results.value[key] = { ...item, state: 'uncertain', retryable: false, message: error?.data?.msg || error?.response?.data?.msg || error?.message || '提交结果未确认，请核实任务' }
      }
    }
    if (submitted && currentSession === session) { ElMessage.success(uiText('已提交 {value0} 个模型下发任务', { value0: submitted })); emit('submitted') }
  } finally {
    if (currentSession === session) { submitting.value = false; emit('busy', false) }
  }
}

async function refreshResults() {
  if (submitting.value || polling || !props.modelValue) return
  polling = true
  const currentSession = session
  try {
    for (const item of Object.values(results.value).filter(item => item.requestId && ['pending', 'uncertain'].includes(item.state))) {
      try {
        drawerIdentity.assertCurrent()
        const response = await getSavedModelAttempt(item.modelId, item.deviceId, item.operationId)
        if (currentSession !== session) return
        drawerIdentity.assertCurrent()
        if (response?.code === 200) results.value[pairKey(item.modelId, item.deviceId)] = { ...item, ...attemptResult(response.data) }
      } catch (error) {
        if (currentSession !== session) return
        try { drawerIdentity.assertCurrent() } catch { onDeviceModelIdentityChanged(); return }
        results.value[pairKey(item.modelId, item.deviceId)] = { ...item, state: 'uncertain', retryable: false,
          message: `无法刷新任务状态：${error?.data?.msg || error?.message || '请核实原任务'}` }
      }
    }
  } finally { if (currentSession === session) polling = false }
}

function onDeviceModelIdentityChanged() {
  session++
  requestSequence++
  clearTimeout(searchTimer)
  clearInterval(identityTimer); clearInterval(resultTimer)
  models.value = []; selected.value = []; results.value = {}
  targetDevices.value = []; activeTargets = ''; polling = false; drawerIdentity = null
  total.value = 0; sourceTotal.value = null; hubTotal.value = null
  submittedIndex.value = 0; batchSize.value = 0; submitting.value = false; loading.value = false
  keyword.value = ''; category.value = ''; page.value = 1; source.value = 'local'
  loadError.value = props.modelValue ? '登录身份已变化，请刷新模型列表并重新选择；已发送的任务请在原账号下核实' : ''
  emit('busy', false)
  emit('update:modelValue', false)
}

watch([() => props.modelValue, () => (props.devices || [props.device]).map(item => item?.deviceId).join('|')], async ([visible, targetKey]) => {
  session++
  requestSequence++
  clearTimeout(searchTimer)
  clearInterval(identityTimer)
  clearInterval(resultTimer)
  submitting.value = false; polling = false; emit('busy', false)
  if (visible) {
    try { drawerIdentity?.assertCurrent() } catch { onDeviceModelIdentityChanged(); return }
    if (activeTargets !== targetKey || !hasResults.value) {
      keyword.value = ''; category.value = ''; page.value = 1; selected.value = []; results.value = {}; sourceTotal.value = null
    }
    targetDevices.value = [...new Map((props.devices || [props.device]).filter(item => item?.deviceId).map(item => [item.deviceId, { ...item }])).values()]
    activeTargets = targetKey
    drawerIdentity = captureComputeIdentity()
    identityTimer = setInterval(() => { try { drawerIdentity.assertCurrent() } catch { clearInterval(identityTimer); onDeviceModelIdentityChanged() } }, 500)
    resultTimer = setInterval(refreshResults, 5000)
    loadModels()
    await refreshResults()
  }
})
onMounted(() => { window.addEventListener('modelHubAuthChanged', onDeviceModelIdentityChanged) })
onBeforeUnmount(() => {
  session++; requestSequence++; clearTimeout(searchTimer); clearInterval(identityTimer); clearInterval(resultTimer)
  window.removeEventListener('modelHubAuthChanged', onDeviceModelIdentityChanged)
})
</script>

<style scoped>
.picker-shell { display: flex; flex-direction: column; height: 100%; min-height: 0; }
.source-tabs { flex: none; }
.source-tabs :deep(.el-tabs__item) { height: 42px; font-size: 14px; padding: 0 22px; }
.source-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; background: #e4eaf2; }
.source-tabs :deep(.el-tabs__active-bar) { height: 3px; border-radius: 3px; }
.picker-header { display: flex; align-items: center; justify-content: space-between; gap: 32px; flex: none; padding-bottom: 16px; border-bottom: 1px solid #e5eaf2; }
.picker-header :deep(.el-tabs__header) { margin-bottom: 0; }
.picker-toolbar { display: flex; justify-content: flex-end; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.picker-toolbar .el-input { width: 340px; min-width: 0; flex: 1; max-width: 340px; }
.picker-toolbar :deep(.el-input__wrapper) { min-height: 38px; border-radius: 8px; background: #fff; box-shadow: 0 0 0 1px #dfe5ee inset; }
.picker-toolbar :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #307bff inset; }
.target-device { display: block; height: 38px; line-height: 38px; padding: 0 12px; font-size: 12px; color: #7b8799; background: #fff; border-radius: 8px; box-shadow: 0 0 0 1px #dfe5ee inset; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.category-tabs { display: flex; gap: 8px; flex-wrap: wrap; margin: 14px 0 20px; }
.category-tabs button { border: 1px solid transparent; border-radius: 7px; background: transparent; padding: 7px 10px; cursor: pointer; color: #7b8799; font-size: 13px; }
.category-tabs button.active { color: #307bff; font-weight: 500; background: #e9f1ff; border-color: #d6e5ff; }
.category-tabs button:focus-visible { outline: 2px solid #307bff; outline-offset: 2px; }
.picker-columns { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 26px; flex: 1; min-height: 0; }
.model-catalog { min-width: 0; display: flex; flex-direction: column; min-height: 0; }
.catalog-content { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior: contain; scrollbar-gutter: stable; padding: 2px 10px 20px 2px; }
.model-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.model-card { padding: 0; min-width: 0; border: 1px solid #e2e8f1; border-radius: 12px; background: #fff; text-align: left; cursor: pointer; overflow: hidden; color: #303846; transition: border-color .15s, box-shadow .15s; font: inherit; }
.model-card:hover:not(:disabled), .model-card.selected { border-color: #307bff; box-shadow: 0 2px 10px #307bff15; }
.model-card:focus-visible { outline: 2px solid #307bff; outline-offset: 2px; }
.model-card.unavailable { opacity: .62; cursor: not-allowed; }
.model-cover { height: auto; aspect-ratio: 2.4; position: relative; background: #edf3fc; }
.model-cover .el-image { width: 100%; height: 100%; }
.cover-placeholder { height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 10px; color: #7b9cc4; background: linear-gradient(135deg, #e5effb, #f3f6fc); font-size: 13px; }
.cover-placeholder .el-icon { font-size: 32px; }
.selection-dot { position: absolute; top: 12px; left: 12px; width: 18px; height: 18px; border: 1px solid #aebed2; border-radius: 50%; background: #ffffffd9; display: flex; align-items: center; justify-content: center; }
.selected .selection-dot { background: #307bff; color: white; border-color: #307bff; }
.version-label { position: absolute; bottom: 8px; right: 8px; background: #ffffffed; color: #65738b; font-size: 11px; padding: 3px 7px; border-radius: 4px; }
.model-card-body { padding: 16px; }
.model-card h3 { font-size: 14px; font-weight: 500; margin: 0 0 8px; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; }
.model-category { font-size: 11px; color: #9098a5; margin: 0 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.model-description { font-size: 12px; color: #7c8594; line-height: 1.7; margin: 0 0 10px; height: 40px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.model-availability { color: #5f82b0; font-size: 11px; }
.model-availability.warning { color: #b58342; }
.selection-panel { overflow-y: auto; padding: 18px; background: #edf2f9; border: 1px solid #e3eaf4; border-radius: 12px; }
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
.submission-result.failed { color: #d34545; }
.submission-result.pending { color: #307bff; }
.result-device { color: #65738b; }
.submission-result.uncertain { color: #ba792c; }
.result-note { font-size: 12px; color: #9098a5; line-height: 1.7; }
.picker-footer { display: flex; flex-direction: column; align-items: center; gap: 8px; padding-top: 12px; border-top: 1px solid #dfe7f2; }
.drawer-footer { display: flex; flex-direction: column; align-items: center; gap: 6px; width: 100%; text-align: center; }
.drawer-footer-actions { display: flex; justify-content: center; gap: 10px; }
.drawer-footer .el-button + .el-button { margin-left: 0; }
.drawer-footer > span { color: #7b8ba2; font-size: 12px; line-height: 20px; }
.drawer-footer .el-button { min-width: 104px; height: 34px; border-radius: 7px; }
.drawer-footer .el-button:not(.el-button--primary) { background: #eaf0f8; border-color: #d9e3f1; color: #55749c; }
.drawer-footer .el-button:not(.el-button--primary):hover:not(:disabled) { background: #dfeafa; border-color: #bfd1ea; color: #3266a8; }
.catalog-error { margin-bottom: 16px; display: flex; flex-direction: column; gap: 12px; align-items: flex-start; }
.catalog-pagination { flex: none; max-width: 100%; }
.el-pagination { justify-content: center; }
@media (max-width: 1200px) { .model-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .picker-columns { grid-template-columns: minmax(0, 1fr) 250px; gap: 20px; } }
@media (max-width: 1000px) { .picker-header { flex-wrap: wrap; gap: 8px; } .picker-toolbar { flex-basis: 100%; justify-content: flex-start; } .picker-toolbar .el-input { max-width: none; } }
@media (max-width: 760px) { .picker-columns { display: flex; flex-direction: column; overflow-y: auto; } .model-catalog { flex: none; } .selection-panel { flex: none; max-height: 250px; } .target-device { display: none; } .picker-toolbar .el-input { max-width: 100%; width: 100%; } .category-tabs { gap: 4px; } .catalog-content { overflow: visible; } }
@media (max-width: 540px) { .model-grid { grid-template-columns: minmax(0, 1fr); } .category-tabs { flex-wrap: nowrap; overflow-x: auto; overflow-y: hidden; flex-shrink: 0; padding-bottom: 6px; } .category-tabs button { flex: none; } .catalog-pagination :deep(.el-pagination) { flex-wrap: wrap; gap: 6px; } .catalog-pagination :deep(.el-pagination__total) { flex-basis: 100%; margin: 0; text-align: center; } .drawer-footer .el-button { min-width: 92px; } }
</style>

<style>
.device-model-drawer.el-drawer { top: 16px; right: 16px; bottom: 16px; height: calc(100% - 32px); padding: 0; background: #f6f8fc; border: 1px solid #ffffff; border-radius: 18px; box-shadow: 0 24px 80px #182d4f26; overflow: hidden; }
.device-model-drawer .el-drawer__header { flex: none; min-height: auto; margin-bottom: 0; padding: 26px 32px 20px; color: #26364c; }
.device-model-drawer .el-drawer__title { font-size: 20px; line-height: 28px; font-weight: 600; letter-spacing: .2px; }
.device-model-drawer .el-drawer__close-btn { width: 32px; height: 32px; border-radius: 8px; color: #8390a3; }
.device-model-drawer .el-drawer__close-btn:hover { background: #eaf0f8; color: #307bff; }
.device-model-drawer .el-drawer__body { padding: 0 32px 18px; min-height: 0; overflow: hidden; }
.device-model-drawer--hub .el-drawer__body { padding-bottom: 16px; }
.device-model-drawer .el-drawer__footer { flex: none; padding: 0 32px 16px; border: 0; background: #f6f8fc; }
.device-model-drawer .el-pagination.is-background .btn-prev, .device-model-drawer .el-pagination.is-background .btn-next, .device-model-drawer .el-pagination.is-background .el-pager li { background: #eaf0f8; color: #607b9f; border: 1px solid #d9e3f1; border-radius: 6px; }
.device-model-drawer .el-pagination.is-background .el-pager li.is-active { background: #3976d3; border-color: #3976d3; color: #fff; }
.device-model-drawer .el-pagination.is-background button:disabled { color: #b4c1d3; border-color: #e5ebf4; background: #f0f4fa; }
.device-model-drawer .el-pagination__total { color: #7b8ba2; }
.model-description-dialog { border-radius: 14px; padding: 24px; }
.model-description-dialog .el-dialog__title { color: #26364c; font-weight: 600; }
@media (max-width: 760px) { .device-model-drawer.el-drawer { top: 8px; right: 8px; bottom: 8px; height: calc(100% - 16px); width: calc(100vw - 16px) !important; border-radius: 14px; } .device-model-drawer .el-drawer__header { padding: 20px 20px 16px; } .device-model-drawer .el-drawer__body { padding: 0 20px 12px; } .device-model-drawer .el-drawer__footer { padding: 0 20px 12px; } }
</style>
