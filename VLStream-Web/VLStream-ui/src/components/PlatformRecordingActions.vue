<template>
  <span v-hasPermi="['vls:menu:video-playback']" class="recording-actions" @click.stop @pointerdown.stop>
    <el-button size="small" @click="open">{{ $tp('录像') }}</el-button>
    <el-button size="small" @click="playback">{{ $tp('回放') }}</el-button>
    <el-dialog v-model="visible" :title="$tp('平台录像')" width="560px" append-to-body destroy-on-close>
      <div v-loading="loading" class="recording-settings">
        <p>{{ $tp('关闭播放器后继续录制，保留时间按当前通道设置。') }}</p>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <el-alert v-if="!canStart" :title="$tp('设备离线或状态未知：可查看历史录像、保存计划，上线后再开始录制。')" type="info" :closable="false" />
        <el-select v-model="selected" :placeholder="$tp('选择通道')" @change="loadPlan">
          <el-option v-for="item in choices" :key="key(item.streamId)" :value="key(item.streamId)" :label="item.name || $tp('设备主码流')" />
        </el-select>
        <div class="recording-status"><span>{{ $tp(recordingState(current)) }}</span>
          <el-button :disabled="loading || !choices.length || (!current?.manual_enabled && !canStart)" :type="current?.manual_enabled ? 'danger' : 'primary'" @click="toggleManual">
            {{ $tp(current?.manual_enabled ? '停止手动录制' : '开始手动录制') }}
          </el-button>
        </div>
        <el-alert v-if="current?.error_code" :title="$tp(errorText(current.error_code))" type="warning" :closable="false" />
        <el-divider />
        <div class="recording-status"><strong>{{ $tp('录像保留时间') }}</strong>
          <el-select v-model="retentionDays" :aria-label="$tp('录像保留时间')" style="width: 150px">
            <el-option v-for="option in retentionOptions" :key="option.days" :value="option.days" :label="$tp(option.label)" />
          </el-select>
        </div>
        <small>{{ $tp('适用于已有和后续录像；缩短期限将在下次清理时删除超期录像，永久不自动清理。') }}</small>
        <el-button :disabled="loading || !choices.length" @click="saveRetention">{{ $tp('保存保留时间') }}</el-button>
        <el-divider />
        <div class="recording-status"><strong>{{ $tp('定时录像') }}</strong><el-switch v-model="plan.enabled" :aria-label="$tp('定时录像')" /></div>
        <el-checkbox-group v-model="plan.weekDays">
          <el-checkbox v-for="(day, i) in days" :key="i" :label="i + 1">{{ $tp(day) }}</el-checkbox>
        </el-checkbox-group>
        <div class="recording-times">
          <el-time-select v-model="plan.startTime" start="00:00" end="23:45" step="00:15" :placeholder="$tp('开始时间')" />
          <span>—</span>
          <el-time-select v-model="plan.endTime" start="00:00" end="23:45" step="00:15" :placeholder="$tp('结束时间')" />
        </div>
        <small>{{ $tp('北京时间；起止相同表示全天，结束早于开始表示跨午夜。') }}</small>
        <small>{{ $tp('关闭定时计划不会停止手动录制；停止手动录制不会关闭定时计划。') }}</small>
        <el-button type="primary" :disabled="loading || !choices.length" @click="savePlan">{{ $tp('保存计划') }}</el-button>
      </div>
    </el-dialog>
  </span>
