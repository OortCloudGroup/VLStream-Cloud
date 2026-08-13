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
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }"><el-button link type="primary" :disabled="!row.online" @click="openPreview(row)">预览</el-button></template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import {
  closeMqttDevicePreview, createMqttDevicePreview, getMqttDeviceMediaStatus,
  getMqttDevicePage, getMqttDeviceStreams
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
</style>
