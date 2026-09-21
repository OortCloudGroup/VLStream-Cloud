<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->
<template>
  <DeviceClassificationLayout
    :key="classificationKey"
    protocol-type="EHOME"
    :selected-device-keys="selectedKeys"
    @filter-change="filterClassification"
    @assigned="loadDevices"
  >
    <section class="ehome-page">
      <header class="page-heading">
        <div class="heading-copy">
          <div class="protocol-icon"><img :src="ehomeIcon" alt="" /></div>
          <div><h1>EHome 设备</h1><p>海康旧版主动接入设备 · 与 ISUP 5.0 分开管理</p></div>
        </div>
        <el-button type="primary" :icon="Plus" @click="guide = true">接入设备</el-button>
      </header>

      <div class="connection-strip">
        <div>
          <span class="status-dot" :class="{ online: service.registrationReady }" />
          <strong>{{ service.registrationReady ? '注册服务就绪' : '注册服务未就绪' }}</strong>
          <span class="service-address">{{ shownAddress }}<span v-if="service.registrationPort">:{{ service.registrationPort }}</span></span>
        </div>
        <el-button link type="primary" @click="guide = true">接入指南 <el-icon><ArrowRight /></el-icon></el-button>
      </div>
      <el-alert v-if="serviceLoaded && !service.registrationReady" :title="service.message || '接入服务未启动'" type="warning" :closable="false" show-icon />

      <div class="list-heading"><h2>设备列表 <span>{{ total }}</span></h2><span>EHome 2.x / 3.x / 4.x</span></div>
      <el-form class="filters" @submit.prevent="search">
        <el-input v-model="query.keyword" placeholder="搜索名称、设备 ID、序列号或 IP" clearable :prefix-icon="Search" @clear="search" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="search"><el-option label="在线" value="ON" /><el-option label="离线" value="OFFLINE" /></el-select>
        <el-select v-model="query.devProtocolVersion" placeholder="全部版本" clearable @change="search"><el-option v-for="version in ['2', '3', '4']" :key="version" :label="`EHome ${version}.x`" :value="version" /></el-select>
        <el-button native-type="submit" type="primary" plain>搜索</el-button>
        <el-button :icon="Refresh" :loading="loading" aria-label="刷新设备列表" @click="refresh" />
      </el-form>
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <el-table v-loading="loading" :data="rows" class="device-table" row-key="id" @selection-change="selection => selectedKeys = selection.map(row => String(row.id))">
        <el-table-column type="selection" width="42" />
        <el-table-column :label="$tp('设备')" min-width="180"><template #default="{ row }"><div class="device-name">{{ row.name || row.deviceId }}</div><div class="device-id">{{ row.deviceId }}</div></template></el-table-column>
        <el-table-column :label="$tp('状态')" width="85"><template #default="{ row }"><span class="device-status" :class="{ online: row.status === 'ON' }"><i />{{ row.status === 'ON' ? '在线' : '离线' }}</span></template></el-table-column>
        <el-table-column :label="$tp('协议版本')" width="115"><template #default="{ row }"><span class="version-label">{{ row.devProtocolVersion ? `EHome ${row.devProtocolVersion}` : '未识别' }}</span></template></el-table-column>
        <el-table-column :label="$tp('设备 IP')" prop="ipAddress" min-width="130" show-overflow-tooltip />
        <el-table-column :label="$tp('最近更新')" prop="updateTime" min-width="160" />
        <el-table-column :label="$tp('操作')" fixed="right" width="160"><template #default="{ row }"><el-button link type="primary" @click="openPreview(row)">通道 / 预览</el-button><el-button link type="primary" @click="openEdit(row)">编辑</el-button></template></el-table-column>
        <template #empty><span /></template>
      </el-table>

      <div v-if="!loading && !rows.length" class="empty-state">
        <el-icon><VideoCamera /></el-icon>
        <h3>{{ loadError ? '设备列表加载失败' : filtered ? '没有找到匹配的设备' : '等待第一台 EHome 设备接入' }}</h3>
        <p>{{ loadError ? '请确认 WVP 服务可用后重试。' : filtered ? '试试其他关键词或筛选条件。' : '在设备端启用 EHome，并填写上方服务器地址和注册端口。' }}</p>
        <el-button v-if="!filtered && !loadError" type="primary" plain @click="guide = true">查看接入步骤</el-button>
        <el-button v-else @click="loadError ? refresh() : resetFilters()">{{ loadError ? '重试' : '清空筛选' }}</el-button>
      </div>
      <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadDevices" />
      <footer class="footnote"><el-icon><InfoFilled /></el-icon>ISUP 5.0 设备继续使用左侧 ISUP 协议入口。</footer>
    </section>
  </DeviceClassificationLayout>

  <el-drawer v-model="guide" title="接入 EHome 设备" size="min(480px, 94vw)" append-to-body>
    <div class="guide-intro"><b>EHome</b><h2>让设备主动连接平台</h2><p>适用于海康 EHome 2.x / 3.x / 4.x，ISUP 5.0 使用独立入口。</p></div>
    <ol class="guide-steps">
      <li><h3>开启平台接入</h3><p>进入设备的网络高级配置，选择 ISUP（原 EHome）并启用。</p></li>
      <li><h3>填写服务器参数</h3><div class="guide-values"><div><span>服务器地址</span><strong>{{ shownAddress }}</strong></div><div><span>注册端口</span><strong>{{ service.registrationPort || '—' }}</strong></div><div><span>取流端口</span><strong>{{ service.streamPort || '—' }}</strong></div></div></li>
      <li><h3>保存并等待上线</h3><p>设备注册后会自动进入列表。选择通道和主 / 子码流即可预览。</p></li>
    </ol>
    <template #footer><el-button type="primary" @click="guide = false">我知道了</el-button></template>
  </el-drawer>

  <el-dialog v-model="preview.visible" :title="`${preview.device?.name || preview.device?.deviceId || '设备'} · 通道预览`" width="min(1080px, 94vw)" append-to-body destroy-on-close @close="closePreview">
    <div class="preview-layout">
      <aside class="channel-panel" v-loading="preview.loading">
        <div class="channel-title">设备通道 <span>{{ preview.channels.length }}</span></div>
        <el-alert v-if="preview.channelError" :title="preview.channelError" type="warning" :closable="false" />
        <button v-for="channel in preview.channels" :key="channel.id" class="channel-item" :class="{ active: preview.channel === channel.id }" @click="selectChannel(channel.id)"><el-icon><VideoCamera /></el-icon>{{ channel.name }}</button>
      </aside>
      <main class="preview-main">
        <div class="preview-controls"><el-radio-group v-model="preview.streamType" @change="stopCurrent"><el-radio-button :value="0">主码流</el-radio-button><el-radio-button :value="1">子码流</el-radio-button></el-radio-group><el-button type="primary" :icon="VideoPlay" :loading="preview.busy" :disabled="!preview.channel || preview.device?.status !== 'ON' || !service.streamReady" @click="play">{{ preview.url ? '重新播放' : '开始预览' }}</el-button></div>
        <div class="preview-video"><EhomePlayer v-if="preview.url" :key="preview.sessionId" :url="preview.url" /><div v-else class="preview-placeholder"><el-icon><VideoCamera /></el-icon><p>{{ preview.message || '选择通道，开始预览' }}</p></div></div>
        <p class="preview-caption">{{ preview.channel ? `通道 ${preview.channel}` : '尚未选择通道' }} · {{ preview.streamType === 0 ? '主码流' : '子码流' }}<span>{{ preview.device?.devProtocolVersion ? `EHome ${preview.device.devProtocolVersion}` : '' }}</span></p>
      </main>
    </div>
  </el-dialog>

  <el-dialog v-model="edit.visible" title="编辑设备" width="min(460px, 94vw)" append-to-body>
    <el-form label-position="top" @submit.prevent="saveEdit"><el-form-item label="设备 ID"><el-input :model-value="edit.deviceId" disabled /></el-form-item><el-form-item label="设备名称" required><el-input v-model="edit.name" maxlength="64" show-word-limit /></el-form-item><el-form-item label="备注"><el-input v-model="edit.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-form>
    <template #footer><el-button @click="edit.visible = false">取消</el-button><el-button type="primary" :loading="edit.saving" @click="saveEdit">保存</el-button></template>
  </el-dialog>
