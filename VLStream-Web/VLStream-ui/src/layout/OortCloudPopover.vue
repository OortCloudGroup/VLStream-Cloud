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
    :width="380"
    :offset="10"
    popper-class="oortcloud-welcome-popper"
    @show="handlePopoverShow"
    @hide="stopRefreshTimer"
  >
    <template #reference>
      <button
        class="oortcloud-entry"
        :class="{ 'is-logged-in': isLoggedIn }"
        type="button"
        aria-label="打开 OortCloud"
      >
        <img class="entry-logo" src="@/assets/img/OortCloud@3x.png" alt="" aria-hidden="true" />
        <span>OortCloud</span>
      </button>
    </template>

    <div
      v-loading="loading"
      class="oortcloud-card"
      :class="{ 'account-view': isLoggedIn }"
    >
      <div class="brand">
        <img class="brand-logo" src="@/assets/img/OortCloud@3x.png" alt="" aria-hidden="true" />
        <div class="brand-copy">
          <div class="brand-name">OortCloud</div>
          <div class="brand-slogan">安全 合规AI能力</div>
        </div>
      </div>

      <h2>欢迎使用 OortCloud！</h2>

      <template v-if="!isLoggedIn">
        <p class="description">
          订阅 OortCloud Token Plan，20元/月起，Qwen，DeepSeek，Kimi，GLM等顶级模型尝鲜，更有OortCodex和DSH For OortCloud Work以及VLStream数据分析生态共享额度，高效开启AI生产力。
        </p>

        <p class="prompt">
          开始使用，登录你的 OortCloud 账户。获得强大模型、高质量的工程、成本分析等。
        </p>

        <el-button class="login-button" type="primary" round @click="handleLogin">
          登录 OortCloud
        </el-button>
      </template>

      <template v-else>
        <div class="account-profile">
          <el-avatar :size="38" :src="account.photo">
            {{ account.userName.slice(0, 1) || 'O' }}
          </el-avatar>
          <span>{{ account.userName || 'OortCloud 用户' }}</span>
        </div>

        <el-alert
          v-if="loadError"
          class="account-error"
          :title="loadError"
          type="warning"
          :closable="false"
          show-icon
        />

        <section class="info-panel statistics-panel">
          <div class="panel-title orange-title">账户统计</div>
          <div class="statistics-grid">
            <div class="stat-item">
              <span class="stat-icon"><el-icon><Wallet /></el-icon></span>
              <div><strong>{{ formattedAccountQuota }}</strong><span>当前余额</span></div>
            </div>
            <div class="stat-item">
              <span class="stat-icon"><el-icon><TrendCharts /></el-icon></span>
              <div><strong>{{ formattedAccountUsed }}</strong><span>历史消耗</span></div>
            </div>
            <div class="stat-item">
              <span class="stat-icon"><el-icon><DataAnalysis /></el-icon></span>
              <div><strong>{{ formattedRequestCount }}</strong><span>请求次数</span></div>
            </div>
          </div>
        </section>

        <section class="info-panel token-panel">
          <div class="panel-title token-title">
            <el-icon><Coin /></el-icon>
            <span>令牌信息</span>
          </div>
          <div class="token-summary">
            <div>令牌名称: {{ tokenUsage.name || selectedToken?.name || '--' }}</div>
            <div>密钥金额: {{ formattedTokenUsed }} / {{ formattedTokenQuota }}</div>
            <div>额度: {{ formattedRawUsed }} / {{ formattedRawQuota }}</div>
            <div>剩余额度: {{ formattedTokenAvailable }}（{{ formattedRawAvailable }}）</div>
            <div v-if="tokenUsage.group || selectedToken?.group">
              分组: {{ tokenUsage.group || selectedToken?.group }}
            </div>
          </div>

          <div class="token-divider"></div>

          <div class="panel-title current-token-title">
            <el-icon><Key /></el-icon>
            <span>当前令牌</span>
          </div>
          <div class="current-token-card">
            <span class="current-token-icon"><el-icon><Key /></el-icon></span>
            <div class="current-token-copy">
              <strong>{{ selectedToken?.name || '--' }}</strong>
              <span>{{ selectedToken?.group || 'default' }} · {{ displayedApiKey }}</span>
            </div>
            <div class="connection-state" :class="{ disconnected: !connected }">
              <i></i>{{ connected ? '已连接' : '未连接' }}
            </div>
            <el-button class="icon-button" circle :disabled="!fullApiKey" @click="showFullKey = !showFullKey">
              <el-icon><Hide v-if="showFullKey" /><View v-else /></el-icon>
            </el-button>
            <el-button class="icon-button" circle :disabled="!fullApiKey" @click="copyApiKey">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </section>

        <div class="account-actions">
          <el-button class="account-action visit-button" type="primary" round @click="handleVisitOortCloud">
            <el-icon><Link /></el-icon>
            访问 OortCloud
          </el-button>
          <el-button class="account-action logout-button" round @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </template>
    </div>
  </el-popover>
