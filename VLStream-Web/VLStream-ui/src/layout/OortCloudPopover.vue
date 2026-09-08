<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom-end"
    trigger="hover"
    :width="560"
    :offset="10"
    popper-class="oortcloud-welcome-popper"
    @show="handlePopoverShow"
  >
    <template #reference>
      <button class="oortcloud-entry" :class="{ 'is-logged-in': isLoggedIn }" type="button" aria-label="打开 OortCloud">
        <img class="entry-logo" src="@/assets/img/OortCloud@3x.png" alt="" aria-hidden="true" />
        <span>OortCloud</span>
      </button>
    </template>

    <div v-loading="loading" class="oortcloud-card" :class="{ 'account-view': isLoggedIn }">
      <div class="brand">
        <img class="brand-logo" src="@/assets/img/OortCloud@3x.png" alt="" aria-hidden="true" />
        <div class="brand-copy">
          <div class="brand-name">OortCloud</div>
          <div class="brand-slogan">安全、合规AI能力</div>
        </div>
      </div>

      <el-alert v-if="loadError" class="account-error" :title="loadError" type="warning" :closable="false" show-icon />

      <template v-if="!isLoggedIn">
        <h2>欢迎使用 OortCloud！</h2>
        <p class="description">订阅 OortCloud Token Plan，20元/月起，Qwen，DeepSeek，Kimi，GLM等顶级模型尝鲜，更有OortCodex和DSH For OortCloud Work以及VLStream数据分析生态共享额度，高效开启AI生产力。</p>
        <p class="prompt">开始使用，登录你的 OortCloud 账户。获得强大模型、高质量的工程、成本分析等。</p>
        <el-button class="login-button" type="primary" round @click="handleLogin">登录 OortCloud</el-button>
      </template>

      <template v-else>
        <div class="welcome-row">
          <strong>欢迎</strong>
          <el-avatar :size="36" :src="account.photo">{{ accountInitial }}</el-avatar>
          <strong class="user-name">{{ account.userName || 'OortCloud 用户' }}</strong>
          <span v-if="accountBadge" class="plan-badge">{{ accountBadge }}</span>
        </div>

        <section class="content-section">
          <h3>用量明细</h3>
          <div class="resource-list">
            <article v-for="resource in resources" :key="resource.id" class="content-panel resource-card">
              <div class="resource-heading">{{ resource.title }}</div>
              <p class="resource-description">{{ resource.description }}</p>
              <div class="resource-usage">
                <span><strong>{{ formatCredits(resource.usedCredits) }}</strong> / {{ resource.unlimited ? '无限' : formatCredits(resource.totalCredits) }}<template v-if="!resource.unlimited">（已使用{{ resource.percentage }}%）</template></span>
                <span v-if="!resource.unlimited">剩余 <strong>{{ formatCredits(resource.remainingCredits) }}</strong></span>
              </div>
              <el-progress v-if="!resource.unlimited" :percentage="resource.percentage" :show-text="false" :stroke-width="5" />
              <div v-else class="unlimited-line">当前订阅为无限额度</div>
            </article>

            <article class="content-panel upgrade-card">
              <div class="resource-heading">获取更多Credits</div>
              <p class="resource-description">你可以随时通过升级订阅计划或购买资源包，获取更多Credits</p>
              <el-button type="primary" class="upgrade-button" @click="handleUpgrade">升级至企业版</el-button>
            </article>
          </div>
        </section>

        <section class="content-section records-section">
          <h3>Credits记录</h3>
          <div class="content-panel records-panel">
            <div class="records-toolbar">
              <el-button type="primary" :loading="recordsLoading" @click="loadUsageRecords"><el-icon><Refresh /></el-icon>刷新</el-button>
              <el-date-picker v-model="dateRange" type="daterange" unlink-panels range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" :teleported="false" @change="loadUsageRecords" />
            </div>
            <p class="records-note">当前您已享受到模型的优惠价格。下方明细为按对话合并计费后的汇总数据，具体消耗以此为准。</p>

            <el-table v-if="usageRecords.length" :data="usageRecords" class="records-table" size="small">
              <el-table-column label="时间" min-width="112"><template #default="scope">{{ formatRecordTime(scope.row.created_at) }}</template></el-table-column>
              <el-table-column label="来源" min-width="115" show-overflow-tooltip><template #default="scope">{{ scope.row.content || scope.row.group || '--' }}</template></el-table-column>
              <el-table-column label="模型分级" min-width="90" show-overflow-tooltip><template #default="scope">{{ scope.row.model_name || '--' }}</template></el-table-column>
              <el-table-column label="Credits" min-width="78"><template #default="scope">{{ formatCredits(scope.row.credits) }}</template></el-table-column>
              <el-table-column label="参考费用" min-width="78"><template #default="scope">{{ formatReferenceCost(scope.row) }}</template></el-table-column>
            </el-table>
            <el-empty v-else :image-size="54" description="当前时间范围内暂无 Credits 记录" />
          </div>
        </section>

        <div class="account-actions">
          <el-button class="account-action visit-button" type="primary" round @click="handleVisitOortCloud"><el-icon><Link /></el-icon>访问 OortCloud</el-button>
          <el-button v-if="!usesPlatformSession" class="account-action logout-button" round @click="handleLogout"><el-icon><SwitchButton /></el-icon>退出登录</el-button>
        </div>
        <p v-if="usesPlatformSession" class="records-note">使用当前平台账号，切换或退出请使用右上角账号菜单。</p>
      </template>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Link, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getModelHubUserInfo, logoutModelHubSession } from '@/api/modelHubUser'
