<template>
  <div class="right_info">
    <el-tooltip :content="isFullscreen ? text.exitFullscreen : text.fullscreen" placement="bottom">
      <button type="button" class="right_info_nine platform-header-icon" @click="toggleFullscreen">
        <img :src="isFullscreen ? exitFullscreenIcon : fullscreenIcon" alt="">
      </button>
    </el-tooltip>

    <el-badge :hidden="!unreadCount" :value="unreadCount" class="platform-notice-badge">
      <el-tooltip :content="text.messageCenter" placement="bottom">
        <button type="button" class="right_info_nine platform-header-icon platform-notice-icon" @click="openNoticePanel">
          <img :src="noticeIcon" alt="">
        </button>
      </el-tooltip>
    </el-badge>

    <el-dropdown class="lang-switch" trigger="hover" @command="switchLocale">
      <button type="button" class="right_info_nine platform-header-icon">
        <img :src="langIcon" alt="">
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="zh" :disabled="locale === 'zh'">中文</el-dropdown-item>
          <el-dropdown-item command="en" :disabled="locale === 'en'">English</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <span class="right_info_tips">{{ text.welcome }}</span>
    <div class="right_info_imgOut">
      <img class="right_info_img" :src="platformImageUrl(userPhoto) || defaultUserIcon" alt="" @error="useDefaultUserIcon">
    </div>

    <el-popover
      v-model:visible="userVisible"
      placement="bottom"
      :width="pxToVw(280)"
      trigger="hover"
      popper-class="popover_panel_more"
      persistent
    >
      <template #reference>
        <div class="right_name_div" @click="userVisible = !userVisible">
          <span class="right_info_tips">{{ userName }}</span>
          <div class="arrow_icon"><img :src="userVisible ? downArrowIcon : upArrowIcon"></div>
        </div>
      </template>
      <div class="account-panel">
        <div class="account-panel-header">
          <img class="account-avatar" :src="platformImageUrl(userPhoto) || defaultUserIcon" alt="" @error="useDefaultUserIcon">
          <div class="account-panel-user">
            <strong>{{ userName || '-' }}</strong>
            <div class="account-company">
              <span>{{ departmentName }}</span>
              <em :class="{ certified: isRealNamed }">{{ isRealNamed ? text.realNamed : text.notRealNamed }}</em>
            </div>
          </div>
        </div>
        <div v-if="isAdmin || isTenantAdmin" class="account-role-tags">
          <span v-if="isAdmin">{{ text.superAdmin }}</span>
          <span v-if="isTenantAdmin">{{ text.tenantAdmin }}</span>
        </div>
        <div
          class="account-action account-switch-action"
          @mouseenter="showAccountList = true"
          @mouseleave="showAccountList = false"
        >
          <span>{{ text.switchAccount }}</span>
          <el-icon><ArrowRight /></el-icon>
          <div v-if="showAccountList" class="account-list-card">
            <div class="account-list-content">
              <div v-for="(group, groupIndex) in accountGroups" :key="groupIndex">
                <div class="account-credential">{{ credentialText(group) }}</div>
                <button
                  v-for="(account, accountIndex) in group.user_list || []"
                  :key="account.user?.user_id || accountIndex"
                  type="button"
                  class="account-list-item"
                  :class="{ current: account.user?.is_current_login }"
                  :disabled="account.user?.is_current_login"
                  @click.stop="changeAccount(account)"
                >
                  <el-avatar :size="32" :src="account.user?.tenant?.company_logo">
                    {{ accountTenantName(account).slice(0, 1) }}
                  </el-avatar>
                  <span class="account-list-company">
                    <strong>{{ accountTenantName(account) }}</strong>
                    <span class="account-list-user-row">
                      {{ account.user?.user_name }}
                      <em v-if="account.user?.is_tenant_creator">{{ text.creator }}</em>
                      <em v-if="account.user?.is_tenant_admin">{{ text.admin }}</em>
                    </span>
                  </span>
                  <el-icon v-if="account.user?.is_current_login" class="account-current-icon"><Check /></el-icon>
                </button>
              </div>
              <el-empty v-if="!accountGroups.length" :description="text.noOtherAccount" :image-size="50" />
            </div>
            <button type="button" class="account-list-more" @click.stop="loginMoreAccount">
              <span class="account-plus">+</span>
              <span>{{ text.loginMoreAccount }}</span>
            </button>
          </div>
        </div>
        <div class="account-divider" />
        <button type="button" class="account-action account-logout" @click="logout">{{ text.logout }}</button>
      </div>
    </el-popover>

    <el-popover v-model:visible="appsVisible" placement="bottom" :width="pxToVw(380)" trigger="hover" popper-class="popover_panel">
      <template #reference>
        <button type="button" class="right_info_nine platform-header-icon" :aria-label="text.apps"><img :src="nineIcon" alt=""></button>
      </template>
      <PlatformAppsPopover v-if="appsVisible" :user-id="userId" @open-app="openApp" @more-apps="openAppMarket" />
    </el-popover>

    <el-popover v-model:visible="moreVisible" placement="bottom" :width="pxToVw(260)" trigger="hover" popper-class="popover_panel_more">
      <template #reference>
        <button type="button" class="right_info_nine platform-header-icon" :aria-label="text.more"><img :src="nineMoreIcon" alt=""></button>
      </template>
      <PlatformMorePopover
        v-if="moreVisible"
        :labels="text"
        :user-id="userId"
        :tenant-id="tenantId"
        :theme-color="themeColor"
        :predefine-colors="themeColors"
        @change-tenant="openTenantSwitch"
        @change-industry="openIndustrySwitch"
        @privacy="openPrivacy"
        @switch-account="openAccountSwitch"
        @logout="logout"
        @theme-change="applyTheme"
      />
    </el-popover>

    <Teleport to="body">
      <div v-show="noticeVisible" class="platform-message-overlay" @click.self="noticeVisible = false">
        <aside class="platform-message-panel">
          <div class="platform-message-header"><span>{{ text.messageCenter }}</span><button type="button" :aria-label="text.close" @click="noticeVisible = false"><el-icon><Close /></el-icon></button></div>
          <div class="platform-message-content">
            <div class="platform-message-tabs">
              <el-tabs v-model="noticeTab" @tab-change="handleMessageTabChange"><el-tab-pane :label="text.unread" name="unread" /><el-tab-pane :label="text.read" name="read" /></el-tabs>
              <el-button v-if="noticeTab === 'unread' && notices.length" @click="markRead">{{ text.clearUnread }}</el-button>
            </div>
            <div class="platform-message-list">
              <button v-for="item in notices" :key="item.id" type="button" class="platform-message-item" @click="showNoticeDetail(item)">
                <span class="platform-message-item-header"><strong>{{ item.title }}</strong><small>{{ formatMessageTime(item.time) }}</small></span>
                <span class="platform-message-item-content">{{ item.content }}</span>
              </button>
              <el-empty v-if="!notices.length" :description="text.noData" />
            </div>
            <el-pagination v-if="messageTotal > pageSize" v-model:current-page="currentPage" class="platform-message-pagination" background layout="prev, pager, next" :page-size="pageSize" :total="messageTotal" :pager-count="5" @current-change="loadNotices" />
          </div>
        </aside>
      </div>
    </Teleport>

    <el-dialog v-model="noticeDetailVisible" :title="text.messageDetail" width="60%" append-to-body>
      <h3 class="notice-detail-title">{{ activeNotice.title }}</h3>
      <p class="notice-detail-time">{{ formatMessageTime(activeNotice.time) }}</p>
      <div class="notice-detail-content">{{ activeNotice.content }}</div>
    </el-dialog>

    <el-dialog v-model="tenantDialogVisible" :title="text.myOrg" width="50%" top="5vh" append-to-body destroy-on-close :close-on-click-modal="false" class="myOrg">
      <PlatformTenantDialog v-if="tenantDialogVisible" :tenant-id="tenantId" @switch-tenant="requestTenantSwitch" />
    </el-dialog>

    <el-dialog v-model="accountDialogVisible" :title="text.myAccount" width="760px" append-to-body destroy-on-close :close-on-click-modal="false">
      <div class="account-dialog-content">
        <section v-if="currentAccount" class="current-account-section">
          <h3><span />{{ text.currentAccount }}</h3>
          <p>{{ currentCredential }}</p>
          <div class="current-account-card"><el-avatar :size="40" :src="currentAccount.user?.tenant?.company_logo">{{ accountTenantName(currentAccount).slice(0, 1) }}</el-avatar><div><strong>{{ accountTenantName(currentAccount) }}</strong><small>{{ currentAccount.user?.user_name }}</small></div></div>
        </section>
        <section>
          <h3><span />{{ text.switchAccount }}</h3>
          <div v-for="(group, groupIndex) in accountGroups" :key="groupIndex" class="account-dialog-group">
            <p>{{ credentialText(group) }}</p>
            <div class="account-dialog-grid">
              <button v-for="(account, accountIndex) in (group.user_list || []).filter(item => !item.user?.is_current_login)" :key="account.user?.user_id || accountIndex" type="button" @click="changeAccount(account)">
                <el-avatar :size="40" :src="account.user?.tenant?.company_logo">{{ accountTenantName(account).slice(0, 1) }}</el-avatar>
                <span><strong>{{ accountTenantName(account) }}</strong><small>{{ account.user?.user_name }}</small></span>
              </button>
            </div>
          </div>
          <el-empty v-if="!accountGroups.length" :description="text.noOtherAccount" :image-size="60" />
          <button type="button" class="dialog-more-account" @click="loadMoreAccounts">{{ text.loginMoreAccount }}</button>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="industryDialogVisible" :title="text.switchIndustry" width="42%" top="10vh" append-to-body class="switch-industry-dialog">
      <PlatformIndustryDialog v-if="industryDialogVisible" :user-id="userId" :tenant-id="tenantId" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Check, Close } from '@element-plus/icons-vue'
