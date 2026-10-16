<template>
  <section class="compute-market" v-loading="loading">
    <header class="heading">
      <h2>{{ $tp('算力市场') }}</h2>
      <div class="toolbar">
        <el-button v-if="route.query.returnTo === 'training'" type="primary" @click="router.push('/algorithm-training')">{{ $tp('返回训练配置') }}</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="load">{{ $tp('刷新') }}</el-button>
      </div>
    </header>
    <el-alert v-if="marketError" class="market-error" show-icon type="warning" :closable="false" :title="marketError" />
    <el-empty v-if="!loading && !resources.length && !marketError" :description="$tp('暂无已发布算力')" />
    <div class="cards">
      <article v-for="item in resources" :key="item.id" class="compute-card" :class="{ selected: item.id === route.query.nodeId }" :aria-label="item.name">
        <div class="card-heading">
          <div class="resource-icon"><el-icon :size="23"><Cpu /></el-icon></div>
          <div class="resource-title"><h3 :title="item.name">{{ item.name }}</h3><span>{{ $tp(providers[item.provider] || item.provider || '未知') }}</span></div>
          <el-tag class="billing-tag" :type="item.billingType === 'FREE' ? 'success' : 'info'" effect="light" round>{{ $tp(item.billingType === 'FREE' ? '免费' : '收费 · 暂未开放') }}</el-tag>
        </div>
        <div class="gpu-spec"><span>GPU</span><strong>{{ item.gpuModel || $tp('规格未登记') }}</strong></div>
        <div class="status-grid">
          <div class="status-item"><span>{{ $tp('运行状态') }}</span><el-tag :type="statusType(item.powerState)" effect="plain" size="small">{{ $tp(states[item.powerState] || '未知') }}</el-tag></div>
          <div class="status-item"><span>{{ $tp('训练环境') }}</span><el-tag :type="statusType(item.healthState)" effect="plain" size="small">{{ $tp(states[item.healthState] || '待检查') }}</el-tag></div>
          <div class="status-item"><span>{{ $tp('使用方式') }}</span><strong>{{ $tp(item.schedulingMode === 'SHARED_GPU_QUEUE' ? '按任务排队' : '整实例独享') }}</strong></div>
          <div class="status-item"><span>{{ $tp('使用状态') }}</span><strong :class="{ 'status-warning': ['OTHER', 'ISOLATED'].includes(item.claimState) }">{{ $tp(claimLabels[item.claimState] || '隔离') }}</strong></div>
        </div>
        <p v-if="item.operationPending" class="status-note">{{ $tp('实例操作处理中') }}</p>
        <details class="resource-details">
          <summary>{{ $tp('详情') }}<el-icon><ArrowDown /></el-icon></summary>
          <dl>
            <div v-if="item.provider !== 'self'"><dt>{{ $tp('厂商容量') }}</dt><dd>{{ $tp(states[item.capacityState] || '未知') }}</dd></div>
            <div v-if="item.taskCount != null"><dt>{{ $tp('当前任务数') }}</dt><dd>{{ item.taskCount }}</dd></div>
            <div><dt>{{ $tp('服务器') }}</dt><dd>{{ item.serverGroup || $tp('未登记') }}</dd></div>
            <div><dt>{{ $tp('GPU 编号') }}</dt><dd>{{ item.gpuIndex ?? '—' }}</dd></div>
            <div v-if="item.provider !== 'self' && item.capacitySource"><dt>{{ $tp('状态来源') }}</dt><dd>{{ $tp(item.capacitySource === 'OPERATOR_CONSOLE' ? '管理员控制台观察' : '厂商接口') }}<span v-if="item.capacityCheckedAt"> · {{ item.capacityCheckedAt }}</span></dd></div>
            <div v-if="item.providerGpuAmount != null"><dt>{{ $tp('实例 GPU 配额') }}</dt><dd>{{ item.providerGpuAmount }} {{ $tp('（非空闲库存）') }}</dd></div>
            <div v-if="item.actualStoppedMode"><dt>{{ $tp('厂商实际停机模式') }}</dt><dd>{{ item.actualStoppedMode }}</dd></div>
            <div v-if="item.taskCount != null"><dt>{{ $tp('预计排队') }}</dt><dd>{{ $tp(item.waitingEstimate || '暂不可估算') }}</dd></div>
            <div v-if="item.billingType === 'POINTS'"><dt>{{ $tp('额度参考值/小时') }}</dt><dd>{{ item.pointsPerHour ?? '—' }}</dd></div>
          </dl>
          <p v-if="item.claimState === 'ISOLATED'">{{ $tp('实例暂不可分配，等待管理员确认。') }}</p>
          <p v-if="item.message">{{ item.message }}</p>
          <div class="detail-actions">
            <el-button v-if="item.canQueryProvider && item.apiControl" link type="primary" :loading="operating === item.id" @click="refresh(item)">{{ $tp('查询厂商状态') }}</el-button>
            <el-button v-if="item.canQueryProvider && item.operationPending && item.apiControl" link type="primary" :loading="operating === item.id" @click="refresh(item, true)">{{ $tp('恢复挂起记录') }}</el-button>
          </div>
        </details>
        <div v-if="item.canClaim || item.claimState === 'MINE' || item.canOperate || item.canRefreshEnvironment || item.canManage" class="card-actions">
          <el-button v-if="item.canClaim" type="primary" :disabled="item.billingType === 'POINTS'" :loading="operating === item.id" @click="claim(item)">{{ $tp('占用整实例') }}</el-button>
          <el-button v-if="item.claimState === 'MINE'" :loading="operating === item.id" @click="release(item)">{{ $tp('释放占用') }}</el-button>
          <el-button v-if="item.canOperate" type="primary" :loading="operating === item.id" :disabled="item.billingType === 'POINTS' || item.operationPending || item.capacityState === 'UNAVAILABLE' || item.provider === 'self' || item.powerState === 'RUNNING'" @click="power(item, 'start')">{{ $tp(item.apiControl ? '启动实例' : '开机指引') }}</el-button>
          <el-button v-if="item.canOperate" :disabled="item.operationPending || item.taskCount > 0 || item.provider === 'self' || item.powerState === 'STOPPED'" :loading="operating === item.id" @click="power(item, 'stop')">{{ $tp(item.apiControl ? '停止实例' : '关机指引') }}</el-button>
          <el-button v-if="item.canRefreshEnvironment || item.canOperate" type="primary" plain :disabled="item.operationPending || item.powerState !== 'RUNNING'" :loading="operating === item.id" @click="health(item)">{{ $tp('刷新环境') }}</el-button>
          <el-button v-if="item.canManage" class="manage-action" link @click="router.push({ path: '/compute-management', query: { ...route.query, nodeId: item.id } })">{{ $tp('管理') }}<el-icon><ArrowRight /></el-icon></el-button>
        </div>
      </article>
    </div>
  </section>
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowRight, Cpu, Refresh } from '@element-plus/icons-vue'
import { listComputeMarket, requestPublicComputePower, claimComputeInstance, releaseComputeClaim, refreshPublicComputeProviderState, refreshComputeHealth } from '@/api/compute'
import { translatePhrase as uiText } from '@/i18n'
import { requireComputePlatformAuthorization, isComputeAuthorizationCurrent, bindComputeDraftToActor } from '@/utils/computePlatformAuth'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
const route = useRoute(), router = useRouter()
const resources = ref([]), loading = ref(false), operating = ref('')
const marketError = ref('')
let marketActive = true, marketRevision = 0, loadEpoch = 0, operationEpoch = 0
let pageIdentity = captureComputeIdentity()
const invalidateMarket = () => {
  marketRevision++; loadEpoch++; operationEpoch++
  resources.value = []; loading.value = false; operating.value = ''; marketError.value = ''
}
const ensureMarketIdentity = () => {
  try { pageIdentity.assertCurrent(); return false } catch {
    invalidateMarket(); pageIdentity = captureComputeIdentity(); return true
  }
}
const currentMarketRequest = (request, epoch) => {
  if (!marketActive || request.revision !== marketRevision || request.epoch !== epoch) return false
  try { request.identity.assertCurrent(); return true } catch { return false }
}
const beginOperation = item => {
  ensureMarketIdentity()
  operating.value = ''
  return { id: item.id, epoch: ++operationEpoch, revision: marketRevision, identity: captureComputeIdentity() }
}
const currentOperation = request => currentMarketRequest(request, operationEpoch)
const providers = { autodl: 'AutoDL', aliyun: '阿里云 EGS', self: '自有服务器' }
const claimLabels = { PENDING_CONFIGURATION: '待配置', FREE: '空闲', MINE: '我的占用', OTHER: '他人占用', ISOLATED: '隔离', SHARED_QUEUE: '免费任务队列' }
const states = { UNKNOWN: '未知', RUNNING: '运行中', STOPPED: '已关机', STARTING: '启动中', STOPPING: '关机中', UNAVAILABLE: '无 GPU 库存', AVAILABLE_HINT: '可尝试启动', READY: '可训练', ERROR: '检查未通过', STALE: '检查已过期', CHECKING: '检查中' }
const statusType = state => ['RUNNING', 'READY'].includes(state) ? 'success' : state === 'ERROR' ? 'danger' : ['STALE', 'STARTING', 'STOPPING', 'CHECKING'].includes(state) ? 'warning' : 'info'
const load = async () => {
  if (!marketActive) return
  ensureMarketIdentity()
  const request = { epoch: ++loadEpoch, revision: marketRevision, identity: captureComputeIdentity() }
  loading.value = true; marketError.value = ''
  try {
    const response = await listComputeMarket()
    if (currentMarketRequest(request, loadEpoch)) resources.value = response.data || []
  } catch {
    if (currentMarketRequest(request, loadEpoch)) marketError.value = uiText('加载失败，请刷新重试')
  } finally { if (currentMarketRequest(request, loadEpoch)) loading.value = false }
}
const runOperation = async (request, execute, title, failure, reloadOnFailure = false) => {
  if (!currentOperation(request)) return
  operating.value = request.id
  try {
    const response = await execute()
    if (!currentOperation(request)) return
    await ElMessageBox.alert(response.data, uiText(title))
    if (currentOperation(request)) await load()
  } catch (error) {
    if (!currentOperation(request)) return
    await ElMessageBox.alert(uiText(error.data?.msg || error.data?.message || failure), uiText(title))
    if (reloadOnFailure && currentOperation(request)) await load()
  } finally { if (currentOperation(request)) operating.value = '' }
}
const power = async (item, action) => {
  const request = beginOperation(item)
  const needsAuthorization = true
  const proof = needsAuthorization ? await requireComputePlatformAuthorization(async context => { bindComputeDraftToActor(context); await router.replace({ query: { ...route.query, nodeId: item.id } }) }) : null
  if (!currentOperation(request) || needsAuthorization && !proof) return
  if (item.apiControl && !await ElMessageBox.confirm(uiText('将向厂商申请实例启停，可能产生费用或启动失败；停止不保证停止计费。请确认已核对实例与停机模式。'), item.name, { type: 'warning' }).then(() => true).catch(() => false)) return
  if (!currentOperation(request) || proof && !isComputeAuthorizationCurrent(proof)) return
  await runOperation(request, () => requestPublicComputePower(item.id, action, proof?.platformToken), '实例操作说明', '实例操作未完成，请刷新状态后重试')
}
const claim = async item => {
  const request = beginOperation(item)
  const proof = await requireComputePlatformAuthorization(context => { bindComputeDraftToActor(context); return router.replace({ query: { ...route.query, nodeId: item.id } }) })
  if (!currentOperation(request) || !proof || !isComputeAuthorizationCurrent(proof)) return
  await runOperation(request, () => claimComputeInstance(item.id, proof.platformToken), '整实例占用', '占用未完成，实例状态可能已变化，请刷新后重试', true)
}
const release = async item => {
  const request = beginOperation(item)
  const proof = await requireComputePlatformAuthorization()
  if (!currentOperation(request) || !proof || !isComputeAuthorizationCurrent(proof)) return
  if (!await ElMessageBox.confirm(uiText('未使用的工作空间可撤销占用；已训练而安全清理未验证的实例将隔离，不会删除现存文件或交给下一用户。'), uiText('安全复用'), { type: 'warning' }).then(() => true).catch(() => false)) return
  if (!currentOperation(request) || !isComputeAuthorizationCurrent(proof)) return
  await runOperation(request, () => releaseComputeClaim(item.id, proof.platformToken), '安全复用', '撤销未完成，请确认任务与回存状态后重试', true)
}
const refresh = async (item, recover = false) => {
  const request = beginOperation(item)
  const proof = await requireComputePlatformAuthorization()
  if (!currentOperation(request) || !proof || !isComputeAuthorizationCurrent(proof)) return
  if (recover && !await ElMessageBox.confirm(uiText('仅在十分钟后，厂商查询明确返回运行或停止终态时，恢复本地挂起记录；不会重发启停，请先核对厂商控制台。'), item.name, { type: 'warning' }).then(() => true).catch(() => false)) return
  if (!currentOperation(request) || !isComputeAuthorizationCurrent(proof)) return
  await runOperation(request, () => refreshPublicComputeProviderState(item.id, recover, proof.platformToken), '厂商状态查询', '厂商状态查询未完成，请刷新后重试', true)
}
const health = async item => {
  const request = beginOperation(item)
  const shared = item.schedulingMode === 'SHARED_GPU_QUEUE'
  const proof = shared ? null : await requireComputePlatformAuthorization()
  if (!currentOperation(request) || !shared && (!proof || !isComputeAuthorizationCurrent(proof))) return
  await runOperation(request, () => refreshComputeHealth(item.id, proof?.platformToken), '环境检查', '检查未完成，请刷新实例状态后重试', true)
}
let timer
const marketIdentityChanged = () => { ensureMarketIdentity(); load() }
const marketStorageChanged = () => { if (ensureMarketIdentity()) load() }
onMounted(() => {
  window.addEventListener('modelHubAuthChanged', marketIdentityChanged)
  window.addEventListener('storage', marketStorageChanged)
  load(); timer = setInterval(() => { const changed = ensureMarketIdentity(); if (changed || !loading.value && !operating.value) load() }, 15000)
})
onUnmounted(() => {
  marketActive = false; invalidateMarket(); clearInterval(timer)
  window.removeEventListener('modelHubAuthChanged', marketIdentityChanged)
  window.removeEventListener('storage', marketStorageChanged)
})
</script>
<style scoped>
.compute-market { padding: 28px; width: 100%; min-width: 0; overflow: auto; box-sizing: border-box; color: var(--el-text-color-primary); }
.heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 24px; }
.heading h2 { margin: 0; font-size: 22px; font-weight: 600; letter-spacing: -.5px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 8px; }
.toolbar .el-button { margin: 0; }
.market-error { margin-bottom: 20px; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 360px), 1fr)); align-items: stretch; gap: 20px; }
.compute-card { display: flex; flex-direction: column; min-width: 0; overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color); box-shadow: 0 3px 14px rgb(25 44 83 / 4%); }
.compute-card.selected { border-color: var(--el-color-primary); box-shadow: 0 0 0 2px var(--el-color-primary-light-8); }
.card-heading { display: flex; align-items: center; gap: 12px; padding: 22px 22px 18px; }
.resource-icon { display: grid; place-items: center; flex: 0 0 44px; height: 44px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); border-radius: 12px; }
.resource-title { flex: 1; min-width: 0; }
.resource-title h3 { margin: 0 0 6px; font-size: 16px; font-weight: 600; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.resource-title > span { font-size: 12px; color: var(--el-text-color-secondary); }
.billing-tag { flex-shrink: 0; max-width: 45%; height: auto; min-height: 24px; padding: 4px 9px; white-space: normal; text-align: center; line-height: 1.4; }
.gpu-spec { display: flex; align-items: baseline; gap: 12px; margin: 0 22px; padding: 15px 16px; background: var(--el-fill-color-lighter); border-radius: 8px; }
.gpu-spec > span { color: var(--el-text-color-secondary); font-size: 11px; letter-spacing: 1px; }
.gpu-spec strong { font-size: 16px; font-weight: 600; overflow-wrap: anywhere; }
.status-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px 16px; padding: 22px; }
.status-item { display: flex; align-items: flex-start; flex-direction: column; gap: 9px; min-width: 0; font-size: 13px; }
.status-item > span:first-child { color: var(--el-text-color-secondary); font-size: 12px; }
.status-item strong { font-weight: 500; line-height: 24px; overflow-wrap: anywhere; }
.status-warning { color: var(--el-color-warning-dark-2); }
.status-note { margin: 0 22px 16px; color: var(--el-color-warning-dark-2); font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; }
.message-preview { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.resource-details { margin: auto 22px 0; border-top: 1px solid var(--el-border-color-extra-light); font-size: 12px; }
.resource-details summary { display: flex; align-items: center; justify-content: space-between; padding: 14px 0; color: var(--el-text-color-secondary); cursor: pointer; list-style: none; }
.resource-details summary::-webkit-details-marker { display: none; }
.resource-details summary:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; border-radius: 4px; }
.resource-details[open] summary .el-icon { transform: rotate(180deg); }
.resource-details dl { margin: 0 0 16px; }
.resource-details dl > div { display: grid; grid-template-columns: minmax(80px, 1fr) minmax(0, 2fr); gap: 12px; margin-bottom: 10px; line-height: 1.6; }
.resource-details dt { color: var(--el-text-color-secondary); }
.resource-details dd { margin: 0; text-align: right; overflow-wrap: anywhere; }
.resource-details p { color: var(--el-text-color-regular); line-height: 1.6; overflow-wrap: anywhere; }
.detail-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.detail-actions:not(:empty) { padding-bottom: 16px; }
.detail-actions .el-button { margin: 0; }
.card-actions { min-height: 65px; box-sizing: border-box; display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding: 16px 22px; border-top: 1px solid var(--el-border-color-extra-light); }
.card-actions .el-button { margin: 0; }
.card-actions .manage-action { margin-left: auto; color: var(--el-text-color-secondary); }
.manage-action .el-icon { margin-left: 4px; }
@media (max-width: 640px) {
  .compute-market { padding: 16px; }
  .heading { margin-bottom: 18px; }
  .heading h2 { font-size: 20px; }
  .cards { gap: 16px; }
  .card-heading { padding: 18px 16px 16px; gap: 10px; }
  .resource-icon { flex-basis: 36px; height: 36px; border-radius: 10px; }
  .resource-title h3 { font-size: 15px; }
  .gpu-spec { margin: 0 16px; }
  .gpu-spec strong { font-size: 14px; }
  .status-grid { padding: 18px 16px; }
  .resource-details { margin: 0 16px; }
  .status-note { margin-inline: 16px; }
  .card-actions { padding: 14px 16px; }
}
</style>
