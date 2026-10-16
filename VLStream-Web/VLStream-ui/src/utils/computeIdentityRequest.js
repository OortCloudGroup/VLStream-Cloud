import request from '@/utils/request'
import { authManager } from '@/utils/auth'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { isMultiTenantMode } from '@/utils/platformSession'

const changed = () => Object.assign(new Error('登录账户或租户已变化，请刷新后重试'), { code: 'COMPUTE_IDENTITY_CHANGED' })
let independentIdentityRevision = 0
window.addEventListener('modelHubAuthChanged', () => { independentIdentityRevision++ })

function currentIdentity() {
  const identity = authManager.getIdentitySnapshot()
  const independentRevision = independentIdentityRevision
  const mode = isMultiTenantMode() ? 'multi' : 'single'
  const platformToken = getOortCloudSession(mode).accessToken
  const assertCurrent = () => {
    if (independentRevision !== independentIdentityRevision || !authManager.isIdentityCurrent(identity) || mode !== (isMultiTenantMode() ? 'multi' : 'single')
      || platformToken !== getOortCloudSession(mode).accessToken) throw changed()
  }
  return { identity, platformToken, assertCurrent }
}

// Also covers a download chooser and asynchronous blob decoding before the browser saves a file.
export const captureComputeIdentity = () => ({ assertCurrent: currentIdentity().assertCurrent })

export function computePathPart(value) {
  const part = value === undefined || value === null ? '' : String(value)
  let decoded = part
  try {
    for (let pass = 0; pass < 8; pass++) {
      const next = decodeURIComponent(decoded)
      if (next === decoded) break
      decoded = next
      if (pass === 7) decoded = ''
    }
  } catch { decoded = '' }
  if (!decoded.trim() || decoded === '.' || decoded === '..' || /[\/\\?#\u0000-\u001f\u007f]/.test(decoded)) {
    throw Object.assign(new Error('训练资源标识无效'), { code: 'COMPUTE_DESTINATION_INVALID' })
  }
  return encodeURIComponent(part)
}

const namespace = /^\/(?:vlsCompute|vlsAlgorithmTraining|vlsAlgorithmModel|vlsContainerInstance|vlsModelDispatch)(?:\/|$)/
function isComputeDestination(config) {
  const url = config.url
  if (config.baseURL !== undefined || typeof url !== 'string' || !namespace.test(url)
    || /[?#\u0000-\u001f\u007f]/.test(url) || url.endsWith('/') || url.includes('//')) return false
  try {
    let decoded = url
    for (let pass = 0; pass < 8; pass++) {
      const next = decodeURIComponent(decoded)
      if (next === decoded) break
      decoded = next
      if (pass === 7) return false
    }
    // Check the path the browser/server would normalize, including encoded dot segments
    // and Windows separators. No API in this module intentionally uses dot navigation.
    const normalizedPath = decoded.replace(/\\/g, '/')
    if (/[?#\u0000-\u001f\u007f]/.test(normalizedPath) || normalizedPath.slice(1).split('/').some(part => !part || part === '.' || part === '..')) return false
    const target = new URL(normalizedPath, 'https://compute-identity.invalid')
    return target.origin === 'https://compute-identity.invalid' && namespace.test(target.pathname)
  } catch { return false }
}

// These APIs accept backend IDs, never a supplier/model download URL. The request interceptor
// remains responsible for the configured business API base and stripping third-party headers.
export function currentIdentityRequest(config) {
  if (!isComputeDestination(config)) {
    return Promise.reject(Object.assign(new Error('请求地址不属于训练后端'), { code: 'COMPUTE_DESTINATION_INVALID' }))
  }
  const { identity, platformToken, assertCurrent } = currentIdentity()
  // Empty single-tenant SSO is explicit: historical local tasks can reach their existing
  // server checks, while known SSO tasks still fail closed. Never substitute a VLS token.
  return Promise.resolve().then(() => {
    assertCurrent()
    return request({ ...config, requirePlatformIdentity: true, platformAuthToken: platformToken || '',
      localAuthToken: identity.token, sensitiveData: true, silentError: true })
  }).then(response => {
    assertCurrent()
    return response
  }, error => {
    assertCurrent()
    const safe = new Error('请求未完成，请核对当前授权与权限')
    safe.code = error?.code
    const data = error?.data || error?.response?.data
    safe.data = { code: data?.code, msg: data?.msg, message: data?.message }
    throw safe
  })
}