import { getPlatformAccounts, getPlatformHeaderUser, getPlatformMessageInfo, getPlatformMessages, logoutPlatform, markPlatformMessageRead, markPlatformMessagesRead, switchPlatformAccount, verifyPlatformToken } from '@/api/platformHeader'
import { getPlatformAccessToken } from '@/utils/request'
import { openApaasWebPage } from '@/utils/apaasApiBase'
import PlatformAppsPopover from './platform-header/PlatformAppsPopover.vue'
import PlatformMorePopover from './platform-header/PlatformMorePopover.vue'
import PlatformTenantDialog from './platform-header/PlatformTenantDialog.vue'
import PlatformIndustryDialog from './platform-header/PlatformIndustryDialog.vue'
import fullscreenIcon from '@/assets/img/svg/platform-fullscreen.svg'
import exitFullscreenIcon from '@/assets/img/svg/platform-exit-fullscreen.svg'
import noticeIcon from '@/assets/img/svg/platform-notice.svg'
import langIcon from '@/assets/img/svg/platform-lang.svg'
import nineIcon from '@/assets/img/svg/platform-nine.svg'
import nineMoreIcon from '@/assets/img/svg/platform-nine-more.svg'
import upArrowIcon from '@/assets/img/login/up_arrow.png'
import downArrowIcon from '@/assets/img/login/down_arrow.png'
import defaultUserIcon from '@/assets/img/login/icon_user.png'