</template>

<script setup>
import { computed, defineComponent, h, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Coin, CopyDocument, DataAnalysis, Hide, Key, Link, SwitchButton, TrendCharts, View, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getModelHubUserInfo, logoutModelHubSession } from '@/api/modelHubUser'
import { getPlatformAccessToken } from '@/utils/request'
import {
  getOortCloudAccountStats,
  getOortCloudQuotaConfig,
  getOortCloudTokenKey,
  getOortCloudTokenList,
  getOortCloudTokenUsage
} from '@/api/oortCloudAccount'
import {
  clearModelHubAuth,
  getModelHubAccessToken,
  openOortCloudModelHub,
  startModelHubLogin
} from '@/utils/modelHubAuth'

const popoverVisible = ref(false)
const authToken = ref(getModelHubAccessToken())
const authVerified = ref(false)
const loading = ref(false)
const loadError = ref('')
const selectedToken = ref(null)
const fullApiKey = ref('')
const showFullKey = ref(false)
const connected = ref(false)
let refreshTimer = null

const account = reactive({
  userId: '',
  userName: '',
  photo: '',
  quota: null,
  usedQuota: null,
  requestCount: null
})
const quotaConfig = reactive({
  quotaPerUnit: 0,
  displayType: 'USD',
  usdExchangeRate: 1,
  customSymbol: '¤',
  customExchangeRate: 1
})
const tokenUsage = reactive({
  name: '',
  group: '',
  quota: null,
  usedQuota: null,
  availableQuota: null,
  unlimitedQuota: false
})

const isLoggedIn = computed(() => authVerified.value)

const normalizeNumber = (value) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

const formatRawQuota = (value) => {
  const number = normalizeNumber(value)
  return number === null ? '--' : Math.round(number).toLocaleString('zh-CN')
}

const formatQuota = (value) => {
  const number = normalizeNumber(value)
  if (number === null) return '--'
  const perUnit = quotaConfig.quotaPerUnit
  if (!perUnit) return formatRawQuota(number)
  const usd = number / perUnit
  switch (quotaConfig.displayType) {
    case 'CNY':
      return `¥${(usd * quotaConfig.usdExchangeRate).toFixed(2)}`
    case 'TOKENS':
      return formatRawQuota(number)
    case 'CUSTOM':
      return `${quotaConfig.customSymbol}${(usd * quotaConfig.customExchangeRate).toFixed(2)}`
    default:
      return `$${usd.toFixed(2)}`
  }
}

