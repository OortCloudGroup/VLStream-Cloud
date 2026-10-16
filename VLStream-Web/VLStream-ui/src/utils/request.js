/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { currentLocale, translateUiMessage } from '@/i18n'
import { logSafeRequestError } from '@/utils/safeRequestError.mjs'
import { isMultiTenantMode, isPlatformTokenFailure } from '@/utils/platformSession'
import { sessionValue, replaceHeader, isTrustedBusinessRequest, clearIdentityHeaders } from '@/utils/requestIdentity.mjs'

export const BLADE_CLIENT_AUTH_HEADER = import.meta.env.VITE_BLADE_CLIENT_AUTH_HEADER || 'Basic c2FiZXI6c2FiZXJfc2VjcmV0'
const PLATFORM_APP_ID = import.meta.env.VITE_PLATFORM_APP_ID || ''
const PLATFORM_SECRET_KEY = import.meta.env.VITE_PLATFORM_SECRET_KEY || ''
const PLATFORM_REQUEST_TYPE = import.meta.env.VITE_PLATFORM_REQUEST_TYPE || 'app'

// baseURL
export const getBaseURL = () => {
  const configuredBaseURL = import.meta.env.VITE_API_BASE_URL
  if (configuredBaseURL !== undefined) {
    return configuredBaseURL
  }

  // , Vite after
  if (import.meta.env.DEV) {
    return ''
  }
  // current VLStream after , also VITE_API_BASE_URL
  return '/bus/apaas-vls-server'
}

// token only Validate , can VLS after will token.
export const getPlatformAccessToken = () => {
  // Callback credentials are used only by the explicit exchange, then stored with its identity.
  return sessionValue(['platformAccessToken'])
}

// VLS will token SpringBlade / Sa-Token user .
export const getLocalSessionToken = () => sessionValue(['accessToken', 'token'])

// : need to token.
export const getStoredToken = () => getPlatformAccessToken() || (isMultiTenantMode() ? '' : getLocalSessionToken())

export const isTrustedApiRequest = config => isTrustedBusinessRequest(config, [
  getBaseURL(), import.meta.env.VITE_WVP_API_BASE_URL || (import.meta.env.DEV ? '/wvp-api' : '/bus/wvp-server'),
  ...(import.meta.env.VITE_APAAS_GATEWAY_PREFIX || import.meta.env.VITE_APAAS_API_BASE
    ? [import.meta.env.VITE_APAAS_GATEWAY_PREFIX || import.meta.env.VITE_APAAS_API_BASE]
    : import.meta.env.DEV ? ['/oort'] : [])
], window.location.origin)

// need to each parameter.
export const applyPlatformGatewayHeaders = (config) => {
  config.headers = config.headers || {}
  if (!isTrustedApiRequest(config)) { clearIdentityHeaders(config.headers); return }
  ;['requesttype', 'appid', 'secretkey'].forEach(name => replaceHeader(config.headers, name))
  config.headers.requesttype = PLATFORM_REQUEST_TYPE
  config.headers['Accept-Language'] = currentLocale.value
  config.headers['Content-Language'] = currentLocale.value.replace('-', '_')
  if (PLATFORM_APP_ID) config.headers.appid = PLATFORM_APP_ID
  if (PLATFORM_SECRET_KEY) config.headers.secretkey = PLATFORM_SECRET_KEY
}

// to SpringBlade Authorization and blade-auth .
// only , Authorization/authorization token.
export const applyAuthHeaders = (config) => {
  config.headers = config.headers || {}
  if (!isTrustedApiRequest(config)) { clearIdentityHeaders(config.headers); return '' }
  const localToken = config.localAuthToken || getLocalSessionToken()
  const platformToken = config.platformAuthToken !== undefined ? config.platformAuthToken : getPlatformAccessToken()
  const authToken = localToken
  const gatewayToken = (config.requirePlatformIdentity || isMultiTenantMode()) ? platformToken : (platformToken || localToken)
  ;['Authorization', 'blade-auth', 'accesstoken', 'tenantid', 'X-VLStream-Token', 'X-WVP-Auth-Source'].forEach(name => replaceHeader(config.headers, name))
  if (isMultiTenantMode() && authToken && !platformToken) {
    const error = new Error('平台登录会话已失效，请重新登录')
    error.code = 'PLATFORM_AUTH_REQUIRED'
    throw error
  }

  if (authToken) {
    const authValue = authToken.toLowerCase().startsWith('bearer ') ? authToken : `Bearer ${authToken}`
    config.headers.Authorization = authValue
    config.headers['blade-auth'] = authValue
  }
  if (gatewayToken) {
    replaceHeader(config.headers, 'accesstoken', gatewayToken.replace(/^Bearer\s+/i, ''))
    const tenantId = sessionValue(['tenantId']) || (isMultiTenantMode() ? '' : '000000')
    // Before a new multi-tenant session is verified, let the server derive its tenant from that session.
    replaceHeader(config.headers, 'tenantId', tenantId || undefined)
  }
  return authToken
}