</template>

<script setup name="EhomeDevice">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, InfoFilled, Plus, Refresh, Search, VideoCamera, VideoPlay } from '@element-plus/icons-vue'
import DeviceClassificationLayout from '@/components/DeviceClassificationLayout/index.vue'
import EhomePlayer from './EhomePlayer.vue'
import ehomeIcon from '@/assets/img/svg/ehome.svg'
import { getChannels, getStatus, keepPreview, listDevices, startPreview, stopPreview, updateDevice } from '@/api/ehome/device'

const rows = ref([]), total = ref(0), loading = ref(false), loadError = ref(''), selectedKeys = ref([]), classificationKey = ref(0)
const guide = ref(false), serviceLoaded = ref(false)
const service = reactive({ registrationReady: false, streamReady: false, host: '', serverAddresses: [], message: '' })
const shownAddress = computed(() => service.host || service.serverAddresses?.join(' / ') || '暂无可用地址')
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '', devProtocolVersion: '' })
const filtered = computed(() => !!(query.keyword || query.status || query.devProtocolVersion || query.categoryId))
const preview = reactive({ visible: false, device: null, channels: [], channel: null, streamType: 0, loading: false, busy: false, message: '', channelError: '', sessionId: '', url: '' })
const edit = reactive({ visible: false, saving: false, id: null, deviceId: '', name: '', remark: '' })
let listSequence = 0, channelSequence = 0, playSequence = 0, refreshTimer, heartbeatTimer, destroyed = false

