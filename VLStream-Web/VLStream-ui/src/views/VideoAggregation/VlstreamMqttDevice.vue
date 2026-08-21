<template>
  <DeviceClassificationLayout
    protocol-type="VLSTREAM"
    :selected-device-keys="classificationDeviceKeys"
    @filter-change="handleClassificationFilter"
    @assigned="loadDevices"
  >
    <div class="protocol-page">
      <el-card shadow="never">
        <template #header>
          <div class="header">
            <div class="header-title">
              <span class="title">VLStream 设备</span>
              <el-tag :type="mediaAvailable ? 'success' : 'danger'" size="small">
                WVP ZLM {{ mediaAvailable ? '可用' : '不可用' }}
              </el-tag>
            </div>
            <div class="filters">
              <el-input v-model="query.keyword" clearable placeholder="设备名称 / ID / 序列号" @keyup.enter="search" />
              <el-select v-model="query.online" clearable placeholder="全部状态">
                <el-option label="在线" :value="true" />
                <el-option label="离线" :value="false" />
              </el-select>
              <el-button type="primary" @click="search">搜索</el-button>
              <el-button @click="reset">重置</el-button>
            </div>
          </div>
        </template>

        <el-alert v-if="serviceError" :title="serviceError" type="error" :closable="false" show-icon class="service-alert" />

        <el-table v-loading="loading" :data="devices" stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column prop="deviceName" label="设备名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="deviceId" label="设备 ID" min-width="190" show-overflow-tooltip />
          <el-table-column prop="deviceModel" label="设备型号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="deviceSerial" label="序列号" min-width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag :type="row.online ? 'success' : 'info'">{{ row.online ? '在线' : '离线' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="ipAddr" label="IP" min-width="120" />
          <el-table-column prop="firmwareVersion" label="RootFS 版本" min-width="110" />
          <el-table-column prop="lastHeartbeatTime" label="最后心跳" min-width="170" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" :disabled="!mediaAvailable" @click="openPreview(row)">预览</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" background
            layout="total, prev, pager, next, sizes" :total="total" @change="loadDevices" />
        </div>
      </el-card>

      <el-dialog v-model="previewVisible" :title="`${currentDevice?.deviceName || currentDevice?.deviceId || ''} 实时预览`"
        width="900px" destroy-on-close @closed="releasePreview">
        <div class="stream-bar">
          <span>视频流</span>
          <el-select v-model="selectedStreamId" placeholder="请选择视频流" @change="startPreview">
            <el-option v-for="stream in streams" :key="stream.id" :value="stream.id"
              :label="`${stream.channelId || stream.streamName || stream.id} (${stream.streamType || stream.protocol || '-'})`" />
          </el-select>
        </div>
        <div v-loading="previewLoading" class="player">
          <rtc-player v-if="webrtcUrl" :video-url="webrtcUrl" :hasaudio="true" />
          <el-empty v-else description="请选择可用视频流" />
        </div>
      </el-dialog>

      <el-dialog v-model="detailVisible" title="设备详情" width="820px" destroy-on-close>
        <div v-loading="detailLoading">
        <el-descriptions v-if="detailDevice" :column="2" border>
          <el-descriptions-item label="设备名称">{{ detailDevice.deviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备 ID">{{ detailDevice.deviceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备型号">{{ detailDevice.deviceModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="在线状态">{{ detailDevice.online ? '在线' : '离线' }}</el-descriptions-item>
          <el-descriptions-item label="IP 地址">{{ detailDevice.ipAddr || '-' }}</el-descriptions-item>
          <el-descriptions-item label="RootFS 版本">{{ detailDevice.firmwareVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后心跳" :span="2">{{ detailDevice.lastHeartbeatTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="firmwareDetail?.upgradeBlockedReason" :title="firmwareDetail.upgradeBlockedReason"
          type="info" :closable="false" show-icon class="firmware-alert" />

        <h4>可用固件升级</h4>
        <el-table :data="firmwareDetail?.availableUpgrades || []" border empty-text="没有更高版本的兼容固件">
          <el-table-column label="目标" width="90"><template #default>RootFS</template></el-table-column>
          <el-table-column prop="currentVersion" label="当前版本" width="120" />
          <el-table-column prop="latestVersion" label="最新版本" width="120" />
          <el-table-column prop="fileName" label="固件包" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="110" align="center">
            <template #default="{ row }">
              <el-button type="primary" size="small" :loading="deployingFirmware"
                :disabled="!firmwareDetail?.canUpgrade" @click="deployFirmware(row)">固件升级</el-button>
            </template>
          </el-table-column>
        </el-table>

        <template v-if="firmwareDetail?.latestTask">
          <h4>最近 OTA 任务</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="目标">RootFS</el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ taskStatusText(firmwareDetail.latestTask.deployStatus) }}
              <el-button v-if="isActiveTask(firmwareDetail.latestTask.deployStatus)" link type="danger"
                :loading="cancellingTask" @click="cancelLatestTask">终止任务</el-button>
            </el-descriptions-item>
            <el-descriptions-item label="版本">{{ firmwareDetail.latestTask.currentVersion }} → {{ firmwareDetail.latestTask.targetVersion }}</el-descriptions-item>
            <el-descriptions-item label="任务 ID">{{ firmwareDetail.latestTask.requestId }}</el-descriptions-item>
            <el-descriptions-item v-if="firmwareDetail.latestTask.failureReason" label="说明" :span="2">
              {{ firmwareDetail.latestTask.failureReason }}
            </el-descriptions-item>
          </el-descriptions>
        </template>

        <h4>视频源</h4>
        <el-table :data="detailStreams" border empty-text="设备没有上报视频源">
          <el-table-column prop="channelId" label="通道" min-width="120" />
          <el-table-column prop="streamType" label="码流类型" width="110" />
          <el-table-column prop="protocol" label="协议" width="90" />
          <el-table-column label="默认流" width="90"><template #default="{ row }">{{ row.defaultStream ? '是' : '否' }}</template></el-table-column>
          <el-table-column prop="url" label="视频源地址" min-width="220" show-overflow-tooltip />
        </el-table>
        </div>
      </el-dialog>
    </div>
  </DeviceClassificationLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import {
  cancelMqttDeviceFirmwareTask,
  createMqttDevicePreview,
  deployMqttDeviceFirmware,
  getMqttDeviceDetail,
  getMqttDeviceMediaStatus,
  getMqttDevicePage,
  getMqttDeviceStreams
} from '@/api/vlstreamMqttDevice'

const loading = ref(false)
const devices = ref([])
const total = ref(0)
const mediaAvailable = ref(false)
const serviceError = ref('')
const classificationDeviceKeys = ref([])
const previewVisible = ref(false)
const previewLoading = ref(false)
const currentDevice = ref(null)
const streams = ref([])
const selectedStreamId = ref(null)
const webrtcUrl = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailStreams = ref([])
const firmwareDetail = ref(null)
const deployingFirmware = ref(false)
const cancellingTask = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', online: undefined })
const detailDevice = computed(() => firmwareDetail.value?.device || currentDevice.value)

function errorMessage(error, fallback) { return error?.response?.data?.msg || error?.message || fallback }

async function loadDevices() {
  loading.value = true
  serviceError.value = ''
  try {
    const result = await getMqttDevicePage(query)
    devices.value = result?.rows || []
    total.value = Number(result?.total || 0)
  } catch (error) {
    devices.value = []
    total.value = 0
    serviceError.value = errorMessage(error, 'WVP 服务不可用，无法加载 VLStream 设备')
  } finally { loading.value = false }
}

async function loadMediaStatus() {
  try { mediaAvailable.value = Boolean((await getMqttDeviceMediaStatus())?.data?.available) }
  catch { mediaAvailable.value = false }
}

function search() { query.pageNum = 1; loadDevices() }
function reset() {
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', online: undefined, categoryType: undefined, categoryId: undefined, unclassified: undefined })
  loadDevices()
}
function handleSelectionChange(selection) { classificationDeviceKeys.value = selection.map(item => String(item.id)) }
function handleClassificationFilter(filter) { Object.assign(query, filter, { pageNum: 1 }); loadDevices() }

async function openDetail(device) {
  currentDevice.value = device
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [streamsResult, detailResult] = await Promise.all([
      getMqttDeviceStreams(device.id),
      getMqttDeviceDetail(device.id)
    ])
    detailStreams.value = streamsResult?.data || []
    firmwareDetail.value = detailResult?.data || null
  }
  catch (error) {
    detailStreams.value = []
    firmwareDetail.value = null
    ElMessage.error(errorMessage(error, '加载设备详情失败'))
  }
  finally { detailLoading.value = false }
}

async function reloadFirmwareDetail() {
  if (!currentDevice.value) return
  firmwareDetail.value = (await getMqttDeviceDetail(currentDevice.value.id))?.data || null
}

async function deployFirmware(candidate) {
  try {
    await ElMessageBox.confirm(
      `确认将设备升级到 RootFS ${candidate.latestVersion}？升级过程中设备将重启。`,
      '固件升级确认', { type: 'warning' }
    )
  } catch { return }
  deployingFirmware.value = true
  try {
    await deployMqttDeviceFirmware(currentDevice.value.id, candidate.firmwareId)
    ElMessage.success('OTA 固件升级指令已下发')
    await reloadFirmwareDetail()
  } catch (error) {
    ElMessage.error(errorMessage(error, '固件升级指令下发失败'))
  } finally { deployingFirmware.value = false }
}

async function cancelLatestTask() {
  const task = firmwareDetail.value?.latestTask
  if (!task) return
  try {
    await ElMessageBox.confirm(
      '终止只会解除 WVP 平台任务锁，不会向设备发送取消指令。请确认设备已经停止升级后再继续。',
      '终止 OTA 任务', { type: 'warning' }
    )
  } catch { return }
  cancellingTask.value = true
  try {
    await cancelMqttDeviceFirmwareTask(currentDevice.value.id, task.requestId)
    ElMessage.success('平台 OTA 任务已终止')
    await reloadFirmwareDetail()
  } finally { cancellingTask.value = false }
}

function isActiveTask(status) {
  return ['CREATED', 'PUBLISHED', 'ACCEPTED', 'DOWNLOADING', 'VERIFYING', 'INSTALLING', 'REBOOTING'].includes(status)
}

function taskStatusText(status) {
  return ({
    CREATED: '待发布', PUBLISHED: '已下发', ACCEPTED: '设备已接收', DOWNLOADING: '下载中',
    VERIFYING: '校验中', INSTALLING: '安装中', REBOOTING: '重启中', SUCCESS: '升级成功',
    FAILED: '升级失败', CANCELLED: '已终止', TIMED_OUT: '已超时'
  })[status] || status || '-'
}

async function openPreview(device) {
  currentDevice.value = device
  try {
    const data = (await getMqttDeviceStreams(device.id))?.data || []
    streams.value = data
    if (!data.length) return ElMessage.warning('设备没有上报可用视频流')
    const preferred = data.find(item => item.defaultStream) || data[0]
    selectedStreamId.value = preferred.id
    previewVisible.value = true
    await startPreview(preferred.id)
  } catch (error) { ElMessage.error(errorMessage(error, '加载视频源失败')) }
}

async function startPreview(streamId) {
  releasePreview()
  if (!streamId || !currentDevice.value) return
  previewLoading.value = true
  try {
    const stream = (await createMqttDevicePreview(currentDevice.value.id, streamId))?.data || {}
    webrtcUrl.value = location.protocol === 'https:' ? stream.rtcs : stream.rtc
    if (!webrtcUrl.value) ElMessage.error('WVP 未返回 WebRTC 播放地址')
  } catch (error) { ElMessage.error(errorMessage(error, '创建预览失败')) }
  finally { previewLoading.value = false }
}

function releasePreview() { webrtcUrl.value = '' }

onMounted(() => { loadDevices(); loadMediaStatus() })
</script>

<style scoped>
.protocol-page { padding: 20px; }
.header, .filters, .stream-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.header-title { display: flex; align-items: center; white-space: nowrap; }
.title { margin-right: 12px; font-size: 18px; font-weight: 600; }
.filters .el-input { width: 240px; }
.filters .el-select { width: 120px; }
.service-alert { margin-bottom: 14px; }
.firmware-alert { margin-top: 14px; }
.pagination { display: flex; justify-content: center; padding-top: 20px; }
.stream-bar { justify-content: flex-start; margin-bottom: 12px; }
.stream-bar .el-select { width: 360px; }
.player { min-height: 480px; background: #000; display: flex; align-items: center; justify-content: center; }
.player :deep(#webRtcPlayerBox), .player :deep(#rtcPlayer) { width: 100%; max-height: 520px; }
h4 { margin: 18px 0 10px; }
@media (max-width: 1200px) { .header { align-items: flex-start; flex-direction: column; } }
</style>