</template>
<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { platformRecording as api, recordingError, recordingState } from '@/api/platformRecording'
import { translatePhrase as t } from '@/i18n'
const props = defineProps({ protocol: { type: String, required: true }, rowId: { type: [String, Number], required: true }, online: { type: Boolean, default: null } })
const canStart = computed(() => props.protocol !== 'VLSTREAM' || props.online === true)
const emit = defineEmits(['changed'])
const router = useRouter()
const visible = ref(false), loading = ref(false), error = ref(''), rows = ref([]), choices = ref([]), selected = ref('')
// An empty stream ID is a valid device-default channel, not an empty selection.
const key = value => `stream:${String(value ?? '')}`
const current = computed(() => rows.value.find(row => key(row.stream_id) === selected.value))
const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const retentionDays = ref(7)
const retentionOptions = [{ days: 7, label: '7天' }, { days: 30, label: '30天' }, { days: 90, label: '90天' }, { days: 0, label: '永久' }]
const plan = reactive({ enabled: false, weekDays: [1, 2, 3, 4, 5, 6, 7], startTime: '00:00', endTime: '00:00' })
let generation = 0, timer, disposed = false
const errorText = code => code === 'DEVICE_OFFLINE' ? '设备离线或在线状态无法确认，不能开始录像' : code === 'STORAGE_FULL' ? '存储空间不足，已暂停录制' : code === 'CAPACITY' ? '录像资源繁忙，等待可用资源' : '未收到可用画面或后台录像环境不可用，请检查设备和服务状态'
function loadPlan() { retentionDays.value = Number(current.value?.retention_days ?? 7); Object.assign(plan, current.value?.plan || { enabled: false, weekDays: [1, 2, 3, 4, 5, 6, 7], startTime: '00:00', endTime: '00:00' }) }
async function refresh(ticket = generation) {
  const values = await api.sources(props.protocol, props.rowId)
  if (ticket !== generation || disposed) return
  rows.value = values
}
async function open() {
  visible.value = true; loading.value = true; error.value = ''; choices.value = []; rows.value = []; selected.value = ''; loadPlan()
  const ticket = ++generation
  try {
    await refresh(ticket)
    let available = []
    try { available = await api.choices(props.protocol, props.rowId) }
    catch (failure) { if (ticket === generation) error.value = recordingError(failure) }
    if (ticket !== generation || disposed) return
    const merged = new Map(rows.value.map(row => [key(row.stream_id), { streamId: row.stream_id, name: row.source?.channelName || row.source?.name }]))
    available.filter(item => item.supported).forEach(item => merged.set(key(item.streamId), item))
    choices.value = [...merged.values()]; selected.value = choices.value.length ? key(choices.value[0].streamId) : ''; loadPlan()
  } catch (failure) { if (ticket === generation) error.value = recordingError(failure) }
  finally { if (ticket === generation) loading.value = false }
}
async function ensureSource() {
  // Fresh authorization only for an explicit new recording/plan; stopping works while offline.
  const choice = choices.value.find(item => key(item.streamId) === selected.value)
  const result = await api.bind({ protocol: props.protocol, deviceRowId: props.rowId, streamId: String(choice?.streamId ?? '') || null })
  return result.id
}
async function mutate(operation) {
  if (loading.value) return
  loading.value = true; error.value = ''
  try { await operation(); await refresh(); emit('changed'); ElMessage.success(t('操作已保存')) }
  catch (failure) { error.value = recordingError(failure) }
  finally { loading.value = false }
}
const toggleManual = () => mutate(async () => {
  const stop = !!current.value?.manual_enabled
  const id = stop ? current.value.id : await ensureSource()
  await api.manual(id, !stop)
})
const savePlan = () => mutate(async () => {
  const id = !plan.enabled && current.value ? current.value.id : await ensureSource()
  await api.plan(id, { ...plan, weekDays: [...plan.weekDays] })
})
const saveRetention = () => mutate(async () => {
  const id = current.value?.id || await ensureSource()
  await api.retention(id, retentionDays.value)
})
function playback() { visible.value = false; router.push({ path: '/video-playback', query: { protocol: props.protocol, rowId: String(props.rowId) } }) }
watch(visible, value => {
  clearInterval(timer)
  if (value) timer = setInterval(() => { if (!loading.value) refresh().catch(failure => { error.value = recordingError(failure) }) }, 5000)
  else generation++
})
watch(() => [props.protocol, props.rowId], () => { visible.value = false; generation++ })
onBeforeUnmount(() => { disposed = true; generation++; clearInterval(timer) })
</script>
<style scoped>
.recording-actions { display: inline-flex; gap: 6px; }
.recording-actions > .el-button + .el-button { margin: 0; }
.recording-settings { display: grid; gap: 16px; }
.recording-settings p { margin: 0; color: #657087; }
.recording-settings small { color: #657087; line-height: 1.5; }
.recording-settings .el-divider { margin: 4px 0; }
.recording-status, .recording-times { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
</style>
