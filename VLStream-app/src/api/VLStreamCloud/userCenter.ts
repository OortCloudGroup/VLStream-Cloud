import axios from 'axios'

// 用户中心配置
const USER_CENTER_CONFIG = {
  baseURL: 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'requesttype': 'app',
    'appid': '6551b0147c4649a894e86bf8de248da4',
    'secretkey': '58f9eeefc65f4b318204ba21f39a8861'
  }
}

// 创建用户中心请求实例
const userCenterRequest = axios.create(USER_CENTER_CONFIG)

// 请求拦截器
userCenterRequest.interceptors.request.use(
  (config) => {
    // 获取token
    const token = sessionStorage.getItem('accessToken') ||
                  localStorage.getItem('accessToken') ||
                  sessionStorage.getItem('token') ||
                  localStorage.getItem('token')

    if (token) {
      // 确保 headers 存在，若不存在则初始化为空对象
      config.headers = config.headers || {}
      config.headers.accesstoken = token
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
userCenterRequest.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    console.error('用户中心请求失败:', error)
    return Promise.reject(error)
  }
)

// 验证Token
export function userCenterVerifyToken(data) {
  // 创建专门的请求实例，确保包含所有必要的请求头
  const request = axios.create({
    baseURL: USER_CENTER_CONFIG.baseURL,
    timeout: USER_CENTER_CONFIG.timeout,
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': USER_CENTER_CONFIG.headers.appid,
      'secretkey': USER_CENTER_CONFIG.headers.secretkey
    }
  })

  // 获取token - 优先级：URL > sessionStorage > localStorage
  const getToken = () => {
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ userCenterVerifyToken使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }

    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      console.log('✅ userCenterVerifyToken使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }

    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      console.log('✅ userCenterVerifyToken使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }

    // console.log('❌ userCenterVerifyToken没有找到任何token')
    return null
  }

  const token = getToken()
  return request.post('/sso/v1/verifyToken', data, {
    headers: {
      ...(token && { accesstoken: token })
    }
  })
}

// 获取用户租户列表
export function getUserTenants(data) {
  console.log('🔍 getUserTenants函数开始执行...')
  console.log('🔍 当前URL:', window.location.href)
  console.log('🔍 当前URL参数:', window.location.search)

  // 创建专门的请求实例，确保包含所有必要的请求头
  const request = axios.create({
    baseURL: USER_CENTER_CONFIG.baseURL,
    timeout: USER_CENTER_CONFIG.timeout,
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': USER_CENTER_CONFIG.headers.appid,
      'secretkey': USER_CENTER_CONFIG.headers.secretkey
    }
  })

  // 获取token - 优先级：URL > sessionStorage > localStorage
  const getToken = () => {
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    console.log('🔍 getUserTenants调试 - URL参数:', {
      search: window.location.search,
      accessToken: urlParams.get('accessToken'),
      token: urlParams.get('token'),
      urlToken: urlToken
    })
    if (urlToken) {
      console.log('✅ getUserTenants使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }

    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    console.log('🔍 getUserTenants调试 - sessionStorage:', {
      accessToken: sessionStorage.getItem('accessToken'),
      token: sessionStorage.getItem('token'),
      sessionToken: sessionToken
    })
    if (sessionToken) {
      console.log('✅ getUserTenants使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }

    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    console.log('🔍 getUserTenants调试 - localStorage:', {
      accessToken: localStorage.getItem('accessToken'),
      token: localStorage.getItem('token'),
      localToken: localToken
    })
    if (localToken) {
      console.log('✅ getUserTenants使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }

    console.log('❌ getUserTenants没有找到任何token')
    return null
  }

  const token = getToken()

  console.log('🔍 getUserTenants调试 - 请求配置:', {
    url: '/sso/v1/getUserTenants',
    headers: request.defaults.headers,
    data: data,
    token: token
  })
  return request.post('/sso/v1/getUserTenants', data, {
    headers: {
      ...(token && { accesstoken: token })
    }
  })
}

// 获取租户信息
export function getTenant(data) {
  // 创建专门的请求实例，确保包含所有必要的请求头
  const request = axios.create({
    baseURL: USER_CENTER_CONFIG.baseURL,
    timeout: USER_CENTER_CONFIG.timeout,
    headers: {
      'Content-Type': 'application/json',
      'requesttype': 'app',
      'appid': USER_CENTER_CONFIG.headers.appid,
      'secretkey': USER_CENTER_CONFIG.headers.secretkey
    }
  })

  // 获取token - 优先级：URL > sessionStorage > localStorage
  const getToken = () => {
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ getTenant使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }

    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      console.log('✅ getTenant使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }

    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      console.log('✅ getTenant使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }

    console.log('❌ getTenant没有找到任何token')
    return null
  }

  const token = getToken()
  return request.post('/sso/v1/getTenant', data, {
    headers: {
      ...(token && { accesstoken: token })
    }
  })
}

// 获取用户信息
export function getUserInfo(data) {
  return userCenterRequest.post('/sso/v1/getUserInfo', data)
}
