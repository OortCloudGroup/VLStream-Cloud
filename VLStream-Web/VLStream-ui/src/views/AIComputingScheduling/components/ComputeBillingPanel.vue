<template>
  <section class="billing-panel" aria-label="平台余额与付费实例">
    <div class="account-line">
      <div><strong>平台账户余额</strong><span v-if="account">可用 {{ currency(account.availableQuota) }} {{ account.currency }} · 冻结 {{ currency(account.heldQuota) }} {{ account.currency }}（上次查询 {{ accountCheckedAt }}）</span><span v-else>{{ capabilities.accountAvailable ? '使用当前统一平台账户' : '账户服务暂未开放' }}</span></div>
      <el-button :disabled="!capabilities.accountAvailable" :loading="busy === 'account'" @click="checkAccount">查询余额</el-button>
    </div>
    <el-alert v-if="error" type="warning" :title="error" :closable="false" show-icon />
    <div v-for="run in visibleRuns" :key="run.runId" class="paid-run" :data-run-id="run.runId">
      <div><strong>{{ resourceName(run.nodeId) }}</strong><span>{{ paidRunState(run.state) }}</span><small>已结算 {{ run.chargedQuota }} {{ run.quotaUnit }}<template v-if="run.remainingQuota !== null"> · 待释放/可抵扣 {{ run.remainingQuota }} {{ run.quotaUnit }}</template></small><small v-if="run.archiveRecovery">本次为付费归档恢复，仅取回原任务产物，不重新训练。</small><small v-if="run.archiveRecoveryAvailable">原任务产物仍待归档；恢复需要确认新的报价和冻结额度。原产物保留在原实例，仍受供应商磁盘保留期限限制。</small><small v-if="run.errorCode">状态确认暂未完成，系统会查询原单；请勿重复开机。</small></div>
      <div class="run-actions">
        <el-button v-if="showArchiveRecovery(run)" :disabled="!capabilities.paidAdmissionEnabled" :loading="busy === `archive:${recoverySourceId(run)}`" @click="recoverArchive(run)">{{ recoveryActionLabel(run) }}</el-button>
        <el-button v-if="run.state === 'RUNNING' && !run.archiveRecovery" @click="prepareTraining(run)">检查环境并前往训练</el-button>
        <el-button v-if="paidRunActive(run)" type="danger" plain :loading="busy === run.runId" @click="stop(run)">停止并结算</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getComputeBillingCapabilities, checkComputeBillingAccount, quoteComputePaidRun, quoteComputeArchiveRecovery, listComputePaidRuns, acceptComputePaidRun, stopComputePaidRun } from '@/api/computeBilling'
import { refreshComputeHealth } from '@/api/compute'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { requireComputePlatformAuthorization, isComputeAuthorizationCurrent } from '@/utils/computePlatformAuth'
import { billingCurrency, paidRunState, paidRunActive, paidRunQuoteText } from '@/utils/computeBillingPresentation.mjs'

const props = defineProps({ resources: { type: Array, default: () => [] } })
const emit = defineEmits(['changed'])
const router = useRouter()
const capabilities = ref({ accountAvailable: false, paidAdmissionEnabled: false })
const account = ref(null), runs = ref([]), busy = ref(''), error = ref('')
const recoveryAttempts = ref({})
const accountCheckedAt = ref('')
const visibleRuns = computed(() => runs.value.filter(run => run.state !== 'QUOTED')
  .filter((run, index) => index < 5 || showArchiveRecovery(run) || (run.archiveRecovery && paidRunActive(run))))
const resourceName = id => props.resources.find(item => String(item.id) === id)?.name || '付费实例'
const currency = quota => billingCurrency(quota, account.value.numerator, account.value.denominator)
let active = true, polling = false, timer, revision = 0
let identity = captureComputeIdentity()
const reset = () => { revision++; account.value = null; accountCheckedAt.value = ''; runs.value = []; recoveryAttempts.value = {}; busy.value = ''; error.value = ''; identity = captureComputeIdentity() }
const current = (snapshot, epoch) => {
  if (!active || epoch !== revision) return false
  try { snapshot.assertCurrent(); return true } catch { reset(); return false }
}
const recoverySourceId = run => run.archiveRecoverySourceRunId || run.runId
const recoveryAttempt = run => recoveryAttempts.value[recoverySourceId(run)]
const showArchiveRecovery = run => run.archiveRecoveryAvailable === true
  || ['QUOTED', 'QUOTE_UNKNOWN', 'ACCEPT_UNKNOWN'].includes(recoveryAttempt(run)?.phase)
const recoveryActionLabel = run => ['QUOTE_UNKNOWN', 'ACCEPT_UNKNOWN'].includes(recoveryAttempt(run)?.phase)
  ? '核对归档恢复订单' : recoveryAttempt(run)?.phase === 'QUOTED' ? '继续确认归档恢复' : '恢复归档（付费）'
