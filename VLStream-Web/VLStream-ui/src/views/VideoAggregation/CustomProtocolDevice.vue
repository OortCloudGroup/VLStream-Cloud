<template>
  <DeviceClassificationLayout protocol-type="CUSTOM" :selected-device-keys="classificationDeviceKeys"
    @filter-change="handleClassificationFilter" @assigned="loadDevices">
    <div class="protocol-page">
      <el-card shadow="never">
        <template #header>
          <div class="header">
            <div class="header-title">
              <span class="title">自定义协议设备</span>
              <el-tag :type="mediaAvailable ? 'success' : 'danger'" size="small">WVP ZLM {{ mediaAvailable ? '可用' : '不可用' }}</el-tag>
            </div>
            <div class="filters">
              <el-input v-model="query.deviceName" clearable placeholder="设备名称" @keyup.enter="search" />
              <el-input v-model="query.deviceCode" clearable placeholder="设备 ID" @keyup.enter="search" />
              <el-select v-model="query.status" clearable placeholder="全部状态">
                <el-option label="在线" value="ONLINE" /><el-option label="离线" value="OFFLINE" /><el-option label="未知" value="UNKNOWN" />
              </el-select>
              <el-button type="primary" @click="search">搜索</el-button><el-button @click="resetQuery">重置</el-button>
            </div>
          </div>
        </template>

        <el-alert v-if="serviceError" :title="serviceError" type="error" :closable="false" show-icon class="service-alert" />

        <div class="toolbar">
          <el-button type="primary" @click="openAdd">新增</el-button>
          <el-button type="success" :disabled="selectedIds.length !== 1" @click="openEdit()">修改</el-button>
          <el-button type="danger" :disabled="selectedIds.length === 0" @click="removeDevices()">删除</el-button>
          <el-button type="warning" @click="exportDevices">导出</el-button>
        </div>

        <el-table v-loading="loading" :data="devices" stripe @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" label="序号" width="70" />
          <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="deviceCode" label="设备 ID" min-width="150" show-overflow-tooltip />
          <el-table-column prop="deviceType" label="设备类型" width="110" />
          <el-table-column label="区域" min-width="100"><template #default="{ row }">{{ row.regionName || '未设置' }}</template></el-table-column>
          <el-table-column label="分组" min-width="100"><template #default="{ row }">{{ row.groupName || '未设置' }}</template></el-table-column>
          <el-table-column label="标签" min-width="150">
            <template #default="{ row }">
              <template v-if="row.tagNames"><el-tag v-for="tag in row.tagNames.split(',')" :key="tag" size="small" class="tag-item">{{ tag }}</el-tag></template>
              <span v-else>未设置</span>
            </template>
          </el-table-column>
          <el-table-column prop="streamUrl" label="视频流路径" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="85"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="165" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="preview(row)">预览</el-button>
              <el-button link type="primary" @click="openRecord(row)">录像</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeDevices(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" background
            layout="total, prev, pager, next, sizes" :total="total" @change="loadDevices" />
        </div>
      </el-card>

      <el-dialog v-model="formVisible" :title="form.id ? '修改设备' : '新增设备'" width="680px" destroy-on-close>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="设备名称" prop="deviceName"><el-input v-model="form.deviceName" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="设备 ID" prop="deviceCode"><el-input v-model="form.deviceCode" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="设备类型" prop="deviceType"><el-select v-model="form.deviceType" style="width:100%"><el-option v-for="type in deviceTypes" :key="type" :label="type" :value="type" /></el-select></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="状态" prop="status"><el-select v-model="form.status" style="width:100%"><el-option label="在线" value="ONLINE" /><el-option label="离线" value="OFFLINE" /><el-option label="未知" value="UNKNOWN" /></el-select></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="视频流路径" prop="streamUrl"><el-input v-model="form.streamUrl" placeholder="rtsp://、rtmp:// 或 http(s)://" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="经度" prop="longitude"><el-input-number v-model="form.longitude" :precision="7" :min="-180" :max="180" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="纬度" prop="latitude"><el-input-number v-model="form.latitude" :precision="7" :min="-90" :max="90" style="width:100%" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="地址" prop="address"><el-input v-model="form.address" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item></el-col>
          </el-row>
        </el-form>
        <template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDevice">保存</el-button></template>
      </el-dialog>

      <el-dialog v-model="previewVisible" :title="previewTitle" width="900px" destroy-on-close @closed="cleanupPlayer">
        <div v-loading="previewLoading" class="player">
          <rtc-player v-if="webrtcUrl" :video-url="webrtcUrl" :hasaudio="true" />
          <div v-else ref="oplayerContainer" class="oplayer-container" />
        </div>
      </el-dialog>

      <el-dialog v-model="recordVisible" :title="`${recordDevice.deviceName || ''} - 录像管理`" width="600px" destroy-on-close>
        <el-alert title="录像及计划由 WVP 的 ZLMediaKit 执行；跨午夜计划按开始日归属。" type="info" :closable="false" class="record-alert" />
        <el-form :model="recordPlan" label-width="100px">
          <el-form-item label="当前状态">
            <el-tag :type="recording ? 'danger' : 'info'">{{ recording ? '录像中' : '未录像' }}</el-tag>
            <el-button v-if="!recording" link type="primary" :disabled="!mediaAvailable" @click="startRecord">立即开始</el-button>
            <el-button v-else link type="danger" @click="stopRecord">立即停止</el-button>
          </el-form-item>
          <el-form-item label="启用计划"><el-switch v-model="recordPlan.enabled" /></el-form-item>
          <el-form-item label="录像星期"><el-checkbox-group v-model="selectedDays"><el-checkbox v-for="day in weekOptions" :key="day.value" :label="day.value">{{ day.label }}</el-checkbox></el-checkbox-group></el-form-item>
          <el-form-item label="录像时段"><el-time-picker v-model="timeRange" is-range value-format="HH:mm" format="HH:mm" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="recordVisible=false">取消</el-button><el-button type="primary" @click="saveRecordPlanForm">保存计划</el-button></template>
      </el-dialog>
    </div>
  </DeviceClassificationLayout>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import RtcPlayer from '@/components/rtcPlayer/index.vue'
import {
  addCustomDevice, deleteCustomDevices, exportCustomDevices, getCustomDevice, getCustomMediaStatus,
  getCustomRecordPlan, getCustomRecordStatus, listCustomDevices, previewCustomDevice,
  saveCustomRecordPlan, startCustomRecord, stopCustomRecord, updateCustomDevice
} from '@/api/customDevice'
import { createOPlayerOptions, ensureOPlayer, getCustomStreamType } from '@/utils/customPlayer'

const loading = ref(false), devices = ref([]), total = ref(0), mediaAvailable = ref(false), serviceError = ref('')
const selectedIds = ref([]), classificationDeviceKeys = ref([]), formVisible = ref(false), formRef = ref(), saving = ref(false)
const previewVisible = ref(false), previewLoading = ref(false), previewTitle = ref('视频预览'), webrtcUrl = ref('')
const oplayerContainer = ref(null), oplayerInstance = shallowRef(null)
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
  loading.value = true; serviceError.value = ''
  try {
    const [list, media] = await Promise.all([listCustomDevices(query), getCustomMediaStatus()])
    devices.value = list?.rows || []; total.value = Number(list?.total || 0); mediaAvailable.value = Boolean(media?.data?.available)
  } catch (error) {
    devices.value = []; total.value = 0; mediaAvailable.value = false
    serviceError.value = errorMessage(error, 'WVP 服务不可用，无法加载自定义协议设备')
  } finally { loading.value = false }
}
function search() { query.pageNum = 1; loadDevices() }
function resetQuery() { Object.assign(query, { pageNum: 1, pageSize: 10, deviceName: '', deviceCode: '', status: undefined, categoryType: undefined, categoryId: undefined, unclassified: undefined }); loadDevices() }
function handleClassificationFilter(filter) { Object.assign(query, filter, { pageNum: 1 }); loadDevices() }
function handleSelectionChange(rows) { selectedIds.value = rows.map(row => String(row.id)); classificationDeviceKeys.value = [...selectedIds.value] }
function resetForm() { Object.assign(form, emptyForm()); nextTick(() => formRef.value?.clearValidate()) }
function openAdd() { resetForm(); formVisible.value = true }
async function openEdit(row) { resetForm(); const id = row?.id || selectedIds.value[0]; Object.assign(form, (await getCustomDevice(id))?.data || {}); formVisible.value = true }
async function saveDevice() {
  await formRef.value.validate(); saving.value = true
  try { form.id ? await updateCustomDevice({ ...form }) : await addCustomDevice({ ...form }); ElMessage.success(form.id ? '修改成功' : '新增成功'); formVisible.value = false; loadDevices() }
  finally { saving.value = false }
}
async function removeDevices(row) {
  const ids = row?.id ? [String(row.id)] : selectedIds.value
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 台设备吗？`, '删除设备', { type: 'warning' })
    await deleteCustomDevices(ids.join(',')); ElMessage.success('删除成功'); loadDevices()
  } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, '删除失败')) }
}
async function exportDevices() { await exportCustomDevices({ ...query }); ElMessage.success('导出任务已完成') }

function cleanupPlayer() {
  if (oplayerInstance.value?.compInstance?.$destroy) oplayerInstance.value.compInstance.$destroy()
  oplayerInstance.value = null; if (oplayerContainer.value) oplayerContainer.value.innerHTML = ''; webrtcUrl.value = ''
}
async function preview(row) {
  if (!row.streamUrl) return ElMessage.warning('设备未配置视频流地址')
  cleanupPlayer(); previewTitle.value = `${row.deviceName} - 视频预览`; previewVisible.value = true; previewLoading.value = true
  try {
    if (['rtsp', 'rtmp'].includes(getCustomStreamType(row.streamUrl))) {
      if (!mediaAvailable.value) throw new Error('RTSP/RTMP 预览需要可用的 WVP ZLM 媒体服务器')
      const stream = (await previewCustomDevice(row.id))?.data || {}
      webrtcUrl.value = location.protocol === 'https:' ? stream.rtcs : stream.rtc
      if (!webrtcUrl.value) throw new Error('WVP ZLM 未返回 WebRTC 播放地址')
    } else {
      await Promise.all([ensureOPlayer(), nextTick()])
      if (!oplayerContainer.value) throw new Error('播放器容器未准备好')
      const { playerConfig, playConfig } = createOPlayerOptions(row.streamUrl)
      const player = new window.OToolBox.OPlayer(oplayerContainer.value, playerConfig)
      oplayerInstance.value = player; player.play({ ...playConfig, name: row.deviceName || '' })
    }
  } catch (error) { ElMessage.error(errorMessage(error, '视频预览失败')); previewVisible.value = false }
  finally { previewLoading.value = false }
}

async function openRecord(row) {
  recordDevice.value = row
  const [plan, status] = await Promise.all([getCustomRecordPlan(row.id), getCustomRecordStatus(row.id)])
  Object.assign(recordPlan, plan?.data || {}); selectedDays.value = (recordPlan.weekDays || '').split(',').filter(Boolean)
  timeRange.value = [recordPlan.startTime || '00:00', recordPlan.endTime || '23:59']; recording.value = Boolean(status?.data); recordVisible.value = true
}
async function startRecord() { await startCustomRecord(recordDevice.value.id); recording.value = true; ElMessage.success('录像已启动') }
async function stopRecord() { await stopCustomRecord(recordDevice.value.id); recording.value = false; ElMessage.success('录像已停止') }
async function saveRecordPlanForm() {
  if (!timeRange.value?.length) return ElMessage.warning('请选择录像时段')
  Object.assign(recordPlan, { weekDays: selectedDays.value.join(','), startTime: timeRange.value[0], endTime: timeRange.value[1] })
  await saveCustomRecordPlan(recordDevice.value.id, { ...recordPlan }); ElMessage.success('录像计划已保存'); recordVisible.value = false
}
function statusType(status) { return status === 'ONLINE' ? 'success' : status === 'OFFLINE' ? 'info' : 'warning' }
function statusText(status) { return status === 'ONLINE' ? '在线' : status === 'OFFLINE' ? '离线' : '未知' }
onMounted(loadDevices)
</script>

<style scoped>
.protocol-page { padding: 20px; }
.header, .filters, .header-title, .toolbar { display: flex; align-items: center; gap: 12px; }
.header { justify-content: space-between; }
.title { font-size: 18px; font-weight: 600; }
.filters .el-input { width: 160px; }.filters .el-select { width: 120px; }
.service-alert, .toolbar { margin-bottom: 14px; }.pagination { display: flex; justify-content: center; padding-top: 20px; }
.tag-item { margin-right: 4px; }.player { min-height: 480px; background: #000; display: flex; align-items: center; justify-content: center; }
.oplayer-container { width: 100%; height: 480px; }.player :deep(#webRtcPlayerBox), .player :deep(#rtcPlayer) { width: 100%; max-height: 520px; }
.record-alert { margin-bottom: 16px; }
@media (max-width: 1320px) { .header { align-items: flex-start; flex-direction: column; }.filters { flex-wrap: wrap; } }
</style>
