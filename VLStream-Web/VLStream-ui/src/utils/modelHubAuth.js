/**
 * Model Hub 登录
 * 1. 跳转平台登录页，拼接 appname + redirect_uri
 * 2. 登录成功后回调到 redirect_uri?accessToken=xxx
 * 3. 本地保存 accessToken，后续调 getUserInfo 展示云平台用户信息
 */

const PLATFORM_BASE_URL = 'https://workup-dev.myoumuamua.com:6433'
const PLATFORM_LOGIN_URL = import.meta.env.VITE_PLATFORM_LOGIN_URL || `${PLATFORM_BASE_URL}/bus/apaas-web/loginPage/index.html`
const OORTCLOUD_MODEL_HUB_URL = import.meta.env.VITE_OORTCLOUD_MODEL_HUB_URL || 'https://vls.oortcloudsmart.com/zh/ModelHub/ModelHub'
const APP_NAME = import.meta.env.VITE_PLATFORM_APP_NAME || 'VLStream'
/**
 * 登录回调地址不要带 ?query：
 * 登录页会再拼 ?access_token=xxx，若 redirect_uri 已有 ? 会变成
 * ?tab=user?access_token=xxx（双问号），导致 token 解析失败并反复跳登录。
 */
const DEFAULT_CALLBACK_PATH = '/bus/vls-ui/cloud-platform'

const ACCESS_TOKEN_KEY = 'modelHubAccessToken'
const PENDING_PUBLISH_KEY = 'pendingPublishToModelHub'
const AUTH_PENDING_KEY = 'modelHubAuthPendingAt'
const RETURN_LOCATION_KEY = 'modelHubReturnLocation'
const AUTH_CALLBACK_PATH_KEY = 'modelHubAuthCallbackPath'
const AUTH_PENDING_MAX_AGE = 30 * 60 * 1000

/** 构建登录页地址 */
export function buildModelHubLoginUrl(redirectUri) {
  const loginUrl = new URL(PLATFORM_LOGIN_URL)
  loginUrl.searchParams.set('appname', APP_NAME)
  loginUrl.searchParams.set('redirect_uri', redirectUri)
  if (!loginUrl.hash) loginUrl.hash = '/'
  return loginUrl.toString()
}

/** 登录回调地址：云平台页（不带 query） */
export function getModelHubRedirectUri() {
  const url = new URL(DEFAULT_CALLBACK_PATH, window.location.origin)
  return url.toString()
}

/** OortCloud 顶栏登录直接回调当前 VLS 业务页，不经过云平台用户信息页。 */
function getCurrentModelHubRedirectUri() {
  const currentUrl = new URL(window.location.href)
  const appBasePath = new URL(import.meta.env.BASE_URL, window.location.origin).pathname
  if (currentUrl.origin === window.location.origin && currentUrl.pathname.startsWith(appBasePath)) {
    return new URL(currentUrl.pathname, window.location.origin).toString()
  }
  return getModelHubRedirectUri()
}

/** 跳转登录页；可传入待发布算法信息，登录回来后继续处理 */
export function startModelHubLogin(pendingPayload, options = {}) {
  if (pendingPayload) {
    sessionStorage.setItem(PENDING_PUBLISH_KEY, JSON.stringify(pendingPayload))
  }
  sessionStorage.setItem(AUTH_PENDING_KEY, String(Date.now()))
  sessionStorage.setItem(
    RETURN_LOCATION_KEY,
    `${window.location.pathname}${window.location.search}${window.location.hash}`
  )
  const redirectUri = options.returnToCurrent ? getCurrentModelHubRedirectUri() : getModelHubRedirectUri()
  sessionStorage.setItem(AUTH_CALLBACK_PATH_KEY, new URL(redirectUri).pathname)
  window.location.href = buildModelHubLoginUrl(redirectUri)
}

/** 云平台用户信息页路径（供路由跳转） */
export function getCloudPlatformUserPath() {
  return { path: '/cloud-platform', query: { tab: 'user' } }
}

/**
 * 兼容登录页回调的异常 URL：
 * - 正常：?access_token=xxx&tenant_id=yyy
 * - 异常：?tab=user?access_token=xxx&tenant_id=yyy（双问号）
 */
