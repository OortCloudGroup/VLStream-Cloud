// Read one active identity, never fill gaps from a different tab's persisted identity.
export function sessionValue(keys) {
  const hasSessionIdentity = ['accessToken', 'token', 'userInfo', 'platformAccessToken']
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