import { authorizeOortCloudLlm } from '@/api/llmReview'
import { getPlatformAccessToken } from '@/utils/request'
import { getOortCloudAccountStats, getOortCloudQuotaConfig, getOortCloudSubscriptionPlans, getOortCloudSubscriptions, getOortCloudTokenKey, getOortCloudTokenList, getOortCloudUsageLogs } from '@/api/oortCloudAccount'
import { clearModelHubAuth, getModelHubAccessToken, openOortCloudModelHub, openOortCodexPricing, startModelHubLogin } from '@/utils/modelHubAuth'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { redirectToPlatformLogin } from '@/utils/platformSession'

const props = defineProps({ tenantMode: { type: String, default: 'single' } })
const usesPlatformSession = computed(() => props.tenantMode === 'multi')
const currentSession = () => getOortCloudSession(props.tenantMode)
const isCurrentSession = session => {
  const current = currentSession()
  return current.accessToken === session.accessToken && current.tenantId === session.tenantId
    && current.usesPlatformSession === session.usesPlatformSession
}
let loadedSession = null
const popoverVisible = ref(false)
const authToken = ref('')
const authVerified = ref(false)
const loading = ref(false)
const recordsLoading = ref(false)
const loadError = ref('')
const llmAuthorizationSynced = ref(false)
const subscriptions = ref([])
const plans = ref([])
const usageRecords = ref([])
const creditsPerCny = ref(25)

const today = new Date()
const sevenDaysAgo = new Date(today)
sevenDaysAgo.setDate(today.getDate() - 6)
sevenDaysAgo.setHours(0, 0, 0, 0)
const dateRange = ref([sevenDaysAgo, today])

const account = reactive({ userId: '', userName: '', photo: '', totalCredits: 0, usedCredits: 0, remainingCredits: 0 })
const isLoggedIn = computed(() => authVerified.value)
const accountInitial = computed(() => (account.userName || 'O').slice(0, 1))
const planMap = computed(() => new Map(plans.value.map((plan) => [Number(plan.id), plan])))
const accountBadge = computed(() => {
  if (subscriptions.value.length > 1) return '多订阅'
  if (subscriptions.value.length === 1) return planMap.value.get(Number(subscriptions.value[0].plan_id))?.title || '订阅版'
  return '按量版'
})

const normalizeNumber = (value, fallback = 0) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

const normalizePercentage = (used, total) => {
  const denominator = normalizeNumber(total)
  if (denominator <= 0) return 0
  return Math.min(100, Math.max(0, Math.round(normalizeNumber(used) / denominator * 100)))
}

const resources = computed(() => {
  if (!subscriptions.value.length) {
    return [{ id: 'wallet', title: '按量付费资源', description: '当前账户公共 Credits 钱包', totalCredits: account.totalCredits, usedCredits: account.usedCredits, remainingCredits: account.remainingCredits, unlimited: false, percentage: normalizePercentage(account.usedCredits, account.totalCredits) }]
  }
  return subscriptions.value.map((subscription) => {
    const plan = planMap.value.get(Number(subscription.plan_id))
    const unlimited = normalizeNumber(subscription.amount_total) === 0
    return {
      id: subscription.id,
      title: subscriptions.value.length > 1 ? `${plan?.title || '订阅资源'} · 订阅 #${subscription.id}` : (plan?.title || '订阅版本的资源'),
      description: `当前计划的月度配额与使用情况（有效期：${formatDate(subscription.start_time)} - ${formatDate(subscription.end_time)}）。`,
      totalCredits: subscription.total_credits,
      usedCredits: subscription.used_credits,
      remainingCredits: subscription.remaining_credits,
      unlimited,
      percentage: unlimited ? 0 : normalizePercentage(subscription.used_credits, subscription.total_credits)
    }
  })
})

