<template>
    <main class="cloud-ai-page">
        <header class="cloud-ai-heading"><div><span class="cloud-ai-eyebrow">云端智能 / 检测配置</span><h1>场景治理</h1><p>组合摄像机与算法，定义每一次检测的内容和告警证据。</p></div><el-button type="primary" :icon="Plus" @click="edit()">新建场景</el-button></header>
        <div class="cloud-ai-toolbar"><el-input v-model="search" placeholder="搜索场景名称" :prefix-icon="Search" clearable @keyup.enter="load(true)" @clear="load(true)" /><el-button @click="load(true)">查询</el-button><el-button :icon="Refresh" :loading="loading" @click="load()">刷新</el-button><span class="cloud-ai-count">共 {{ total }} 个场景</span></div>
        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
        <el-table v-loading="loading" :data="rows" row-key="id">
            <el-table-column label="场景" min-width="220"><template #default="{ row }"><span class="cloud-ai-main-text">{{ row.name }}</span><span class="cloud-ai-secondary">{{ row.description || '尚未填写场景说明' }}</span></template></el-table-column>
            <el-table-column label="检测范围" min-width="155"><template #default="{ row }">{{ row.config.cameras?.length || 0 }} 路摄像机 · {{ row.config.models?.length || 0 }} 个模型<span class="cloud-ai-secondary">每台摄像机执行全部所选算法</span></template></el-table-column>
            <el-table-column label="执行资源" min-width="170"><template #default="{ row }">{{ row.config.nodeName || '待配置 GPU' }}<span class="cloud-ai-secondary">{{ row.config.models?.length ? `告警录像 · 前 ${row.config.preSeconds} 秒 / 后 ${row.config.postSeconds} 秒` : '请补全原有场景的检测配置' }}</span></template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '已启用' : '未启用' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="250" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="edit(row)">配置</el-button><el-button link type="primary" :disabled="!row.enabled" @click="router.push({ path: '/algorithm-task', query: { sceneId: row.id, create: '1' } })">创建任务</el-button><el-dropdown trigger="click" @command="command => action(command, row)"><el-button link>更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="toggle">{{ row.enabled ? '停用场景' : '启用场景' }}</el-dropdown-item><el-dropdown-item command="runs">查看运行记录</el-dropdown-item><el-dropdown-item command="delete" divided>删除场景</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column>
            <template #empty><el-empty class="cloud-ai-empty" description="还没有场景。从摄像机与算法组合开始。"><el-button type="primary" @click="edit()">新建场景</el-button></el-empty></template>
        </el-table>
        <div class="cloud-ai-footer"><el-pagination background layout="total, sizes, prev, pager, next" :page-sizes="[10, 20, 50]" :total="total" v-model:page-size="size" v-model:current-page="current" @size-change="load(true)" @current-change="load()" /></div>
        <el-dialog v-model="opened" :title="editingId ? '配置场景' : '新建场景'" width="880px" class="cloud-ai-dialog" :close-on-click-modal="false" destroy-on-close>
            <el-alert v-if="formError" :title="formError" type="error" :closable="false" show-icon />
            <div v-if="modelOptions.some(model => model.canArchive)" class="cloud-ai-note">以下旧版模型已核验来源，完成归档后即可选择：<el-button v-for="model in modelOptions.filter(model => model.canArchive)" :key="model.modelId" link type="primary" :loading="archiving === model.modelId" :disabled="!!archiving" @click="archiveModel(model)">归档 {{ model.name }}</el-button></div>
            <el-form ref="formRef" :model="form" label-position="top" :rules="rules" v-loading="formLoading">
                <section class="cloud-ai-section"><h3>01 · 场景与执行资源</h3><div class="cloud-ai-form-grid"><el-form-item label="场景名称" prop="name"><el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：作业区安全帽巡检" /></el-form-item><el-form-item label="自有 / 公司 GPU" prop="nodeId"><el-select v-model="form.nodeId" placeholder="选择免费 GPU" @change="loadModels"><el-option v-for="node in nodeOptions" :key="node.id" :value="node.id" :label="`${node.name} · ${node.gpuModel || 'GPU'}`" :disabled="!node.enabled" /></el-select><el-button link type="primary" :loading="checkingNode" :disabled="!form.nodeId" @click="checkNode">检查连接与运行环境</el-button></el-form-item></div><el-form-item label="场景说明"><el-input v-model="form.description" type="textarea" :rows="2" maxlength="1000" placeholder="说明监测范围和业务用途" /></el-form-item></section>
                <section class="cloud-ai-section"><div class="cloud-ai-inline" style="justify-content:space-between"><h3>02 · 摄像机 <span class="cloud-ai-help">{{ cameras.length }} 路</span></h3><el-button :icon="Plus" @click="cameraOpen = true">添加摄像机</el-button></div><div v-if="cameras.length" class="cloud-ai-selection"><div v-for="camera in cameras" :key="camera.grantId" class="cloud-ai-selection-row"><div><span class="cloud-ai-main-text">{{ camera.name }}</span><span class="cloud-ai-secondary">{{ camera.protocol }} · {{ camera.type === 'CAMERA_RTC' ? 'WebRTC' : camera.type }} · {{ camera.deviceId }}</span></div><el-button link type="danger" @click="cameras = cameras.filter(item => item.grantId !== camera.grantId)">移除</el-button></div></div><p v-else class="cloud-ai-help">从各协议设备目录选择摄像机与视频通道。</p></section>
                <section class="cloud-ai-section"><h3>03 · 固定模型与告警规则</h3><el-select v-model="selectedModels" multiple filterable placeholder="选择算法库中可用的 YOLO 检测模型" style="width:100%" :disabled="!form.nodeId" :loading="modelsLoading" @change="syncRules"><el-option v-for="model in modelOptions" :key="model.modelId" :value="model.modelId" :label="`${model.name} · v${model.version}${model.available ? '' : '（不可用）'}`" :disabled="!model.available"><span>{{ model.name }} · v{{ model.version }}</span><small v-if="!model.available" style="margin-left:12px;color:#909399">{{ model.reason }}</small></el-option></el-select><p class="cloud-ai-help">模型版本随场景固定。每台摄像机运行全部所选算法；未指定告警类别时，检测全部类别。</p><div v-for="rule in form.models" :key="rule.modelId" class="cloud-ai-model-rule"><strong>{{ modelFor(rule.modelId).name }} · v{{ modelFor(rule.modelId).version }}</strong><el-form-item label="告警类别"><el-select v-model="rule.classes" multiple placeholder="全部类别" style="width:100%"><el-option v-for="(label, id) in modelFor(rule.modelId).labels" :key="id" :label="label" :value="Number(id)" /></el-select></el-form-item><div class="cloud-ai-form-grid"><el-form-item label="置信度阈值"><el-input-number v-model="rule.confidence" :min="0.01" :max="1" :step="0.05" :precision="2" /></el-form-item><el-form-item label="同摄像机 / 模型 / 类别上报间隔（秒）"><el-input-number v-model="rule.reportInterval" :min="1" :max="86400" :step="5" /></el-form-item></div></div></section>
                <section class="cloud-ai-section"><h3>04 · 检测频率与证据</h3><div class="cloud-ai-form-grid"><el-form-item label="检测采样频率（帧 / 秒）"><el-input-number v-model="form.sampleFps" :min="0.2" :max="30" :step="0.2" :precision="1" /></el-form-item><el-form-item label="保存后启用"><el-switch v-model="form.enabled" active-text="可创建任务" inactive-text="保留配置" /></el-form-item><el-form-item label="告警前录像（秒）"><el-input-number v-model="form.preSeconds" :min="0" :max="60" /></el-form-item><el-form-item label="告警后录像（秒）"><el-input-number v-model="form.postSeconds" :min="1" :max="120" /></el-form-item></div><p class="cloud-ai-help">同时保存原图、标框图和告警录像。刚启动或断流时，录像会注明实际保留时长。</p></section>
            </el-form><template #footer><el-button :disabled="saving" @click="opened = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="formLoading || modelsLoading" @click="save">保存场景</el-button></template>
        </el-dialog><CameraSelector v-model:open="cameraOpen" @select="addCamera" />
    </main>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { cloudAi, cloudError } from '@/api/cloudAi'
