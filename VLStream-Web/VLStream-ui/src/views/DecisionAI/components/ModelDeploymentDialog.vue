<template>
  <el-dialog :model-value="modelValue" :title="$tp(batch ? '模型部署结果' : '批量配置算法模型')" width="min(900px, 94vw)"
    append-to-body :close-on-click-modal="!busy" :close-on-press-escape="!busy" :show-close="!busy"
    :before-close="close" @update:model-value="emit('update:modelValue', $event)">
    <el-alert :title="$tp(modelDeploymentImpact)" type="warning" show-icon :closable="false" />
    <template v-if="!batch && !operationId">
      <p>{{ $tp('影响范围') }}：<strong>{{ targets.length }}</strong> {{ $tp('台摄像机，每批最多20台；仅支持 detect 目标检测训练、具备 aiInfer 能力的 Hi3519DV500 / OM。') }}</p>
      <div class="deployment-targets"><el-tag v-for="device in targets" :key="device.deviceId">{{ device.deviceName || device.deviceId }} · {{ device.deviceId }}</el-tag></div>
      <el-form label-width="110px" class="deployment-form">
        <el-form-item :label="$tp('算法')">
          <el-select v-if="!model" v-model="algorithmId" filterable :loading="algorithmLoading" :disabled="busy" style="width:100%" :placeholder="$tp('请选择算法')">
            <el-option v-for="algorithm in algorithms" :key="algorithm.id" :label="algorithm.algorithmName || algorithm.name" :value="String(algorithm.id)" />
          </el-select>
          <span v-if="model">{{ model.modelName }} · {{ $tp('固定所选保存版本') }}</span>
        </el-form-item>
        <el-form-item :label="$tp('模型格式')"><el-tag>OM · Hi3519DV500</el-tag></el-form-item>
        <el-form-item :label="$tp('模型来源')">{{ $tp(model ? '固定所选模型的 OM、PT 和训练来源，不因后续重训替换。' : '从最近20次已完成训练中选择首份可用 OM；提交后锁定版本及双文件校验值。') }}</el-form-item>
      </el-form>
      <el-checkbox v-model="confirmed" :disabled="busy">{{ $tp('我已确认摄像机范围及推理模型切换影响') }}</el-checkbox>
    </template>
    <div v-else class="deployment-results">
      <p class="batch-id">{{ $tp('批次编号') }}：{{ operationId }}</p>
      <p v-if="batch">{{ $tp('成功') }} {{ batch.succeeded }} · {{ $tp('失败') }} {{ batch.failed }} · {{ $tp('进行中') }} {{ batch.pending }} · {{ $tp('未确认') }} {{ batch.unknown }}</p>
      <p v-if="batch">{{ batch.fileName }} · {{ $tp('训练编号') }} {{ batch.trainingId }} · SHA-256 {{ batch.sha256 }}</p>
      <el-table :data="batch?.items || []" max-height="360" row-key="deviceId">
        <el-table-column prop="deviceId" :label="$tp('摄像机业务编号')" min-width="140" />
        <el-table-column :label="$tp('状态')" min-width="160"><template #default="{ row }"><el-tag :type="deploymentTag(row)">{{ $tp(deploymentStatus(row)) }}</el-tag></template></el-table-column>
        <el-table-column :label="$tp('原因 / 任务编号')" min-width="230"><template #default="{ row }"><div>{{ row.reason }}</div><small>{{ row.requestId }}</small></template></el-table-column>
        <el-table-column prop="attempt" :label="$tp('尝试次数')" width="90" />
      </el-table>
      <p>{{ $tp('只有设备 SUCCESS 回执且模型与类别哈希均匹配，才计为成功。关闭窗口不会取消已提交任务；可从最近批次恢复查看。') }}</p>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="deployment-error" />
    <template #footer>
      <el-button :disabled="busy" @click="close()">{{ $tp(batch || operationId ? '关闭' : '取消') }}</el-button>
      <el-button v-if="operationId" :loading="refreshing" :disabled="busy" @click="refresh">{{ $tp('刷新结果') }}</el-button>
      <el-button v-if="operationId && !batch && submitRequest" :disabled="busy || refreshing" @click="recoverSubmission">{{ $tp('恢复原提交') }}</el-button>
      <el-button v-if="operationId" type="primary" :loading="busy" :disabled="!retryableDeviceIds(batch).length || refreshing" @click="retryFailed">{{ $tp('仅重试失败 / 未发送项') }} ({{ retryableDeviceIds(batch).length }})</el-button>
      <el-button v-else type="primary" :loading="busy" :disabled="!confirmed || (!algorithmId && !model) || !targets.length" @click="submit">{{ $tp('确认部署') }} ({{ targets.length }})</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { v4 as uuid } from 'uuid'
import { translatePhrase as uiText } from '@/i18n'
import { getAlgorithmPage } from '@/api/algorithmManagement'
import { submitModelBatch, getModelBatch, retryModelBatch } from '@/api/modelDispatchBatch'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { modelDeploymentImpact, deploymentStatus, deploymentTag, retryableDeviceIds, batchSelectionError } from '@/utils/modelDispatchBatch.mjs'

const props = defineProps({ modelValue: Boolean, devices: { type: Array, default: () => [] }, existingOperationId: { type: String, default: '' }, model: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'submitted', 'busy'])
const targets = ref([]), algorithms = ref([]), algorithmId = ref(''), algorithmLoading = ref(false)
const confirmed = ref(false), batch = ref(null), operationId = ref(''), busy = ref(false), refreshing = ref(false), error = ref('')
const submitRequest = ref(null)
let session = 0, poll, identityTimer, identity
let retryRequest = null
const message = ex => ex?.data?.msg || ex?.data?.message || ex?.response?.data?.msg || ex?.message || uiText('请求结果未确认，请刷新该批次核实')

