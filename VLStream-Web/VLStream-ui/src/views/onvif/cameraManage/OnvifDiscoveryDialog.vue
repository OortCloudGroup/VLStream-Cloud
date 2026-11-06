<template>
  <el-dialog :model-value="modelValue" :title="$tp('发现并添加摄像头')" width="min(1240px, 96vw)"
    append-to-body :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy"
    @update:model-value="close">
    <el-alert :title="$tp('设备须通过 ONVIF 和真实取流验证后才能添加。已添加设备回显已有资料，不重复添加。')"
      type="info" :closable="false" />
    <div class="discovery-account">
      <span>{{ $tp('批量账号') }}</span>
      <el-input v-model="account.username" :placeholder="$tp('用户名')" :disabled="busy" />
      <el-input v-model="account.password" type="password" show-password :placeholder="$tp('密码')" :disabled="busy" />
      <el-select v-model="account.auth" :disabled="busy" :aria-label="$tp('批量认证方式')">
        <el-option label="WS-UsernameToken" value="1" />
        <el-option label="Digest" value="2" />
      </el-select>
      <el-button :disabled="busy || !selected.length" @click="applyAccount">{{ $tp('应用到所选') }}</el-button>
    </div>
    <el-table :data="rows" row-key="key" border max-height="520" @selection-change="selected = $event">
      <el-table-column type="selection" width="44" :selectable="row => !row.existingId && row.state !== 'added' && !busy" />
      <el-table-column type="expand" width="36">
        <template #default="{ row }">
          <el-form v-if="row.result && !row.existingId" :model="row.result" label-width="90px" class="discovery-details" :disabled="busy">
            <el-form-item :label="$tp('通道')">
              <el-select v-model="row.result.channel">
                <el-option v-for="(_, channel) in row.result.streamUris" :key="channel" :label="channel" :value="channel" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$tp('播放类型')">
              <el-select v-model="row.result.playType">
                <el-option :label="$tp('本地')" value="1" /><el-option :label="$tp('推流')" value="2" /><el-option label="EasyNTS" value="3" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="row.result.playType === '2'" :label="$tp('流id')"><el-input v-model="row.result.streamId" maxlength="100" /></el-form-item>
            <el-form-item v-if="row.result.playType === '3'" :label="$tp('EasyNTS地址')"><el-input v-model="row.result.easyNTSUrl" maxlength="200" /></el-form-item>
            <el-form-item :label="$tp('厂商')"><el-input v-model="row.result.firm" maxlength="30" /></el-form-item>
            <el-form-item :label="$tp('型号')"><el-input v-model="row.result.model" maxlength="20" /></el-form-item>
            <el-form-item :label="$tp('固件版本')"><el-input v-model="row.result.firmwareVersion" maxlength="100" /></el-form-item>
          </el-form>
          <p v-else class="discovery-hint">{{ $tp(row.existingId ? '此摄像头已添加，资料修改请使用设备列表的修改入口。' : '认证成功后可在此选择通道和补充设备资料。') }}</p>
        </template>
      </el-table-column>
      <el-table-column label="IP" prop="ip" width="150" />
      <el-table-column :label="$tp('设备名称')" min-width="130">
        <template #default="{ row }"><el-input v-model="row.name" :disabled="busy || !!row.existingId || row.state === 'added'" :aria-label="$tp('设备名称')" /></template>
      </el-table-column>
      <el-table-column :label="$tp('用户名')" min-width="115">
        <template #default="{ row }"><el-input v-model="row.username" :disabled="busy || row.state === 'added'" @input="invalidateAuthentication(row)" :aria-label="$tp('用户名')" /></template>
      </el-table-column>
      <el-table-column :label="$tp('密码')" min-width="140">
        <template #default="{ row }"><el-input v-model="row.password" type="password" show-password :disabled="busy || row.state === 'added'" @input="invalidateAuthentication(row)" :aria-label="$tp('密码')" /></template>
      </el-table-column>
      <el-table-column :label="$tp('认证方式')" width="170">
        <template #default="{ row }"><el-select v-model="row.auth" :disabled="busy || row.state === 'added'" @change="invalidateAuthentication(row)" :aria-label="$tp('认证方式')"><el-option label="WS-UsernameToken" value="1" /><el-option label="Digest" value="2" /></el-select></template>
      </el-table-column>
      <el-table-column :label="$tp('结果')" min-width="170">
        <template #default="{ row }">
          <el-tag v-if="row.existingId || row.state === 'added'" type="success">{{ $tp('已添加') }}</el-tag>
          <el-tag v-if="row.state === 'authenticated'" type="success">{{ $tp('认证成功') }}</el-tag>
          <el-tag v-else-if="row.state === 'failed' || row.state === 'saveFailed'" type="danger">{{ $tp(row.state === 'failed' ? '认证失败' : '添加失败') }}</el-tag>
          <el-tag v-else-if="!row.existingId && row.state !== 'added'" type="info">{{ $tp('待认证') }}</el-tag>
          <div v-if="row.error" class="discovery-error">{{ $tp(row.error) }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="$tp('操作')" width="90" fixed="right">
        <template #default="{ row }"><el-button link type="primary" :disabled="busy || row.state === 'added'" :loading="row.busy" @click="authenticateOne(row)" v-hasPermi="['onvif:device:add']">{{ $tp('认证') }}</el-button></template>
      </el-table-column>
    </el-table>
    <p class="discovery-hint">{{ $tp('已选择') }} {{ selected.length }} {{ $tp('台，可添加') }} {{ readyCount }} {{ $tp('台。展开行可调整通道与设备资料。') }}</p>
    <p v-if="summary" role="status">{{ $tp('本次添加成功') }} {{ summary.added }} {{ $tp('台，失败') }} {{ summary.failed }} {{ $tp('台，跳过') }} {{ summary.skipped }} {{ $tp('台') }}</p>
    <template #footer>
      <el-button :disabled="busy" @click="close(false)">{{ $tp('关闭') }}</el-button>
      <el-button :disabled="busy || !selected.length" :loading="authenticating" @click="authenticateSelected" v-hasPermi="['onvif:device:add']">{{ $tp('认证所选') }}</el-button>
      <el-button type="primary" :disabled="busy || !readyCount" :loading="saving" @click="saveSelected" v-hasPermi="['onvif:device:add']">{{ $tp('添加认证成功的所选设备') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { addDevice, addOnvif } from '@/api/onvif/device'
import { authenticateRow, canRegister, discoveryRows, invalidateAuthentication, registerRows } from '@/utils/onvifDiscovery.mjs'

const props = defineProps({ modelValue: Boolean, discovered: { type: Array, default: () => [] }, existing: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue', 'added'])
const rows = ref([])
const selected = ref([])
const authenticating = ref(false)
const saving = ref(false)
const summary = ref(null)
const account = reactive({ username: '', password: '', auth: '1' })
const busy = computed(() => authenticating.value || saving.value || rows.value.some(row => row.busy))
const readyCount = computed(() => selected.value.filter(canRegister).length)
let generation = 0
watch(() => props.modelValue, open => {
  generation += 1
  selected.value = []
  summary.value = null
  account.username = ''; account.password = ''; account.auth = '1'
  rows.value = open ? discoveryRows(props.discovered, props.existing) : []
})
function close(value) { if (!busy.value) emit('update:modelValue', value) }
function applyAccount() {
  for (const row of selected.value) {
    row.username = account.username; row.password = account.password; row.auth = account.auth
    invalidateAuthentication(row)
  }
}
async function authenticateOne(row) {
  const current = generation
  await authenticateRow(row, addOnvif, () => props.modelValue && current === generation)
}
async function authenticateSelected() {
  authenticating.value = true
  try { for (const row of selected.value) await authenticateOne(row) }
  finally { authenticating.value = false }
}
async function saveSelected() {
  saving.value = true
  const current = generation
  try {
    summary.value = await registerRows(selected.value, addDevice, () => props.modelValue && current === generation)
    if (summary.value.added) emit('added')
  } finally { saving.value = false }
}
</script>

<style scoped>
.discovery-account { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin: 18px 0; }
.discovery-account .el-input { width: 190px; }
.discovery-account .el-select { width: 180px; }
.discovery-details { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 0 16px; padding: 18px 24px 0; }
.discovery-hint { color: #64748b; font-size: 13px; line-height: 1.7; margin: 12px 16px; }
.discovery-error { color: #c0392b; font-size: 12px; line-height: 1.6; margin-top: 6px; overflow-wrap: anywhere; }
</style>
