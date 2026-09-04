/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'

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
  const urlParams = new URLSearchParams(window.location.search)
  const urlToken = urlParams.get('accessToken') || urlParams.get('access_token') || urlParams.get('token')
  if (urlToken) {
    return urlToken
  }

  return sessionStorage.getItem('platformAccessToken') || localStorage.getItem('platformAccessToken')
}

// VLS will token SpringBlade / Sa-Token user .
export const getLocalSessionToken = () => sessionStorage.getItem('accessToken')
  || sessionStorage.getItem('token')
  || localStorage.getItem('accessToken')
  || localStorage.getItem('token')

// : need to token.
export const getStoredToken = () => getPlatformAccessToken() || getLocalSessionToken()

// need to each parameter.
export const applyPlatformGatewayHeaders = (config) => {
  config.headers = config.headers || {}
  delete config.headers.requestType
  delete config.headers.appID
  delete config.headers.secretKey
  config.headers.requesttype = PLATFORM_REQUEST_TYPE
  if (PLATFORM_APP_ID) config.headers.appid = PLATFORM_APP_ID
  if (PLATFORM_SECRET_KEY) config.headers.secretkey = PLATFORM_SECRET_KEY
}

// to SpringBlade Authorization and blade-auth .
// only , Authorization/authorization token.
export const applyAuthHeaders = (config) => {
  const localToken = config.localAuthToken || getLocalSessionToken()
  const platformToken = getPlatformAccessToken()
  const authToken = localToken
  const gatewayToken = platformToken || localToken

  if (authToken) {
    const authValue = authToken.toLowerCase().startsWith('bearer ') ? authToken : `Bearer ${authToken}`
    config.headers.Authorization = authValue
    config.headers['blade-auth'] = authValue
  }
  if (gatewayToken) {
    delete config.headers.AccessToken
    config.headers.accesstoken = gatewayToken.replace(/^Bearer\s+/i, '')
    const tenantId = sessionStorage.getItem('tenantId') || localStorage.getItem('tenantId') || '000000'
    config.headers.tenantId = tenantId
  }
  return authToken
}

// token OAuth client Basic , can old user token.
export const applyBladeClientAuthHeaders = (config) => {
  delete config.headers.Authorization
  delete config.headers.authorization
  delete config.headers['blade-auth']
  delete config.headers.AccessToken
  delete config.headers.accesstoken
  config.headers.Authorization = BLADE_CLIENT_AUTH_HEADER
}

// Check current whether is SpringBlade token Get interface.
export const isBladeAuthTokenRequest = (config) => {
  return (config.url || '').includes('/blade-auth/token')
}

// SpringBlade msg field, interface message field.
export const getResponseMessage = (data, fallback) => {
  return data?.msg || data?.message || fallback
}

// Process HTTP 200 failed SpringBlade .
export const handleBusinessError = (data, response, fallback = '请求失败', showMessage = true) => {
  if (data?.success === false) {
    const message = getResponseMessage(data, fallback)
    const businessError = new Error(message)
    businessError.response = response
    businessError.data = data

    if (showMessage) {
      ElMessage.error(message)
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
    console.error('认证请求错误:', error)
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
    console.error('请求错误:', error)
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
    console.error('图片上传请求错误:', error)
    return Promise.reject(error)
  }
)

//
request.interceptors.response.use(
  response => {
    const { data } = response

    return handleBusinessError(data, response)
  },
  error => {
    console.error('响应错误:', error)

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

    ElMessage.error(message)
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
    console.error('图片上传响应错误:', error)

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

    ElMessage.error(message)
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
    console.error('认证响应错误:', error)

    if (error.response) {
      console.error('认证请求失败:', error.response.status, error.response.config?.url)
    }

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

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
export { imageUploadRequest, authRequest }
