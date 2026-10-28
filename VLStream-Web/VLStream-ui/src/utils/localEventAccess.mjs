export const LOCAL_EVENT_AUTH_SCOPE = 'local-events'
let localEventScope = false

export const isLocalEventRoute = meta => meta?.authScope === LOCAL_EVENT_AUTH_SCOPE
export const selectRouteAuthScope = meta => { localEventScope = isLocalEventRoute(meta) }
export const isLocalEventScopeActive = () => localEventScope

function apiPath(config, configuredBase, origin) {
  try {
    const base = new URL(configuredBase || '/', origin)
    const url = String(config.url || '')
    const absolute = /^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(url)
    const actualBase = config.baseURL !== undefined ? config.baseURL : configuredBase
    const target = new URL(absolute ? url : String(actualBase || '').replace(/\/$/, '') + '/' + url.replace(/^\//, ''), origin)
    const prefix = base.pathname.replace(/\/$/, '')
    if (target.origin !== base.origin || target.username || target.password
      || (prefix && !target.pathname.startsWith(prefix + '/'))) return ''
    return target.pathname.slice(prefix.length) || '/'
  } catch { return '' }
}

// Transport scope only: backend local session, tenant and permission checks remain mandatory.
export function isLocalEventReadRequest(config, configuredBase, origin) {
  if (!config.localSessionOnly || config.requirePlatformIdentity) return false
  const path = apiPath(config, configuredBase, origin)
  const method = String(config.method || 'get').toLowerCase()
  if (method === 'get') return ['/blade-system/user/info', '/vlsEventManagement/page', '/system/platform-logo/current'].includes(path)
    || /^\/vlsEventManagement\/\d+$/.test(path)
  return method === 'post' && ['/task/v1/event_list', '/task/v1/event_item_list'].includes(path)
}

// These read-only WVP filters are mounted by the single-tenant event list.
export function isLocalEventClassificationRead(config, configuredBase, origin) {
  return localEventScope && !config.requirePlatformIdentity && String(config.method || 'get').toLowerCase() === 'get'
    && ['/device/classification/tree', '/device/classification/device-ids'].includes(apiPath(config, configuredBase, origin))
}
