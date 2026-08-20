import axios from 'axios'
import { ElMessage } from 'element-plus'

export const BLADE_CLIENT_AUTH_HEADER = import.meta.env.VITE_BLADE_CLIENT_AUTH_HEADER || 'Basic c2FiZXI6c2FiZXJfc2VjcmV0'
const PLATFORM_APP_ID = import.meta.env.VITE_PLATFORM_APP_ID || ''
const PLATFORM_SECRET_KEY = import.meta.env.VITE_PLATFORM_SECRET_KEY || ''
const PLATFORM_REQUEST_TYPE = import.meta.env.VITE_PLATFORM_REQUEST_TYPE || 'app'

// 根据环境确定baseURL
export const getBaseURL = () => {
  const configuredBaseURL = import.meta.env.VITE_API_BASE_URL
  if (configuredBaseURL !== undefined) {
    return configuredBaseURL
  }

  // 开发环境使用相对路径，通过Vite代理转发到本地后端
  if (import.meta.env.DEV) {
    return ''
  }
  // 生产环境默认访问线上网关，也可通过 VITE_API_BASE_URL 覆盖
  return 'http://oort.oortcloudsmart.com:21410/bus/vls-server'
}

// 按 URL、sessionStorage、localStorage 的顺序读取当前 token。
export const getStoredToken = () => {
  const urlParams = new URLSearchParams(window.location.search)
  const urlToken = urlParams.get('accessToken') || urlParams.get('access_token') || urlParams.get('token')
  if (urlToken) {
    return urlToken
  }

  const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
  if (sessionToken) {
    return sessionToken
  }

  return localStorage.getItem('accessToken') || localStorage.getItem('token')
}

// 统一平台网关要求每个请求都携带应用身份参数。
export const applyPlatformGatewayHeaders = (config) => {
  config.headers = config.headers || {}
  config.headers.requestType = PLATFORM_REQUEST_TYPE
  if (PLATFORM_APP_ID) config.headers.appID = PLATFORM_APP_ID
  if (PLATFORM_SECRET_KEY) config.headers.secretKey = PLATFORM_SECRET_KEY
}

// 为请求写入 SpringBlade 识别的 Authorization 与 blade-auth 头。
// 只写入规范大小写，避免浏览器把 Authorization/authorization 合并成不可识别的重复 token。
export const applyAuthHeaders = (config) => {
  const token = getStoredToken()
  if (token) {
    const authValue = token.toLowerCase().startsWith('bearer ') ? token : `Bearer ${token}`
    config.headers.Authorization = authValue
    config.headers['blade-auth'] = authValue
    config.headers.AccessToken = token
    const tenantId = sessionStorage.getItem('tenantId') || localStorage.getItem('tenantId') || '000000'
    config.headers.tenantId = tenantId
  }
  return token
}

// 登录取 token 时必须使用 OAuth client Basic 认证，不能携带旧用户 token。
export const applyBladeClientAuthHeaders = (config) => {
  delete config.headers.Authorization
  delete config.headers.authorization
  delete config.headers['blade-auth']
  delete config.headers.AccessToken
  delete config.headers.accesstoken
  config.headers.Authorization = BLADE_CLIENT_AUTH_HEADER
}

// 判断当前请求是否是 SpringBlade token 获取接口。
export const isBladeAuthTokenRequest = (config) => {
  return (config.url || '').includes('/blade-auth/token')
}

// 优先读取 SpringBlade 的 msg 字段，再兼容其他接口的 message 字段。
export const getResponseMessage = (data, fallback) => {
  return data?.msg || data?.message || fallback
}

// 处理 HTTP 200 但业务状态失败的 SpringBlade 响应。
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

// 创建axios实例 - 用于标注API (18080端口)
const request = axios.create({
  baseURL: getBaseURL(),
  timeout: 600000, // 10分钟超时，用于支持长时间的训练任务
  headers: {
    'Content-Type': 'application/json'
  }
})

// 创建专门的图片上传请求实例 - 用于图片上传API (8080端口)
const imageUploadRequest = axios.create({
  baseURL: getBaseURL(),
  timeout: 30000, // 图片上传需要更长的超时时间
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})

// 创建专门的认证请求实例 - 用于跨域认证API
const authRequest = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  // 生产环境添加跨域配置
  withCredentials: true
})

// 认证请求拦截器
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

// 请求拦截器
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

// 图片上传请求拦截器
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

// 响应拦截器
request.interceptors.response.use(
  response => {
    const { data } = response
    
    return handleBusinessError(data, response)
  },
  error => {
    console.error('响应错误:', error)
    
    // 对于video-record相关的API，不显示错误提示，让调用方自己处理
    if (error.config && error.config.url && error.config.url.includes('/video-record/')) {
      return Promise.reject(error)
    }
    
    let message = '请求失败'
    if (error.response) {
      // 服务器返回错误状态码
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
          // 对于标注图片接口的404错误，不显示错误提示
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
      // 网络错误
      message = '网络连接失败，请检查网络'
    } else {
      // 其他错误
      message = error.message || '请求失败'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// 图片上传响应拦截器
imageUploadRequest.interceptors.response.use(
  response => {
    const { data } = response
    
    return handleBusinessError(data, response, '图片上传失败')
  },
  error => {
    console.error('图片上传响应错误:', error)
    
    let message = '图片上传失败'
    if (error.response) {
      // 服务器返回错误状态码
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
      // 网络错误
      message = '网络连接失败，请检查网络'
    } else {
      // 其他错误
      message = error.message || '图片上传失败'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// 认证请求响应拦截器
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
    
    // 对于认证相关的API，不显示错误提示，让调用方自己处理
    if (error.config && error.config.url && (error.config.url.includes('/auth/') || error.config.url.includes('/blade-auth/'))) {
      return Promise.reject(error)
    }
    
    let message = '认证请求失败'
    if (error.response) {
      // 服务器返回错误状态码
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
      // 网络错误
      message = '网络连接失败，请检查网络'
    } else {
      // 其他错误
      message = error.message || '认证请求失败'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request 
export { imageUploadRequest, authRequest }
