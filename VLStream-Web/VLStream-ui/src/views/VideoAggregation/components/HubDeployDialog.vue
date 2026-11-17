<template>
  <el-dialog :model-value="modelValue" :title="$tp('下发 Model Hub 模型')" width="min(620px, 94vw)" append-to-body
    :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" @update:model-value="close">
    <p>{{ model?.name || model?.alias }}</p>
    <el-alert type="info" :closable="false" :title="$tp('仅支持兼容的 Hi3519DV500 目标检测 OM 模型。服务端将校验固定版本、类别文件、双文件摘要和设备权限。')" />
    <el-form label-position="top" class="hub-deploy-form">
      <el-form-item :label="$tp('关联本租户算法')">
        <el-select v-model="algorithmId" filterable remote :remote-method="loadAlgorithms" :loading="algorithmLoading" :disabled="busy || Boolean(attempt)" :placeholder="$tp('搜索目标检测算法')" style="width:100%">
          <el-option v-for="item in algorithms" :key="item.id" :value="String(item.id)" :label="item.name" />
        </el-select>
      </el-form-item>
    </el-form>
    <p>{{ $tp('目标设备：') }}{{ devices.map(device => device.deviceName || device.deviceId).join('、') }}</p>
    <el-alert v-if="error" type="error" :title="error" :closable="false" show-icon />
    <el-table v-if="results.length" :data="results" size="small">
      <el-table-column prop="deviceId" :label="$tp('设备编号')" min-width="120" />
      <el-table-column :label="$tp('下发状态')" min-width="180"><template #default="{ row }">{{ resultText(row) }}</template></el-table-column>
      <el-table-column prop="requestId" :label="$tp('任务编号')" min-width="170" show-overflow-tooltip />
    </el-table>
    <p class="deployment-note">{{ $tp('请求受理不等于部署成功，最终以设备回执为准。关闭窗口不会撤销已受理任务；结果未确认时请核实原任务，不要重复下发。') }}</p>
    <p v-if="attempt" class="deployment-note">{{ $tp('操作编号：') }}{{ attempt.operationId }}</p>
    <template #footer>
      <el-button :disabled="busy" @click="close(false)">{{ $tp('关闭') }}</el-button>
      <el-button v-if="attempt" type="primary" :loading="busy" @click="submit(true)">{{ $tp('核实原任务') }}</el-button>
      <el-button v-else type="primary" :loading="busy" :disabled="!algorithmId || !devices.length" @click="submit(false)">{{ $tp('确认下发') }}</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import { translatePhrase as uiText } from '@/i18n'
import { getAlgorithmPage } from '@/api/algorithmManagement'
import { deployHubModel } from '@/api/modelHubCatalog'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { isMultiTenantMode, redirectToPlatformLogin } from '@/utils/platformSession'
import { startModelHubLogin } from '@/utils/modelHubAuth'
import { hubDeploymentPayload, checkedHubDeploymentResults, hubAttemptStore } from '@/utils/modelHubDeployment.mjs'
import { attemptResult } from '@/utils/savedModelDispatch.mjs'
const props = defineProps({ modelValue: Boolean, model: Object, devices: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue', 'busy', 'submitted'])
const busy = ref(false), algorithmLoading = ref(false), algorithms = ref([]), algorithmId = ref(''), error = ref(''), results = ref([]), attempt = ref(null)
let sequence = 0, identity = null, attemptKey = ''
const resultText = row => row.error || uiText(attemptResult({ requestId: row.requestId, state: row.status }).message)
function close(value) { if (!busy.value) emit('update:modelValue', value) }
async function loadAlgorithms(name = '') {
  const revision = ++sequence
  algorithmLoading.value = true
  try {
    identity?.assertCurrent()
    const response = await getAlgorithmPage({ current: 1, size: 100, category: 'detect', name })
    identity?.assertCurrent()
    if (revision !== sequence) return
    if (Number(response?.code) !== 200) throw new Error(response?.msg || '算法读取失败')
    algorithms.value = (response.data?.records || []).filter(item => item.category === 'detect')
  } catch (e) { if (revision === sequence) error.value = e.message || uiText('读取算法失败') }
  finally { if (revision === sequence) algorithmLoading.value = false }
}
watch(() => props.modelValue, visible => {
  if (!visible) return
  identity = captureComputeIdentity()
  attemptKey = JSON.stringify([props.model?.uid, props.model?.published_commit, props.devices.map(item => String(item.deviceId)).sort()])
  const previous = hubAttemptStore.get(attemptKey, identity)
  attempt.value = previous?.payload || null; results.value = previous?.results || []; algorithmId.value = attempt.value?.algorithmId || ''; error.value = ''
  loadAlgorithms()
})
async function submit(recover) {
  if (busy.value) return
  const session = getOortCloudSession(isMultiTenantMode() ? 'multi' : 'single')
  if (!session.accessToken) {
    error.value = uiText('请先登录 OortCloud，再确认下发')
    if (isMultiTenantMode()) redirectToPlatformLogin(); else startModelHubLogin(null, { returnToCurrent: true })
    return
  }
  busy.value = true; emit('busy', true); error.value = ''
  try {
    identity.assertCurrent()
    if (!recover) attempt.value = hubDeploymentPayload(props.model, algorithmId.value, props.devices, uuidv4())
    if (!attempt.value) throw new Error('没有可核实的原任务')
    hubAttemptStore.set(attemptKey, { payload: attempt.value, results: results.value }, identity)
    const data = await deployHubModel(attempt.value, session)
    identity.assertCurrent()
    results.value = checkedHubDeploymentResults(data, attempt.value.deviceIds)
    hubAttemptStore.set(attemptKey, { payload: attempt.value, results: results.value }, identity)
    emit('submitted')
  } catch (e) { error.value = e.message || uiText('提交结果未确认，请核实原任务') }
  finally { busy.value = false; emit('busy', false) }
}
onBeforeUnmount(() => { sequence++ })
</script>
<style scoped>
.hub-deploy-form { margin-top: 18px; }
.deployment-note { color: #7b8799; line-height: 1.7; font-size: 12px; overflow-wrap: anywhere; }
</style>