const formattedAccountQuota = computed(() => formatQuota(account.quota))
const formattedAccountUsed = computed(() => formatQuota(account.usedQuota))
const formattedRequestCount = computed(() => {
  const value = normalizeNumber(account.requestCount)
  return value === null ? '--' : Math.round(value).toLocaleString('zh-CN')
})
const formattedTokenQuota = computed(() => tokenUsage.unlimitedQuota ? '∞' : formatQuota(tokenUsage.quota))
const formattedTokenUsed = computed(() => formatQuota(tokenUsage.usedQuota))
const formattedTokenAvailable = computed(() => tokenUsage.unlimitedQuota ? '∞' : formatQuota(tokenUsage.availableQuota))
const formattedRawQuota = computed(() => tokenUsage.unlimitedQuota ? '∞' : formatRawQuota(tokenUsage.quota))
const formattedRawUsed = computed(() => formatRawQuota(tokenUsage.usedQuota))
const formattedRawAvailable = computed(() => tokenUsage.unlimitedQuota ? '∞' : formatRawQuota(tokenUsage.availableQuota))
const displayedApiKey = computed(() => {
  if (!fullApiKey.value) return selectedToken.value?.maskedKey || '--'
  if (showFullKey.value) return fullApiKey.value
  return selectedToken.value?.maskedKey || `${fullApiKey.value.slice(0, 7)}********${fullApiKey.value.slice(-4)}`
})

const OortCloudLogo = defineComponent({
  name: 'OortCloudLogo',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 80 80',
        fill: 'none',
        xmlns: 'http://www.w3.org/2000/svg',
        'aria-hidden': 'true'
      },
      [
        h('path', {
          d: 'M22 32C9.85 32 0 40.95 0 52s9.85 20 22 20c8.27 0 14.22-4.08 18-9.49C43.78 67.92 49.73 72 58 72c12.15 0 22-8.95 22-20s-9.85-20-22-20c-8.27 0-14.22 4.08-18 9.49C36.22 36.08 30.27 32 22 32Zm0 11c6.25 0 9.23 3.74 12.05 9C31.23 57.26 28.25 61 22 61c-5.52 0-10-4.03-10-9s4.48-9 10-9Zm36 0c5.52 0 10 4.03 10 9s-4.48 9-10 9c-6.25 0-9.23-3.74-12.05-9C48.77 46.74 51.75 43 58 43Z',
          fill: 'currentColor'
        }),
        h('path', {
          d: 'M63 2c.76 7.7 4.3 11.24 12 12-7.7.76-11.24 4.3-12 12-.76-7.7-4.3-11.24-12-12 7.7-.76 11.24-4.3 12-12Z',
          fill: 'currentColor'
        }),
        h('path', {
          d: 'M75 22c.38 4.62 2.38 6.62 7 7-4.62.38-6.62 2.38-7 7-.38-4.62-2.38-6.62-7-7 4.62-.38 6.62-2.38 7-7Z',
          fill: 'currentColor'
        })
      ]
    )
  }
})

const parseConnection = (user) => {
  let connection = user?.newapi_channel_conn || user?.newApiConnection || user?.new_api
  if (typeof connection === 'string') {
    try {
      connection = JSON.parse(connection)
    } catch {
      connection = null
    }
  }
  return connection && typeof connection === 'object' ? connection : {}
}

const applyUserInfo = (response) => {
  if (response?.code !== 200 || !response?.data) {
    const error = new Error(response?.msg || response?.message || 'OortCloud 登录校验失败')
    error.authenticationCode = response?.code
    throw error
  }
  const outer = response?.data || {}
  const user = outer.userInfo || outer.user || outer
  account.userId = user.userId || user.user_id || user.oort_uuid || user.id || ''
  account.userName = user.userName || user.user_name || user.oort_name || user.realName || user.name || ''
  account.photo = user.photo || user.oort_photo || user.picture || user.avatar || ''
  return user
}

const applyQuotaConfig = (data) => {
  quotaConfig.quotaPerUnit = normalizeNumber(data?.quota_per_unit) || 0
  quotaConfig.displayType = String(data?.quota_display_type || 'USD').toUpperCase()
  quotaConfig.usdExchangeRate = normalizeNumber(data?.usd_exchange_rate) || 1
  quotaConfig.customSymbol = data?.custom_currency_symbol || '¤'
  quotaConfig.customExchangeRate = normalizeNumber(data?.custom_currency_exchange_rate) || 1
}