import CameraSelector from './CameraSelector.vue'
import './cloud-ai.css'
const router = useRouter(), rows = ref([]), total = ref(0), current = ref(1), size = ref(20), search = ref(''), loading = ref(false), error = ref('')
const opened = ref(false), editingId = ref(null), form = ref({}), formRef = ref(), formError = ref(''), saving = ref(false), formLoading = ref(false)
const nodeOptions = ref([]), modelOptions = ref([]), selectedModels = ref([]), modelsLoading = ref(false), cameras = ref([]), cameraOpen = ref(false)
const archiving = ref(null), checkingNode = ref(false)
const rules = { name: [{ required: true, whitespace: true, message: '请填写场景名称', trigger: 'blur' }], nodeId: [{ required: true, message: '请选择 GPU', trigger: 'change' }] }
let listGeneration = 0, modelGeneration = 0, editGeneration = 0
async function load(reset = false) {
    if (reset) current.value = 1
    const generation = ++listGeneration; loading.value = true; error.value = ''
    try { const data = await cloudAi.list('scenes', { page: current.value, size: size.value, search: search.value }); if (generation === listGeneration) { rows.value = data.records; total.value = Number(data.total) } }
    catch (failure) { if (generation === listGeneration) error.value = cloudError(failure) } finally { if (generation === listGeneration) loading.value = false }
}
async function edit(row) {
    const generation = ++editGeneration; editingId.value = row?.id || null; formError.value = ''; formLoading.value = true; opened.value = true
    form.value = { name: '', description: '', nodeId: null, enabled: true, preSeconds: 5, postSeconds: 10, sampleFps: 2, models: [] }
    cameras.value = []; selectedModels.value = []; modelOptions.value = []
    try {
        const [nodes, detail] = await Promise.all([cloudAi.nodes(), row ? cloudAi.get('scenes', row.id) : null])
        if (generation !== editGeneration) return
        nodeOptions.value = nodes
        if (detail) { form.value = { ...form.value, ...detail.config, name: detail.name, description: detail.description, enabled: !!detail.enabled, revision: detail.revision }; cameras.value = detail.config.cameras || []; selectedModels.value = form.value.models.map(model => String(model.modelId)); modelOptions.value = form.value.models.map(model => ({ ...model, available: true })) }
        if (form.value.nodeId) await loadModels(false)
    } catch (failure) { formError.value = cloudError(failure) } finally { if (generation === editGeneration) formLoading.value = false }
}
async function loadModels(clear = true) {
    const generation = ++modelGeneration; modelsLoading.value = true
    if (clear !== false) { selectedModels.value = []; form.value.models = [] }
    try { const data = await cloudAi.models(form.value.nodeId); if (generation === modelGeneration) modelOptions.value = data }
    catch (failure) { if (generation === modelGeneration) formError.value = cloudError(failure) } finally { if (generation === modelGeneration) modelsLoading.value = false }
}
const modelFor = id => modelOptions.value.find(model => model.modelId === id) || form.value.models.find(model => model.modelId === id) || {}
async function checkNode() {
    const node = nodeOptions.value.find(item => item.id === form.value.nodeId)
    if (!node) return
    checkingNode.value = true; formError.value = ''
    try {
        const message = await cloudAi.checkNode(node)
        nodeOptions.value = await cloudAi.nodes()
        if (nodeOptions.value.find(item => item.id === node.id)?.state !== 'READY') formError.value = typeof message === 'string' ? message : 'GPU 检查未通过，请检查实例运行环境'
        else ElMessage.success('GPU 连接和运行环境检查通过')
    } catch (failure) { formError.value = cloudError(failure) } finally { checkingNode.value = false }
}
async function archiveModel(model) {
    archiving.value = model.modelId; formError.value = ''
    try { await cloudAi.archiveModel(model.modelId); await loadModels(false); ElMessage.success('模型与类别归档完成') }
    catch (failure) { formError.value = cloudError(failure) } finally { archiving.value = null }
}
function syncRules() { form.value.models = selectedModels.value.map(id => form.value.models.find(rule => rule.modelId === id) || { ...modelFor(id), modelId: id, confidence: 0.5, reportInterval: 30, classes: [] }) }
function addCamera(camera) { if (!cameras.value.some(item => item.protocol === camera.protocol && item.deviceRowId === camera.deviceRowId && item.streamId === camera.streamId)) cameras.value.push(camera) }
async function save() {
    if (saving.value || !await formRef.value.validate().catch(() => false)) return
    formError.value = ''
    if (!cameras.value.length || !form.value.models.length) { formError.value = '请至少选择一路摄像机和一个算法模型'; return }
    saving.value = true
    try { await cloudAi.save('scenes', editingId.value, { ...form.value, cameras: cameras.value.map(item => item.grantId), models: form.value.models.map(({ modelId, confidence, reportInterval, classes }) => ({ modelId, confidence, reportInterval, classes })) }); ElMessage.success('场景已保存'); opened.value = false; await load() }
    catch (failure) { formError.value = cloudError(failure) } finally { saving.value = false }
}
async function action(command, row) {
    if (command === 'runs') { router.push({ path: '/algorithm-task', query: { sceneId: row.id, tab: 'runs' } }); return }
    try {
        if (command === 'delete') { await ElMessageBox.confirm(`删除“${row.name}”？被任务引用的场景需要先解除引用。`, '删除场景', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }); await cloudAi.remove('scenes', row.id) }
        else { if (row.enabled) await ElMessageBox.confirm('停用后将跳过后续定时执行，当前运行可在任务页停止。', '停用场景', { confirmButtonText: '停用', cancelButtonText: '取消' }); await cloudAi.enabled('scenes', row.id, !row.enabled) }
        await load()
    } catch (failure) { if (!['cancel', 'close'].includes(failure)) error.value = cloudError(failure) }
}
onMounted(load)
</script>
