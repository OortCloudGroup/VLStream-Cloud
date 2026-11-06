<template>
    <main class="cloud-ai-page">
        <header class="cloud-ai-heading"><div><span class="cloud-ai-eyebrow">{{ $tp("云端智能 / 运行调度") }}</span><h1>{{ runsOnly ? '任务状态' : '任务配置' }}</h1><p>{{ runsOnly ? '查看任务是否正常运行、结束或异常，追踪每路摄像机与模型的执行情况。' : '立即运行一次场景，或安排固定时间巡检。执行记录独立保留。' }}</p></div><el-button v-if="runsOnly" @click="router.push('/algorithm-task')">{{ $tp("前往任务配置") }}</el-button><el-button v-else type="primary" :icon="Plus" @click="edit()">{{ $tp("创建任务") }}</el-button></header>
        <div class="cloud-ai-toolbar"><el-input v-model="search" :placeholder="tab === 'tasks' ? '搜索任务名称' : '搜索任务、场景或运行编号'" :prefix-icon="Search" clearable @keyup.enter="load(true)" @clear="load(true)" /><el-select v-model="sceneId" :placeholder="$tp(&quot;全部场景&quot;)" clearable filterable @change="load(true)"><el-option v-for="scene in scenes" :key="scene.id" :label="scene.name" :value="scene.id" /></el-select><el-select v-if="tab === 'runs'" v-model="state" :placeholder="$tp(&quot;全部运行状态&quot;)" clearable @change="load(true)"><el-option v-for="(item, key) in states" :key="key" :label="item[0]" :value="key" /></el-select><el-button :icon="Refresh" :loading="loading" @click="load()">{{ $tp("刷新") }}</el-button><span class="cloud-ai-count">{{ tab === 'runs' ? '每 10 秒刷新 · ' : '' }}{{ $tp("共") }} {{ total }} {{ $tp("条") }}</span></div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
        <el-table v-if="tab === 'tasks'" key="tasks" v-loading="loading" :data="loadedTab === tab ? rows : []" row-key="id">
            <el-table-column :label="$tp(&quot;任务 / 场景&quot;)" min-width="210"><template #default="{ row }"><span class="cloud-ai-main-text">{{ row.name }}</span><span class="cloud-ai-secondary">{{ sceneName(row.scene_id) }}</span></template></el-table-column>
            <el-table-column :label="$tp(&quot;运行规则（北京时间）&quot;)" min-width="265"><template #default="{ row }"><span class="cloud-ai-rule-summary">{{ scheduleText(row.schedule) }}</span><span v-if="['DAILY', 'WEEKLY'].includes(row.schedule.kind) && row.schedule.firstDate" class="cloud-ai-secondary">{{ row.schedule.firstDate }} {{ $tp("起") }}{{ row.schedule.lastDate ? '，至 ' + row.schedule.lastDate : '，长期有效' }}</span></template></el-table-column>
            <el-table-column :label="$tp(&quot;下次运行&quot;)" min-width="170"><template #default="{ row }">{{ row.enabled ? formatTime(row.next_start) : '—' }}</template></el-table-column>
            <el-table-column :label="$tp(&quot;配置状态&quot;)" width="110"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.schedule.kind === 'MANUAL' ? '已提交' : row.enabled ? '已启用' : '已停用' }}</el-tag></template></el-table-column>
            <el-table-column :label="$tp(&quot;操作&quot;)" width="225" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="row.schedule.kind === 'MANUAL' ? edit(row, true) : edit(row)">{{ row.schedule.kind === 'MANUAL' ? '再次执行' : '编辑' }}</el-button><el-button v-if="row.schedule.kind !== 'MANUAL'" link @click="toggle(row)">{{ row.enabled ? '停用' : '启用' }}</el-button><el-button link type="danger" @click="remove(row)">{{ $tp("删除") }}</el-button></template></el-table-column>
            <template #empty><el-empty class="cloud-ai-empty" :description="$tp(&quot;尚无任务。选择一个已启用场景，安排首次运行。&quot;)" /></template>
        </el-table>
        <el-table v-else key="runs" v-loading="loading && !rows.length" :data="loadedTab === tab ? rows : []" row-key="id">
            <el-table-column :label="$tp(&quot;任务 / 场景&quot;)" min-width="210"><template #default="{ row }"><span class="cloud-ai-main-text">{{ row.snapshot.taskName }}</span><span class="cloud-ai-secondary">{{ row.snapshot.sceneName }} {{ $tp("· 配置 v") }}{{ row.snapshot.sceneRevision }}</span></template></el-table-column>
            <el-table-column :label="$tp(&quot;运行状态&quot;)" min-width="210"><template #default="{ row }"><el-tag :type="states[row.state]?.[1] || 'info'">{{ states[row.state]?.[0] || row.state }}</el-tag><span class="cloud-ai-secondary">{{ row.message }}</span></template></el-table-column>
            <el-table-column :label="$tp(&quot;开始 / 结束&quot;)" min-width="180"><template #default="{ row }">{{ formatTime(row.started_at) }}<span class="cloud-ai-secondary">{{ formatTime(row.ended_at) }}</span></template></el-table-column>
            <el-table-column :label="$tp(&quot;执行资源&quot;)" min-width="145"><template #default="{ row }">{{ row.snapshot.nodeName }}<span class="cloud-ai-secondary">{{ row.snapshot.cameras?.length }} {{ $tp("路 ×") }} {{ row.snapshot.models?.length }} {{ $tp("个模型") }}</span></template></el-table-column>
            <el-table-column :label="$tp(&quot;操作&quot;)" width="195" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showRun(row.id)">{{ $tp("详情") }}</el-button><el-button link type="primary" @click="analysis(row.id)">{{ $tp("事件") }}</el-button><el-button v-if="!terminal(row.state)" link type="danger" :disabled="!!row.cancel_requested" @click="stop(row)">{{ $tp("停止") }}</el-button></template></el-table-column>
            <template #empty><el-empty class="cloud-ai-empty" :description="$tp(&quot;当前筛选下没有运行记录。&quot;)" /></template>
        </el-table>
        <div class="cloud-ai-footer"><el-pagination background layout="total, prev, pager, next" :total="total" :page-size="20" v-model:current-page="current" @current-change="load()" /></div>
        <el-dialog v-model="opened" :title="editingId ? '编辑定时任务' : '创建任务'" width="740px" class="cloud-ai-dialog" :close-on-click-modal="false" destroy-on-close>
            <el-alert v-if="formError" :title="formError" type="error" :closable="false" show-icon><el-button v-if="form.sceneId && /GPU|算力/.test(formError)" link type="primary" :loading="checkingGpu" @click="checkSceneGpu">{{ $tp("检查场景 GPU") }}</el-button></el-alert>
            <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
                <section class="cloud-ai-section"><div class="cloud-ai-form-grid"><el-form-item :label="$tp(&quot;任务名称&quot;)" prop="name"><el-input v-model="form.name" maxlength="100" :placeholder="$tp(&quot;例如：作业区早班巡检&quot;)" /></el-form-item><el-form-item :label="$tp(&quot;运行场景&quot;)" prop="sceneId"><el-select v-model="form.sceneId" filterable remote :remote-method="loadScenes" :placeholder="$tp(&quot;选择已启用的场景&quot;)"><el-option v-for="scene in scenes" :key="scene.id" :label="scene.name" :value="scene.id" :disabled="!scene.enabled" /></el-select></el-form-item></div><el-form-item :label="$tp(&quot;运行方式&quot;)"><el-radio-group v-model="form.schedule.kind" :disabled="!!editingId"><el-radio-button value="MANUAL">{{ $tp("手工立即运行") }}</el-radio-button><el-radio-button value="ONCE">{{ $tp("指定日期一次") }}</el-radio-button><el-radio-button value="DAILY">{{ $tp("每天") }}</el-radio-button><el-radio-button value="WEEKLY">{{ $tp("每周") }}</el-radio-button></el-radio-group></el-form-item></section>
                <section v-if="form.schedule.kind === 'MANUAL'" class="cloud-ai-section"><el-form-item :label="$tp(&quot;运行时长（分钟）&quot;)"><el-input-number v-model="minutes" :min="1" :max="525600" :step="5" /></el-form-item><p class="cloud-ai-note">{{ $tp("保存后立即提交。GPU 忙时排队，收到画面并开始检测后计算运行时长；可在任务状态中提前停止。") }}</p></section>
                <section v-else class="cloud-ai-section"><h3>{{ $tp("执行时间 · Asia/Shanghai") }}</h3><div v-if="form.schedule.kind === 'ONCE'" class="cloud-ai-form-grid"><el-form-item :label="$tp(&quot;开始日期与时间&quot;)"><el-date-picker v-model="form.schedule.onceStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" :placeholder="$tp(&quot;开始时间&quot;)" /></el-form-item><el-form-item :label="$tp(&quot;结束日期与时间&quot;)"><el-date-picker v-model="form.schedule.onceEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" :placeholder="$tp(&quot;结束时间&quot;)" /></el-form-item></div><template v-else><el-form-item v-if="form.schedule.kind === 'WEEKLY'" :label="$tp(&quot;执行星期&quot;)"><el-checkbox-group v-model="form.schedule.weekdays"><el-checkbox v-for="(day, index) in weekdays" :key="day" :value="index + 1">{{ $tp("周") }}{{ day }}</el-checkbox></el-checkbox-group></el-form-item><div class="cloud-ai-form-grid"><el-form-item :label="$tp(&quot;每天开始时刻&quot;)"><el-time-picker v-model="form.schedule.startTime" value-format="HH:mm:ss" :placeholder="$tp(&quot;开始时刻&quot;)" /></el-form-item><el-form-item :label="$tp(&quot;每天结束时刻&quot;)"><el-time-picker v-model="form.schedule.endTime" value-format="HH:mm:ss" :placeholder="$tp(&quot;结束时刻&quot;)" /></el-form-item><el-form-item :label="$tp(&quot;有效期开始日期&quot;)"><el-date-picker v-model="form.schedule.firstDate" value-format="YYYY-MM-DD" :placeholder="$tp(&quot;开始日期&quot;)" /></el-form-item><el-form-item :label="$tp(&quot;有效期结束日期（可选）&quot;)"><el-date-picker v-model="form.schedule.lastDate" value-format="YYYY-MM-DD" :placeholder="$tp(&quot;不填写则长期有效&quot;)" /></el-form-item></div></template><p class="cloud-ai-note">{{ $tp("GPU 忙时按顺序等待。超过本次结束时间则跳过并记录原因，不抢占现有任务，不延长时间窗口。") }}</p><el-form-item :label="$tp(&quot;启用定时配置&quot;)" style="margin-top:18px"><el-switch v-model="form.enabled" /></el-form-item></section>
            </el-form><template #footer><el-button :disabled="saving" @click="opened = false">{{ $tp("取消") }}</el-button><el-button type="primary" :loading="saving" @click="save">{{ form.schedule.kind === 'MANUAL' ? '创建并立即运行' : '保存定时任务' }}</el-button></template>
        </el-dialog>
        <el-drawer v-model="runOpened" :title="$tp(&quot;运行详情&quot;)" size="min(720px, 95vw)" class="cloud-ai-drawer" destroy-on-close>
            <el-alert v-if="runError" :title="runError" type="error" :closable="false" />
            <template v-if="run"><div class="cloud-ai-inline"><el-tag :type="states[run.state]?.[1]">{{ states[run.state]?.[0] || run.state }}</el-tag><strong>{{ run.snapshot.taskName }}</strong></div><p class="cloud-ai-note" style="margin-top:20px">{{ run.message }}</p><dl class="cloud-ai-details"><dt>{{ $tp("场景") }}</dt><dd>{{ run.snapshot.sceneName }} {{ $tp("· 配置 v") }}{{ run.snapshot.sceneRevision }}</dd><dt>{{ $tp("运行编号") }}</dt><dd>{{ run.id }}</dd><dt>{{ $tp("执行 GPU") }}</dt><dd>{{ run.snapshot.nodeName }}</dd><dt>{{ $tp("计划开始") }}</dt><dd>{{ formatTime(run.planned_start) }}</dd><dt>{{ $tp("计划结束") }}</dt><dd>{{ run.planned_end ? formatTime(run.planned_end) : `开始检测后运行 ${run.duration_seconds} 秒` }}</dd><dt>{{ $tp("实际开始") }}</dt><dd>{{ formatTime(run.started_at) }}</dd><dt>{{ $tp("结束时间") }}</dt><dd>{{ formatTime(run.ended_at) }}</dd><dt>{{ $tp("最后心跳") }}</dt><dd>{{ formatTime(run.heartbeat_at) }}</dd></dl><section class="cloud-ai-section"><h3>{{ $tp("摄像机状态") }}</h3><el-table :data="run.detail?.cameras || []"><el-table-column prop="name" :label="$tp(&quot;摄像机&quot;)" min-width="140" /><el-table-column prop="receivedFrames" :label="$tp(&quot;接收帧数&quot;)" width="100" /><el-table-column prop="inferences" :label="$tp(&quot;检测次数&quot;)" width="100" /><el-table-column prop="message" :label="$tp(&quot;状态&quot;)" min-width="150" /></el-table><p v-if="!run.detail?.cameras?.length" class="cloud-ai-help">{{ $tp("执行器尚未开始接收画面。") }}</p></section><section class="cloud-ai-section"><h3>{{ $tp("模型执行") }}</h3><el-table :data="run.detail?.pairs || []"><el-table-column :label="$tp(&quot;摄像机&quot;)" min-width="140"><template #default="{ row }">{{ run.snapshot.cameras.find(camera => camera.grantId === row.cameraKey)?.name || '—' }}</template></el-table-column><el-table-column prop="name" :label="$tp(&quot;模型&quot;)" /><el-table-column prop="count" :label="$tp(&quot;检测次数&quot;)" width="100" /></el-table></section></template>
            <template #footer><el-button @click="showRun(run.id)" :disabled="!run">{{ $tp("刷新") }}</el-button><el-button v-if="run && !terminal(run.state)" type="danger" plain :disabled="!!run.cancel_requested" @click="stop(run)">{{ $tp("停止运行") }}</el-button><el-button type="primary" :disabled="!run" @click="analysis(run.id)">{{ $tp("查看检测事件") }}</el-button></template>
        </el-drawer>
    </main>