// token OAuth client Basic , can old user token.
export const applyBladeClientAuthHeaders = (config) => {
  config.headers = config.headers || {}
  if (!isTrustedApiRequest(config)) { clearIdentityHeaders(config.headers); return }
  ;['Authorization', 'blade-auth', 'accesstoken'].forEach(name => replaceHeader(config.headers, name))
  delete config.headers.Authorization
  delete config.headers.authorization
  delete config.headers['blade-auth']
  delete config.headers.AccessToken
  delete config.headers.accesstoken
  config.headers.Authorization = BLADE_CLIENT_AUTH_HEADER
  replaceHeader(config.headers, 'accesstoken', isMultiTenantMode() ? getPlatformAccessToken() : undefined)
}

// Check current whether is SpringBlade token Get interface.
export const isBladeAuthTokenRequest = (config) => {
  return (config.url || '').includes('/blade-auth/token')
}

// SpringBlade msg field, interface message field.
export const getResponseMessage = (data, fallback) => {
  return data?.msg || data?.message || fallback
}

// A late response from a previous identity must not invalidate a newer login/tenant session.
export const invalidateApiResponse = response => {
  if (!response?.config) return false
  const headers = response.config.headers || {}
  const header = name => Object.entries(headers).find(([key]) => key.toLowerCase() === name.toLowerCase())?.[1]
  const normalize = value => String(value || '').replace(/^Bearer\s+/i, '')
  const currentPlatform = normalize(getPlatformAccessToken())
  const currentLocal = normalize(getLocalSessionToken())
  const usedLocal = normalize(header('X-VLStream-Token') || header('Authorization') || header('blade-auth'))
  const usedTenant = normalize(header('tenantid'))
  const defaultTenant = isMultiTenantMode() ? '' : '000000'
  const currentTenant = normalize(sessionValue(['tenantId', 'tenant_id'])) || defaultTenant
  const matchesCurrent = usedLocal === currentLocal && usedTenant === currentTenant
  const matchesStorage = storage => normalize(storage.getItem('accessToken') || storage.getItem('token')) === usedLocal
    && (normalize(storage.getItem('tenantId') || storage.getItem('tenant_id')) || defaultTenant) === usedTenant
  if (isPlatformTokenFailure(response.data) && currentPlatform && matchesCurrent && normalize(header('accesstoken')) === currentPlatform) {
    for (const storage of [sessionStorage, localStorage]) {
      if (matchesStorage(storage) && normalize(storage.getItem('platformAccessToken')) === currentPlatform) storage.removeItem('platformAccessToken')
    }
    return true
  }
  if ((response.status === 401 || Number(response.data?.code) === 401) && currentLocal && matchesCurrent) {
    for (const storage of [sessionStorage, localStorage]) {
      if (!matchesStorage(storage)) continue
      ;['accessToken', 'token', 'userInfo', 'tenantId', 'tenant_id', 'wvpAccessToken'].forEach(key => storage.removeItem(key))
    }
    return true
  }
  return false
}

// Process HTTP 200 failed SpringBlade .
export const handleBusinessError = (data, response, fallback = '请求失败', showMessage = true) => {
  invalidateApiResponse(response)
  const code = Number(data?.code)
  if (data?.success === false || (Number.isFinite(code) && code >= 400)) {
    const message = getResponseMessage(data, fallback)
    const businessError = new Error(message)
    businessError.response = response
    businessError.data = data

    if (showMessage) {
      ElMessage.error(translateUiMessage(message))
    }

    return Promise.reject(businessError)
  }

  return data
}

// axiosinstance - annotationAPI (18080 )
const request = axios.create({
  baseURL: getBaseURL(),
  timeout: 600000, // 10 , trainingtask
  headers: {
    'Content-Type': 'application/json'
  }
})

// instance - API (8080 )
const imageUploadRequest = axios.create({
  baseURL: getBaseURL(),
  timeout: 30000, // need to
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})

// instance - API
const authRequest = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  // configuration
  withCredentials: true
})

//
authRequest.interceptors.request.use(
  config => {
    applyPlatformGatewayHeaders(config)

    const shouldUseBladeClientAuth = config.useBladeClientAuth || isBladeAuthTokenRequest(config)
    if (shouldUseBladeClientAuth) {
      applyBladeClientAuthHeaders(config)
    } else if (!config.skipTokenAuth) {
      applyAuthHeaders(config)
    }

    return config
  },
  error => {
    invalidateApiResponse(error.response)
    logSafeRequestError('认证请求未能发送', error)
    return Promise.reject(error)
  }
)

//
request.interceptors.request.use(
  config => {
    applyPlatformGatewayHeaders(config)
    applyAuthHeaders(config)

    return config
  },
  error => {
    invalidateApiResponse(error.response)
    logSafeRequestError('请求未能发送', error)
    return Promise.reject(error)
  }
)

