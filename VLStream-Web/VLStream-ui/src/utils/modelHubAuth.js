/**
 * Model Hub 登录
 * 1. 跳转平台登录页，拼接 appname + redirect_uri
 * 2. 登录成功后回调到 redirect_uri?accessToken=xxx
 * 3. 本地保存 accessToken，后续调 getUserInfo 展示云平台用户信息
 */

const PLATFORM_BASE_URL = 'https://workup-dev.myoumuamua.com:6433'
const LOGIN_PAGE_PATH = '/bus/apaas-web/loginPage/index.html'
const MODEL_HUB_PAGE_PATH = '/bus/apaas-web/newapi/index.html'
const APP_NAME = 'OortToolKit'
/**
 * 登录回调地址不要带 ?query：
 * 登录页会再拼 ?access_token=xxx，若 redirect_uri 已有 ? 会变成
 * ?tab=user?access_token=xxx（双问号），导致 token 解析失败并反复跳登录。
 */
const DEFAULT_CALLBACK_PATH = '/bus/vls-ui/cloud-platform'

const ACCESS_TOKEN_KEY = 'modelHubAccessToken'
const PENDING_PUBLISH_KEY = 'pendingPublishToModelHub'

/** 构建登录页地址 */
export function buildModelHubLoginUrl(redirectUri) {
  const loginUrl = new URL(`${PLATFORM_BASE_URL}${LOGIN_PAGE_PATH}`)
  loginUrl.searchParams.set('appname', APP_NAME)
  loginUrl.searchParams.set('redirect_uri', redirectUri)
  return loginUrl.toString()
}

/** 登录回调地址：云平台页（不带 query） */
export function getModelHubRedirectUri() {
  const url = new URL(DEFAULT_CALLBACK_PATH, window.location.origin)
  return url.toString()
}

/** 跳转登录页；可传入待发布算法信息，登录回来后继续处理 */
export function startModelHubLogin(pendingPayload) {
  if (pendingPayload) {
    sessionStorage.setItem(PENDING_PUBLISH_KEY, JSON.stringify(pendingPayload))
  }
  window.location.href = buildModelHubLoginUrl(getModelHubRedirectUri())
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
  sessionStorage.setItem('accessToken', accessToken)
  localStorage.setItem('accessToken', accessToken)
}

export function getModelHubAccessToken() {
  return (
    sessionStorage.getItem(ACCESS_TOKEN_KEY) ||
    localStorage.getItem(ACCESS_TOKEN_KEY) ||
    ''
  )
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

/** 打开 Model Hub（带 accessToken） */
export function openModelHubWithToken(accessToken) {
  const token = accessToken || getModelHubAccessToken()
  if (!token) {
    throw new Error('缺少 accessToken，请先登录')
  }
  const websiteUrl = new URL(`${PLATFORM_BASE_URL}${MODEL_HUB_PAGE_PATH}`)
  websiteUrl.searchParams.set('accessToken', token)
  window.open(websiteUrl.toString(), '_blank', 'noopener,noreferrer')
}
