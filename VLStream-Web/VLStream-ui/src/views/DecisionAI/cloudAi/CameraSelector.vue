<template>
    <el-dialog :model-value="open" title="选择摄像机" width="760px" class="cloud-ai-dialog" append-to-body @update:model-value="$emit('update:open', $event)">
        <div class="cloud-ai-toolbar">
            <el-select v-model="protocol" aria-label="设备协议" @change="load"><el-option v-for="(label, item) in protocolNames" :key="item" :label="label" :value="item" /></el-select>
            <el-input v-model="search" placeholder="搜索设备名称" clearable @keyup.enter="load" @clear="load" /><el-button @click="load">搜索</el-button>
        </div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <el-table v-loading="loading" :data="rows" max-height="390" row-key="id">
            <el-table-column label="摄像机" min-width="220"><template #default="{ row }"><span class="cloud-ai-main-text">{{ row.deviceName || row.name || row.deviceId }}</span><span class="cloud-ai-secondary">{{ row.deviceId || row.deviceCode || row.ip || '设备主码流' }}</span></template></el-table-column>
            <el-table-column label="视频源" min-width="190"><template #default="{ row }"><el-select v-if="streams[row.id]?.length" v-model="selectedStreams[row.id]" placeholder="选择码流" aria-label="视频源"><el-option v-for="source in streams[row.id]" :key="String(source.streamId)" :label="`${source.name || '主码流'} · ${mediaName(source.type)}`" :value="source.streamId || 'main'" :disabled="!source.supported" /></el-select><span v-else-if="streams[row.id]" class="cloud-ai-help">暂无可用通道</span><el-button v-else link type="primary" :loading="streamLoading === row.id" @click="loadStreams(row)">查看视频源</el-button></template></el-table-column>
            <el-table-column width="85" align="right"><template #default="{ row }"><el-button link type="primary" :disabled="!selectedStreams[row.id]" :loading="adding === row.id" @click="add(row)">添加</el-button></template></el-table-column>
        </el-table>
        <div class="cloud-ai-footer"><el-pagination small layout="prev, pager, next" :page-size="10" :total="total" v-model:current-page="current" @current-change="load(false)" /></div>
        <p class="cloud-ai-help">支持各协议设备的有效视频源。平台在任务运行时开流，由 GPU 实际收帧判断可用性；无需保持浏览器打开。</p>
        <template #footer><el-button @click="$emit('update:open', false)">完成选择</el-button></template>
    </el-dialog>
</template>
<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { cloudAi, cloudError } from '@/api/cloudAi'
const props = defineProps({ open: Boolean })
const emit = defineEmits(['update:open', 'select'])
const protocolNames = { VLSTREAM: 'VLStream', ONVIF: 'ONVIF', RTSP: 'RTSP', ISUP: 'ISUP', EHOME: 'EHome', GB28181: '国标 GB28181', DAHUA: '大华', CUSTOM: '自定义' }
const mediaName = type => ({ CAMERA_RTC: 'WebRTC', PLATFORM: '平台开流', HTTP: 'HTTP 视频', MEDIA: '网络视频' }[type] || type || '无可接收视频源')
const protocol = ref('VLSTREAM'), search = ref(''), current = ref(1), rows = ref([]), total = ref(0), loading = ref(false), error = ref('')
const streams = reactive({}), selectedStreams = reactive({}), streamLoading = ref(null), adding = ref(null)
let generation = 0
async function load(reset = true) {
    if (reset !== false) current.value = 1
    const request = ++generation; loading.value = true; error.value = ''; rows.value = []
    streamLoading.value = null; adding.value = null
    Object.keys(streams).forEach(key => delete streams[key]); Object.keys(selectedStreams).forEach(key => delete selectedStreams[key])
    try {
        const data = await cloudAi.devices(protocol.value, { pageNum: current.value, pageSize: 10, name: search.value, deviceName: search.value })
        if (request !== generation) return
        rows.value = data.rows || data.records || data.list || []; total.value = Number(data.total || 0)
    } catch (failure) { if (request === generation) error.value = cloudError(failure) }
    finally { if (request === generation) loading.value = false }
}
async function loadStreams(row) {
    const request = generation; streamLoading.value = row.id
    try { const data = await cloudAi.streams(protocol.value, row.id); if (request === generation) { streams[row.id] = data; selectedStreams[row.id] = data.find(item => item.supported)?.streamId || (data.some(item => item.supported) ? 'main' : null) } }
    catch (failure) { if (request === generation) error.value = cloudError(failure) } finally { if (request === generation) streamLoading.value = null }
}
async function add(row) {
    const request = generation; adding.value = row.id
    try {
        const source = await cloudAi.bind({ protocol: protocol.value, deviceRowId: String(row.id), streamId: selectedStreams[row.id] === 'main' ? null : selectedStreams[row.id] })
        if (request === generation && props.open) { emit('select', source); ElMessage.success('已加入场景') }
    } catch (failure) { if (request === generation) error.value = cloudError(failure) } finally { if (request === generation) adding.value = null }
}
watch(() => props.open, value => { if (value) load(); else generation++ })
</script>