const formatCredits = (value) => normalizeNumber(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
const formatDate = (timestamp) => {
  const value = normalizeNumber(timestamp)
  return value ? new Date(value * 1000).toLocaleDateString('zh-CN', { year: 'numeric', month: 'numeric', day: 'numeric' }) : '--'
}
const formatRecordTime = (timestamp) => {
  const value = normalizeNumber(timestamp)
  return value ? new Date(value * 1000).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false }) : '--'
}
const formatReferenceCost = (record) => {
  const directAmount = normalizeNumber(record?.amount_cny, NaN)
  const amount = Number.isFinite(directAmount) ? directAmount : normalizeNumber(record?.credits) / Math.max(creditsPerCny.value, 1)
  return `¥${amount.toFixed(2)}`
}
const normalizeSubscriptions = (data) => (Array.isArray(data?.subscriptions) ? data.subscriptions : []).map((item) => item?.subscription || item).filter((item) => item && item.id !== undefined)

const applyUserInfo = (response) => {
  if (response?.code !== 200 || !response?.data) {
    const error = new Error(response?.msg || response?.message || 'OortCloud 登录校验失败')
    error.authenticationCode = response?.code
    throw error
  }
  const outer = response.data
  const user = outer.userInfo || outer.user || outer
  account.userId = user.userId || user.user_id || user.oort_uuid || user.id || ''
  account.userName = user.userName || user.user_name || user.oort_name || user.realName || user.name || ''
  account.photo = user.photo || user.oort_photo || user.picture || user.avatar || ''
}

const isAuthenticationFailure = (error) => {
  const status = Number(error?.response?.status)
  const code = Number(error?.authenticationCode ?? error?.response?.data?.code)
  const message = String(error?.response?.data?.message || error?.response?.data?.msg || error?.message || '')
  return status === 401 || status === 403 || [401, 403, 4004].includes(code) || /access\s*token.*(?:无效|失效)|无效的\s*access\s*token|校验不通过/i.test(message)
}

const resetAccountState = () => {
  authVerified.value = false
  llmAuthorizationSynced.value = false
  Object.assign(account, { userId: '', userName: '', photo: '', totalCredits: 0, usedCredits: 0, remainingCredits: 0 })
  subscriptions.value = []
  plans.value = []
  usageRecords.value = []
  loadError.value = ''
}

const loadUsageRecords = async () => {
  if (!authVerified.value || recordsLoading.value || !dateRange.value?.length) return
  const session = currentSession()
  if (!loadedSession || !isCurrentSession(loadedSession)) return
  recordsLoading.value = true
  try {
    const start = new Date(dateRange.value[0])
    const end = new Date(dateRange.value[1])
    start.setHours(0, 0, 0, 0)
    end.setHours(23, 59, 59, 999)
    const data = await getOortCloudUsageLogs({ p: 1, page_size: 20, type: 2, start_timestamp: Math.floor(start.getTime() / 1000), end_timestamp: Math.floor(end.getTime() / 1000) }, session)
    if (!isCurrentSession(session)) return
    usageRecords.value = Array.isArray(data?.items) ? data.items : []
  } catch (error) {
    if (!isCurrentSession(session)) return
    loadError.value = error?.response?.data?.message || error?.message || 'Credits 记录加载失败'
  } finally {
    recordsLoading.value = false
  }
}