async function loadDevices(silent = false) {
  const sequence = ++listSequence
  if (silent !== true) loading.value = true
  try { const result = await listDevices({ ...query }); if (sequence !== listSequence) return; rows.value = result.rows || []; total.value = result.total || 0; loadError.value = '' }
  catch { if (sequence === listSequence) { rows.value = []; total.value = 0; loadError.value = '设备列表加载失败，请检查 WVP 服务' } }
  finally { if (sequence === listSequence) loading.value = false }
}
async function loadStatus() {
  try { const result = await getStatus(); Object.assign(service, result.data); serviceLoaded.value = true }
  catch { Object.assign(service, { registrationReady: false, streamReady: false, message: '无法获取接入服务状态' }); serviceLoaded.value = true }
}
function refresh(silent = false) { return Promise.all([loadDevices(silent === true), loadStatus()]) }
function search() { query.pageNum = 1; selectedKeys.value = []; loadDevices() }
function resetFilters() { Object.assign(query, { keyword: '', status: '', devProtocolVersion: '', categoryType: undefined, categoryId: undefined, unclassified: undefined }); classificationKey.value++; search() }
function filterClassification(filter) { Object.assign(query, filter); search() }
function openEdit(row) { Object.assign(edit, { ...row, visible: true, saving: false, name: row.name || '', remark: row.remark || '' }) }
async function saveEdit() { if (!edit.name.trim()) return ElMessage.warning('请输入设备名称'); edit.saving = true; try { await updateDevice(edit.id, { name: edit.name.trim(), remark: edit.remark }); edit.visible = false; ElMessage.success('已保存'); loadDevices() } finally { edit.saving = false } }
async function openPreview(row) { await stopCurrent(); Object.assign(preview, { visible: true, device: row, channels: [], channel: null, streamType: 0, message: '', channelError: '' }); await loadChannels() }
async function loadChannels() {
  const sequence = ++channelSequence; preview.loading = true; preview.channelError = ''
  try { if (preview.device.status !== 'ON') { preview.channelError = '设备离线，上线后可查询通道'; preview.message = '设备当前离线'; return }; const result = await getChannels(preview.device.id); if (sequence !== channelSequence || !preview.visible) return; preview.channels = result.data || []; preview.channel = preview.channels[0]?.id || null; if (!preview.channels.length) preview.channelError = '设备未返回可用通道' }
  catch (error) { if (sequence === channelSequence) preview.channelError = error.message || '通道查询失败' }
  finally { if (sequence === channelSequence) preview.loading = false }
}
async function stopCurrent() { ++playSequence; clearInterval(heartbeatTimer); const id = preview.sessionId; preview.sessionId = ''; preview.url = ''; preview.busy = false; if (id) { try { await stopPreview(id) } catch { /* server lease expires if unavailable */ } } }
async function selectChannel(id) { await stopCurrent(); preview.channel = id; preview.message = '' }
async function play() {
  await stopCurrent(); const sequence = ++playSequence; preview.busy = true; preview.message = '正在请求设备视频流…'
  try { const result = await startPreview(preview.device.id, { channel: preview.channel, streamType: preview.streamType }); if (sequence !== playSequence || !preview.visible || destroyed) { await stopPreview(result.data.sessionId); return }; preview.sessionId = result.data.sessionId; preview.url = result.data.url; heartbeatTimer = setInterval(async () => { const id = preview.sessionId; if (!id) return; try { await keepPreview(id) } catch { if (preview.sessionId === id) { await stopCurrent(); preview.message = '视频会话已断开，请重新播放' } } }, 20000) }
  catch (error) { if (sequence === playSequence) preview.message = error.message || '取流失败，请重试' }
  finally { if (sequence === playSequence) preview.busy = false }
}
function closePreview() { ++channelSequence; stopCurrent() }
onMounted(() => { refresh(); refreshTimer = setInterval(() => { if (!document.hidden && !preview.visible && !loading.value) refresh(true) }, 15000) })
onBeforeUnmount(() => { destroyed = true; ++listSequence; ++channelSequence; clearInterval(refreshTimer); stopCurrent() })
</script>

