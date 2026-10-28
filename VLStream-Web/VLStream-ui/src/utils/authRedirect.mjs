import { stripAuthCallbackParams } from './authCallbackParams.mjs'

// Return a router path inside this app. Never accept a different origin or login loop.
export function safeAuthReturnPath(value, base = '/', origin = window.location.origin) {
  try {
    const raw = String(value || '/workspace')
    if (/[\\\u0000-\u0020]/.test(raw) || /^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(raw)) return '/workspace'
    const appBase = new URL(base, origin).pathname.replace(/\/?$/, '/')
    const url = new URL(raw.startsWith('/') ? raw : '/' + raw, origin)
    let path = url.pathname
    if (appBase !== '/' && path.startsWith(appBase)) path = '/' + path.slice(appBase.length)
    if (/^\/login(?:\/|$)/i.test(path) || path.startsWith('/bus/')) return '/workspace'
    const decoded = decodeURIComponent(path)
    if (/[\\\u0000-\u0020]/.test(decoded) || decoded.startsWith('//') || decoded.includes('..')
      || /^\/login(?:\/|$)/i.test(decoded)) return '/workspace'
    return stripAuthCallbackParams(path + url.search + url.hash)
  } catch { return '/workspace' }
}

export function appLoginUrl(returnPath, base, origin) {
  const appBase = new URL(base, origin)
  const url = new URL('login', appBase)
  url.searchParams.set('redirect', safeAuthReturnPath(returnPath, base, origin))
  return url.toString()
}