const applyTokenUsage = (data) => {
  tokenUsage.name = data?.name || ''
  tokenUsage.group = data?.group || data?.group_name || ''
  tokenUsage.quota = data?.quota ?? data?.total_quota ?? data?.total_granted ?? null
  tokenUsage.usedQuota = data?.used_quota ?? data?.usedQuota ?? data?.total_used ?? null
  tokenUsage.availableQuota = data?.available_quota ?? data?.total_available ?? null
  tokenUsage.unlimitedQuota = data?.unlimited_quota === true
}

const resetAccountState = () => {
  authVerified.value = false
  account.userId = ''
  account.userName = ''
  account.photo = ''
  account.quota = null
  account.usedQuota = null
  account.requestCount = null
  selectedToken.value = null
  fullApiKey.value = ''
  showFullKey.value = false
  connected.value = false
  loadError.value = ''
  applyTokenUsage({})
}

const isAuthenticationFailure = (error) => {
  const status = Number(error?.response?.status)
  const code = Number(error?.authenticationCode ?? error?.response?.data?.code)
  const message = String(
    error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.message ||
    ''
  )
  return status === 401 || status === 403 || [401, 403, 4004].includes(code) || /access\s*token.*(?:无效|失效)|无效的\s*access\s*token|校验不通过/i.test(message)
}

const loadAccount = async () => {
  authToken.value = getModelHubAccessToken()
  if (!authToken.value || loading.value) return

  loading.value = true
  loadError.value = ''
  connected.value = false
  try {
    const userResponse = await getModelHubUserInfo({ accessToken: authToken.value, desensitize: true })
    const user = applyUserInfo(userResponse)
    const tokens = await getOortCloudTokenList()
    authVerified.value = true
    if (!tokens.length) throw new Error('当前用户暂无可用令牌')

    const selectedStorageKey = `oortcloud.newApiTokenId.${account.userId || 'current'}`
    const savedId = Number(localStorage.getItem(selectedStorageKey))
    selectedToken.value = tokens.find((token) => token.id === savedId) || tokens[0]
    localStorage.setItem(selectedStorageKey, String(selectedToken.value.id))
    fullApiKey.value = await getOortCloudTokenKey(selectedToken.value.id)

    const modelBaseUrl = parseConnection(user).url
    const results = await Promise.allSettled([
      getOortCloudAccountStats(),
      getOortCloudQuotaConfig(),
      getOortCloudTokenUsage(fullApiKey.value, modelBaseUrl)
    ])
    const accountAuthError = results
      .slice(0, 2)
      .find((result) => result.status === 'rejected' && isAuthenticationFailure(result.reason))
    if (accountAuthError) throw accountAuthError.reason

    const errors = []
    if (results[0].status === 'fulfilled') {
      const stats = results[0].value
      account.quota = stats?.quota ?? null
      account.usedQuota = stats?.used_quota ?? null
      account.requestCount = stats?.request_count ?? null
    } else {
      errors.push('账户统计加载失败')
    }
    if (results[1].status === 'fulfilled') {
      applyQuotaConfig(results[1].value)
    } else {
      errors.push('额度配置加载失败')
    }
    if (results[2].status === 'fulfilled') {
      applyTokenUsage(results[2].value)
      connected.value = true
    } else {
      errors.push('令牌用量加载失败')
    }
    loadError.value = errors.join('，')
  } catch (error) {
    if (isAuthenticationFailure(error)) {
      clearModelHubAuth()
      stopRefreshTimer()
      resetAccountState()
      authToken.value = ''
      ElMessage.warning('OortCloud 登录已失效，请重新登录')
    } else {
      loadError.value = error?.response?.data?.message || error?.response?.data?.msg || error?.message || 'OortCloud 账户加载失败'
    }
  } finally {
    loading.value = false
  }
}

