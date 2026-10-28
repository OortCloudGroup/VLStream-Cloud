<template>
  <section class="billing-panel" aria-label="平台余额与付费实例">
    <div class="account-line">
      <div><strong>平台账户余额</strong><span v-if="account">可用 {{ currency(account.availableQuota) }} {{ account.currency }} · 冻结 {{ currency(account.heldQuota) }} {{ account.currency }}（上次查询 {{ accountCheckedAt }}）</span><span v-else>{{ capabilities.accountAvailable ? '使用当前统一平台账户' : '账户服务暂未开放' }}</span></div>
      <el-button :disabled="!capabilities.accountAvailable" :loading="busy === 'account'" @click="checkAccount">查询余额</el-button>
    </div>
    <el-alert v-if="error" type="warning" :title="error" :closable="false" show-icon />
    <div v-for="run in visibleRuns" :key="run.runId" class="paid-run" :data-run-id="run.runId">
      <div><strong>{{ resourceName(run.nodeId) }}</strong><span>{{ paidRunState(run.state) }}</span><small>已结算 {{ run.chargedQuota }} {{ run.quotaUnit }}<template v-if="run.remainingQuota !== null"> · 待释放/可抵扣 {{ run.remainingQuota }} {{ run.quotaUnit }}</template></small><small v-if="run.errorCode">状态确认暂未完成，系统会查询原单；请勿重复开机。</small></div>
      <div class="run-actions">
        <el-button v-if="run.state === 'RUNNING'" @click="prepareTraining(run)">检查环境并前往训练</el-button>
        <el-button v-if="paidRunActive(run)" type="danger" plain :loading="busy === run.runId" @click="stop(run)">停止并结算</el-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getComputeBillingCapabilities, checkComputeBillingAccount, quoteComputePaidRun, listComputePaidRuns, acceptComputePaidRun, stopComputePaidRun } from '@/api/computeBilling'
import { refreshComputeHealth } from '@/api/compute'
import { captureComputeIdentity } from '@/utils/computeIdentityRequest'
import { requireComputePlatformAuthorization, isComputeAuthorizationCurrent } from '@/utils/computePlatformAuth'
import { billingCurrency, paidRunState, paidRunActive, paidRunQuoteText } from '@/utils/computeBillingPresentation.mjs'

const props = defineProps({ resources: { type: Array, default: () => [] } })
const emit = defineEmits(['changed'])
const router = useRouter()
const capabilities = ref({ accountAvailable: false, paidAdmissionEnabled: false })
const account = ref(null), runs = ref([]), busy = ref(''), error = ref('')
const accountCheckedAt = ref('')
const visibleRuns = computed(() => runs.value.filter(run => run.state !== 'QUOTED').slice(0, 5))
const resourceName = id => props.resources.find(item => String(item.id) === id)?.name || '付费实例'
const currency = quota => billingCurrency(quota, account.value.numerator, account.value.denominator)
let active = true, polling = false, timer, revision = 0
let identity = captureComputeIdentity()
const reset = () => { revision++; account.value = null; accountCheckedAt.value = ''; runs.value = []; busy.value = ''; error.value = ''; identity = captureComputeIdentity() }
const current = (snapshot, epoch) => {
  if (!active || epoch !== revision) return false
  try { snapshot.assertCurrent(); return true } catch { reset(); return false }
}
const refreshRuns = async () => {
  if (!active || polling) return
  const snapshot = captureComputeIdentity(), epoch = revision
  polling = true
  try { const response = await listComputePaidRuns(); if (current(snapshot, epoch)) runs.value = response.data || [] }
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
const stop = run => execute(run.runId, async (proof, valid) => {
  const accepted = await ElMessageBox.confirm('将结束本实例中您自己的训练任务，等待产物回存后请求停止。按已确认的运行用量结算，未消费冻结余额在停止和对账后释放；请等待状态完成。', '停止并结算', { confirmButtonText: '停止并结算', cancelButtonText: '继续运行', type: 'warning' }).then(() => true).catch(() => false)
  if (accepted && valid()) await stopComputePaidRun(run.runId)
})
const prepareTraining = run => execute(run.runId, async (proof, valid) => {
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
