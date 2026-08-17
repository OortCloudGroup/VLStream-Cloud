<template>
  <div class="mqtt-device-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <div>
            <span class="title">VLStream 设备</span>
            <el-tag :type="mediaAvailable ? 'success' : 'danger'" size="small">
              ZLM {{ mediaAvailable ? '可用' : '不可用' }}
            </el-tag>
          </div>
          <div class="filters">
            <el-input v-model="query.keyword" clearable placeholder="设备名称 / ID / 序列号" @keyup.enter="loadDevices" />
            <el-select v-model="query.online" clearable placeholder="全部状态">
              <el-option label="在线" :value="true" />
              <el-option label="离线" :value="false" />
            </el-select>
            <el-button type="primary" @click="search">搜索</el-button>
            <el-button @click="reset">重置</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="devices" stripe>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="deviceName" label="设备名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备 ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deviceSerial" label="序列号" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.online ? 'success' : 'info'">{{ row.online ? '在线' : '离线' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="ipAddr" label="IP" min-width="120" />
        <el-table-column prop="firmwareVersion" label="固件版本" min-width="110" />
        <el-table-column prop="lastHeartbeatTime" label="最后心跳" min-width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" :disabled="!row.online" @click="openPreview(row)">预览</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" background
          layout="total, prev, pager, next, sizes" :total="total" @change="loadDevices" />
      </div>
    </el-card>

    <el-dialog v-model="previewVisible" :title="`${currentDevice?.deviceName || currentDevice?.deviceId || ''} 实时预览`"
      width="900px" destroy-on-close @closed="releasePreview">
      <div class="stream-bar">
        <span>视频流</span>
        <el-select v-model="selectedStreamId" placeholder="请选择视频流" @change="startPreview">
          <el-option v-for="stream in streams" :key="stream.id" :value="stream.id"
            :label="`${stream.streamName || stream.channelId} (${stream.streamType})`" />
        </el-select>
      </div>
      <div v-loading="previewLoading" class="player">
        <rtc-player v-if="webrtcUrl" :video-url="webrtcUrl" />
        <el-empty v-else description="请选择可用视频流" />
      </div>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="设备详情" width="780px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail?.device" :column="2" border>
          <el-descriptions-item label="设备名称">{{ detail.device.deviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备 ID">{{ detail.device.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="设备型号">{{ detail.device.deviceModel || '尚未上报' }}</el-descriptions-item>
          <el-descriptions-item label="在线状态">{{ detail.device.online ? '在线' : '离线' }}</el-descriptions-item>
          <el-descriptions-item label="Application 版本">{{ detail.device.applicationVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="RootFS 版本">{{ detail.device.rootfsVersion || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="detail?.upgradeBlockedReason" :title="detail.upgradeBlockedReason" type="info"
          :closable="false" show-icon class="upgrade-alert" />

        <h4>可用固件升级</h4>
        <el-table :data="detail?.availableUpgrades || []" border empty-text="没有更高版本的兼容固件">
          <el-table-column label="目标" width="110">
            <template #default="{ row }">{{ targetLabel(row.target) }}</template>
          </el-table-column>
          <el-table-column prop="currentVersion" label="当前版本" width="130" />
          <el-table-column prop="latestVersion" label="最新版本" width="130" />
          <el-table-column prop="fileName" label="固件包" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button type="primary" :disabled="!detail?.canUpgrade" :loading="deployingFirmwareId === row.firmwareId"
                @click="deployFirmware(row)">固件升级</el-button>
            </template>
          </el-table-column>
        </el-table>

        <template v-if="detail?.latestTask">
          <h4>最近 OTA 任务</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="目标">{{ targetLabel(detail.latestTask.target) }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ taskStatusLabel(detail.latestTask.deployStatus) }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ detail.latestTask.currentVersion }} → {{ detail.latestTask.targetVersion }}</el-descriptions-item>
            <el-descriptions-item label="任务 ID">{{ detail.latestTask.requestId }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.latestTask.failureReason" label="失败原因" :span="2">
              {{ detail.latestTask.failureReason }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import {
  closeMqttDevicePreview, createMqttDevicePreview, deployMqttDeviceFirmware, getMqttDeviceDetail,
  getMqttDeviceMediaStatus, getMqttDevicePage, getMqttDeviceStreams
} from '@/api/vlstreamMqttDevice'

const loading = ref(false)
const devices = ref([])
const total = ref(0)
const mediaAvailable = ref(false)
const previewVisible = ref(false)
const previewLoading = ref(false)
const currentDevice = ref(null)
const streams = ref([])
const selectedStreamId = ref(null)
const activeStreamId = ref(null)
const webrtcUrl = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailDeviceId = ref(null)
const deployingFirmwareId = ref(null)
const query = reactive({ current: 1, size: 10, keyword: '', online: null })

const unwrap = response => response?.data ?? response

async function loadDevices() {
  loading.value = true
  try {
    const page = unwrap(await getMqttDevicePage(query)) || {}
    devices.value = page.records || []
    total.value = Number(page.total || 0)
  } finally { loading.value = false }
}

async function loadMediaStatus() {
  try { mediaAvailable.value = Boolean(unwrap(await getMqttDeviceMediaStatus())?.available) } catch { mediaAvailable.value = false }
}

function search() { query.current = 1; loadDevices() }
function reset() { Object.assign(query, { current: 1, size: 10, keyword: '', online: null }); loadDevices() }

async function openDetail(device) {
  detailDeviceId.value = device.id
  detailVisible.value = true
  await loadDetail()
}

async function loadDetail() {
  if (!detailDeviceId.value) return
  detailLoading.value = true
  try {
    detail.value = unwrap(await getMqttDeviceDetail(detailDeviceId.value)) || null
  } catch (error) {
    detail.value = null
    ElMessage.error(error?.response?.data?.msg || error?.message || '加载设备详情失败')
  } finally { detailLoading.value = false }
}

async function deployFirmware(candidate) {
  if (!detail.value?.canUpgrade || !detailDeviceId.value) return
  try {
    await ElMessageBox.confirm(
      `确定将 ${targetLabel(candidate.target)} 从 ${candidate.currentVersion} 升级到 ${candidate.latestVersion} 吗？`,
      '下发固件升级',
      { type: 'warning', confirmButtonText: '确认下发', cancelButtonText: '取消' }
    )
    deployingFirmwareId.value = candidate.firmwareId
    const task = unwrap(await deployMqttDeviceFirmware(detailDeviceId.value, candidate.firmwareId))
    ElMessage.success(`OTA 指令已发布，任务状态：${taskStatusLabel(task?.deployStatus)}`)
    await Promise.all([loadDetail(), loadDevices()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.msg || error?.message || 'OTA 指令下发失败')
    }
  } finally { deployingFirmwareId.value = null }
}

function targetLabel(target) { return target === 'rootfs' ? 'RootFS' : '应用程序' }

function taskStatusLabel(status) {
  const labels = {
    CREATED: '已创建', PUBLISHED: '已发布', ACCEPTED: '设备已接收', DOWNLOADING: '下载中',
    VERIFYING: '校验中', INSTALLING: '安装中', REBOOTING: '重启验证中', SUCCESS: '升级成功', FAILED: '升级失败'
  }
  return labels[status] || status || '-'
}

async function openPreview(device) {
  currentDevice.value = device
  previewVisible.value = true
  const data = unwrap(await getMqttDeviceStreams(device.id)) || []
  streams.value = data
  const preferred = data.find(item => item.isDefault) || data[0]
  selectedStreamId.value = preferred?.id || null
  if (preferred) await startPreview(preferred.id)
}

async function startPreview(streamId) {
  await releasePreview()
  if (!streamId || !currentDevice.value) return
  previewLoading.value = true
  try {
    const data = unwrap(await createMqttDevicePreview(currentDevice.value.id, streamId))
    activeStreamId.value = streamId
    webrtcUrl.value = data?.webrtcUrl || ''
    if (!webrtcUrl.value) ElMessage.error('后端未返回 WebRTC 地址')
  } catch (error) { ElMessage.error(error?.message || '创建预览失败') }
  finally { previewLoading.value = false }
}

async function releasePreview() {
  const deviceId = currentDevice.value?.id
  const streamId = activeStreamId.value
  activeStreamId.value = null
  webrtcUrl.value = ''
  if (deviceId && streamId) {
    try { await closeMqttDevicePreview(deviceId, streamId) } catch { /* ZLM auto_close remains the fallback. */ }
  }
}

onMounted(() => { loadDevices(); loadMediaStatus() })
</script>

<style scoped>
.mqtt-device-page { padding: 20px; }
.header, .filters, .stream-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.title { margin-right: 12px; font-size: 18px; font-weight: 600; }
.filters .el-input { width: 240px; }
.filters .el-select { width: 120px; }
.pagination { display: flex; justify-content: center; padding-top: 20px; }
.stream-bar { justify-content: flex-start; margin-bottom: 12px; }
.stream-bar .el-select { width: 360px; }
.player { min-height: 480px; background: #000; display: flex; align-items: center; justify-content: center; }
.player :deep(#webRtcPlayerBox) { max-height: 520px; }
.upgrade-alert { margin: 16px 0; }
h4 { margin: 18px 0 10px; }
</style>