const stopRefreshTimer = () => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const startRefreshTimer = () => {
  stopRefreshTimer()
  if (isLoggedIn.value) {
    refreshTimer = window.setInterval(loadAccount, 60 * 1000)
  }
}

const handlePopoverShow = async () => {
  authToken.value = getModelHubAccessToken()
  if (authToken.value) await loadAccount()
  startRefreshTimer()
}

const handleAuthChanged = async () => {
  authToken.value = getModelHubAccessToken()
  resetAccountState()
  if (authToken.value) await loadAccount()
}

const copyApiKey = async () => {
  if (!fullApiKey.value) return
  try {
    await navigator.clipboard.writeText(fullApiKey.value)
    ElMessage.success('令牌已复制')
  } catch {
    ElMessage.error('复制失败，请重试')
  }
}

const handleVisitOortCloud = () => {
  openOortCloudModelHub(getPlatformAccessToken())
}

const handleLogout = async () => {
  try {
    await logoutModelHubSession()
  } catch {
    ElMessage.warning('OortCloud 远端退出失败，已清理本地登录状态')
  }
  if (account.userId) {
    localStorage.removeItem(`oortcloud.newApiTokenId.${account.userId}`)
  }
  stopRefreshTimer()
  resetAccountState()
  authToken.value = ''
  ElMessage.success('已退出 OortCloud')
}

const handleLogin = async () => {
  if (getModelHubAccessToken()) {
    authToken.value = getModelHubAccessToken()
    await loadAccount()
    return
  }

  startModelHubLogin(null, { returnToCurrent: true })
}

onMounted(async () => {
  window.addEventListener('modelHubAuthChanged', handleAuthChanged)
  authToken.value = getModelHubAccessToken()
  if (authToken.value) await loadAccount()
})
onBeforeUnmount(() => {
  stopRefreshTimer()
  window.removeEventListener('modelHubAuthChanged', handleAuthChanged)
})
</script>

<style scoped>
.oortcloud-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  color: #8a94a6;
  background: transparent;
  font: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.oortcloud-entry.is-logged-in {
  color: #287cff;
}

.oortcloud-entry:hover,
.oortcloud-entry:focus-visible {
  background: rgba(138, 148, 166, 0.1);
  outline: none;
}

.oortcloud-entry.is-logged-in:hover,
.oortcloud-entry.is-logged-in:focus-visible {
  background: rgba(40, 124, 255, 0.08);
}

.entry-logo {
  width: 24px;
  height: 24px;
  flex: none;
  filter: grayscale(1);
  opacity: 0.62;
  transition: filter 0.2s ease, opacity 0.2s ease;
}

.oortcloud-entry.is-logged-in .entry-logo {
  filter: none;
  opacity: 1;
}

.oortcloud-card {
  box-sizing: border-box;
  max-height: calc(100vh - 82px);
  overflow-y: auto;
  padding: 18px 18px 16px;
  color: #3a3a3a;
  background: #f1f7ff;
  border-radius: 10px;
}

.oortcloud-card.account-view {
  padding: 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 9px;
}

.brand-logo {
  width: 38px;
  height: 38px;
  flex: none;
  color: #287cff;
}

.brand-copy {
  line-height: 1.15;
}

.brand-name {
  margin-bottom: 3px;
  font-size: 18px;
  font-weight: 500;
}

.brand-slogan {
  font-size: 12px;
}

h2 {
  margin: 36px 0 20px;
  font-size: 20px;
  line-height: 1.35;
  font-weight: 700;
}

.account-view h2 {
  margin: 16px 0 10px;
  font-size: 18px;
}

p {
  margin: 0;
}

.description {
  color: #6b6b6b;
  font-size: 12px;
  line-height: 1.65;
}

