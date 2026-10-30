<template>
  <section class="compute-market" v-loading="loading">
    <h2 class="market-title">{{ marketText('算力市场') }}</h2>
    <ComputeBillingPanel ref="billingPanel" :resources="resources" @changed="load" />
    <div v-if="route.query.returnTo === 'training'" class="return-action"><el-button type="primary" @click="router.push('/algorithm-training')">{{ marketText('返回训练配置') }}</el-button></div>
    <el-alert v-if="marketError" class="market-error" show-icon type="warning" :closable="false" :title="marketError" />
    <el-empty v-if="!loading && !resources.length && !marketError" :description="marketText('暂无已发布算力')" />
    <div class="cards">
      <article v-for="item in pagedResources" :key="item.id" class="compute-card" :class="{ selected: item.id === route.query.nodeId }" :aria-label="marketResourceName(item)" :data-resource-id="item.id">
        <div class="card-heading">
          <div class="resource-title">
            <div class="machine-meta"><span>{{ marketResourceName(item) || marketText('名称未提供') }}</span></div>
            <h3>{{ getMarketSpecifications(item)?.gpuLabel || item.gpuModel || marketText('规格未登记') }}</h3>
          </div>
          <span class="billing-tag" :class="{ free: item.billingType === 'FREE' }">{{ marketText(item.billingType === 'FREE' ? '免费' : item.paidAdmissionEnabled ? '收费' : '收费 · 暂未开放') }}</span>
        </div>
        <div class="resource-body">
          <template v-if="getMarketSpecifications(item)">
            <div class="spec-column"><h4>{{ marketText(getMarketSpecifications(item).shared ? '共享主机配置' : '每GPU分配') }}</h4><p><span>CPU:</span> {{ formatMarketCpu(getMarketSpecifications(item), marketText) }}</p><p><span>{{ marketText('内存') }}:</span> {{ getMarketSpecifications(item).memory }}</p></div>
            <div class="spec-column"><h4>{{ marketText(getMarketSpecifications(item).shared ? '硬盘（文件系统）' : '硬盘') }}</h4><p><span>{{ marketText('系统盘') }}:</span> {{ getMarketSpecifications(item).systemDisk }}</p><p><span>{{ marketText('数据盘') }}:</span> {{ getMarketSpecifications(item).dataDiskShared ? marketText('与系统盘共用') : getMarketSpecifications(item).dataDisk }}</p></div>
            <div class="spec-column"><h4>{{ marketText('其它') }}</h4><p><span>{{ marketText('GPU驱动') }}:</span> {{ getMarketSpecifications(item).driver }}</p><p :title="marketText('驱动报告的CUDA上限，不代表训练环境内置运行时版本')"><span>{{ marketText('CUDA版本') }}:</span> ≤ {{ getMarketSpecifications(item).cudaMax }}</p></div>
          </template>
          <p v-else class="spec-unverified">{{ marketText('硬件规格待核验。') }}</p>
          <div class="resource-price" :class="{ 'price-pending': item.billingType !== 'FREE' && marketPricePresentation(item) === null }">
            <div class="price-line"><strong>{{ item.billingType === 'FREE' ? marketText('免费') : marketPricePresentation(item)?.amount ?? marketText('价格待确认') }}</strong><small v-if="item.billingType !== 'FREE' && marketPricePresentation(item) !== null">{{ marketText(marketPricePresentation(item).unit) }}</small></div>
            <p v-if="!(item.billingType === 'FREE' && item.schedulingMode === 'SHARED_GPU_QUEUE')" class="price-note">{{ marketText(item.billingType === 'FREE' ? '整实例独享' : item.paidAdmissionEnabled ? '确认报价后按用量结算' : marketPricePresentation(item)?.reference ? '参考价 · 磁盘另费' : '收费暂未开放') }}</p>
            <el-button v-if="item.canClaim" type="primary" :disabled="item.billingType === 'POINTS'" :loading="operating === item.id" @click="claim(item)">{{ marketText('占用整实例') }}</el-button>
            <el-button v-else-if="item.billingType === 'POINTS'" type="primary" :disabled="!item.paidAdmissionEnabled || item.operationPending || !['FREE', 'MINE'].includes(item.claimState) || (item.powerState !== 'STOPPED' && !(item.configurationManaged && item.provider === 'autodl' && item.powerState === 'UNKNOWN'))" @click="billingPanel?.start(item)">{{ marketText(item.paidAdmissionEnabled ? '付费开机' : '租赁暂未开放') }}</el-button>
            <el-button v-else-if="item.schedulingMode === 'SHARED_GPU_QUEUE'" type="primary" @click="router.push('/algorithm-training')">{{ marketText('前往训练') }}</el-button>
          </div>
        </div>
        <p v-if="item.operationPending" class="status-note">{{ marketText('实例操作处理中') }}</p>
        <div v-if="item.canQueryProvider && item.apiControl" class="detail-actions">
          <el-button v-if="item.canQueryProvider && item.apiControl" link type="primary" :loading="operating === item.id" @click="refresh(item)">{{ marketText('查询厂商状态') }}</el-button>
          <el-button v-if="item.canQueryProvider && item.operationPending && item.apiControl" link type="primary" :loading="operating === item.id" @click="refresh(item, true)">{{ marketText('恢复挂起记录') }}</el-button>
        </div>
        <div v-if="item.claimState === 'MINE' || item.canOperate || item.canRefreshEnvironment || item.canManage" class="card-actions">
          <el-button v-if="item.claimState === 'MINE'" :loading="operating === item.id" @click="release(item)">{{ marketText('释放占用') }}</el-button>
          <el-button v-if="item.canOperate && item.billingType !== 'POINTS'" type="primary" :loading="operating === item.id" :disabled="item.operationPending || item.capacityState === 'UNAVAILABLE' || item.provider === 'self' || item.powerState === 'RUNNING'" @click="power(item, 'start')">{{ marketText(item.apiControl ? '启动实例' : '开机指引') }}</el-button>
          <el-button v-if="item.canOperate && item.billingType !== 'POINTS'" :disabled="item.operationPending || item.taskCount > 0 || item.provider === 'self' || item.powerState === 'STOPPED'" :loading="operating === item.id" @click="power(item, 'stop')">{{ marketText(item.apiControl ? '停止实例' : '关机指引') }}</el-button>
          <el-button v-if="item.canRefreshEnvironment || item.canOperate" type="primary" plain :disabled="item.operationPending || (item.schedulingMode !== 'SHARED_GPU_QUEUE' && item.powerState !== 'RUNNING')" :loading="operating === item.id" @click="health(item)">{{ marketText('刷新环境') }}</el-button>
          <el-button v-if="item.canManage" class="manage-action" link @click="router.push({ path: '/compute-management', query: { ...route.query, nodeId: item.id } })">{{ marketText('管理') }}<el-icon><ArrowRight /></el-icon></el-button>
        </div>
      </article>
    </div>
    <el-pagination v-if="resources.length > pageSize" class="market-pagination" v-model:current-page="page" :page-size="pageSize" :total="resources.length" layout="prev, pager, next" />
  </section>