const loadAccount = async () => {
  const session = currentSession()
  if (loading.value) return
  if (!loadedSession || !isCurrentSession(loadedSession)) resetAccountState()
  loadedSession = session
  authToken.value = session.accessToken
  if (!authToken.value) return
  loading.value = true
  loadError.value = ''
  try {
    const userResponse = await getModelHubUserInfo({ desensitize: true }, session)
    if (!isCurrentSession(session)) return
    applyUserInfo(userResponse)
    authVerified.value = true
    const errors = []
    // Viewing a shared platform account must not replace the tenant's LLM API key.
    if (!session.usesPlatformSession && !llmAuthorizationSynced.value) {
      try {
        const tokens = await getOortCloudTokenList(session)
        if (!isCurrentSession(session)) return
        const selectedToken = tokens.find((token) => token.status === 1)
        if (!selectedToken) throw new Error('OortCloud 账户没有启用的 API 令牌')
        const apiKey = await getOortCloudTokenKey(selectedToken.id, session)
        if (!isCurrentSession(session)) return
        await authorizeOortCloudLlm({
          platformUserId: account.userId,
          platformUserName: account.userName,
          apiKey
        })
        llmAuthorizationSynced.value = true
      } catch (error) {
        errors.push(error?.response?.data?.msg || error?.message || '大模型使用资格同步失败')
      }
    }
    const results = await Promise.allSettled([getOortCloudAccountStats(session), getOortCloudSubscriptions(session), getOortCloudSubscriptionPlans(session), getOortCloudQuotaConfig(session)])
    if (!isCurrentSession(session)) return
    const authError = results.find((result) => result.status === 'rejected' && isAuthenticationFailure(result.reason))
    if (authError && !session.usesPlatformSession) throw authError.reason
    if (results[0].status === 'fulfilled') {
      const stats = results[0].value
      account.totalCredits = stats?.total_credits ?? stats?.quota ?? 0
      account.usedCredits = stats?.used_credits ?? stats?.used_quota ?? 0
      account.remainingCredits = stats?.remaining_credits ?? Math.max(0, normalizeNumber(account.totalCredits) - normalizeNumber(account.usedCredits))
    } else errors.push('账户资源加载失败')
    if (results[1].status === 'fulfilled') subscriptions.value = normalizeSubscriptions(results[1].value)
    else errors.push('订阅资源加载失败')
    if (results[2].status === 'fulfilled') plans.value = results[2].value
    else errors.push('套餐信息加载失败')
    if (results[3].status === 'fulfilled') creditsPerCny.value = normalizeNumber(results[3].value?.credits_per_cny, 25)
    else errors.push('Credits 配置加载失败')
    loadError.value = errors.join('，')
    await loadUsageRecords()
  } catch (error) {
    if (!isCurrentSession(session)) return
    if (isAuthenticationFailure(error)) {
      resetAccountState()
      authToken.value = ''
      if (session.usesPlatformSession) {
        redirectToPlatformLogin()
      } else {
        clearModelHubAuth()
        ElMessage.warning('OortCloud 登录已失效，请重新登录')
      }
    } else loadError.value = error?.response?.data?.message || error?.response?.data?.msg || error?.message || 'OortCloud 账户加载失败'
  } finally {
    loading.value = false
    if (!isCurrentSession(session)) await loadAccount()
  }
}

const handlePopoverShow = async () => {
  await loadAccount()
}
const handleAuthChanged = async () => {
  resetAccountState()
  await loadAccount()
}
const handleVisitOortCloud = () => openOortCloudModelHub(getPlatformAccessToken())
const handleUpgrade = () => openOortCodexPricing(getPlatformAccessToken())
const handleLogout = async () => {
  try { await logoutModelHubSession() } catch { ElMessage.warning('OortCloud 远端退出失败，已清理本地登录状态') }
  resetAccountState()
  authToken.value = ''
  ElMessage.success('已退出 OortCloud')
}
const handleLogin = async () => {
  if (usesPlatformSession.value) {
    if (currentSession().accessToken) await loadAccount()
    else redirectToPlatformLogin()
    return
  }
  if (getModelHubAccessToken()) {
    authToken.value = getModelHubAccessToken()
    await loadAccount()
    return
  }
  startModelHubLogin(null, { returnToCurrent: true })
}

onMounted(async () => {
  window.addEventListener('modelHubAuthChanged', handleAuthChanged)
  await loadAccount()
})
watch(() => props.tenantMode, handleAuthChanged)
onBeforeUnmount(() => window.removeEventListener('modelHubAuthChanged', handleAuthChanged))
</script>