const props = defineProps({
  fallbackUser: { type: Object, default: () => ({}) },
  fallbackTenant: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['switch-tenant'])
const DEFAULT_PERSONAL_TENANT_ID = '0e391fd7-1033-4f09-88c0-187582fee462'
const locale = ref(localStorage.getItem('language') || 'zh')
const headerUser = ref({})
const accountGroups = ref([])
const notices = ref([])
const unreadCount = ref(0)
const messageTotal = ref(0)
const currentPage = ref(1)
const pageSize = 6
const noticeTab = ref('unread')
const noticeVisible = ref(false)
const noticeDetailVisible = ref(false)
const tenantDialogVisible = ref(false)
const accountDialogVisible = ref(false)
const industryDialogVisible = ref(false)
const activeNotice = ref({})
const userVisible = ref(false)
const appsVisible = ref(false)
const moreVisible = ref(false)
const showAccountList = ref(false)
const isFullscreen = ref(Boolean(document.fullscreenElement))
const themeColors = ['#F05A1F', '#2856A5', '#C3030F', '#000000', '#6E00FE', '#DBA101']
const themeColor = ref(localStorage.getItem('theme') || '#2856A5')

const messages = {
  zh: {
    fullscreen: '全屏', exitFullscreen: '退出全屏', messageCenter: '消息中心', welcome: '欢迎您', realNamed: '已实名', notRealNamed: '未实名', superAdmin: '超级管理员', tenantAdmin: '租户管理员', switchAccount: '切换账号', creator: '创建人', admin: '管理员', noOtherAccount: '暂无其他账号', loginMoreAccount: '登录更多账号', logout: '退出登录', apps: '应用', noApps: '暂无应用', moreApps: '更多应用', more: '更多', currentTenant: '当前租户', privacy: '隐私', feedback: '反馈', theme: '主题', switchTheme: '切换主题色', close: '关闭', unread: '未读', read: '已读', clearUnread: '清除未读', noData: '暂无数据', messageDetail: '消息详情', myOrg: '我的组织', tenant: '租户', currentSelection: '当前选择', noOtherTenant: '暂无其他组织', myAccount: '我的账号', currentAccount: '当前账号', switchIndustry: '切换行业/场景/职能', industry: '行业', scene: '场景', function: '职能', noIndustry: '当前租户未配置相关配置', logoutConfirm: '确定退出登录吗？', prompt: '提示', confirm: '确定', cancel: '取消', operationSuccess: '操作成功', switchSuccess: '切换成功', sessionExpired: '用户信息已失效，请重新登录'
  },
  en: {
    fullscreen: 'Fullscreen', exitFullscreen: 'Exit fullscreen', messageCenter: 'Messages', welcome: 'Welcome', realNamed: 'Verified', notRealNamed: 'Unverified', superAdmin: 'Super admin', tenantAdmin: 'Tenant admin', switchAccount: 'Switch account', creator: 'Creator', admin: 'Admin', noOtherAccount: 'No other accounts', loginMoreAccount: 'Sign in to another account', logout: 'Sign out', apps: 'Apps', noApps: 'No apps', moreApps: 'More apps', more: 'More', currentTenant: 'Current tenant', privacy: 'Privacy', feedback: 'Feedback', theme: 'Theme', switchTheme: 'Switch theme color', close: 'Close', unread: 'Unread', read: 'Read', clearUnread: 'Mark all as read', noData: 'No data', messageDetail: 'Message details', myOrg: 'My organization', tenant: 'Tenant', currentSelection: 'Current', noOtherTenant: 'No other organizations', myAccount: 'My account', currentAccount: 'Current account', switchIndustry: 'Switch industry / scene / function', industry: 'Industry', scene: 'Scene', function: 'Function', noIndustry: 'No related configuration for this tenant', logoutConfirm: 'Are you sure you want to sign out?', prompt: 'Prompt', confirm: 'OK', cancel: 'Cancel', operationSuccess: 'Success', switchSuccess: 'Switched successfully', sessionExpired: 'Your session has expired. Please sign in again.'
  }
}

const text = computed(() => messages[locale.value] || messages.zh)
const userName = computed(() => headerUser.value.oort_name || headerUser.value.user_name || headerUser.value.userName || props.fallbackUser?.userName || props.fallbackUser?.user_name || '')
const userInitial = computed(() => String(userName.value || '用').slice(0, 1))
const userPhoto = computed(() => headerUser.value.oort_photo || headerUser.value.photo || props.fallbackUser?.photo || '')
const departmentName = computed(() => headerUser.value.oort_depname || headerUser.value.dep_name || '')
const identityList = computed(() => headerUser.value.user_ident_info?.user_ident || headerUser.value.user_ident || [])
const realNameIdentity = computed(() => identityList.value.find(item => Number(item.identity_type) === 2))
const isRealNamed = computed(() => Number(realNameIdentity.value?.status ?? headerUser.value.realNameStatus ?? headerUser.value.real_name_status) === 1)
const tenantId = computed(() => headerUser.value.tenant_id || headerUser.value.tenantId || headerUser.value.tenant?.tenant_id || props.fallbackTenant?.id || '')
const userId = computed(() => headerUser.value.user_id || headerUser.value.userId || headerUser.value.id || props.fallbackUser?.userId || '')
const isAdmin = computed(() => Boolean(headerUser.value.isAdmin || headerUser.value.is_admin))
const isTenantAdmin = computed(() => Boolean(headerUser.value.isTenantAdmin || headerUser.value.is_tenant_admin || headerUser.value.user?.is_tenant_admin))
const credentialText = group => group?.credential?.credential || group?.credential || ''
const currentAccount = computed(() => {
  for (const group of accountGroups.value) {
    const account = (group.user_list || []).find(item => item.user?.is_current_login)
    if (account) return account
  }
  return null
})
const currentCredential = computed(() => {
  const group = accountGroups.value.find(item => (item.user_list || []).some(account => account.user?.is_current_login))
  return credentialText(group)
})

const pxToVw = value => String(window.innerWidth * Number(value) / 1920)
const platformOrigin = () => {
  try { return new URL(import.meta.env.VITE_PLATFORM_LOGIN_URL, window.location.origin).origin } catch (error) { return window.location.origin }
}
const platformImageUrl = value => {
  if (!value) return ''
  const imageUrl = String(value)
  const gatewayMatch = imageUrl.match(/\/(?:oort)?wj1\/(.*)$/i)
  if (gatewayMatch) return `${platformOrigin()}/bus/wj1/${gatewayMatch[1]}`
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl
  return new URL(imageUrl, `${platformOrigin()}/`).toString()
}
const useDefaultUserIcon = event => { if (event?.target) event.target.src = defaultUserIcon }

const syncFullscreenState = () => { isFullscreen.value = Boolean(document.fullscreenElement) }
const toggleFullscreen = async() => {
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await document.documentElement.requestFullscreen()
  } catch (error) { console.warn('全屏切换失败', error) }
}
const switchLocale = value => {
  locale.value = value === 'en' ? 'en' : 'zh'
  localStorage.setItem('language', locale.value)
  document.documentElement.lang = locale.value === 'en' ? 'en' : 'zh-CN'
}
const formatMessageTime = value => {
  if (!value) return ''
  const numericValue = Number(value)
  const normalizedValue = Number.isFinite(numericValue) && numericValue > 0 && numericValue < 1e12
    ? numericValue * 1000
    : value
  const date = new Date(normalizedValue)
  if (Number.isNaN(date.getTime())) return value
  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
const loadNotices = async() => {
  try {
    const result = await getPlatformMessages(noticeTab.value === 'read' ? 1 : 0, currentPage.value, pageSize)
    notices.value = result.list || []
    messageTotal.value = result.total || 0
    if (noticeTab.value === 'unread') unreadCount.value = result.count || 0
  } catch (error) {
    notices.value = []
    messageTotal.value = 0
    if (noticeTab.value === 'unread') unreadCount.value = 0
    console.warn('加载平台消息失败', error)
  }
}
const handleMessageTabChange = async() => { currentPage.value = 1; await loadNotices() }
const openNoticePanel = async() => { noticeVisible.value = true; currentPage.value = 1; await loadNotices() }
const showNoticeDetail = async item => {
  activeNotice.value = item || {}
  noticeDetailVisible.value = true
  const [detailResult, statusResult] = await Promise.allSettled([
    getPlatformMessageInfo(item?.id),
    markPlatformMessageRead(item?.id)
  ])
  if (detailResult.status === 'fulfilled') activeNotice.value = { ...activeNotice.value, ...detailResult.value }
  if (statusResult.status === 'fulfilled' && noticeTab.value === 'unread') await loadNotices()
}
const markRead = async() => {
  try {
    const result = await markPlatformMessagesRead()
    if (Number(result?.code ?? 200) !== 200) throw new Error(result?.msg || result?.message || '操作失败')
    await loadNotices()
    ElMessage.success(text.value.operationSuccess)
  } catch (error) { ElMessage.error(error?.message || '操作失败') }
}
const ensurePlatformSession = async() => {
  try {
    const result = await verifyPlatformToken()
    if (Number(result?.code ?? 200) === 200) return true
  } catch (error) { console.warn('平台令牌校验失败', error) }
  ElMessage.warning(text.value.sessionExpired)
  return false
}
const platformQuery = () => `accessToken=${encodeURIComponent(getPlatformAccessToken() || '')}&fromWhere=console_manage`
const openApp = async app => {
  const path = app.apk_url || app.url || app.path
  if (!path || !(await ensurePlatformSession())) return
  sessionStorage.setItem('tempObj', JSON.stringify({ accessToken: getPlatformAccessToken(), fromWhere: 'console_manage' }))
  const query = platformQuery()
  const target = Number(app.open_mod) === 2 ? '_self' : Number(app.open_mod) === 3 ? (app.applabel || '_blank') : '_blank'
  const features = Number(app.open_mod) === 1 ? '' : 'popup,location=no'
  if (/^https?:\/\//i.test(path) || path.startsWith('//')) window.open(`${path}${path.includes('?') ? '&' : '?'}${query}`, target, features)
  else openApaasWebPage(path, query, target, features)
  appsVisible.value = false
}
const openAppMarket = async() => {
  if (!(await ensurePlatformSession())) return
  openApaasWebPage('/app_market/index.html#/moreApp', platformQuery())
  appsVisible.value = false
}
const accountTenantName = account => {
  const tenant = account?.user?.tenant || {}
  if (account?.user?.is_tenant_admin) return tenant.tenant_name || text.value.tenant
  return tenant.tenant_id === DEFAULT_PERSONAL_TENANT_ID ? '平台个人用户' : (tenant.tenant_name || text.value.tenant)
}
const replacePlatformToken = token => {
  sessionStorage.setItem('platformAccessToken', token)
  localStorage.setItem('platformAccessToken', token)
  const nextUrl = new URL(window.location.href)
  nextUrl.searchParams.set('accessToken', token)
  window.location.replace(nextUrl.toString())
}
const changeAccount = async account => {
  if (!account || account.user?.is_current_login) return
  try {
    await ElMessageBox.confirm(`${text.value.switchAccount}: ${account.user?.tenant?.tenant_name || text.value.tenant}`, text.value.prompt, { confirmButtonText: text.value.confirm, cancelButtonText: text.value.cancel, type: 'warning' })
    const result = await switchPlatformAccount(account)
    if (Number(result?.code ?? 200) !== 200) throw new Error(result?.msg || result?.message || '切换账号失败')
    const nextToken = result?.data?.accessToken || result?.data?.access_token || result?.accessToken
    if (!nextToken) throw new Error('平台未返回新账号令牌')
    ElMessage.success(text.value.switchSuccess)
    replacePlatformToken(nextToken)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.message || '切换账号失败')
  }
}
const openTenantSwitch = () => { moreVisible.value = false; tenantDialogVisible.value = true }
const requestTenantSwitch = tenant => { tenantDialogVisible.value = false; emit('switch-tenant', tenant) }
const openAccountSwitch = async() => {
  moreVisible.value = false
  userVisible.value = false
  accountDialogVisible.value = true
  const [accountsResult, userResult] = await Promise.allSettled([getPlatformAccounts(), getPlatformHeaderUser()])
  if (accountsResult.status === 'fulfilled') accountGroups.value = Array.isArray(accountsResult.value) ? accountsResult.value : []
  if (userResult.status === 'fulfilled') headerUser.value = userResult.value || headerUser.value
}
const loadMoreAccounts = async() => {
  try { accountGroups.value = await getPlatformAccounts(1) } catch (error) { console.warn('加载更多平台账号失败', error) }
}
const loginMoreAccount = async() => { userVisible.value = false; accountDialogVisible.value = true; await loadMoreAccounts() }
const openPrivacy = () => window.open(import.meta.env.VITE_PLATFORM_PRIVACY_POLICY_URL || 'https://oortcloudsmart.com/privacypolicy-oort.html', '_blank')
const mixWithWhite = (color, alpha) => {
  const hex = String(color).replace('#', '')
  if (!/^[0-9a-f]{6}$/i.test(hex)) return color
  const channel = index => Math.round(parseInt(hex.slice(index, index + 2), 16) * alpha + 255 * (1 - alpha)).toString(16).padStart(2, '0')
  return `#${channel(0)}${channel(2)}${channel(4)}`
}
const applyTheme = color => {
  themeColor.value = color
  localStorage.setItem('theme', color)
  const root = document.documentElement
  root.style.setProperty('--el-color-primary', color)
  root.style.setProperty('--el-color-primary-light-3', mixWithWhite(color, 0.7))
  root.style.setProperty('--el-color-primary-light-5', mixWithWhite(color, 0.5))
  root.style.setProperty('--el-color-primary-light-8', mixWithWhite(color, 0.05))
  root.style.setProperty('--el-color-primary-light-9', mixWithWhite(color, 0.03))
  root.style.setProperty('--el-color-primary-hb', mixWithWhite(color, 0.1))
  root.style.setProperty('--el-color-primary-hb2', mixWithWhite(color, 0.2))
  root.style.setProperty('--el-menu-hover-bg-color', mixWithWhite(color, 0.04))
}
const openIndustrySwitch = () => { moreVisible.value = false; industryDialogVisible.value = true }
const logout = async() => {
  try {
    await ElMessageBox.confirm(text.value.logoutConfirm, text.value.prompt, { confirmButtonText: text.value.confirm, cancelButtonText: text.value.cancel, type: 'warning' })
    const result = await logoutPlatform()
    if (Number(result?.code ?? 200) !== 200) throw new Error(result?.msg || result?.message || '退出失败')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.message || '退出失败')
    return
  }
  ;['platformAccessToken', 'platformUserInfo', 'accessToken', 'userInfo', 'tenantId'].forEach(key => {
    sessionStorage.removeItem(key)
    localStorage.removeItem(key)
  })
  window.location.href = import.meta.env.VITE_PLATFORM_LOGIN_URL || '/bus/apaas-web/loginPage/index.html'
}