</template>
<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { cloudAi, cloudError } from '@/api/cloudAi'
import { states, terminal, formatTime as formatRunTime, scheduleText as formatSchedule } from './presentation'
import { currentLocale, translateUiMessage } from '@/i18n'
const formatTime = value => formatRunTime(value, currentLocale.value)
const scheduleText = rule => formatSchedule(rule, translateUiMessage, currentLocale.value)
import './cloud-ai.css'
const props = defineProps({ runsOnly: Boolean })
const route = useRoute(), router = useRouter(), tab = computed(() => props.runsOnly ? 'runs' : 'tasks')
const loadedTab = ref(tab.value)
const rows = ref([]), total = ref(0), current = ref(1), search = ref(''), sceneId = ref(route.query.sceneId || ''), state = ref(''), loading = ref(false), error = ref(''), scenes = ref([])
const opened = ref(false), editingId = ref(null), saving = ref(false), formError = ref(''), formRef = ref(), minutes = ref(30)
const weekdays = ['一', '二', '三', '四', '五', '六', '日']
const fresh = () => ({ name: '', sceneId: sceneId.value || '', enabled: true, schedule: { kind: 'MANUAL', durationSeconds: 1800, weekdays: [1, 2, 3, 4, 5], firstDate: new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Shanghai' }), lastDate: '', startTime: '08:00:00', endTime: '18:00:00', onceStart: '', onceEnd: '' } })
const form = ref(fresh()), runOpened = ref(false), run = ref(null), runError = ref('')
const checkingGpu = ref(false)
const rules = { name: [{ required: true, whitespace: true, message: '请填写任务名称', trigger: 'blur' }], sceneId: [{ required: true, message: '请选择场景', trigger: 'change' }] }
let generation = 0, detailGeneration = 0, timer, polling = false
const sceneName = id => scenes.value.find(item => item.id === id)?.name || '关联场景'
async function loadScenes(search = '') { try { scenes.value = (await cloudAi.list('scenes', { page: 1, size: 100, search })).records } catch (failure) { error.value = cloudError(failure) } }
async function load(reset = false) {
    if (reset) current.value = 1
    const request = ++generation, requestedTab = tab.value; loading.value = true; error.value = ''
    try { const result = await cloudAi.list(requestedTab, { page: current.value, size: 20, search: search.value, sceneId: sceneId.value || undefined, state: state.value || undefined }); if (request === generation && tab.value === requestedTab) { rows.value = result.records; loadedTab.value = requestedTab; total.value = Number(result.total) } }
    catch (failure) { if (request === generation) error.value = cloudError(failure) } finally { if (request === generation) loading.value = false }
}
function redirectLegacyRuns() {
    if (props.runsOnly || route.query.tab !== 'runs') return false
    const query = { ...route.query }; delete query.tab; delete query.create
    router.replace({ path: '/task-status', query }); return true
}
async function edit(row, repeat = false) {
    editingId.value = repeat ? null : row?.id || null; formError.value = ''; form.value = fresh(); minutes.value = 30
    if (row) { form.value = { ...form.value, sceneId: row.scene_id, name: row.name, revision: row.revision, enabled: !!row.enabled, schedule: { ...form.value.schedule, ...row.schedule } }; minutes.value = Math.max(1, Math.round((row.schedule.durationSeconds || 1800) / 60)) }
    opened.value = true; await loadScenes()
}
async function save() {
    if (saving.value || !await formRef.value.validate().catch(() => false)) return
    saving.value = true; formError.value = ''; form.value.schedule.durationSeconds = Math.round(minutes.value * 60)
    try { await cloudAi.save('tasks', editingId.value, form.value); ElMessage.success(form.value.schedule.kind === 'MANUAL' ? '已提交，正在等待执行' : '定时任务已保存'); opened.value = false; if (form.value.schedule.kind === 'MANUAL') await router.push({ path: '/task-status', query: { sceneId: form.value.sceneId } }); else await load(true) }
    catch (failure) { formError.value = cloudError(failure) } finally { saving.value = false }
}
async function checkSceneGpu() {
    if (checkingGpu.value) return
    checkingGpu.value = true
    try {
        const scene = await cloudAi.get('scenes', form.value.sceneId)
        const node = (await cloudAi.nodes()).find(item => String(item.id) === String(scene.config.nodeId))
        if (!node) throw new Error('场景的 GPU 已不可用，请编辑场景重新选择')
        await cloudAi.checkNode(node)
        if ((await cloudAi.nodes()).find(item => item.id === node.id)?.state !== 'READY') throw new Error('GPU 检查未通过，请检查实例运行环境')
        formError.value = ''; ElMessage.success('GPU 检查通过，可以重新提交任务')
    } catch (failure) { formError.value = cloudError(failure) } finally { checkingGpu.value = false }
}
async function toggle(row) { try { await cloudAi.enabled('tasks', row.id, !row.enabled); await load() } catch (failure) { error.value = cloudError(failure) } }
async function remove(row) { try { await ElMessageBox.confirm(`删除“${row.name}”的配置？历史运行记录仍会保留。`, '删除任务配置', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }); await cloudAi.remove('tasks', row.id); await load() } catch (failure) { if (!['cancel', 'close'].includes(failure)) error.value = cloudError(failure) } }
async function showRun(id) { const request = ++detailGeneration; runOpened.value = true; runError.value = ''; if (run.value?.id !== id) run.value = null; try { const detail = await cloudAi.get('runs', id); if (request === detailGeneration && runOpened.value) run.value = detail } catch (failure) { if (request === detailGeneration) runError.value = cloudError(failure) } }
async function stop(row) { try { await ElMessageBox.confirm('停止本次检测？已生成的告警将完成录像收尾，确认资源释放后显示“已停止”。', '停止运行', { confirmButtonText: '停止运行', cancelButtonText: '继续运行', type: 'warning' }); await cloudAi.stop(row.id); ElMessage.success('停止请求已提交'); if (runOpened.value) await showRun(row.id); await load() } catch (failure) { if (!['cancel', 'close'].includes(failure)) error.value = cloudError(failure) } }
const analysis = id => router.push({ path: '/intelligent-analysis-request', query: { runId: id } })
watch(() => route.query, value => { if (redirectLegacyRuns()) return; sceneId.value = value.sceneId || ''; load(true) })
onMounted(async () => { if (redirectLegacyRuns()) return; await Promise.all([load(), loadScenes()]); if (!props.runsOnly && route.query.create) { edit(); const query = { ...route.query }; delete query.create; await router.replace({ path: route.path, query }) } timer = setInterval(async () => { if (document.hidden || polling || opened.value) return; polling = true; try { if (tab.value === 'runs') await load(); if (runOpened.value && run.value && !terminal(run.value.state)) await showRun(run.value.id) } finally { polling = false } }, 10000) })
onUnmounted(() => { clearInterval(timer); generation++; detailGeneration++ })
</script>