<style scoped>
/* Logged-in account dashboard popover. */
.oortcloud-entry { display: inline-flex; align-items: center; gap: 6px; padding: 8px 10px; border: 0; border-radius: 6px; color: #8a94a6; background: transparent; font: inherit; font-size: 14px; cursor: pointer; transition: color 0.2s ease, background-color 0.2s ease; }
.oortcloud-entry.is-logged-in { color: #287cff; }
.oortcloud-entry:hover, .oortcloud-entry:focus-visible { background: rgba(138, 148, 166, 0.1); outline: none; }
.oortcloud-entry.is-logged-in:hover, .oortcloud-entry.is-logged-in:focus-visible { background: rgba(40, 124, 255, 0.08); }
.entry-logo { width: 24px; height: 24px; flex: none; filter: grayscale(1); opacity: 0.62; }
.oortcloud-entry.is-logged-in .entry-logo { filter: none; opacity: 1; }
.oortcloud-card { box-sizing: border-box; max-height: calc(100vh - 82px); overflow-y: auto; padding: 22px 24px; color: #30343b; background: #eef6fd; border-radius: 14px; }
.brand { display: flex; align-items: center; gap: 9px; }
.brand-logo { width: 40px; height: 40px; flex: none; }
.brand-copy { line-height: 1.15; }
.brand-name { margin-bottom: 3px; font-size: 18px; font-weight: 500; }
.brand-slogan { font-size: 12px; }
h2 { margin: 36px 0 20px; font-size: 20px; }
p { margin: 0; }
.description { color: #6b6b6b; font-size: 12px; line-height: 1.65; }
.prompt { margin-top: 20px; font-size: 12px; line-height: 1.65; font-weight: 700; }
.login-button { min-width: 146px; height: 36px; margin-top: 22px; background: #287cff; }
.welcome-row { display: flex; align-items: center; gap: 9px; margin: 26px 0 22px; font-size: 22px; }
.welcome-row :deep(.el-avatar) { border: 2px solid #f9cc27; background: #e9f1fb; color: #287cff; }
.user-name { max-width: 230px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.plan-badge { padding: 4px 10px; border-radius: 5px; color: #287cff; background: #ddecff; font-size: 12px; font-weight: 500; }
.account-error { margin-bottom: 14px; }
.content-section { margin-top: 20px; }
.content-section h3 { margin: 0 0 12px; padding-left: 9px; border-left: 3px solid #287cff; font-size: 17px; line-height: 1; }
.resource-list { display: grid; gap: 12px; }
.content-panel { padding: 17px; border-radius: 8px; background: #fff; box-shadow: 0 2px 8px rgba(62, 91, 126, 0.04); }
.resource-heading { font-size: 16px; font-weight: 500; }
.resource-description { margin-top: 6px; color: #999; font-size: 12px; line-height: 1.5; }
.resource-usage { display: flex; justify-content: space-between; margin: 16px 0 8px; color: #666; font-size: 14px; }
.resource-usage strong { color: #30343b; font-size: 16px; font-weight: 500; }
.resource-card :deep(.el-progress-bar__outer) { background: #f1f2f4; }
.resource-card :deep(.el-progress-bar__inner) { background: #287cff; }
.unlimited-line { margin-top: 14px; color: #287cff; font-size: 13px; }
.upgrade-card { padding-bottom: 16px; }
.upgrade-button { margin-top: 14px; background: #287cff; }
.records-section { margin-top: 24px; }
.records-panel { padding: 16px; }
.records-toolbar { display: flex; align-items: center; gap: 10px; }
.records-toolbar :deep(.el-date-editor) { width: 300px; }
.records-note { margin: 10px 0 12px; color: #aaa; font-size: 12px; line-height: 1.5; }
.records-table { width: 100%; --el-table-border-color: #e7e9ed; --el-table-header-bg-color: #fff; }
.records-table :deep(th.el-table__cell) { color: #68707d; font-weight: 500; }
.records-table :deep(.el-table__inner-wrapper::before) { display: none; }
.records-panel :deep(.el-empty) { padding: 12px 0 2px; }
.account-actions { display: grid; grid-template-columns: 1fr; gap: 12px; width: 360px; margin: 20px auto 0; }
.account-action { width: 100%; height: 40px; margin: 0 !important; font-weight: 600; }
.visit-button { border: none; background: #287cff; }
.logout-button { border: none; color: #3f3f3f; background: #e4e4e4; }
.logout-button:hover { color: #287cff; background: #dce8f7; }
@media (max-width: 900px) { .oortcloud-entry span { display: none; } }
</style>

<style>
.el-popper.el-popover.oortcloud-welcome-popper { padding: 0 !important; overflow: hidden; border: 1px solid #287cff !important; border-radius: 14px !important; background: #eef6fd !important; box-shadow: 0 10px 26px rgba(43, 83, 125, 0.2) !important; }
.el-popper.oortcloud-welcome-popper .el-popper__arrow::before { border-color: #287cff; background: #eef6fd; }
</style>
