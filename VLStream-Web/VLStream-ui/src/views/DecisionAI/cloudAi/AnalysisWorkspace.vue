<template>
    <main class="cloud-ai-page">
        <header class="cloud-ai-heading"><div><span class="cloud-ai-eyebrow">云端智能 / 检测事件</span><h1>智能分析</h1><p>查看运行中发现的事件与证据，在主动安全中完成处置。</p></div><el-button :icon="Refresh" :loading="loading" @click="load()">刷新事件</el-button></header>
        <div v-if="runId" class="cloud-ai-note">正在查看指定运行的检测事件 <el-button link type="primary" @click="clearRun">查看全部事件</el-button></div>
        <div class="cloud-ai-toolbar"><el-input v-model="search" placeholder="搜索事件类型、场景或摄像机" :prefix-icon="Search" clearable @keyup.enter="load(true)" @clear="load(true)" /><el-select v-model="sceneId" placeholder="全部场景" clearable filterable @change="load(true)"><el-option v-for="scene in scenes" :key="scene.id" :value="scene.id" :label="scene.name" /></el-select><el-select v-model="status" placeholder="全部处置状态" clearable @change="load(true)"><el-option label="待处理" value="pending" /><el-option label="正在处理" value="processing" /><el-option label="已完成" value="completed" /><el-option label="已关闭" value="closed" /></el-select><span class="cloud-ai-count">每 10 秒刷新 · 共 {{ total }} 条事件</span></div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
        <el-table v-loading="loading && !rows.length" :data="rows" row-key="eventId">
            <el-table-column label="告警画面" width="145"><template #default="{ row }"><el-image v-if="row.media.annotated" :src="row.media.annotated" class="cloud-ai-thumb" fit="contain" :preview-src-list="[row.media.annotated]" preview-teleported /><span v-else class="cloud-ai-help">图片暂不可用</span></template></el-table-column>
            <el-table-column label="事件 / 摄像机" min-width="190"><template #default="{ row }"><span class="cloud-ai-main-text">{{ row.event_type }}</span><span class="cloud-ai-secondary">{{ row.detail.cameraName }} · {{ row.detail.modelName }}</span></template></el-table-column>
            <el-table-column label="事件时间" min-width="170"><template #default="{ row }">{{ formatTime(row.detected_at) }}</template></el-table-column>
            <el-table-column label="处置状态" width="110"><template #default="{ row }"><el-tag :type="['completed', 'closed'].includes(row.event_status) ? 'success' : 'warning'">{{ { pending: '待处理', processing: '正在处理', completed: '已完成', closed: '已关闭' }[row.event_status] || row.event_status }}</el-tag></template></el-table-column>
            <el-table-column label="录像" width="110"><template #default="{ row }"><span>{{ mediaLabels[row.media_state] || row.media_state }}</span></template></el-table-column>
            <el-table-column label="操作" width="200" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="selected = row; opened = true">查看证据</el-button><el-button link type="primary" @click="handle(row)">前往处置</el-button></template></el-table-column>
            <template #empty><el-empty class="cloud-ai-empty" description="尚无符合条件的检测事件。场景运行后，算法告警会显示在这里。"><el-button @click="router.push('/algorithm-task')">查看任务</el-button></el-empty></template>
        </el-table>
        <div class="cloud-ai-footer"><el-pagination background layout="total, prev, pager, next" :total="total" :page-size="20" v-model:current-page="current" @current-change="load()" /></div>
        <el-dialog v-model="opened" title="检测事件证据" width="900px" class="cloud-ai-dialog" destroy-on-close><CloudEventEvidence v-if="selected" :event-id="selected.eventId" /><template #footer><el-button @click="opened = false">关闭</el-button><el-button type="primary" @click="handle(selected)">在主动安全中处置</el-button></template></el-dialog>
    </main>
</template>
<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { cloudAi, cloudError } from '@/api/cloudAi'
import { formatTime } from './presentation'
import CloudEventEvidence from './CloudEventEvidence.vue'
import './cloud-ai.css'
const route = useRoute(), router = useRouter(), runId = ref(route.query.runId || ''), sceneId = ref(route.query.sceneId || '')
const search = ref(''), status = ref(''), current = ref(1), rows = ref([]), total = ref(0), loading = ref(false), error = ref(''), scenes = ref([]), opened = ref(false), selected = ref(null)
const mediaLabels = { PENDING: '生成中', READY: '完整片段', PARTIAL: '部分片段', FAILED: '录像失败' }
let generation = 0, timer
async function load(reset = false) {
    if (reset) current.value = 1
    const request = ++generation; loading.value = true; error.value = ''
    try { const data = await cloudAi.list('events', { page: current.value, size: 20, search: search.value, status: status.value || undefined, sceneId: sceneId.value || undefined, runId: runId.value || undefined }); if (request === generation) { rows.value = data.records; total.value = Number(data.total) } }
    catch (failure) { if (request === generation) error.value = cloudError(failure) } finally { if (request === generation) loading.value = false }
}
const handle = row => router.push({ path: '/event-management', query: { eventId: row.eventId, from: 'cloud-ai' } })
const clearRun = () => router.replace({ path: route.path, query: {} })
watch(() => route.query, value => { runId.value = value.runId || ''; sceneId.value = value.sceneId || ''; load(true) })
onMounted(async () => { load(); try { scenes.value = (await cloudAi.list('scenes', { page: 1, size: 100 })).records } catch (failure) { error.value = cloudError(failure) } timer = setInterval(() => { if (!document.hidden && !loading.value && !opened.value) load() }, 10000) })
onUnmounted(() => { clearInterval(timer); generation++ })
</script>
