<template>
  <DeviceClassificationLayout
    protocol-type="VLSTREAM"
    :selected-device-keys="classificationDeviceKeys"
    @filter-change="handleClassificationFilter"
    @assigned="loadDevices"
  >
    <div class="device-table-panel">
      <div class="depNameBox_out flexRowAC">
        <div class="depNameBox flexRowAC">
          <el-tag :type="mediaAvailable ? 'success' : 'danger'">
            ZLM {{ mediaAvailable ? '可用' : '不可用' }}
          </el-tag>
        </div>
        <div class="searchHeight_out flexRowAC">
          <search-height-box
            keyword="keyword"
            placeholder="设备名称 / ID / 序列号"
            :data="searchData"
            @handle="searchResetFn"
          />
          <export-excel-pdf />
        </div>
      </div>

      <table-self
        class="new_table"
        header-cell-class-name="header_tenant_cell"
        stripe
        v-loading="loading"
        :data="devices"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" :width="clacPXToVW(55)" />
        <el-table-column label="序号" :width="clacPXToVW(65)">
          <template #default="scope">
            {{ scope.$index + (query.current - 1) * query.size + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备 ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deviceSerial" label="序列号" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" :width="clacPXToVW(90)" align="center">
          <template #default="{ row }">
            <span v-if="row.online" class="staBtns WX">在线</span>
            <span v-else class="staBtns">离线</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddr" label="IP" min-width="120" />
        <el-table-column prop="firmwareVersion" label="固件版本" min-width="110" />
        <el-table-column prop="lastHeartbeatTime" label="最后心跳" min-width="170" />
        <el-table-column label="操作" align="right" fixed="right" :width="clacPXToVW(180)">
          <template #default="{ row }">
            <div class="operateAppBox flexRowAC" @click.stop>
              <div class="new_table_svg_group" @click="openDetail(row)">
                <oort-svg-icon width="14" height="14" name="detail_icon" class="new_table_svg_group_svg" />
                <span>详情</span>
              </div>
              <div
                class="new_table_svg_group"
                :class="{ 'is-disabled': !row.online }"
                @click="row.online && openPreview(row)"
              >
                <oort-svg-icon width="14" height="14" name="play" class="new_table_svg_group_svg" />
                <span>预览</span>
              </div>
            </div>
          </template>
        </el-table-column>
      </table-self>

      <div class="paginationBox flexRowAC">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          background
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          class="justifyAlign"
          @change="loadDevices"
        />
      </div>
    </div>
  </DeviceClassificationLayout>

  <el-dialog
    v-model="previewVisible"
    :title="`${currentDevice?.deviceName || currentDevice?.deviceId || ''} 实时预览`"
    width="50%"
    destroy-on-close
    @closed="releasePreview"
  >
    <div class="stream-bar">
      <span>视频流</span>
      <el-select v-model="selectedStreamId" placeholder="请选择视频流" @change="startPreview">
        <el-option
          v-for="stream in streams"
          :key="stream.id"
          :value="stream.id"
          :label="`${stream.streamName || stream.channelId} (${stream.streamType})`"
        />
      </el-select>
    </div>
    <div v-loading="previewLoading" class="player">
      <rtc-player v-if="webrtcUrl" :video-url="webrtcUrl" />
      <el-empty v-else description="请选择可用视频流" />
    </div>
  </el-dialog>

  <el-dialog v-model="detailVisible" title="设备详情" width="45%" destroy-on-close>
    <div v-loading="detailLoading">
      <el-descriptions v-if="detail?.device" :column="2" border>
        <el-descriptions-item label="设备名称">{{ detail.device.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备 ID">{{ detail.device.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ detail.device.deviceModel || '尚未上报' }}</el-descriptions-item>
        <el-descriptions-item label="在线状态">{{ detail.device.online ? '在线' : '离线' }}</el-descriptions-item>
        <el-descriptions-item label="Application 版本">{{ detail.device.applicationVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="RootFS 版本">{{ detail.device.rootfsVersion || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="detail?.upgradeBlockedReason"
        :title="detail.upgradeBlockedReason"
        type="info"
        :closable="false"
        show-icon
        class="upgrade-alert"
      />

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
            <el-button
              type="primary"
              :disabled="!detail?.canUpgrade"
              :loading="deployingFirmwareId === row.firmwareId"
              @click="deployFirmware(row)"
            >固件升级</el-button>
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
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import { clacPXToVW } from '@/utils/wvpCompat'
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
const classificationDeviceKeys = ref([])
const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  online: null,
  categoryType: undefined,
  categoryId: undefined,
  unclassified: undefined
})

const searchData = ref([
  { label: '设备名称/ID/序列号', value: 'keyword', type: 'text', default: '' },
  { label: '状态', value: 'online', type: 'select', option: [{ label: '在线', value: true }, { label: '离线', value: false }], default: '' }
])

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

function handleClassificationFilter(filter) {
  Object.assign(query, filter, { current: 1 })
  loadDevices()
}

function searchResetFn(val) {
  query.current = 1
  query.keyword = val?.keyword || ''
  query.online = val?.online === '' || val?.online === undefined || val?.online === null ? null : val.online
  loadDevices()
}

function handleSelectionChange(selection) {
  classificationDeviceKeys.value = selection.map(item => String(item.id))
}

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
.stream-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.stream-bar .el-select { width: 360px; }
.player { min-height: 480px; background: #000; display: flex; align-items: center; justify-content: center; }
.player :deep(#webRtcPlayerBox) { max-height: 520px; }
.upgrade-alert { margin: 16px 0; }
h4 { margin: 18px 0 10px; }
.staBtns {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
}
.staBtns.WX {
  color: #67c23a;
  background: #f0f9eb;
}
.operateAppBox {
  justify-content: flex-end;
  gap: 2px;
}
</style>