function extractCallbackParams(href) {
  const result = {
    accessToken: '',
    tenantId: ''
  }

  try {
    const url = new URL(href)
    result.accessToken =
      url.searchParams.get('accessToken') ||
      url.searchParams.get('access_token') ||
      ''
    result.tenantId =
      url.searchParams.get('tenantId') ||
      url.searchParams.get('tenant_id') ||
      ''
  } catch {
    // ignore
  }

  if (result.accessToken) {
    return result
  }

  // 兜底：从整段 href 用正则抠出 token（兼容双 ?）
  const tokenMatch = href.match(/[?&#](?:accessToken|access_token)=([^&#]+)/i)
  if (tokenMatch?.[1]) {
    result.accessToken = decodeURIComponent(tokenMatch[1])
  }
  const tenantMatch = href.match(/[?&#](?:tenantId|tenant_id)=([^&#]+)/i)
  if (tenantMatch?.[1]) {
    result.tenantId = decodeURIComponent(tenantMatch[1])
  }

  return result
}

function clearPendingModelHubAuth() {
  sessionStorage.removeItem(AUTH_PENDING_KEY)
  sessionStorage.removeItem(RETURN_LOCATION_KEY)
  sessionStorage.removeItem(AUTH_CALLBACK_PATH_KEY)
}

function isPendingModelHubCallback() {
  const callbackPath = sessionStorage.getItem(AUTH_CALLBACK_PATH_KEY) || new URL(DEFAULT_CALLBACK_PATH, window.location.origin).pathname
  if (window.location.pathname !== callbackPath) {
    return false
  }

  const pendingAt = Number(sessionStorage.getItem(AUTH_PENDING_KEY))
  if (!pendingAt || Date.now() - pendingAt > AUTH_PENDING_MAX_AGE) {
    clearPendingModelHubAuth()
    return false
  }
  return true
}

function getSafeReturnLocation() {
  const fallback = DEFAULT_CALLBACK_PATH
  const rawLocation = sessionStorage.getItem(RETURN_LOCATION_KEY)
  if (!rawLocation) {
    return fallback
  }

  try {
    const returnUrl = new URL(rawLocation, window.location.origin)
    const appBasePath = new URL(import.meta.env.BASE_URL, window.location.origin).pathname
    if (
      returnUrl.origin === window.location.origin &&
      returnUrl.pathname.startsWith(appBasePath)
    ) {
      return `${returnUrl.pathname}${returnUrl.search}${returnUrl.hash}`
    }
  } catch {
    // ignore invalid or unsafe return location
  }
  return fallback
}

/**
 * 在 Vue Router 启动前接管本次 OortCloud 授权回调。
 * 只处理由 startModelHubLogin 发起且仍在有效期内的回调，避免把 VLStream
 * 自身登录回调里的同名 accessToken 当成 OortCloud Token。
 */
export function capturePendingModelHubCallback() {
  if (!isPendingModelHubCallback()) {
    return null
  }

  const { accessToken, tenantId } = extractCallbackParams(window.location.href)
  if (!accessToken) {
    return null
  }

  const returnLocation = getSafeReturnLocation()
  saveModelHubAccessToken(accessToken)
  if (tenantId) {
    sessionStorage.setItem('modelHubTenantId', tenantId)
    localStorage.setItem('modelHubTenantId', tenantId)
  }
  clearPendingModelHubAuth()

  // 路由守卫运行前恢复发起登录的业务页，同时移除回调 Token。
  window.history.replaceState({}, '', returnLocation)
  return accessToken
}

/** 从 URL 回调中解析并保存 accessToken */
export function captureModelHubTokenFromUrl() {
  const href = window.location.href
  const { accessToken, tenantId } = extractCallbackParams(href)

  if (!accessToken) {
    return null
  }

  saveModelHubAccessToken(accessToken)
  if (tenantId) {
    sessionStorage.setItem('modelHubTenantId', tenantId)
    localStorage.setItem('modelHubTenantId', tenantId)
  }

  // 清掉回调参数，恢复成干净的云平台地址，并带上 tab=user
  const cleanUrl = new URL('/bus/vls-ui/cloud-platform', window.location.origin)
  cleanUrl.searchParams.set('tab', 'user')
  window.history.replaceState({}, '', cleanUrl.toString())

  return accessToken
}

export function saveModelHubAccessToken(accessToken) {
  if (!accessToken) return
  sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  window.dispatchEvent(new CustomEvent('modelHubAuthChanged'))
}

export function getModelHubAccessToken() {
  return (
    sessionStorage.getItem(ACCESS_TOKEN_KEY) ||
    localStorage.getItem(ACCESS_TOKEN_KEY) ||
    ''
  )
}

/** 只清理 OortCloud 登录态，保留 VLStream 主系统会话。 */
export function clearModelHubAuth() {
  ;[sessionStorage, localStorage].forEach((storage) => {
    storage.removeItem(ACCESS_TOKEN_KEY)
    storage.removeItem('modelHubTenantId')
  })
  sessionStorage.removeItem(PENDING_PUBLISH_KEY)
  clearPendingModelHubAuth()
  window.dispatchEvent(new CustomEvent('modelHubAuthChanged'))
}

export function getPendingModelHubPublish() {
  try {
    const raw = sessionStorage.getItem(PENDING_PUBLISH_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function clearPendingModelHubPublish() {
  sessionStorage.removeItem(PENDING_PUBLISH_KEY)
}

/** 打开公开 Model Hub。目标站不接收外部 Token，禁止将凭证放入 URL。 */
export function openOortCloudModelHub() {
  window.open(OORTCLOUD_MODEL_HUB_URL, '_blank', 'noopener,noreferrer')
}
