<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

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
              <span class="title">{{ $tp('VLStream 设备') }}</span>
              <el-tag :type="mediaAvailable ? 'success' : 'danger'" size="small">
                WVP ZLM {{ mediaAvailable ? $tp('可用') : $tp('不可用') }}
              </el-tag>
            </div>
            <div class="filters">
              <el-input v-model="query.keyword" clearable :placeholder="$tp('设备名称 / ID / 序列号')" @keyup.enter="search" />
              <el-select v-model="query.online" clearable :placeholder="$tp('全部状态')">
                <el-option :label="$tp('在线')" :value="true" />
                <el-option :label="$tp('离线')" :value="false" />
              </el-select>
              <el-button type="primary" @click="search">{{ $tp('搜索') }}</el-button>
              <el-button @click="reset">{{ $tp('重置') }}</el-button>
            </div>
          </div>
        </template>

        <el-alert v-if="serviceError" :title="serviceError" type="error" :closable="false" show-icon class="service-alert" />

        <el-table v-loading="loading" :data="devices" stripe scrollbar-always-on @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" fixed="left" />
          <el-table-column type="index" :label="$tp('序号')" width="56" fixed="left" />
          <el-table-column :label="$tp('在线状态')" width="100" fixed="left" align="center">
            <template #default="{ row }">
              <el-tag :type="row.online ? 'success' : 'info'" effect="light" class="online-status">
                <span class="status-dot" aria-hidden="true" />{{ row.online ? $tp('在线') : $tp('离线') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deviceName" :label="$tp('设备名称')" min-width="150" show-overflow-tooltip />
          <el-table-column prop="deviceId" :label="$tp('设备 ID')" min-width="190" show-overflow-tooltip />
          <el-table-column prop="deviceModel" :label="$tp('设备型号')" min-width="150" show-overflow-tooltip />
          <el-table-column v-if="hasDeviceSerial" prop="deviceSerial" :label="$tp('序列号')" min-width="140" show-overflow-tooltip />
          <el-table-column prop="ipAddr" label="IP" min-width="120" />
          <el-table-column prop="firmwareVersion" :label="$tp('RootFS 版本')" min-width="110" />
          <el-table-column :label="$tp('设备能力')" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ capabilityText(row.capabilitiesJson) }}</template>
          </el-table-column>
          <el-table-column :label="$tp('最近上线')" min-width="180">
            <template #default="{ row }">{{ formatDeviceTime(row.lastOnlineTime) }}</template>
          </el-table-column>
          <el-table-column :label="$tp('最后心跳')" min-width="180">
            <template #default="{ row }">{{ formatDeviceTime(row.lastHeartbeatTime) }}</template>
          </el-table-column>
          <el-table-column :label="$tp('操作')" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">{{ $tp('详情') }}</el-button>
              <el-button link type="primary" :disabled="row.online !== true" @click="openPreview(row)">{{ $tp('播放') }}</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" background
            layout="total, prev, pager, next, sizes" :total="total" @change="loadDevices" />
        </div>
      </el-card>

      <el-dialog v-model="previewVisible" class="locale-dialog locale-dialog--wide" :title="previewDialogTitle"
        destroy-on-close @closed="releasePreview">
        <div class="stream-bar">
          <span>{{ $tp('视频流') }}</span>
          <el-select v-model="selectedStreamId" :placeholder="$tp('请选择视频流')" @change="startPreview">
            <el-option v-for="stream in streams" :key="stream.id" :value="stream.id"
              :label="`${stream.channelId || stream.streamName || stream.id} (${stream.streamType || stream.protocol || '-'})`" />
          </el-select>
        </div>
        <div v-loading="previewLoading" class="player">
          <rtc-player v-if="webrtcUrl" :video-url="webrtcUrl" :hasaudio="true" />
          <camera-rtc-player v-else-if="cameraRtcConfig" :device-id="cameraRtcConfig.cameraId"
            :socket-url="cameraRtcConfig.socketUrl" />
          <el-empty v-else :description="$tp('请选择可用视频流')" />
        </div>
      </el-dialog>

      <el-dialog v-model="detailVisible" class="locale-dialog locale-dialog--wide" :title="$tp('设备详情')" destroy-on-close>
        <div v-loading="detailLoading">
        <el-descriptions v-if="detailDevice" :column="2" border>
          <el-descriptions-item :label="$tp('设备名称')">{{ detailDevice.deviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('设备 ID')">{{ detailDevice.deviceId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('设备型号')">{{ detailDevice.deviceModel || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('在线状态')">{{ detailDevice.online ? $tp('在线') : $tp('离线') }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('IP 地址')">{{ detailDevice.ipAddr || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('RootFS 版本')">
            <div class="firmware-version-actions">
              <span>{{ detailDevice.firmwareVersion || '-' }}</span>
              <el-button v-if="firmwareDetail?.availableUpgrades?.length > 0" type="primary" link @click="firmwareVisible = true">{{ $tp('固件升级') }}</el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item :label="$tp('开机时间')">
            {{ deviceBootTimeText(detailDevice) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$tp('在线时长')">{{ deviceOnlineDurationText(detailDevice, detailClock) }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('最后心跳')" :span="2">{{ formatDeviceTime(detailDevice.lastHeartbeatTime) }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('设备能力')" :span="2">{{ capabilityText(detailDevice.capabilitiesJson) }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('位置坐标')" :span="2">{{ deviceLocationText(detailDevice) }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('设备管理后台')" :span="2">
            <div class="remote-management-entry">
              <template v-if="remoteManagementVisible">
                <el-tag v-if="!remoteManagementStatus?.featureEnabled" type="info">{{ $tp('功能未启用') }}</el-tag>
                <el-tag v-else-if="remoteManagementStatus?.configured"
                  :type="remoteManagementReady ? 'success' : 'warning'">
                  {{ remoteManagementReady ? $tp('远程管理可用') : remoteManagementBlockReason }}
                </el-tag>
                <el-tag v-else type="info">{{ $tp('尚未激活') }}</el-tag>
              </template>
              <el-button v-if="remoteManagementStatus?.featureEnabled" type="primary"
                :loading="remoteManagementOpening" :disabled="!remoteManagementReady"
                @click="openRemoteManagement"> {{ $tp('打开管理后台') }} </el-button>
              <el-button v-if="remoteManagementStatus?.featureEnabled" :loading="remoteManagementActionBusy"
                @click="issueRemoteManagementEnrollment">
                {{ remoteManagementStatus?.configured ? $tp('重新生成激活码') : $tp('生成激活码') }}
              </el-button>
              <el-button v-if="remoteManagementStatus?.configured && remoteManagementStatus?.desiredState !== 'REVOKED'"
                :loading="remoteManagementActionBusy"
                @click="changeRemoteManagementState(remoteManagementStatus?.desiredState === 'DISABLED' ? 'ENABLED' : 'DISABLED')">
                {{ remoteManagementStatus?.desiredState === 'DISABLED' ? $tp('启用') : $tp('停用') }}
              </el-button>
              <el-button v-if="remoteManagementStatus?.configured && remoteManagementStatus?.desiredState !== 'REVOKED'"
                type="danger" plain :loading="remoteManagementActionBusy"
                @click="changeRemoteManagementState('REVOKED')"> {{ $tp('吊销') }} </el-button>
              <span class="snapshot-note">{{ $tp('通过短期安全会话访问，登录仍使用 IPC 自身账号。') }}</span>
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="model-heading">
          <h4>{{ $tp('设备运行模型') }}</h4>
          <div>
            <el-button size="small" type="primary" :disabled="Boolean(modelBusy)" @click="openModelDeploy">{{ detailDevice?.online ? $tp('模型下发') : $tp('查看模型') }}</el-button>
            <el-button size="small" :loading="modelBusy === 'query'" :disabled="!detailDevice?.online || Boolean(modelBusy)" @click="refreshModels">{{ $tp('刷新') }}</el-button>
          </div>
        </div>
        <el-alert v-if="modelError" :title="modelError" type="warning" :closable="false" show-icon />
        <el-table :data="reportedModels" border scrollbar-always-on :empty-text="liveModels !== null ? $tp('设备当前无模型') : detailDevice?.modelsJson == null ? $tp('设备尚未上报模型信息') : $tp('设备上报的模型列表为空')">
          <el-table-column prop="modelId" :label="$tp('模型 ID')" min-width="140" show-overflow-tooltip />
          <el-table-column prop="modelName" :label="$tp('模型名称')" min-width="140" show-overflow-tooltip />
          <el-table-column prop="format" :label="$tp('格式')" width="90" />
          <el-table-column :label="$tp('状态')" width="100"><template #default="{ row }">{{ modelStatusText(row.status) }}</template></el-table-column>
          <el-table-column :label="$tp('操作')" width="85" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" :loading="modelBusy === `delete:${row.modelId}`"
                :disabled="!detailDevice?.online || !row.modelId || Boolean(modelBusy)" @click="removeModel(row)">{{ $tp('删除') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p class="snapshot-note">{{ liveModels === null ? $tp('当前显示设备最近一次上报的模型，点击刷新查询设备。') : $tp('当前显示本次设备查询结果。') }}{{ $tp('离线时仅供查看。') }}</p>

        <h4>{{ $tp('视频源') }}</h4>
        <el-table :data="detailStreams" border scrollbar-always-on :empty-text="$tp('设备没有上报视频源')">
          <el-table-column prop="channelId" :label="$tp('通道')" min-width="120" />
          <el-table-column prop="streamType" :label="$tp('码流类型')" width="110" />
          <el-table-column prop="protocol" :label="$tp('协议')" width="90" />
          <el-table-column :label="$tp('默认流')" width="90"><template #default="{ row }">{{ row.defaultStream ? $tp('是') : $tp('否') }}</template></el-table-column>
          <el-table-column :label="$tp('视频源地址')" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.sourceUrl || row.url || $tp('未上报') }}</template>
          </el-table-column>
        </el-table>
        </div>
      </el-dialog>
      <DeviceModelDrawer v-model="modelDeployVisible" :device="detailDevice"
        @busy="modelBusy = $event ? 'deploy' : ''" />
      <el-dialog v-model="enrollmentVisible" class="locale-dialog" :title="$tp('IPC Agent 激活信息')"
        destroy-on-close>
        <el-alert :title="$tp('激活码只在本次显示。请通过安全渠道交给设备端，设备注册成功后立即失效。')"
          type="warning" :closable="false" show-icon />
        <el-descriptions v-if="enrollmentView" :column="1" border class="enrollment-details">
          <el-descriptions-item :label="$tp('设备 ID')">{{ enrollmentView.deviceId }}</el-descriptions-item>
          <el-descriptions-item :label="$tp('一次性激活码')">
            <el-input :model-value="enrollmentView.enrollmentCode" readonly />
          </el-descriptions-item>
          <el-descriptions-item :label="$tp('过期时间')">{{ formatDeviceTime(enrollmentView.expiresAt) }}</el-descriptions-item>
        </el-descriptions>
      </el-dialog>
      <el-dialog v-model="firmwareVisible" class="locale-dialog locale-dialog--wide" :title="$tp('固件升级')" destroy-on-close>
        <el-alert v-if="firmwareDetail?.upgradeBlockedReason" :title="firmwareDetail.upgradeBlockedReason"
          type="info" :closable="false" show-icon class="firmware-alert" />

        <h4>{{ $tp('可用固件升级') }}</h4>
        <el-table :data="firmwareDetail?.availableUpgrades || []" border scrollbar-always-on :empty-text="$tp('没有更高版本的兼容固件')">
          <el-table-column :label="$tp('目标')" width="90"><template #default>RootFS</template></el-table-column>
          <el-table-column prop="currentVersion" :label="$tp('当前版本')" width="120" />
          <el-table-column prop="latestVersion" :label="$tp('最新版本')" width="120" />
          <el-table-column prop="fileName" :label="$tp('固件包')" min-width="220" show-overflow-tooltip />
          <el-table-column :label="$tp('操作')" width="110" align="center">
            <template #default="{ row }">
              <el-button type="primary" size="small" :loading="deployingFirmware"
                :disabled="!firmwareDetail?.canUpgrade" @click="deployFirmware(row)">{{ $tp('固件升级') }}</el-button>
            </template>
          </el-table-column>
        </el-table>

        <template v-if="firmwareDetail?.latestTask">
          <h4>{{ $tp('最近 OTA 任务') }}</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="$tp('目标')">RootFS</el-descriptions-item>
            <el-descriptions-item :label="$tp('状态')">
              {{ taskStatusText(firmwareDetail.latestTask.deployStatus) }}
              <el-button v-if="isActiveTask(firmwareDetail.latestTask.deployStatus)" link type="danger"
                :loading="cancellingTask" @click="cancelLatestTask">{{ $tp('终止任务') }}</el-button>
            </el-descriptions-item>
            <el-descriptions-item :label="$tp('版本')">{{ firmwareDetail.latestTask.currentVersion }} → {{ firmwareDetail.latestTask.targetVersion }}</el-descriptions-item>
            <el-descriptions-item :label="$tp('任务 ID')">{{ firmwareDetail.latestTask.requestId }}</el-descriptions-item>
            <el-descriptions-item v-if="firmwareDetail.latestTask.failureReason" :label="$tp('说明')" :span="2">
              {{ firmwareDetail.latestTask.failureReason }}
            </el-descriptions-item>
          </el-descriptions>
        </template>

      </el-dialog>
    </div>
  </DeviceClassificationLayout>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { monitorDevicePreview } from '@/utils/devicePreviewGuard'
import { ElMessage, ElMessageBox } from 'element-plus'
import CameraRtcPlayer from '@/components/CameraRtcPlayer.vue'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import { capabilityText, deviceBootTimeText, deviceOnlineDurationText, deviceLocationText, formatDeviceTime, modelStatusText, parseSnapshot } from '@/utils/deviceStateDisplay'
import { parseCameraRtcConfig } from '@/utils/oplayer'
import { queryDeviceModels, deleteDeviceModel } from '@/api/deviceModels'
import {
  changeIpcTunnelDesiredState,
  createIpcTunnelAccessSession,
  getIpcTunnelStatus,
  issueIpcTunnelEnrollment
} from '@/api/ipcTunnel'
import DeviceModelDrawer from './components/DeviceModelDrawer.vue'
import { translatePhrase } from '@/i18n'
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
const hasDeviceSerial = computed(() => devices.value.some(device => String(device.deviceSerial ?? '').trim().length > 0))
const total = ref(0)
const mediaAvailable = ref(false)
const serviceError = ref('')
const classificationDeviceKeys = ref([])
const previewVisible = ref(false)
const previewLoading = ref(false)
const currentDevice = ref(null)
const previewDialogTitle = computed(() => {
  const name = currentDevice.value?.deviceName || currentDevice.value?.deviceId || ''
  const title = translatePhrase('实时预览')
  return name ? `${name} - ${title}` : title
})
const streams = ref([])
const selectedStreamId = ref(null)
const webrtcUrl = ref('')
const cameraRtcConfig = ref(null)
const firmwareVisible = ref(false)
const detailVisible = ref(false)
const detailClock = ref(Date.now())
let detailClockTimer
watch(detailVisible, (visible) => {
  clearInterval(detailClockTimer)
  if (visible) {
    detailClock.value = Date.now()
    detailClockTimer = setInterval(() => { detailClock.value = Date.now() }, 1000)
  }
})
onBeforeUnmount(() => clearInterval(detailClockTimer))
const detailLoading = ref(false)
const detailStreams = ref([])
const firmwareDetail = ref(null)
const deployingFirmware = ref(false)
const cancellingTask = ref(false)
const remoteManagementOpening = ref(false)
const remoteManagementActionBusy = ref(false)
const remoteManagementVisible = ref(false)
const remoteManagementStatus = ref(null)
const enrollmentVisible = ref(false)
const enrollmentView = ref(null)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', online: undefined })
const detailDevice = computed(() => firmwareDetail.value?.device || currentDevice.value)

const liveModels = ref(null)
const modelBusy = ref('')
const modelError = ref('')
const modelDeployVisible = ref(false)
let modelSession = 0
const reportedModels = computed(() => liveModels.value ?? parseSnapshot(detailDevice.value?.modelsJson) ?? [])
const remoteManagementReady = computed(() => {
  const status = remoteManagementStatus.value
  return Boolean(status?.configured
    && status.featureEnabled
    && status.desiredState === 'ENABLED'
    && status.agentOnline
    && status.tunnelStatus === 'ONLINE'
    && status.localWebStatus === 'AVAILABLE'
    && status.routeStatus === 'APPLIED')
})
const remoteManagementBlockReason = computed(() => {
  const status = remoteManagementStatus.value
  if (!status?.featureEnabled) return uiText('功能未启用')
  if (!status?.configured) return uiText('尚未激活')
  if (status.desiredState === 'REVOKED') return uiText('已吊销')
  if (status.desiredState === 'DISABLED') return uiText('已停用')
  if (!status.agentOnline) return uiText('Agent 离线')
  if (status.routeStatus !== 'APPLIED') return status.routeStatus === 'FAILED' ? uiText('路由应用失败') : uiText('路由待生效')
  if (status.tunnelStatus !== 'ONLINE') return uiText('隧道未连接')
  if (status.localWebStatus !== 'AVAILABLE') return uiText('IPC 后台不可用')
  return uiText('暂不可用')
})

watch(detailVisible, visible => {
  if (!visible) { modelSession++; modelDeployVisible.value = false }
})

async function refreshModels() {
  if (modelBusy.value || !detailDevice.value?.online) return
  const session = modelSession
  const deviceId = detailDevice.value.deviceId
  modelBusy.value = 'query'
  modelError.value = ''
  try {
    const result = await queryDeviceModels(deviceId)
    if (session !== modelSession) return
    if (!Array.isArray(result?.data)) throw new Error('设备未返回有效模型列表')
    liveModels.value = result.data
  } catch (error) {
    if (session === modelSession) modelError.value = modelErrorMessage(error, '查询设备模型失败')
  } finally { if (session === modelSession) modelBusy.value = '' }
}

async function removeModel(model) {
  const session = modelSession
  const deviceId = detailDevice.value?.deviceId
  if (modelBusy.value || !detailDevice.value?.online || !model.modelId) return
  try {
    await ElMessageBox.confirm(uiText('确认从当前设备删除“{value0}”？运行中的模型将先停止再卸载，平台模型库文件保留。', { value0: model.modelName || model.modelId }), uiText('删除设备模型'), { type: 'warning' })
  } catch { return }
  if (session !== modelSession || modelBusy.value) return
  modelBusy.value = `delete:${model.modelId}`
  modelError.value = ''
  try {
    await deleteDeviceModel(deviceId, model.modelId)
    if (session !== modelSession) return
    liveModels.value = reportedModels.value.filter(item => item.modelId !== model.modelId)
    ElMessage.success(uiText('设备已确认模型删除成功'))
  } catch (error) {
    if (session === modelSession) modelError.value = modelErrorMessage(error, '删除结果未确认，请刷新列表核实')
  } finally { if (session === modelSession) modelBusy.value = '' }
}

function openModelDeploy() {
  if (modelBusy.value || !detailDevice.value) return
  modelDeployVisible.value = true
}

function errorMessage(error, fallback) { return error?.response?.data?.msg || error?.message || fallback }
function modelErrorMessage(error, fallback) {
  return error?.response?.status === 404
    ? uiText('当前后端尚未提供设备模型管理接口，请更新并重启 VLS 后端后重试')
    : errorMessage(error, fallback)
}

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
  const session = ++modelSession
  liveModels.value = null
  modelError.value = ''
  modelBusy.value = ''
  modelDeployVisible.value = false
  currentDevice.value = device
  remoteManagementVisible.value = false
  remoteManagementStatus.value = null
  firmwareDetail.value = null
  detailStreams.value = []
  firmwareVisible.value = false
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [streamsResult, detailResult] = await Promise.all([
      getMqttDeviceStreams(device.id),
      getMqttDeviceDetail(device.id)
    ])
    if (session !== modelSession) return
    detailStreams.value = streamsResult?.data || []
    firmwareDetail.value = detailResult?.data || null
    await loadRemoteManagementStatus(device.deviceId)
  }
  catch (error) {
    if (session !== modelSession) return
    detailStreams.value = []
    firmwareDetail.value = null
    ElMessage.error(errorMessage(error, uiText('加载设备详情失败')))
  }
  finally { if (session === modelSession) detailLoading.value = false }
}

async function loadRemoteManagementStatus(deviceId = detailDevice.value?.deviceId) {
  if (!deviceId) return
  try {
    const result = await getIpcTunnelStatus(deviceId)
    remoteManagementStatus.value = result?.data || { configured: false, deviceId }
    remoteManagementVisible.value = true
  } catch (error) {
    remoteManagementStatus.value = null
    remoteManagementVisible.value = false
  }
}

async function issueRemoteManagementEnrollment() {
  const deviceId = detailDevice.value?.deviceId
  if (!deviceId || remoteManagementActionBusy.value) return
  if (remoteManagementStatus.value?.configured) {
    try {
      await ElMessageBox.confirm(
        uiText('重新生成激活码会立即撤销当前 Agent Token 和未过期的浏览器会话。确认继续？'),
        uiText('重新激活远程管理'), { type: 'warning' }
      )
    } catch { return }
  }
  remoteManagementActionBusy.value = true
  try {
    const result = await issueIpcTunnelEnrollment(deviceId)
    enrollmentView.value = result?.data || null
    enrollmentVisible.value = Boolean(enrollmentView.value?.enrollmentCode)
    await loadRemoteManagementStatus(deviceId)
  } catch (error) {
    if (!error?.response) ElMessage.error(errorMessage(error, uiText('生成Agent激活码失败')))
  } finally {
    remoteManagementActionBusy.value = false
  }
}

async function changeRemoteManagementState(state) {
  const deviceId = detailDevice.value?.deviceId
  if (!deviceId || remoteManagementActionBusy.value) return
  if (state === 'REVOKED') {
    try {
      await ElMessageBox.confirm(
        uiText('吊销后当前 Agent Token、隧道和浏览器会话都将失效，恢复时必须重新激活。确认继续？'),
        uiText('吊销远程管理'), { type: 'warning' }
      )
    } catch { return }
  }
  remoteManagementActionBusy.value = true
  try {
    await changeIpcTunnelDesiredState(deviceId, state)
    await loadRemoteManagementStatus(deviceId)
    ElMessage.success(state === 'ENABLED' ? uiText('远程管理已启用') : state === 'DISABLED' ? uiText('远程管理已停用') : uiText('远程管理已吊销'))
  } finally {
    remoteManagementActionBusy.value = false
  }
}

async function reloadFirmwareDetail() {
  if (!currentDevice.value) return
  const session = modelSession
  const result = await getMqttDeviceDetail(currentDevice.value.id)
  if (session === modelSession) firmwareDetail.value = result?.data || null
}

async function openRemoteManagement() {
  const deviceId = detailDevice.value?.deviceId
  if (!deviceId || remoteManagementOpening.value) return
  const targetWindow = window.open('about:blank', '_blank')
  if (!targetWindow) {
    ElMessage.warning(uiText('浏览器已拦截新窗口，请允许本站打开弹窗后重试'))
    return
  }
  targetWindow.opener = null
  targetWindow.document.title = '正在建立安全连接'
  targetWindow.document.body.textContent = '正在建立 IPC 远程管理安全连接…'
  remoteManagementOpening.value = true
  try {
    const result = await createIpcTunnelAccessSession(deviceId)
    const accessUrl = result?.data?.accessUrl
    if (!accessUrl) throw new Error('平台未返回远程管理访问地址')
    targetWindow.location.replace(accessUrl)
  } catch (error) {
    targetWindow.close()
    if (!error?.response) ElMessage.error(errorMessage(error, uiText('创建远程管理会话失败')))
  } finally {
    remoteManagementOpening.value = false
  }
}

async function deployFirmware(candidate) {
  try {
    await ElMessageBox.confirm(
      uiText('确认将设备升级到 RootFS {value0}？升级过程中设备将重启。', { value0: candidate.latestVersion }),
      uiText('固件升级确认'), { type: 'warning' }
    )
  } catch { return }
  deployingFirmware.value = true
  try {
    await deployMqttDeviceFirmware(currentDevice.value.id, candidate.firmwareId)
    ElMessage.success(uiText('OTA 固件升级指令已下发'))
    await reloadFirmwareDetail()
  } catch (error) {
    ElMessage.error(errorMessage(error, uiText('固件升级指令下发失败')))
  } finally { deployingFirmware.value = false }
}

async function cancelLatestTask() {
  const task = firmwareDetail.value?.latestTask
  if (!task) return
  try {
    await ElMessageBox.confirm(
      uiText('终止只会解除 WVP 平台任务锁，不会向设备发送取消指令。请确认设备已经停止升级后再继续。'),
      uiText('终止 OTA 任务'), { type: 'warning' }
    )
  } catch { return }
  cancellingTask.value = true
  try {
    await cancelMqttDeviceFirmwareTask(currentDevice.value.id, task.requestId)
    ElMessage.success(uiText('平台 OTA 任务已终止'))
    await reloadFirmwareDetail()
  } finally { cancellingTask.value = false }
}

function isActiveTask(status) {
  return ['CREATED', 'PUBLISHED', 'ACCEPTED', 'DOWNLOADING', 'VERIFYING', 'INSTALLING', 'REBOOTING'].includes(status)
}

function taskStatusText(status) {
  return ({
    CREATED: uiText('待发布'), PUBLISHED: uiText('已下发'), ACCEPTED: uiText('设备已接收'), DOWNLOADING: uiText('下载中'),
    VERIFYING: uiText('校验中'), INSTALLING: uiText('安装中'), REBOOTING: uiText('重启中'), SUCCESS: uiText('升级成功'),
    FAILED: uiText('升级失败'), CANCELLED: uiText('已终止'), TIMED_OUT: uiText('已超时')
  })[status] || status || '-'
}

async function openPreview(device) {
  if (device.online !== true) return ElMessage.warning(uiText('设备离线，不允许播放'))
  currentDevice.value = device
  try {
    const data = (await getMqttDeviceStreams(device.id))?.data || []
    streams.value = data
    if (!data.length) return ElMessage.warning(uiText('设备没有上报可用视频流'))
    const preferred = data.find(item => item.defaultStream) || data[0]
    selectedStreamId.value = preferred.id
    previewVisible.value = true
    await startPreview(preferred.id)
  } catch (error) { ElMessage.error(errorMessage(error, uiText('加载视频源失败'))) }
}

let previewGeneration = 0
let stopPreviewMonitor = () => {}
watch(previewVisible, visible => { if (!visible) releasePreview() })

async function startPreview(streamId) {
  releasePreview()
  if (!streamId || !currentDevice.value) return
  const generation = previewGeneration
  const id = currentDevice.value.id
  previewLoading.value = true
  try {
    const device = (await getMqttDeviceDetail(id))?.data?.device
    if (generation !== previewGeneration || !previewVisible.value) return
    if (device?.online !== true) {
      currentDevice.value.online = false
      previewVisible.value = false
      return ElMessage.warning(uiText('设备离线，不允许播放'))
    }
    stopPreviewMonitor = monitorDevicePreview(async () => (await getMqttDeviceDetail(id))?.data?.device, () => {
      releasePreview()
      previewVisible.value = false
      ElMessage.warning(uiText('设备离线或状态无法确认，已停止播放'))
    })
    const stream = (await createMqttDevicePreview(currentDevice.value.id, streamId))?.data || {}
    if (generation !== previewGeneration || !previewVisible.value) return
    if (stream.playMode === 'cameraRTC' && stream.url) {
      cameraRtcConfig.value = parseCameraRtcConfig(stream.url)
      return
    }
    webrtcUrl.value = location.protocol === 'https:' ? (stream.rtcs || stream.webrtcUrl) : (stream.rtc || stream.webrtcUrl)
    if (!webrtcUrl.value) ElMessage.error(uiText('WVP 未返回可用播放地址'))
  } catch (error) { ElMessage.error(errorMessage(error, uiText('创建预览失败'))) }
  finally { previewLoading.value = false }
}

function releasePreview() {
  previewGeneration += 1
  stopPreviewMonitor()
  previewLoading.value = false
  cameraRtcConfig.value = null
  webrtcUrl.value = ''
}

onMounted(() => { loadDevices(); loadMediaStatus() })
onBeforeUnmount(releasePreview)
</script>

<style scoped>
.protocol-page { padding: 20px; }
.header, .filters, .stream-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.header-title { display: flex; align-items: center; white-space: nowrap; }
.title { margin-right: 12px; font-size: 18px; font-weight: 600; }
.filters .el-input { width: 240px; }
.filters .el-select { width: 120px; }
.service-alert { margin-bottom: 14px; }
.online-status { font-weight: 600; }
.status-dot { display: inline-block; width: 7px; height: 7px; margin-right: 6px; border-radius: 50%; background: currentColor; }
.firmware-alert { margin-top: 14px; }
.pagination { display: flex; justify-content: center; padding-top: 20px; }
.stream-bar { justify-content: flex-start; margin-bottom: 12px; }
.stream-bar .el-select { width: 360px; }
.player { min-height: 480px; background: #000; display: flex; align-items: center; justify-content: center; }
.player :deep(#webRtcPlayerBox), .player :deep(#rtcPlayer) { width: 100%; max-height: 520px; }
.camera-rtc-player { width: 100%; height: 480px; }
.model-heading { display: flex; align-items: center; justify-content: space-between; margin-top: 18px; }
.firmware-version-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.remote-management-entry { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.enrollment-details { margin-top: 16px; }
.snapshot-note { color: #909399; font-size: 12px; }
h4 { margin: 18px 0 10px; }
@media (max-width: 1200px) { .header { align-items: flex-start; flex-direction: column; } }
</style>
