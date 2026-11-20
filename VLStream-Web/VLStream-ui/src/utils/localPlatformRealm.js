import { extractAuthCallbackParams, stripAuthCallbackParams } from '@/utils/authCallbackParams.mjs'

const REALM_KEY = 'vlsLocalPlatformRealm'
const CLOUD_PENDING_KEY = 'modelHubAuthPendingAt'
const PLATFORM_PENDING_KEY = 'vlsPlatformLoginPendingAt'
const CLOUD_KEYS = ['modelHubAccessToken', 'modelHubTenantId', CLOUD_PENDING_KEY, 'modelHubReturnLocation',
  'modelHubAuthCallbackPath', 'modelHubAuthCallbackConsumedAt', 'pendingPublishToModelHub']
const PLATFORM_KEYS = ['platformAccessToken', 'platformUserInfo', PLATFORM_PENDING_KEY]
const LINKED_VLS_KEYS = ['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo',
  'tenantId', 'tenant_id', 'wvpAccessToken', 'apaas_token', 'refresh_token']

function hasPlatformBinding(storage) {
  if (storage.getItem('platformAccessToken')) return true
  try {
    return Boolean(JSON.parse(storage.getItem('userInfo'))?.platformAccessToken)
  } catch {
    return false
  }
}

function originatedAt(referrer, origin) {
  try {
    return new URL(referrer).origin === origin
  } catch {
    return false
  }
}

function isRecent(storage, key, lifetime) {
  const age = Date.now() - Number(storage.getItem(key))
  return age >= 0 && age <= lifetime
}

export function initializeLocalPlatformRealm() {
  if (!import.meta.env.DEV || import.meta.env.VITE_PLATFORM_SESSION_ISOLATION !== 'true') return
  const realm = new URL(import.meta.env.VITE_PLATFORM_LOGIN_URL).origin
  const pending = sessionStorage.getItem(REALM_KEY) === realm
    && (isRecent(sessionStorage, CLOUD_PENDING_KEY, 30 * 60_000)
      || isRecent(sessionStorage, PLATFORM_PENDING_KEY, 30_000))

  for (const storage of [sessionStorage, localStorage]) {
    if (storage.getItem(REALM_KEY) === realm) continue
    const linked = hasPlatformBinding(storage)
    for (const key of [...CLOUD_KEYS, ...PLATFORM_KEYS, ...(linked ? LINKED_VLS_KEYS : [])]) {
      storage.removeItem(key)
    }
    storage.setItem(REALM_KEY, realm)
  }

  const href = window.location.href
  if (!extractAuthCallbackParams(href).accessToken) return
  const referrer = document.referrer
  if (!originatedAt(referrer, realm) && !(pending && (!referrer || originatedAt(referrer, window.location.origin)))) {
    window.history.replaceState({}, '', stripAuthCallbackParams(href))
  }
}