function clearTimers() { clearTimeout(poll); clearInterval(identityTimer) }
function invalidate() {
  session++; clearTimers(); targets.value = []; algorithms.value = []; algorithmId.value = ''
  batch.value = null; operationId.value = ''; confirmed.value = false; busy.value = false; refreshing.value = false
  retryRequest = null; submitRequest.value = null; error.value = uiText('登录身份或租户已变化，已清空选择；已发送任务请回原账号核实')
  emit('update:modelValue', false)
}
function current(version) { if (version !== session || !props.modelValue) return false; try { identity.assertCurrent(); return true } catch { invalidate(); return false } }
function scheduleRefresh() {
  clearTimeout(poll)
  if (props.modelValue && operationId.value && !busy.value && (batch.value?.pending || batch.value?.unknown)) poll = setTimeout(refresh, 3000)
}
async function refresh() {
  if (refreshing.value || !operationId.value) return
  const version = session; refreshing.value = true
  try {
    const response = await getModelBatch(operationId.value)
    if (!current(version)) return
    if (response?.code !== 200 || !response.data) throw new Error(response?.msg || '无法读取批次')
    batch.value = response.data; error.value = ''
  } catch (ex) { if (current(version)) error.value = message(ex) }
  finally { if (version === session) { refreshing.value = false; scheduleRefresh() } }
}
async function submit() {
  if (busy.value || operationId.value || !confirmed.value) return
  error.value = batchSelectionError(targets.value)
  if (error.value) return
  const version = session
  operationId.value = uuid(); busy.value = true
  try {
    identity.assertCurrent()
    submitRequest.value = { operationId: operationId.value, modelType: 'om',
      ...(props.model ? { modelId: props.model.id } : { algorithmId: algorithmId.value }), deviceIds: targets.value.map(item => String(item.deviceId)) }
    const response = await submitModelBatch(submitRequest.value)
    if (!current(version)) return
    if (response?.code !== 200 || !response.data) throw new Error(response?.msg || '未收到批次结果')
    batch.value = response.data; emit('submitted', response.data)
  } catch (ex) {
    if (current(version)) error.value = `${message(ex)}；${uiText('提交编号已保留，请刷新核实，勿重新创建相同部署')}`
  } finally { if (version === session) { busy.value = false; scheduleRefresh() } }
}
async function recoverSubmission() {
  if (busy.value || refreshing.value || !submitRequest.value) return
  const version = session; busy.value = true
  try {
    identity.assertCurrent()
    const response = await submitModelBatch(submitRequest.value)
    if (!current(version)) return
    if (response?.code !== 200 || !response.data) throw new Error(response?.msg || '原提交结果未确认')
    batch.value = response.data; error.value = ''; emit('submitted', response.data)
  } catch (ex) { if (current(version)) error.value = message(ex) }
  finally { if (version === session) { busy.value = false; scheduleRefresh() } }
}
async function retryFailed() {
  if (busy.value || refreshing.value) return
  const deviceIds = retryableDeviceIds(batch.value)
  if (!deviceIds.length) return
  const version = session; busy.value = true; error.value = ''
  // Keep this exact key/body after a lost HTTP response, until its outcome is read.
  retryRequest ||= { retryId: uuid(), deviceIds }
  try {
    identity.assertCurrent()
    const response = await retryModelBatch(operationId.value, retryRequest)
    if (!current(version)) return
    if (response?.code !== 200 || !response.data) throw new Error(response?.msg || '重试结果未确认')
    batch.value = response.data; retryRequest = null
  } catch (ex) { if (current(version)) error.value = message(ex) }
  finally { if (version === session) { busy.value = false; scheduleRefresh() } }
}
function close(done) {
  if (busy.value) return
  clearTimers(); session++
  if (typeof done === 'function') done()
  emit('update:modelValue', false)
}
watch(busy, value => emit('busy', value))
watch(() => props.modelValue, async visible => {
  session++; clearTimers()
  if (!visible) return
  const version = session; identity = captureComputeIdentity(); retryRequest = null; submitRequest.value = null
  targets.value = props.devices.map(device => ({ ...device })); confirmed.value = false
  batch.value = null; algorithms.value = []; error.value = ''; algorithmId.value = props.model ? String(props.model.algorithmId || '') : ''
  operationId.value = props.existingOperationId; busy.value = false; refreshing.value = false
  identityTimer = setInterval(() => { try { identity.assertCurrent() } catch { invalidate() } }, 500)
  if (operationId.value) { refresh(); return }
  if (props.model) return
  algorithmLoading.value = true
  try {
    const response = await getAlgorithmPage({ current: 1, size: 1000 })
    if (!current(version)) return
    if (response?.code !== 200 || !Array.isArray(response?.data?.records)) throw new Error('算法列表加载失败')
    algorithms.value = response.data.records
  } catch (ex) { if (current(version)) error.value = message(ex) }
  finally { if (version === session) algorithmLoading.value = false }
})
onBeforeUnmount(() => { session++; clearTimers(); emit('busy', false) })
</script>

<style scoped>
.deployment-targets { display:flex; flex-wrap:wrap; gap:8px; max-height:140px; overflow:auto; }
.deployment-form { margin-top:22px; }
.deployment-results { margin-top:16px; }
.deployment-results p { line-height:1.7; overflow-wrap:anywhere; color:#606266; font-size:13px; }
.deployment-results small,.batch-id { color:#909399; overflow-wrap:anywhere; }
.deployment-error { margin-top:16px; }
</style>