onMounted(async() => {
  document.addEventListener('fullscreenchange', syncFullscreenState)
  document.documentElement.lang = locale.value === 'en' ? 'en' : 'zh-CN'
  applyTheme(themeColor.value)
  try {
    headerUser.value = await getPlatformHeaderUser()
  } catch (error) {
    headerUser.value = {}
    console.warn('加载平台用户信息失败，使用 VLS 当前用户:', error)
  }
  const results = await Promise.allSettled([
    getPlatformAccounts(),
    getPlatformMessages(0, 1, pageSize)
  ])
  if (results[0].status === 'fulfilled') accountGroups.value = Array.isArray(results[0].value) ? results[0].value : []
  if (results[1].status === 'fulfilled') {
    notices.value = results[1].value.list || []
    unreadCount.value = results[1].value.count || 0
    messageTotal.value = results[1].value.total || 0
  }
})
onBeforeUnmount(() => document.removeEventListener('fullscreenchange', syncFullscreenState))
</script>

<style>
.popover_panel { border-radius: var(--common-border-radius) !important; background-color: #edf3f9 !important; }
.popover_panel .el-popper__arrow::before { background-color: #edf3f9 !important; }
.popover_panel_more { padding: 0 !important; border-radius: var(--common-border-radius) !important; background-color: #edf3f9 !important; }
.popover_panel_more .el-popper__arrow::before { background-color: #edf3f9 !important; }
</style>

<style scoped lang="scss">
.right_name_div { display: flex; flex-direction: row; align-items: center; gap: 6px; cursor: pointer; }
.right_info { min-width: 180px; height: 100%; position: relative; display: flex; align-items: center; justify-content: center; color: var(--el-color-primary); }
.right_info_tips { min-width: 48px; max-width: 180px; margin: 0 2px; color: var(--el-color-primary); font-size: 16px; }
.right_info_img { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; }
.right_info_imgOut { margin: 0 16px; display: flex; align-items: center; }
.right_info_nine { width: 24px; height: 24px; margin: 0 12px; border-radius: 0; cursor: pointer; transition: border-radius 0.5s ease; }
.platform-header-icon { padding: 0; display: inline-flex; align-items: center; justify-content: center; border: 0; color: #333; background: transparent;
  img { width: 24px; height: 24px; display: block; }
  &:hover { border-radius: 4px; }
}
.platform-notice-badge { margin: 0 2px; display: inline-flex; align-items: center; .platform-notice-icon { margin: 0; } }
.lang-switch { margin: 0 2px; }
.arrow_icon { width: 12px; height: 12px; img { width: 12px; display: block; } }
.account-panel { width: 100%; padding-top: 20px; border-radius: 8px; background: #edf3f9; }
.account-panel-header { display: flex; align-items: center; padding: 0 24px 10px; }
.account-avatar { width: 56px; height: 56px; margin-right: 16px; border-radius: 50%; background: #e1e1e1; object-fit: cover; }
.account-panel-user { min-width: 0; flex: 1; strong { display: block; color: #222; font-size: 16px; word-break: break-all; } }
.account-company { margin-top: 2px; display: flex; align-items: center; flex-wrap: wrap; color: #555; font-size: 15px; em { margin-left: 4px; padding: 2px 4px; color: #fff; font-size: 12px; font-style: normal; background: #c7cfe2; &.certified { background: #0dcb69; } } }
.account-role-tags { padding: 8px 24px 0; display: flex; gap: 6px; span { padding: 1px 6px; color: var(--el-color-primary); font-size: 10px; border-radius: 4px; background: var(--el-color-primary-hb, #e8efff); } }
.account-action { width: 100%; min-height: 48px; padding: 14px 24px; position: relative; display: flex; align-items: center; border: 0; color: #222; font-size: 16px; text-align: left; background: #edf3f9; cursor: pointer; &:hover { background: #dde3ea; } .el-icon { margin-left: auto; } }
.account-switch-action { margin-top: 4px; }
.account-divider { height: 1px; }
.account-list-card { width: 320px; padding: 10px; position: absolute; top: 0; right: 100%; z-index: 10; display: flex; flex-direction: column; border-radius: 8px; background: #edf3f9; box-shadow: 0 4px 24px rgb(0 0 0 / 12%); }
.account-list-content { max-height: 520px; overflow-y: auto; }
.account-credential { margin-bottom: 10px; color: #575656; font-size: 14px; }
.account-list-item { width: 100%; padding: 12px 24px; display: flex; align-items: center; gap: 12px; border: 0; border-bottom: 1px solid #f0f0f0; color: #222; text-align: left; background: transparent; cursor: pointer; &:hover:not(:disabled) { border-radius: 3px; background: #dde3ea; } &.current { background: #fafafa; cursor: not-allowed; } }
.account-list-company { min-width: 0; display: flex; flex: 1; flex-direction: column; strong { overflow: hidden; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; } }
.account-list-user-row { margin-top: 3px; display: flex; align-items: center; gap: 8px; font-size: 13px; em { padding: 1px 6px; color: var(--el-color-primary); font-size: 10px; font-style: normal; border-radius: 4px; background: var(--el-color-primary-hb, #e8efff); } }
.account-current-icon { color: var(--el-color-primary); }
.account-list-more { width: 100%; padding: 12px 24px; display: flex; align-items: center; gap: 8px; border: 0; color: #1976d2; font-size: 15px; background: transparent; cursor: pointer; &:hover { border-radius: 3px; background: #dde3ea; } }
.account-plus { width: 18px; height: 18px; display: inline-flex; align-items: center; justify-content: center; border: 2px solid currentColor; border-radius: 50%; line-height: 14px; }
.platform-message-overlay { position: fixed; top: 60px; right: 0; z-index: 2100; height: calc(100% - 60px); display: flex; justify-content: flex-end; }
.platform-message-panel { width: 380px; height: 100%; display: flex; flex-direction: column; overflow: hidden; border-radius: 8px 0 0; color: #303133; background: #fff; box-shadow: -2px 0 8px rgb(0 0 0 / 10%); }
.platform-message-header { padding: 15px 20px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee; font-size: 18px; font-weight: 700; background: #f5f5f5; button { padding: 4px; border: 0; background: transparent; cursor: pointer; } }
.platform-message-content { min-height: 0; display: flex; flex: 1; flex-direction: column; }
.platform-message-tabs { padding: 0 10px; display: flex; align-items: center; justify-content: space-between; .el-tabs { flex: 1; } }
.platform-message-list { min-height: 0; padding: 10px; display: flex; flex: 1; flex-direction: column; gap: 15px; overflow-y: auto; }
.platform-message-item { width: 100%; padding: 15px; display: flex; flex-direction: column; gap: 8px; border: 0; border-radius: 8px; color: #333; text-align: left; background: #f9f9f9; cursor: pointer; &:hover { background: #f0f0f0; } }
.platform-message-item-header { display: flex; align-items: center; justify-content: space-between; gap: 10px; strong { overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; } small { flex-shrink: 0; color: #999; font-size: 12px; } }
.platform-message-item-content { overflow: hidden; color: #666; font-size: 14px; line-height: 1.5; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.platform-message-pagination { margin: 16px 0 40px; display: flex; justify-content: center; }
.notice-detail-title { margin: 0 0 8px; }
.notice-detail-time { margin: 0 0 16px; color: #909399; }
.notice-detail-content { line-height: 1.8; white-space: pre-wrap; }
.account-dialog-content h3 { margin: 0 0 16px; display: flex; align-items: center; gap: 8px; color: #191919; font-size: 18px; font-weight: 400; }
.account-dialog-content h3 > span { width: 4px; height: 22px; background: var(--el-color-primary); }
.current-account-section { margin-bottom: 32px; }
.current-account-section > p, .account-dialog-group > p { margin: 0 0 16px; color: #575656; }
.current-account-card { display: flex; align-items: center; gap: 10px; div { display: flex; flex-direction: column; gap: 5px; } small { color: #999; } }
.account-dialog-grid { margin-bottom: 20px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 20px; }
.account-dialog-grid button { min-height: 120px; padding: 20px; display: flex; flex-direction: column; align-items: flex-start; gap: 12px; border: 0; border-radius: 4px; color: #333; background: #fff; box-shadow: 0 0 10px rgb(0 0 0 / 7%); cursor: pointer; &:hover { background: var(--el-color-primary-hb, #e8edff); } > span { display: flex; flex-direction: column; gap: 5px; text-align: left; } small { color: #999; } }
.dialog-more-account { padding: 0; border: 0; color: #1976d2; text-decoration: underline; background: transparent; cursor: pointer; }
</style>
