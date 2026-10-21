<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <DeviceClassificationLayout protocol-type="CUSTOM" :selected-device-keys="classificationDeviceKeys"
    @filter-change="handleClassificationFilter" @assigned="loadDevices">
    <div class="protocol-page">
      <el-card shadow="never">
        <template #header>
          <div class="custom-device-header">
            <div class="custom-device-header-title">
              <span class="title">{{ $tp('自定义协议设备') }}</span>
              <el-tag :type="mediaTagType" size="small" :title="mediaError">WVP ZLM {{ $tp(mediaLabel) }}</el-tag>
            </div>
            <div class="custom-device-filters">
              <el-input v-model="query.deviceName" clearable :placeholder="$tp('设备名称')" @keyup.enter="search" />
              <el-input v-model="query.deviceCode" clearable :placeholder="$tp('设备 ID')" @keyup.enter="search" />
              <el-select v-model="query.status" clearable :placeholder="$tp('全部状态')">
                <el-option :label="$tp('在线')" value="ONLINE" /><el-option :label="$tp('离线')" value="OFFLINE" /><el-option :label="$tp('未知')" value="UNKNOWN" />
              </el-select>
              <el-button type="primary" @click="search">{{ $tp('搜索') }}</el-button><el-button @click="resetQuery">{{ $tp('重置') }}</el-button>
            </div>
          </div>
        </template>

        <div class="custom-device-actions">
          <el-button type="primary" @click="openAdd">{{ $tp('新增') }}</el-button>
          <el-button type="success" :disabled="selectedIds.length !== 1" @click="openEdit()">{{ $tp('修改') }}</el-button>
          <el-button type="danger" :disabled="selectedIds.length === 0" @click="removeDevices()">{{ $tp('删除') }}</el-button>
          <el-button type="warning" @click="exportDevices">{{ $tp('导出') }}</el-button>
        </div>

        <el-table v-loading="loading" :data="devices" stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" />
          <el-table-column type="index" :label="$tp('序号')" width="50" />
          <el-table-column prop="deviceName" :label="$tp('设备名称')" min-width="120" show-overflow-tooltip />
          <el-table-column :label="$tp('源状态')" width="110"><template #default="{ row }"><CustomAvailabilityBadge :device="row" :now="statusClock" show-reason /></template></el-table-column>
          <el-table-column prop="deviceCode" :label="$tp('设备 ID')" min-width="150" show-overflow-tooltip />
          <el-table-column prop="deviceType" :label="$tp('设备类型')" width="110" />
          <el-table-column :label="$tp('区域')" min-width="100"><template #default="{ row }">{{ row.regionName || $tp('未设置') }}</template></el-table-column>
          <el-table-column :label="$tp('分组')" min-width="100"><template #default="{ row }">{{ row.groupName || $tp('未设置') }}</template></el-table-column>
          <el-table-column :label="$tp('标签')" min-width="150">
            <template #default="{ row }">
              <template v-if="row.tagNames"><el-tag v-for="tag in row.tagNames.split(',')" :key="tag" size="small" class="tag-item">{{ tag }}</el-tag></template>
              <span v-else>{{ $tp('未设置') }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="streamUrl" :label="$tp('视频流路径')" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createTime" :label="$tp('创建时间')" width="165" />
          <el-table-column :label="$tp('操作')" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="preview(row)">{{ $tp('预览') }}</el-button>
              <el-button link type="primary" @click="openRecord(row)">{{ $tp('录像') }}</el-button>
              <el-button link type="primary" @click="openEdit(row)">{{ $tp('编辑') }}</el-button>
              <el-button link type="danger" @click="removeDevices(row)">{{ $tp('删除') }}</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" background
            layout="total, prev, pager, next, sizes" :total="total" @change="loadDevices" />
        </div>
      </el-card>

      <el-dialog v-model="formVisible" :title="form.id ? $tp('修改设备') : $tp('新增设备')" width="680px" destroy-on-close>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item :label="$tp('设备名称')" prop="deviceName"><el-input v-model="form.deviceName" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item :label="$tp('设备 ID')" prop="deviceCode"><el-input v-model="form.deviceCode" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item :label="$tp('设备类型')" prop="deviceType"><el-select v-model="form.deviceType" style="width:100%"><el-option v-for="type in deviceTypes" :key="type" :label="type" :value="type" /></el-select></el-form-item></el-col>
            <el-col :span="12"><el-form-item :label="$tp('配置状态')" prop="status"><el-select v-model="form.status" style="width:100%"><el-option :label="$tp('在线')" value="ONLINE" /><el-option :label="$tp('离线')" value="OFFLINE" /><el-option :label="$tp('未知')" value="UNKNOWN" /></el-select></el-form-item></el-col>
            <el-col :span="24"><el-form-item :label="$tp('视频流路径')" prop="streamUrl"><el-input v-model="form.streamUrl" :placeholder="$tp('rtsp://、rtmp:// 或 http(s)://')" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item :label="$tp('经度')" prop="longitude"><el-input-number v-model="form.longitude" :precision="7" :min="-180" :max="180" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item :label="$tp('纬度')" prop="latitude"><el-input-number v-model="form.latitude" :precision="7" :min="-90" :max="90" style="width:100%" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item :label="$tp('地址')" prop="address"><el-input v-model="form.address" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item :label="$tp('备注')" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item></el-col>
          </el-row>
        </el-form>
        <template #footer><el-button @click="formVisible=false">{{ $tp('取消') }}</el-button><el-button type="primary" :loading="saving" @click="saveDevice">{{ $tp('保存') }}</el-button></template>
      </el-dialog>

      <el-dialog v-model="previewVisible" :title="previewTitle" width="900px" destroy-on-close>
        <div class="player">
          <SquareProtocolDevicePlayer v-if="previewVisible" :key="previewDevice.id"
            :source="previewSource" :device-name="previewDevice.deviceName" />
        </div>
      </el-dialog>

      <el-dialog v-model="recordVisible" :title="$tp('{value0} - 录像管理', { value0: recordDevice.deviceName || '' })" width="600px" destroy-on-close>
        <el-alert :title="$tp('录像及计划由 WVP 的 ZLMediaKit 执行；跨午夜计划按开始日归属。')" type="info" :closable="false" class="record-alert" />
        <el-form :model="recordPlan" label-width="100px">
          <el-form-item :label="$tp('当前状态')">
            <el-tag :type="recording ? 'danger' : 'info'">{{ recording ? $tp('录像中') : $tp('未录像') }}</el-tag>
            <el-button v-if="!recording" link type="primary" :disabled="!mediaAvailable" @click="startRecord">{{ $tp('立即开始') }}</el-button>
            <el-button v-else link type="danger" @click="stopRecord">{{ $tp('立即停止') }}</el-button>
          </el-form-item>
          <el-form-item :label="$tp('启用计划')"><el-switch v-model="recordPlan.enabled" /></el-form-item>
          <el-form-item :label="$tp('录像星期')"><el-checkbox-group v-model="selectedDays"><el-checkbox v-for="day in weekOptions" :key="day.value" :label="day.value">{{ day.label }}</el-checkbox></el-checkbox-group></el-form-item>
          <el-form-item :label="$tp('录像时段')"><el-time-picker v-model="timeRange" is-range value-format="HH:mm" format="HH:mm" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="recordVisible=false">{{ $tp('取消') }}</el-button><el-button type="primary" @click="saveRecordPlanForm">{{ $tp('保存计划') }}</el-button></template>
      </el-dialog>
    </div>
  </DeviceClassificationLayout>
</template>

<script setup>
import { translatePhrase as uiText } from '@/i18n'

import { nextTick, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import SquareProtocolDevicePlayer from './components/SquareProtocolDevicePlayer.vue'
import { stampCustomStatus } from '@/utils/customDeviceState'
import { useCustomAvailabilityClock } from '@/utils/useCustomAvailabilityClock'
import { useWvpMediaStatus } from '@/utils/useWvpMediaStatus'
import CustomAvailabilityBadge from '@/components/CustomAvailabilityBadge.vue'
import {
  addCustomDevice, deleteCustomDevices, exportCustomDevices, getCustomDevice, getCustomMediaStatus,
  getCustomRecordPlan, getCustomRecordStatus, listCustomDevices,
  saveCustomRecordPlan, startCustomRecord, stopCustomRecord, updateCustomDevice
} from '@/api/customDevice'

const loading = ref(false), devices = ref([]), total = ref(0)
const { mediaAvailable, mediaLabel, mediaTagType, mediaError, loadMediaStatus } = useWvpMediaStatus(getCustomMediaStatus)
const selectedIds = ref([]), classificationDeviceKeys = ref([]), formVisible = ref(false), formRef = ref(), saving = ref(false)
const previewVisible = ref(false), previewTitle = ref('视频预览'), previewDevice = ref({})
const previewSource = ref({})
const statusClock = useCustomAvailabilityClock(() => devices.value)
let statusTimer, disposed = false, listGeneration = 0
const refreshPresence = () => {
  statusClock.value = Date.now()
  if (!document.hidden && !loading.value && !disposed) { loadDevices(); loadMediaStatus() }
}
const recordVisible = ref(false), recording = ref(false), recordDevice = ref({}), selectedDays = ref([]), timeRange = ref(['00:00', '23:59'])
const query = reactive({ pageNum: 1, pageSize: 10, deviceName: '', deviceCode: '', status: undefined })
const emptyForm = () => ({ id: undefined, deviceName: '', deviceCode: '', streamUrl: '', deviceType: '摄像头', status: 'UNKNOWN', longitude: undefined, latitude: undefined, address: '', remark: '' })
const form = reactive(emptyForm())
const recordPlan = reactive({ enabled: false, weekDays: '1,2,3,4,5,6,7', startTime: '00:00', endTime: '23:59' })
const rules = {
  deviceName: [{ required: true, message: '设备名称不能为空', trigger: 'blur' }],
  deviceCode: [{ required: true, message: '设备 ID 不能为空', trigger: 'blur' }],
  streamUrl: [{ required: true, message: '视频流路径不能为空', trigger: 'blur' }, { pattern: /^(rtsp|rtmp|https?):\/\//i, message: '请输入有效的视频流地址', trigger: 'blur' }]
}
const deviceTypes = ['球机', '云台', '摄像头', '枪机', '半球', '其他']
const weekOptions = [['1','周一'],['2','周二'],['3','周三'],['4','周四'],['5','周五'],['6','周六'],['7','周日']].map(([value,label]) => ({ value,label }))

function errorMessage(error, fallback) { return error?.response?.data?.msg || error?.message || fallback }
async function loadDevices() {
  const generation = ++listGeneration, requestedAt = Date.now()
  loading.value = true
  try {
    const list = await listCustomDevices({ ...query })
    if (disposed || generation !== listGeneration) return
    statusClock.value = Date.now()
    devices.value = (list?.rows || []).map(row => stampCustomStatus(row, requestedAt)); total.value = Number(list?.total || 0)
  } catch {
    if (disposed || generation !== listGeneration) return
    devices.value = []; total.value = 0
    selectedIds.value = []; classificationDeviceKeys.value = []
    // The request layer reports the actual list error; media status is independent.
  } finally { if (generation === listGeneration) loading.value = false }
}
function search() { query.pageNum = 1; loadDevices() }
function resetQuery() { Object.assign(query, { pageNum: 1, pageSize: 10, deviceName: '', deviceCode: '', status: undefined, categoryType: undefined, categoryId: undefined, unclassified: undefined }); loadDevices() }
function handleClassificationFilter(filter) { Object.assign(query, filter, { pageNum: 1 }); loadDevices() }
function handleSelectionChange(rows) { selectedIds.value = rows.map(row => String(row.id)); classificationDeviceKeys.value = [...selectedIds.value] }
function resetForm() { Object.assign(form, emptyForm()); nextTick(() => formRef.value?.clearValidate()) }
function openAdd() { resetForm(); formVisible.value = true }
async function openEdit(row) {
  resetForm(); const id = row?.id || selectedIds.value[0]
  const device = (await getCustomDevice(id))?.data || {}
  Object.assign(form, device, { status: device.configuredStatus ?? device.status ?? 'UNKNOWN' }); formVisible.value = true
}
async function saveDevice() {
  await formRef.value.validate(); saving.value = true
  try { form.id ? await updateCustomDevice({ ...form }) : await addCustomDevice({ ...form }); ElMessage.success(form.id ? uiText('修改成功') : uiText('新增成功')); formVisible.value = false; loadDevices() }
  finally { saving.value = false }
}
async function removeDevices(row) {
  const ids = row?.id ? [String(row.id)] : selectedIds.value
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(uiText('确定删除选中的 {value0} 台设备吗？', { value0: ids.length }), uiText('删除设备'), { type: 'warning' })
    await deleteCustomDevices(ids.join(',')); ElMessage.success(uiText('删除成功')); loadDevices()
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, uiText('删除失败'))) }
}
async function exportDevices() { await exportCustomDevices({ ...query }); ElMessage.success(uiText('导出任务已完成')) }

function preview(row) {
  if (!row.streamUrl) return ElMessage.warning(uiText('设备未配置视频流地址'))
  previewDevice.value = row; previewSource.value = { protocol: 'CUSTOM', rowId: String(row.id) }
  previewTitle.value = `${row.deviceName} - 视频预览`; previewVisible.value = true
}

async function openRecord(row) {
  recordDevice.value = row
  const [plan, status] = await Promise.all([getCustomRecordPlan(row.id), getCustomRecordStatus(row.id)])
  Object.assign(recordPlan, plan?.data || {}); selectedDays.value = (recordPlan.weekDays || '').split(',').filter(Boolean)
  timeRange.value = [recordPlan.startTime || '00:00', recordPlan.endTime || '23:59']; recording.value = Boolean(status?.data); recordVisible.value = true
}
async function startRecord() { await startCustomRecord(recordDevice.value.id); recording.value = true; ElMessage.success(uiText('录像已启动')) }
async function stopRecord() { await stopCustomRecord(recordDevice.value.id); recording.value = false; ElMessage.success(uiText('录像已停止')) }
async function saveRecordPlanForm() {
  if (!timeRange.value?.length) return ElMessage.warning(uiText('请选择录像时段'))
  Object.assign(recordPlan, { weekDays: selectedDays.value.join(','), startTime: timeRange.value[0], endTime: timeRange.value[1] })
  await saveCustomRecordPlan(recordDevice.value.id, { ...recordPlan }); ElMessage.success(uiText('录像计划已保存')); recordVisible.value = false
}
onMounted(() => {
  loadDevices(); loadMediaStatus(); statusTimer = window.setInterval(refreshPresence, 30000)
  window.addEventListener('focus', refreshPresence)
  document.addEventListener('visibilitychange', refreshPresence)
})
onBeforeUnmount(() => {
  disposed = true; previewVisible.value = false; window.clearInterval(statusTimer)
  window.removeEventListener('focus', refreshPresence)
  document.removeEventListener('visibilitychange', refreshPresence)
})
</script>

<style scoped>
.protocol-page { padding: 20px; }
.custom-device-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px 20px;
}
.custom-device-header-title {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}
.title { font-size: 18px; font-weight: 600; }
.custom-device-filters {
  display: flex;
  flex: 1 1 520px;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}
.custom-device-filters .el-input { width: 150px; }
.custom-device-filters .el-select { width: 120px; }
.custom-device-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.custom-device-actions :deep(.el-button + .el-button) { margin-left: 0; }
.pagination { display: flex; justify-content: center; padding-top: 20px; }
.tag-item { margin-right: 4px; }.player { min-height: 480px; background: #000; display: flex; align-items: center; justify-content: center; }
.oplayer-container { width: 100%; height: 480px; }.player :deep(#webRtcPlayerBox), .player :deep(#rtcPlayer) { width: 100%; max-height: 520px; }
.record-alert { margin-bottom: 16px; }
@media (max-width: 1200px) {
  .custom-device-filters { flex-basis: 100%; justify-content: flex-start; }
}
</style>
