import { sessionValue } from './requestIdentity.mjs'
import { appLoginUrl } from './authRedirect.mjs'

let revision = 0
let redirecting = false
const normalize = value => String(value || '').replace(/^Bearer\s+/i, '')
const authKeys = ['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo',
  'platformAccessToken', 'wvpAccessToken', 'tenantId', 'tenant_id', 'apaas_token', 'refresh_token', 'tempObj']

export const getIdentityRevision = () => revision
export const advanceIdentityRevision = () => { revision++; redirecting = false }
export const isSessionRedirecting = () => redirecting
export const isSessionExpiryError = error => error?.code === 'VLS_SESSION_EXPIRED'
export const currentRequestIdentity = () => ({
  revision,
  token: normalize(sessionValue(['accessToken', 'token'])),
  platformToken: normalize(sessionValue(['platformAccessToken'])),
  tenantId: sessionValue(['tenantId', 'tenant_id'])
})
export const captureRequestIdentity = config => { config.vlsIdentity = currentRequestIdentity(); return config }

export function isLoginFailureResponse(response) {
  if (!response || [403, 404].includes(response.status) || response.status >= 500) return false
  const code = Number(response.data?.code)
  if (code >= 500 && code < 600) return false
  // 403 always remains a permission failure, including an expired-token-looking message.
  if (code === 403) return false
  const message = String(response.data?.msg || response.data?.message || '').toLowerCase().trim()
  return response.status === 401 || code === 401 || code === 4004 || code === 4444
    || ['无效的accesstoken', 'accesstoken无效', 'token失效'].includes(message)
}

export function hasRequestIdentity(config) {
  return Boolean(config?.vlsIdentity?.token || Object.entries(config?.headers || {})
    .find(([key, value]) => /^(authorization|blade-auth|x-vlstream-token)$/i.test(key)
      && value && !/^Basic\s/i.test(String(value))))
}

export const isLoginEntryRequest = config => /\/(?:blade-auth\/(?:token|logout)|sso\/v\d+\/(?:mode|exchangeToken|login|getCaptcha|refreshToken))(?:[/?]|$)/i.test('/' + String(config?.url || '').replace(/^\//, ''))

// Called only for a trusted API. Compare the complete sent identity, including its generation.
export function expireRequestIdentity(config) {
  const headers = config?.headers || {}
  const header = name => Object.entries(headers).find(([key]) => key.toLowerCase() === name.toLowerCase())?.[1]
  const current = currentRequestIdentity()
  const used = config?.vlsIdentity || {
    revision,
    token: normalize(header('X-VLStream-Token') || header('Authorization') || header('blade-auth')),
    platformToken: current.platformToken ? normalize(header('accesstoken')) : '',
    tenantId: header('tenantid') || ''
  }
  if (!used.token || used.revision !== current.revision
    || !['token', 'platformToken', 'tenantId'].every(key => used[key] === current[key])) return false
  // A queued platform request can still carry headers from before its interceptor captured a generation.
  if (used.platformToken && !config.vlsLocalSessionOnly && normalize(header('accesstoken')) !== used.platformToken) return false
  const sentLocal = normalize(header('X-VLStream-Token') || header('blade-auth') || header('Authorization'))
  if (sentLocal && sentLocal !== used.token && sentLocal !== used.platformToken) return false
  const matches = storage => normalize(storage.getItem('accessToken') || storage.getItem('token')) === used.token
    && normalize(storage.getItem('platformAccessToken')) === used.platformToken
    && (storage.getItem('tenantId') || storage.getItem('tenant_id') || '') === used.tenantId
  const clearSession = matches(sessionStorage), clearLocal = matches(localStorage)
  if (clearSession) {
    authKeys.forEach(key => sessionStorage.removeItem(key))
    // Explicitly signed out: another tab's persisted identity must not become this tab's fallback.
    sessionStorage.setItem('userInfo', 'null')
  }
  if (clearLocal) authKeys.forEach(key => localStorage.removeItem(key))
  // Also mask localStorage fallback if this tab originally used the persisted identity.
  if (!clearSession) sessionStorage.setItem('userInfo', 'null')
  revision++
  if (!redirecting) {
    redirecting = true
    const base = import.meta.env.BASE_URL
    const currentUrl = new URL(window.location.href)
    const path = currentUrl.pathname + currentUrl.search + currentUrl.hash
    const login = new URL('login', new URL(base, currentUrl.origin))
    if (currentUrl.pathname !== login.pathname) window.location.replace(appLoginUrl(path, base, currentUrl.origin))
  }
  return true
}

export function sessionExpiryError(response) {
  const error = new Error('')
  error.code = 'VLS_SESSION_EXPIRED'
  error.isInterceptorDetour = true
  error.response = response
  return error
}

// Page catch handlers may add their own generic toast after an interceptor rejects.
// Silence messages only while this document is leaving for login; ordinary failures stay visible.
export function installSessionMessageGuard(message) {
  for (const type of ['error', 'warning', 'success', 'info']) {
    const original = message[type]
    message[type] = (...args) => redirecting ? { close() {} } : original(...args)
  }
}