//
imageUploadRequest.interceptors.request.use(
  config => {
    applyPlatformGatewayHeaders(config)
    applyAuthHeaders(config)
    return config
  },
  error => {
    invalidateApiResponse(error.response)
    logSafeRequestError('图片上传请求未能发送', error)
    return Promise.reject(error)
  }
)

//
request.interceptors.response.use(
  response => {
    const { data } = response

    return handleBusinessError(data, response, '请求失败', !response.config?.silentError)
  },
  error => {
    invalidateApiResponse(error.response)
    if (axios.isCancel(error)) return Promise.reject(error)
    logSafeRequestError('请求失败', error)

    // video-recordrelated API, prompt / tip, Process
    if (error.config && error.config.url && error.config.url.includes('/video-record/')) {
      return Promise.reject(error)
    }

    let message = '请求失败'
    if (error.response) {
      // service
      const { status, data } = error.response
      switch (status) {
        case 400:
          message = getResponseMessage(data, '请求参数错误')
          break
        case 401:
          message = getResponseMessage(data, '未授权，请重新登录')
          break
        case 403:
          message = getResponseMessage(data, '拒绝访问')
          break
        case 404:
          // annotation interface 404 , prompt / tip
          if (error.config.url && error.config.url.includes('/annotation-images/dataset/')) {
            console.warn('标注图片数据不存在，可能是后端服务未启动或数据库无数据')
            return Promise.reject(error)
          }
          message = getResponseMessage(data, '请求地址不存在')
          break
        case 503:
          message = getResponseMessage(data, '服务暂时不可用，请稍后重试')
          break
        case 500:
          message = getResponseMessage(data, '服务器内部错误')
          break
        default:
          message = getResponseMessage(data, `请求失败 (${status})`)
      }
    } else if (error.request) {
      //
      message = '网络连接失败，请检查网络'
    } else {
      //
      message = error.message || '请求失败'
    }

    if (!error.config?.silentError) ElMessage.error(translateUiMessage(message))
    return Promise.reject(error)
  }
)

//
imageUploadRequest.interceptors.response.use(
  response => {
    const { data } = response

    return handleBusinessError(data, response, '图片上传失败')
  },
  error => {
    invalidateApiResponse(error.response)
    logSafeRequestError('图片上传请求失败', error)

    let message = '图片上传失败'
    if (error.response) {
      // service
      const { status, data } = error.response
      switch (status) {
        case 400:
          message = getResponseMessage(data, '图片上传参数错误')
          break
        case 401:
          message = getResponseMessage(data, '未授权，请重新登录')
          break
        case 403:
          message = getResponseMessage(data, '拒绝访问')
          break
        case 404:
          message = getResponseMessage(data, '图片上传地址不存在')
          break
        case 413:
          message = getResponseMessage(data, '图片文件过大')
          break
        case 500:
          message = getResponseMessage(data, '服务器内部错误')
          break
        default:
          message = getResponseMessage(data, `图片上传失败 (${status})`)
      }
    } else if (error.request) {
      //
      message = '网络连接失败，请检查网络'
    } else {
      //
      message = error.message || '图片上传失败'
    }

    ElMessage.error(translateUiMessage(message))
    return Promise.reject(error)
  }
)

//
authRequest.interceptors.response.use(
  response => {
    const { data } = response
    const url = response.config?.url || ''
    const shouldLetCallerHandle = url.includes('/auth/') || url.includes('/blade-auth/')

    return handleBusinessError(data, response, '认证请求失败', !shouldLetCallerHandle)
  },
  error => {
    invalidateApiResponse(error.response)
    logSafeRequestError('认证请求失败', error)

    // related API, prompt / tip, Process
    if (error.config && error.config.url && (error.config.url.includes('/auth/') || error.config.url.includes('/blade-auth/'))) {
      return Promise.reject(error)
    }

    let message = '认证请求失败'
    if (error.response) {
      // service
      const { status, data } = error.response
      switch (status) {
        case 400:
          message = getResponseMessage(data, '认证参数错误')
          break
        case 401:
          message = getResponseMessage(data, '未授权，请重新登录')
          break
        case 403:
          message = getResponseMessage(data, '拒绝访问')
          break
        case 404:
          message = getResponseMessage(data, '认证地址不存在')
          break
        case 500:
          message = getResponseMessage(data, '服务器内部错误')
          break
        default:
          message = getResponseMessage(data, `认证请求失败 (${status})`)
      }
    } else if (error.request) {
      //
      message = '网络连接失败，请检查网络'
    } else {
      //
      message = error.message || '认证请求失败'
    }

    ElMessage.error(translateUiMessage(message))
    return Promise.reject(error)
  }
)

export default request
export { imageUploadRequest, authRequest }