const observeRecoveryAttempts = rows => {
  for (const [sourceId, attempt] of Object.entries(recoveryAttempts.value)) {
    const matches = rows.filter(run => run.archiveRecovery === true && run.archiveRecoverySourceRunId === sourceId
      && (attempt.quote ? run.runId === attempt.quote.runId : run.state === 'QUOTED' && Number(run.quoteUntilMs) > Date.now()))
    if (matches.length !== 1) continue
    const observed = matches[0]
    if (attempt.phase === 'QUOTE_UNKNOWN' && observed.state === 'QUOTED') {
      recoveryAttempts.value[sourceId] = { phase: 'QUOTED', quote: observed }
    } else if (['ACCEPT_UNKNOWN', 'ACCEPTED', 'OBSERVED'].includes(attempt.phase) && observed.state !== 'QUOTED') {
      recoveryAttempts.value[sourceId] = { phase: 'OBSERVED', quote: observed }
    }
  }
}
const refreshRuns = async () => {
  if (!active || polling) return
  const snapshot = captureComputeIdentity(), epoch = revision
  polling = true
  try {
    const response = await listComputePaidRuns()
    if (current(snapshot, epoch)) { runs.value = response.data || []; observeRecoveryAttempts(runs.value) }
  }
  catch { if (current(snapshot, epoch)) error.value = '计费状态暂未确认，请稍后刷新；不要重复开机。' }
  finally { polling = false }
}
const execute = async (id, action) => {
  if (busy.value) return
  const snapshot = captureComputeIdentity(), epoch = revision
  busy.value = id; error.value = ''
  try {
    const proof = await requireComputePlatformAuthorization()
    if (!proof || !current(snapshot, epoch) || !isComputeAuthorizationCurrent(proof)) return
    await action(proof, () => current(snapshot, epoch) && isComputeAuthorizationCurrent(proof))
    if (current(snapshot, epoch)) { await refreshRuns(); emit('changed') }
  } catch (failure) { if (current(snapshot, epoch)) error.value = failure?.data?.msg || failure?.data?.message || failure?.message || '操作尚未确认，请查看原单状态。' }
  finally { if (current(snapshot, epoch)) busy.value = '' }
}
const checkAccount = () => execute('account', async (proof, valid) => {
  const accepted = await ElMessageBox.confirm('允许 VLS 查询平台余额并按您确认的 GPU 用量扣费。统一平台可能保存此计费授权，可在平台撤销。本次仅查询余额，不开机或扣费。', '平台余额', { confirmButtonText: '同意并查询', cancelButtonText: '取消' }).then(() => true).catch(() => false)
  if (!accepted || !valid()) return
  const response = await checkComputeBillingAccount()
  if (valid()) { account.value = response.data; accountCheckedAt.value = new Date().toLocaleTimeString() }
})
const start = item => execute(String(item.id), async (proof, valid) => {
  if (!capabilities.value.paidAdmissionEnabled || !item.paidAdmissionEnabled) throw new Error('当前实例尚未开放付费开机')
  const quote = (await quoteComputePaidRun(item.id)).data
  if (!valid()) return
  const accepted = await ElMessageBox.confirm(paidRunQuoteText(quote), '确认本次付费开机', { confirmButtonText: '确认报价并开机', cancelButtonText: '取消', type: 'warning' }).then(() => true).catch(() => false)
  if (!accepted || !valid()) return
  await acceptComputePaidRun(quote.runId)
})
const archiveQuoteText = (quote, run) => {
  if (quote?.archiveRecovery !== true || quote.archiveRecoverySourceRunId !== recoverySourceId(run)
    || String(quote.nodeId) !== String(run.nodeId) || !quote.runId || quote.runId === run.runId
    || !Array.isArray(quote.archiveRecoveryJobIds) || !quote.archiveRecoveryJobIds.length
    || quote.archiveRecoveryJobIds.some(id => typeof id !== 'string' || !id.trim())
    || !Number.isSafeInteger(quote.quoteUntilMs) || quote.quoteUntilMs <= Date.now()) throw new Error('归档恢复报价尚未确认或已过期，请核对原订单。')
  // Reuse the existing strict money/rate validation without its normal-training copy.
  paidRunQuoteText(quote)
  return `归档恢复价格 ${quote.hourlyPrice} ${quote.currency}/整实例小时。本次使用新订单、新冻结 ${quote.reserveQuota} ${quote.quotaUnit}（含停止安全预留），不会复用旧订单冻结。开机后仅取回原任务产物，不重新训练；原产物仍受供应商磁盘保留期限限制。运行时限 ${quote.maximumRunSeconds} 秒；归档完成或到期会请求停止，实际确认关机前仍计费，停止与对账后释放未消费余额。允许 VLS 查询余额并按已确认用量扣费；统一平台可能保存此计费授权，可在平台撤销。是否确认报价并开机恢复归档？`
}
const recoverArchive = run => execute(`archive:${recoverySourceId(run)}`, async (proof, valid) => {
  if (!capabilities.value.paidAdmissionEnabled) throw new Error('当前尚未开放付费归档恢复')
  const sourceId = recoverySourceId(run)
  let attempt = recoveryAttempts.value[sourceId]
  if (['QUOTE_UNKNOWN', 'ACCEPT_UNKNOWN', 'ACCEPTED'].includes(attempt?.phase)) {
    await refreshRuns()
    if (valid()) error.value = recoveryAttempts.value[sourceId]?.phase === 'QUOTED'
      ? '已查到原恢复报价，请再次点击并核对报价；尚未开机。'
      : recoveryAttempts.value[sourceId]?.phase === 'OBSERVED' ? '' : '恢复请求结果仍未确认，仅查询原订单；不会自动新建订单或再次开机。'
    return
  }
  if (attempt?.phase === 'OBSERVED' && !['COMPLETED', 'CANCELLED'].includes(attempt.quote.state)) return
  if (attempt?.phase === 'QUOTED' && Number.isSafeInteger(attempt.quote?.quoteUntilMs) && attempt.quote.quoteUntilMs <= Date.now()) attempt = null
  if (!attempt || attempt.phase === 'OBSERVED') {
    if (run.archiveRecoveryAvailable !== true) throw new Error('当前原任务尚不可恢复归档，请刷新订单状态')
    recoveryAttempts.value[sourceId] = { phase: 'QUOTE_UNKNOWN', quote: null }
    let quote
    try { quote = (await quoteComputeArchiveRecovery(run.runId)).data }
    catch { throw new Error('归档恢复报价结果未确认，请先核对原订单；不会自动重复创建。') }
    if (!valid()) return
    // Keep the returned ID even when its fields fail validation; never silently create another order.
    attempt = { phase: 'QUOTED', quote }
    recoveryAttempts.value[sourceId] = attempt
  }
  const quote = attempt.quote
  const message = archiveQuoteText(quote, run)
  const accepted = await ElMessageBox.confirm(message, '确认付费归档恢复', { confirmButtonText: '确认报价并开机恢复', cancelButtonText: '取消', type: 'warning' }).then(() => true).catch(() => false)
  if (!accepted || !valid()) return
  archiveQuoteText(quote, run)
  recoveryAttempts.value[sourceId] = { phase: 'ACCEPT_UNKNOWN', quote }
  try { await acceptComputePaidRun(quote.runId) }
  catch { throw new Error('归档恢复订单受理结果未确认，请核对原订单；不会自动再次提交。') }
  if (valid()) recoveryAttempts.value[sourceId] = { phase: 'ACCEPTED', quote }
})
const stop = run => execute(run.runId, async (proof, valid) => {
  const official = props.resources.some(item => String(item.id) === String(run.nodeId) && item.configurationManaged === true && item.provider === 'autodl' && item.billingType === 'POINTS')
  const message = official || run.archiveRecovery
    ? '将请求停止本次实例。系统确认本轮训练进程退出后限时等待归档；到期保留原磁盘和产物并请求关机，未归档产物仍待明确恢复。按实际确认关机前的运行用量结算，未消费冻结余额在停止和对账后释放；请等待状态完成。'
    : '将结束本实例中您自己的训练任务，等待产物回存后请求停止。按已确认的运行用量结算，未消费冻结余额在停止和对账后释放；请等待状态完成。'
  const accepted = await ElMessageBox.confirm(message, '停止并结算', { confirmButtonText: '停止并结算', cancelButtonText: '继续运行', type: 'warning' }).then(() => true).catch(() => false)
  if (accepted && valid()) await stopComputePaidRun(run.runId)
})
const prepareTraining = run => execute(run.runId, async (proof, valid) => {
  if (run.archiveRecovery) throw new Error('归档恢复订单仅取回原产物，不能启动新训练')
  const response = await refreshComputeHealth(run.nodeId, proof.platformToken)
  if (!valid()) return
  await ElMessageBox.alert(typeof response.data === 'string' ? response.data : '环境检查已提交。请在训练配置中等待实例就绪后开始训练。', '训练环境')
  if (valid()) await router.push({ path: '/algorithm-training', query: { nodeId: run.nodeId } })
})
defineExpose({ start })
onMounted(async () => {
  const epoch = revision
  try {
    const response = await getComputeBillingCapabilities()
    if (current(identity, epoch)) {
      capabilities.value = response.data || capabilities.value
      if (capabilities.value.accountAvailable) await refreshRuns()
    }
  } catch { /* Existing free-resource flow remains available. */ }
  window.addEventListener('modelHubAuthChanged', reset)
  timer = setInterval(() => {
    try { identity.assertCurrent() } catch { reset(); return }
    if (account.value || runs.value.length) refreshRuns()
  }, 10000)
})
onUnmounted(() => { active = false; clearInterval(timer); window.removeEventListener('modelHubAuthChanged', reset) })
</script>

<style scoped>
.billing-panel { max-width: 1200px; margin: 0 auto 16px; border-bottom: 1px solid #e5e7eb; padding-bottom: 16px; }
.account-line, .paid-run { display: flex; gap: 12px; align-items: center; justify-content: space-between; padding: 10px 0; }
.account-line span, .paid-run span, .paid-run small { display: block; margin-top: 5px; color: #606266; }
.paid-run { border-top: 1px solid #eee; margin-top: 8px; }
.run-actions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
@media (max-width: 700px) { .account-line, .paid-run { align-items: flex-start; flex-direction: column; } }
</style>
