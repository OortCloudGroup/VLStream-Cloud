<template>
  <div ref="container" class="right-info-sdk-container" />
  <div v-if="switching" class="identity-switch-overlay">{{ translatePhrase('正在切换账号，请稍候') }}</div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { logout as logoutLocal } from '@/api/system/localAuth'
import { getPlatformHeaderUser } from '@/api/platformHeader'
import { currentLocale, setLocale, translatePhrase } from '@/i18n'
import { authManager } from '@/utils/auth'
import { getPlatformAccessToken } from '@/utils/request'
import { isLocalEventScopeActive } from '@/utils/localEventAccess.mjs'

const emit = defineEmits(['ready-change', 'admin-status-change'])
const container = ref(null)
const switching = ref(false)
let bar
let disposed = false
let changingIdentity = false
let token = ''
let timer
let script

function loginAgain() {
  authManager.clearAllTokens()
  authManager.redirectToPlatformLogin()
}

function handleAutomaticExpiry() {
  if (disposed || changingIdentity) return
  if (!isLocalEventScopeActive()) { loginAgain(); return }
  window.clearInterval(timer)
  bar?.destroy()
  bar = undefined
  emit('ready-change', false)
}

async function endLocalSession() {
  const result = await logoutLocal()
  if (result?.code !== undefined && Number(result.code) !== 200) throw new Error('本地会话注销失败')
}

async function handleSwitch(data) {
  if (disposed || changingIdentity) return
  changingIdentity = true
  switching.value = true
  const identity = authManager.getIdentitySnapshot()
  const nextToken = data?.accessToken || data?.access_token
  // The SDK has already switched the platform identity. Do not continue using
  // the old VLS session while the new platform identity is being validated.
  try {
    if (!nextToken) throw new Error('平台未返回新账号令牌')
    await endLocalSession()
    if (disposed) return
    if (!authManager.isIdentityCurrent(identity)) { window.location.reload(); return }
    const user = await authManager.exchangePlatformToken(nextToken)
    if (disposed) return
    if (!user) {
      const current = authManager.getIdentitySnapshot()
      if ((!current.token || current.token === identity.token) && (!current.platformToken || current.platformToken === identity.platformToken)) loginAgain()
      else window.location.reload()
      return
    }
    window.location.reload()
  } catch (error) {
    if (authManager.isIdentityCurrent(identity, true)) {
      ElMessage.error(translatePhrase('账号切换失败，请重新登录'))
      loginAgain()
    }
  }
}

async function handleLogout() {
  if (disposed || changingIdentity) return
  changingIdentity = true
  try { await endLocalSession() } catch { /* The platform logout may already have invalidated its token. */ }
  if (!disposed) loginAgain()
}

function loadSdk(url) {
  if (window.RightInfoSdk?.mountBar) return Promise.resolve(window.RightInfoSdk)
  return new Promise((resolve, reject) => {
    script = document.createElement('script')
    script.src = url
    script.async = true
    script.onload = () => window.RightInfoSdk?.mountBar ? resolve(window.RightInfoSdk) : reject(new Error('SDK 不包含用户信息区'))
    script.onerror = () => reject(new Error('SDK 加载失败'))
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  try {
    const platformBase = new URL(import.meta.env.VITE_PLATFORM_API_BASE || '/', window.location.origin)
    const loginUrl = new URL(import.meta.env.VITE_PLATFORM_LOGIN_URL || '/bus/apaas-web/loginPage/index.html', window.location.origin)
    const sdkUrl = new URL(import.meta.env.VITE_RIGHT_INFO_SDK_URL || '/bus/apaas-web/lib/right-info-sdk.min.js', loginUrl.origin)
    const platformWebBase = `${loginUrl.origin}${loginUrl.pathname.split('/loginPage/')[0]}`.replace(/\/+$/, '')
    token = getPlatformAccessToken()
    if (!token) throw new Error('缺少平台登录状态')
    const sdk = await loadSdk(sdkUrl.href)
    if (disposed) return
    bar = sdk.mountBar(container.value, {
      baseUrl: `${platformBase.href.replace(/\/+$/, '')}/`,
      gateWay: 'bus/',
      frontURLStr: '/bus/apaas-web',
      accessToken: token,
      appID: import.meta.env.VITE_PLATFORM_APP_ID || '',
      secretKey: import.meta.env.VITE_PLATFORM_SECRET_KEY || '',
      requestType: import.meta.env.VITE_PLATFORM_REQUEST_TYPE || 'app',
      appMarketPath: `${platformWebBase}/app_market/index.html#/moreApp`,
      locale: currentLocale.value,
      showTokenPlan: true,
      onLocaleChange: setLocale,
      onLogout: handleLogout,
      onSwitchAccount: handleSwitch,
      onAuthExpired: handleAutomaticExpiry,
      onError: () => { /* SDK presents its own error; retain the host navigation. */ }
    })
    await bar.ready
    if (disposed) return
    emit('ready-change', true)
    getPlatformHeaderUser().then(user => {
      if (!disposed) emit('admin-status-change', Boolean(user?.isAdmin || user?.is_admin))
    }).catch(() => {})
    timer = window.setInterval(async () => {
      if (disposed || changingIdentity) return
      const nextToken = getPlatformAccessToken()
      if (nextToken === token) return
      if (!nextToken) { handleAutomaticExpiry(); return }
      token = nextToken
      try { bar.updateToken(token); await bar.reload() } catch {
        window.clearInterval(timer)
        bar?.destroy()
        bar = undefined
        emit('ready-change', false)
      }
    }, 2000)
  } catch (error) {
    if (!disposed) {
      bar?.destroy()
      bar = undefined
      emit('ready-change', false)
      ElMessage.warning(translatePhrase('用户信息组件加载失败，已使用原有导航'))
    }
  }
})

watch(currentLocale, locale => bar?.updateOptions({ locale }))
onBeforeUnmount(() => {
  disposed = true
  window.clearInterval(timer)
  bar?.destroy()
  script?.remove()
})
</script>

<style scoped>
.right-info-sdk-container { display: flex; align-items: center; }
.identity-switch-overlay { position: fixed; inset: 0; z-index: 99999; display: grid; place-items: center; color: #fff; background: rgb(0 0 0 / 55%); }
</style>
