// Read one active identity, never fill gaps from a different tab's persisted identity.
const identityKeys = ['accessToken', 'token', 'userInfo', 'platformAccessToken']
const restoredKeys = [...identityKeys, 'tenantId', 'tenant_id', 'wvpAccessToken']

// Restore a fresh tab as one unit. Never promote a lone token alias into a half-session.
export function bootstrapRequestIdentity() {
  const hasSessionIdentity = identityKeys.some(key => sessionStorage.getItem(key) !== null)
  const source = hasSessionIdentity ? sessionStorage : localStorage
  const snapshot = Object.fromEntries(restoredKeys.map(key => [key, source.getItem(key)]))
  const token = snapshot.accessToken || snapshot.token || ''
  const userInfo = snapshot.userInfo
  let valid = Boolean(token && !['null', 'undefined'].includes(token))
  if (userInfo !== null) {
    try {
      const user = JSON.parse(userInfo)
      valid = valid && user && typeof user === 'object' && !Array.isArray(user)
        && (!user.accessToken || user.accessToken === token)
        && (!user.tenantId || String(user.tenantId) === (snapshot.tenantId || snapshot.tenant_id))
        && (!user.platformAccessToken || user.platformAccessToken === snapshot.platformAccessToken)
    } catch { valid = false }
  }
  valid = valid && restoredKeys.every(key => source.getItem(key) === snapshot[key])
  if (!valid) {
    // Preserve the explicit sign-out boundary, and leave another tab's persistent state alone.
    ;['accessToken', 'token', 'userInfo', 'platformAccessToken', 'tenantId', 'tenant_id', 'wvpAccessToken']
      .forEach(key => sessionStorage.removeItem(key))
    sessionStorage.setItem('userInfo', 'null')
    return false
  }
  if (!hasSessionIdentity) {
    for (const [key, value] of Object.entries(snapshot)) if (value !== null) sessionStorage.setItem(key, value)
  }
  // Legacy event pages read this alias. It belongs to the selected identity only.
  sessionStorage.setItem('token', token)
  return true
}

export function sessionValue(keys) {
  const hasSessionIdentity = identityKeys
    .some(key => sessionStorage.getItem(key) !== null)
  const storage = hasSessionIdentity ? sessionStorage : localStorage
  for (const key of keys) { const value = storage.getItem(key); if (value) return value }
  return ''
}

export function replaceHeader(headers, name, value) {
  for (const key of Object.keys(headers)) if (key.toLowerCase() === name.toLowerCase()) delete headers[key]
  if (value !== undefined && value !== null && value !== '') headers[name] = value
}

export function clearIdentityHeaders(headers) {
  ;['Authorization', 'blade-auth', 'accesstoken', 'X-VLStream-Token', 'X-WVP-Auth-Source',
    'tenantid', 'appid', 'secretkey', 'requesttype'].forEach(name => replaceHeader(headers, name))
}

// Credentials are restricted to the configured API bases or same-origin APaaS business services.
export function isTrustedBusinessRequest(config, bases, origin) {
  try {
    const root = origin || 'http://localhost'
    const base = config.baseURL !== undefined ? config.baseURL : bases[0]
    const url = String(config.url || '')
    const absolute = /^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(url)
    const target = new URL(absolute ? url : String(base || '').replace(/\/$/, '') + '/' + url.replace(/^\//, ''), root)
    if (target.username || target.password || !['http:', 'https:'].includes(target.protocol)) return false
    if (target.origin === new URL(root).origin && /^\/bus\/apaas-[^/]+(?:\/|$)/.test(target.pathname)) return true
    return bases.some(value => {
      const allowed = new URL(value || '/', root)
      const prefix = allowed.pathname.replace(/\/$/, '')
      return target.origin === allowed.origin && (!prefix || target.pathname === prefix || target.pathname.startsWith(prefix + '/'))
    })
  } catch { return false }
}
