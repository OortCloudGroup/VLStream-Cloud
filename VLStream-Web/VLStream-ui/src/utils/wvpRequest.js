/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { applyPlatformGatewayHeaders, getStoredToken, getPlatformAccessToken, getLocalSessionToken, isTrustedApiRequest, invalidateApiResponse } from '@/utils/request'
import { isMultiTenantMode } from '@/utils/platformSession'
import { normalizeWvpResponse } from '@/utils/wvpResponse.mjs'
import { replaceHeader, sessionValue, clearIdentityHeaders } from '@/utils/requestIdentity.mjs'

export const getWvpBaseURL = () => {
  const configuredBaseURL = import.meta.env.VITE_WVP_API_BASE_URL
  if (configuredBaseURL) return configuredBaseURL
  return import.meta.env.DEV ? '/wvp-api' : '/bus/wvp-server'
}

export const getWvpToken = () => {
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
  const token = getWvpToken()
  const federatedToken = getVlstreamFederatedToken()
  const tenantId = sessionValue(['tenantId'])
  ;['accesstoken', 'X-VLStream-Token', 'tenantid', 'Authorization', 'blade-auth'].forEach(name => replaceHeader(config.headers, name))
  if (isMultiTenantMode() && (!token || !federatedToken)) {
    const error = new Error('平台登录会话已失效，请重新登录')
    error.code = 'PLATFORM_AUTH_REQUIRED'
    throw error
  }
  replaceHeader(config.headers, 'accesstoken', token ? token.replace(/^Bearer\s+/i, '') : undefined)
  replaceHeader(config.headers, 'X-VLStream-Token', federatedToken ? federatedToken.replace(/^Bearer\s+/i, '') : undefined)
  replaceHeader(config.headers, 'tenantid', tenantId || undefined)

  applyPlatformGatewayHeaders(config)
  replaceHeader(config.headers, 'X-WVP-Auth-Source', 'vlstream')
  return config
}

wvpRequest.interceptors.request.use(applyWvpHeaders)

wvpRequest.interceptors.response.use(
  (response) => {
    invalidateApiResponse(response)
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }

    try {
      return normalizeWvpResponse(response.data)
    } catch (error) {
      // Preserve the business body and request context for callers and identity checks.
      error.response = { ...response, data: error.data || response.data }
      invalidateApiResponse(error.response)
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
  },
  (error) => {
    invalidateApiResponse(error.response)
    const message = error.response?.data?.msg
      || (error.code === 'ECONNABORTED' ? 'WVP 接口请求超时' : 'WVP 服务连接失败')
    ElMessage.error(message)
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