</template>
<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { listComputeMarket, requestPublicComputePower, claimComputeInstance, releaseComputeClaim, refreshPublicComputeProviderState, refreshComputeHealth } from '@/api/compute'
import { translatePhrase as uiText, currentLocale } from '@/i18n'
import { requireComputePlatformAuthorization, isComputeAuthorizationCurrent, bindComputeDraftToActor } from '@/utils/computePlatformAuth'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { marketResourceName, marketPricePresentation, marketDisplayResources } from '@/utils/computeMarketPresentation'
import { getMarketSpecifications, formatMarketCpu } from '@/utils/computeMarketSpecifications'
import ComputeBillingPanel from './components/ComputeBillingPanel.vue'
const billingPanel = ref(null)
const marketCopy = {"核": "cores", "共享主机配置": "Shared host", "硬盘（文件系统）": "Storage (filesystem)", "与系统盘共用": "Shared with system disk", "驱动报告的CUDA上限，不代表训练环境内置运行时版本": "Maximum CUDA version reported by the driver, not the training runtime version", "硬件规格待核验。": "Hardware specifications await verification.", "SSH只读核验": "Read-only SSH verification", "用户确认市场规格": "User-confirmed market specifications", "CPU和内存为共享主机可见总量；硬盘为已挂载文件系统总容量，不是剩余容量；未挂载磁盘不计入数据盘。": "CPU and memory are shared host totals. Disk sizes are mounted filesystem capacities, not free space. Unmounted disks are excluded.", "CPU和内存为共享主机可见总量；硬盘为已挂载文件系统总容量，不是剩余容量。": "CPU and memory are shared host totals. Disk sizes are mounted filesystem capacities, not free space.", "此处为确认的市场规格，不代表实例自定义扩容后的文件系统容量。": "Confirmed market specifications, not the instance filesystem after custom expansion.", "全部":"All", "内存":"Memory", "按任务排队":"Task queue", "详情":"Details", "整实例独享":"Exclusive instance", "规格未登记":"Specifications unavailable", "暂无匹配资源":"No matching resources", "GPU数量": "GPU count", "每GPU分配": "Per GPU", "未提供": "Not provided", "名称未提供": "Name unavailable", "可租用至": "Rentable until", "空闲/总量": "Idle / total", "硬盘": "Storage", "系统盘": "System disk", "数据盘": "Data disk", "其它": "Other", "GPU驱动": "GPU driver", "CUDA版本": "CUDA version", "租赁暂未开放": "Renting unavailable", "前往训练": "Go to training", "参考价 · 磁盘另费": "Reference · disk billed separately", "收费 · 暂未开放": "Paid · unavailable"}
const marketText = key => currentLocale.value !== 'zh-CN' && marketCopy[key] ? marketCopy[key] : uiText(key)
const route = useRoute(), router = useRouter()
const resources = ref([]), loading = ref(false), operating = ref('')
const marketError = ref('')
const page = ref(1), pageSize = 20
const pagedResources = computed(() => resources.value.slice((page.value - 1) * pageSize, page.value * pageSize))
watch(() => resources.value.length, length => { page.value = Math.min(page.value, Math.max(1, Math.ceil(length / pageSize))) })
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
const load = async () => {
  if (!marketActive) return
  ensureMarketIdentity()
  const request = { epoch: ++loadEpoch, revision: marketRevision, identity: captureComputeIdentity() }
  loading.value = true; marketError.value = ''
  try {
    const response = await listComputeMarket()
    if (currentMarketRequest(request, loadEpoch)) resources.value = marketDisplayResources(response.data || [])
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

.compute-market { padding: 24px 28px; width: 100%; min-width: 0; box-sizing: border-box; color: #333; background: #fff; font-family: Arial, 'Microsoft YaHei', sans-serif; }
.market-title { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
.return-action { max-width: 1200px; margin: 0 auto 12px; text-align: right; }
.market-error { max-width: 1200px; margin: 0 auto 16px; }
.cards { display: flex; flex-direction: column; gap: 16px; max-width: 1200px; margin: auto; }
.compute-card { position: relative; min-width: 0; border: 1px solid #d8d8dc; border-radius: 3px; background: #fff; overflow: hidden; }
.compute-card.selected { border-color: #3b5ef5; box-shadow: 0 0 0 1px #3b5ef5; }
.card-heading { min-height: 60px; box-sizing: border-box; display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: end; padding: 6px 28px; background: #f2f2f2; }
.resource-title { min-width: 0; padding-right: 24px; }
.machine-meta { color: #999; font-size: 12px; line-height: 18px; overflow-wrap: anywhere; }
.resource-title h3 { margin: 2px 0 0; font-size: 1.125rem; font-weight: 300; line-height: 1.5rem; overflow-wrap: anywhere; }
.billing-tag { align-self: start; padding-top: 7px; color: #ee4a40; font-size: 12px; line-height: 18px; }
.billing-tag.free { color: #369365; }
.resource-body { display: grid; grid-template-columns: 27.7% 27.7% 17.5% 27.1%; min-height: 114px; box-sizing: border-box; padding: 12px 28px 8px; }
.spec-column { min-width: 0; padding-right: 12px; font-size: 14px; line-height: 20px; color: #111; }
.spec-column h4 { margin: 0 0 8px; color: #999; font-size: 14px; line-height: 20px; font-weight: 400; }
.spec-column p { margin: 0 0 8px; overflow-wrap: anywhere; }
.spec-column p > span { color: #666; margin-right: 4px; }
.spec-unverified { grid-column: span 3; margin: 8px 20px 8px 0; color: #999; font-size: 14px; line-height: 24px; }
.resource-price { min-width: 0; align-self: start; }
.price-line { display: flex; align-items: baseline; gap: 2px; flex-wrap: wrap; min-height: 30px; }
.price-line strong { color: #ee493e; font-size: 24px; line-height: 30px; font-weight: 700; overflow-wrap: anywhere; }
.price-line small { font-size: 13px; }
.price-pending strong { font-size: 20px; }
.price-note { margin: 0 0 8px; color: #999; font-size: 12px; line-height: 18px; }
.resource-price .el-button { --el-button-bg-color: #3b5ef5; --el-button-border-color: #3b5ef5; --el-button-hover-bg-color: #284bdf; --el-button-hover-border-color: #284bdf; height: 32px; min-width: 80px; padding: 0 18px; border-radius: 2px; font-size: 14px; }
.resource-price .el-button.is-disabled { --el-button-disabled-bg-color: #9dacf7; --el-button-disabled-border-color: #9dacf7; }
.status-note { margin: 0 28px 8px; color: #a46513; font-size: 12px; }
.detail-actions, .card-actions { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 10px; }
.detail-actions:not(:empty), .card-actions { padding: 0 28px 12px; }
.card-actions .el-button { margin: 0; }
.market-pagination { justify-content: flex-end; max-width: 1200px; margin: 16px auto; }
@media (max-width: 1400px) {
  .resource-body { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 14px 16px; }
  .spec-unverified { grid-column: 1; }
  .resource-price { grid-column: 2; }
}
@media (max-width: 760px) {
  .compute-market { padding: 16px; }
  .card-heading { padding: 10px 16px; }
  .resource-title h3 { font-size: 1rem; font-weight: 300; line-height: 1.375rem; }
  .billing-tag { padding-top: 0; max-width: 90px; text-align: right; }
  .resource-body { padding: 12px 16px 8px; gap: 14px 12px; }
  .spec-column { font-size: 13px; padding: 0; }
  .spec-column h4 { font-size: 13px; }
  .price-line strong { font-size: 23px; }
  .price-pending strong { font-size: 18px; }
  .detail-actions:not(:empty), .card-actions { padding: 0 16px 12px; }
}
@media (max-width: 640px) {
  /* Keep the market usable inside the console's fixed-width navigation shell. */
  :global(.layout-container:has(.compute-market) .layout-header) { height: auto !important; }
  :global(.layout-container:has(.compute-market) .header-content) { flex-wrap: wrap; padding: 10px 16px 0; }
  :global(.layout-container:has(.compute-market) .logo) { min-width: 0; }
  :global(.layout-container:has(.compute-market) .header-right) { margin-left: auto; }
  :global(.layout-container:has(.compute-market) .top-menu) { order: 3; flex-basis: 100%; justify-content: flex-start; overflow-x: auto; }
  :global(.layout-container:has(.compute-market) .top-menu .menu-item) { flex-shrink: 0; white-space: nowrap; height: 44px; }
  :global(.layout-main:has(.compute-market)) { flex-direction: column; gap: 0; }
  :global(.layout-main:has(.compute-market) .layout-sidebar) { width: 100% !important; height: auto !important; margin-top: 0 !important; }
  :global(.layout-main:has(.compute-market) .sidebar-menu) { display: flex; width: 100%; height: auto !important; padding: 8px; box-sizing: border-box; overflow-x: auto; }
  :global(.layout-main:has(.compute-market) .sidebar-menu .el-menu-item) { flex-shrink: 0; }
  :global(.layout-content:has(> .compute-market)) { width: 100%; min-height: 0; margin-top: 0 !important; }


}
</style>