.prompt {
  margin-top: 20px;
  font-size: 12px;
  line-height: 1.65;
  font-weight: 700;
}

.login-button {
  min-width: 146px;
  height: 36px;
  margin-top: 22px;
  padding: 0 22px;
  border: none;
  background: #287cff;
  font-size: 14px;
}

.account-profile {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 500;
}

.account-profile :deep(.el-avatar) {
  border: 2px solid #f9cc27;
  background: #e9f1fb;
  color: #287cff;
}

.account-error {
  margin-bottom: 10px;
}

.info-panel {
  padding: 11px;
  border: 1px solid #e2ecf8;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 3px 10px rgba(47, 93, 145, 0.05);
}

.statistics-panel {
  margin-bottom: 9px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 9px;
  color: #4a4a4a;
  font-size: 15px;
  font-weight: 600;
}

.orange-title {
  padding-left: 7px;
  border-left: 3px solid #ff6a24;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.stat-item {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 8px 6px;
  border-radius: 9px;
  background: #eef5fb;
}

.stat-item .stat-icon,
.current-token-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin-right: 6px;
  flex: none;
  border-radius: 50%;
  background: #e3ebf3;
  font-size: 15px;
}

.stat-item div {
  min-width: 0;
}

.stat-item strong,
.stat-item span {
  display: block;
}

.stat-item strong {
  overflow: hidden;
  margin-bottom: 3px;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-item div > span {
  color: #747474;
  font-size: 10px;
  white-space: nowrap;
}

.token-panel {
  margin-bottom: 10px;
}

.token-title,
.current-token-title {
  color: #666666;
}

.token-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3px 10px;
  color: #777777;
  font-size: 11px;
  line-height: 1.45;
}

.token-summary div {
  min-width: 0;
  overflow-wrap: anywhere;
}

.token-divider {
  height: 1px;
  margin: 10px 0;
  border-top: 1px dashed #c8cdd3;
}

.current-token-card {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 8px;
  border-radius: 9px;
  background: #eef8ff;
}

.current-token-icon {
  border-radius: 7px;
}

.current-token-copy {
  min-width: 0;
  flex: 1;
}

.current-token-copy strong,
.current-token-copy span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-token-copy strong {
  margin-bottom: 2px;
  font-size: 12px;
}

.current-token-copy span {
  color: #777777;
  font-size: 11px;
}

.connection-state {
  display: flex;
  align-items: center;
  margin: 0 5px;
  flex: none;
  color: #777777;
  font-size: 11px;
}

.connection-state i {
  width: 8px;
  height: 8px;
  margin-right: 5px;
  border-radius: 50%;
  background: #0fc764;
}

.connection-state.disconnected i {
  background: #a8abb2;
}

.icon-button {
  width: 24px;
  min-width: 24px;
  height: 24px;
  margin-left: 4px !important;
  padding: 4px;
  flex: none;
  border-color: #cdd3da;
  background: transparent;
  font-size: 12px;
}

.account-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.account-action {
  width: 100%;
  height: 34px;
  margin: 0 !important;
  font-size: 13px;
  font-weight: 600;
}

.visit-button {
  border: none;
  background: #287cff;
}

.logout-button {
  border: none;
  color: #3f3f3f;
  background: #e4e4e4;
}

.logout-button:hover {
  color: #287cff;
  background: #dce8f7;
}

@media (max-width: 900px) {
  .oortcloud-entry span {
    display: none;
  }
}
</style>

<style>
.el-popper.el-popover.oortcloud-welcome-popper {
  padding: 0 !important;
  overflow: hidden;
  border: 1px solid #dce8f7 !important;
  border-radius: 10px !important;
  background: #f1f7ff !important;
  box-shadow: 0 8px 24px rgba(43, 83, 125, 0.18) !important;
}

.el-popper.oortcloud-welcome-popper .el-popper__arrow::before {
  border-color: #dce8f7;
  background: #f1f7ff;
}
</style>