<style scoped lang="scss">
.ehome-page { padding: 24px; min-height: 650px; color: #253247; background: #fff; }
.page-heading,.heading-copy,.connection-strip,.connection-strip>div,.list-heading,.filters,.preview-controls { display:flex; align-items:center; }
.page-heading { justify-content:space-between; gap:16px; margin-bottom:24px; }.heading-copy{gap:13px}.heading-copy h1{font-size:22px;margin:0 0 7px}.heading-copy p{margin:0;color:#8793a4;font-size:12px}.protocol-icon{display:grid;place-items:center;width:50px;height:50px;color:#378bfa;background:#edf5ff;border-radius:14px}.protocol-icon img{width:28px;height:28px}
.connection-strip{justify-content:space-between;gap:10px;padding:12px 16px;background:#f6f9fd;border:1px solid #e8eef6;border-radius:9px;font-size:12px}.connection-strip>div{gap:8px}.service-address{color:#8390a2;margin-left:8px;font-family:ui-monospace,monospace}.status-dot{width:7px;height:7px;border-radius:50%;background:#e6a23c}.status-dot.online{background:#24b989;box-shadow:0 0 0 3px #e2f5ee}
.list-heading{justify-content:space-between;margin:26px 0 16px}.list-heading h2{font-size:16px;margin:0}.list-heading h2 span{margin-left:7px;padding:2px 7px;background:#f0f5fa;border-radius:5px;font-size:12px}.list-heading>span{font-size:11px;color:#98a4b3}.filters{gap:9px;margin-bottom:18px;flex-wrap:wrap}.filters>.el-input{flex:1 1 220px}.filters>.el-select{width:120px}.filters>.el-button{margin:0}
.device-table{--el-table-header-bg-color:#f7f9fc;--el-table-border-color:#eef2f7;font-size:12px}.device-table :deep(.el-table__empty-block){display:none}.device-name{font-weight:500;color:#33465e;margin-bottom:4px}.device-id{font-size:11px;color:#97a3b3}.device-status{display:inline-flex;align-items:center;gap:5px;color:#909aa7}.device-status i{width:6px;height:6px;border-radius:50%;background:currentColor}.device-status.online{color:#21a878}.version-label{padding:4px 6px;border-radius:4px;background:#f1f5fc;color:#6584aa;font-size:11px}
.empty-state{text-align:center;padding:55px 15px}.empty-state>.el-icon{font-size:46px;color:#8bb5ea}.empty-state h3{font-size:16px;font-weight:500;color:#536781}.empty-state p{font-size:12px;color:#97a4b5}.footnote{display:flex;align-items:center;gap:6px;margin-top:24px;font-size:11px;color:#9aa6b5}
.guide-intro{padding:24px;margin-bottom:25px;border-radius:12px;background:#f2f7ff}.guide-intro b{color:#418ff5}.guide-intro h2{margin:12px 0;font-size:22px}.guide-intro p,.guide-steps p{color:#8593a5;line-height:1.8}.guide-steps li{margin-bottom:22px}.guide-values{padding:5px 13px;border:1px solid #e7edf6;border-radius:8px}.guide-values>div{display:flex;gap:10px;padding:9px 0}.guide-values span{width:78px;color:#8a98a9}.guide-values strong{overflow-wrap:anywhere}
.preview-layout{display:flex;min-height:430px;gap:18px}.channel-panel{width:190px;flex-shrink:0;padding:14px;background:#f6f8fc;border-radius:9px;overflow-y:auto}.channel-title{margin-bottom:18px;color:#6f8198}.channel-title span{float:right}.channel-item{display:flex;align-items:center;gap:9px;width:100%;padding:12px;border:0;border-radius:6px;background:transparent;color:#6f8198;cursor:pointer}.channel-item.active{color:#398ef4;background:#e7f0ff}.preview-main{flex:1;min-width:0}.preview-controls{justify-content:space-between;margin-bottom:14px}.preview-video{aspect-ratio:16/9;min-height:340px;overflow:hidden;border-radius:9px;background:#101c2d}.preview-placeholder{display:flex;min-height:340px;flex-direction:column;align-items:center;justify-content:center;color:#9dacc0}.preview-placeholder>.el-icon{font-size:39px}.preview-caption{font-size:12px;color:#8896a8}.preview-caption span{float:right}
@media(max-width:800px){.protocol-icon{display:none}.preview-layout{flex-direction:column}.channel-panel{width:auto;max-height:150px}}
</style>
