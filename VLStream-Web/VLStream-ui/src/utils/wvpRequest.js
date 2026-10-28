/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { applyPlatformGatewayHeaders, getStoredToken, getPlatformAccessToken, getLocalSessionToken, isTrustedApiRequest, handleSessionFailureAsync } from '@/utils/request'
import { captureRequestIdentity, isSessionRedirecting, sessionExpiryError } from '@/utils/sessionExpiry'
import { isMultiTenantMode } from '@/utils/platformSession'
import { normalizeWvpResponse } from '@/utils/wvpResponse.mjs'
import { replaceHeader, sessionValue, clearIdentityHeaders } from '@/utils/requestIdentity.mjs'
import { SHARED_CAMERA_RTC_AUTH_HEADERS } from '@/utils/sharedCameraRtc.mjs'
import { isLocalEventClassificationRead } from '@/utils/localEventAccess.mjs'

export const getWvpBaseURL = () => {
  const configuredBaseURL = import.meta.env.VITE_WVP_API_BASE_URL
  if (configuredBaseURL) return configuredBaseURL
  return import.meta.env.DEV ? '/wvp-api' : '/bus/wvp-server'
}

export const getWvpToken = () => {
  // The VLS console uses one active identity in both tenant modes.
  // An obsolete independent WVP token must not override a live VLS federation session.
  if (getLocalSessionToken()) return getPlatformAccessToken() || (isMultiTenantMode() ? '' : getLocalSessionToken())
  // An old WVP-local token must never replace the current platform identity in multi mode.
  if (isMultiTenantMode()) return getPlatformAccessToken()
  const urlParams = new URLSearchParams(window.location.search)
  return urlParams.get('wvpAccessToken')
    || sessionValue(['wvpAccessToken'])
    || getStoredToken()
}

// WVP VLStream Validate after .
// token accessToken , can .
export const getVlstreamFederatedToken = getLocalSessionToken

const wvpRequest = axios.create({
  baseURL: getWvpBaseURL(),
  timeout: 1000000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

export const applyWvpHeaders = (config) => {
  config.headers = config.headers || {}
  if (!isTrustedApiRequest(config)) { clearIdentityHeaders(config.headers); return config }
  captureRequestIdentity(config)
  if (isSessionRedirecting() && !getLocalSessionToken()) throw sessionExpiryError()
  const localRead = isLocalEventClassificationRead(config, getWvpBaseURL(), window.location.origin)
  config.vlsLocalSessionOnly = localRead
  const token = localRead ? getLocalSessionToken() : getWvpToken()
  const federatedToken = getVlstreamFederatedToken()
  const tenantId = sessionValue(['tenantId', 'tenant_id'])
  ;['accesstoken', 'X-VLStream-Token', 'tenantid', 'Authorization', 'blade-auth'].forEach(name => replaceHeader(config.headers, name))
  if (isMultiTenantMode() && (!token || !federatedToken)) {
    const error = new Error('平台登录会话已失效，请重新登录')
    error.code = 'PLATFORM_AUTH_REQUIRED'
    error.config = config
    throw error
  }
  replaceHeader(config.headers, 'accesstoken', token ? token.replace(/^Bearer\s+/i, '') : undefined)
  replaceHeader(config.headers, 'X-VLStream-Token', federatedToken ? federatedToken.replace(/^Bearer\s+/i, '') : undefined)
  replaceHeader(config.headers, 'tenantid', tenantId || undefined)

  applyPlatformGatewayHeaders(config)
  replaceHeader(config.headers, 'X-WVP-Auth-Source', 'vlstream')
  return config
}

// Read the current identity through the same WVP/gateway path at the moment the socket opens.
export const getSharedCameraRtcAuthHeaders = () => {
  const currentToken = getVlstreamFederatedToken()
  if (typeof currentToken !== 'string' || !currentToken.trim().replace(/^Bearer(?:\s+|$)/i, '').trim()) {
    const error = new Error('登录会话已失效，请重新登录')
    error.code = 'PLATFORM_AUTH_REQUIRED'
    throw error
  }
  const config = applyWvpHeaders({ baseURL: getWvpBaseURL(), url: '/vlstream/device/shared/camera-rtc', headers: {} })
  const allowed = new Set(SHARED_CAMERA_RTC_AUTH_HEADERS.map(name => name.toLowerCase()))
  return Object.fromEntries(Object.entries(config.headers).filter(([name]) => allowed.has(name.toLowerCase())))
}

wvpRequest.interceptors.request.use(applyWvpHeaders)

wvpRequest.interceptors.response.use(
  async (response) => {
    const sessionError = await handleSessionFailureAsync(response)
    if (sessionError) return Promise.reject(sessionError)
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }

    try {
      return normalizeWvpResponse(response.data)
    } catch (error) {
      // Preserve the business body and request context for callers and identity checks.
      error.response = { ...response, data: error.data || response.data }
      const sessionError = await handleSessionFailureAsync(error)
      if (sessionError) return Promise.reject(sessionError)
      if (!response.config?.silentError) ElMessage.error(error.message)
      return Promise.reject(error)
    }
  },
  async (error) => {
    if (axios.isCancel(error)) return Promise.reject(error)
    const sessionError = await handleSessionFailureAsync(error)
    if (sessionError) return Promise.reject(sessionError)
    const message = error.response?.data?.msg
      || (error.code === 'ECONNABORTED' ? 'WVP 接口请求超时' : 'WVP 服务连接失败')
    if (!error.config?.silentError) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export async function downloadWvp(url, params, filename, config = {}) {
  const data = await wvpRequest.post(url, params, {
    responseType: 'blob',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    ...config
  })
  saveAs(new Blob([data]), filename)
}

export default wvpRequest
