import { verifyToken } from '@/api/auth'
import { syncUserToLocal } from '@/api/userSync'

// 兼容 axios 原始响应与 request 拦截器已解包的 SpringBlade 响应。
const normalizeApiResponse = (response) => {
  if (response?.code !== undefined || response?.success !== undefined) {
    return response
  }

  return response?.data || response
}

export class AuthManager {
  constructor() {
    this.appId = '6551b0147c4649a894e86bf8de248da4'
    this.secretKey = '58f9eeefc65f4b318204ba21f39a8861'
    this.userCenterUrl = 'http://oort.oortcloudsmart.com:21410'
  }

  // 检查外部平台登录状态（优先级最高）
  async checkExternalPlatformLogin() {
    try {
      // console.log('检查外部统一用户平台登录状态...')
      
      // 优先级1：检查URL中的accessToken（最高优先级）
      const urlParams = new URLSearchParams(window.location.search)
      const urlToken = urlParams.get('accessToken')
      if (urlToken) {
        console.log('检测到URL中的accessToken:', urlToken)
        
        // 暂停其他token验证，避免冲突
        if (window.storageEventSync) {
          window.storageEventSync.pauseTokenValidation = true
        }
        
        try {
          const userInfo = await this.verifyToken(urlToken)
          if (userInfo) {
            console.log('URL Token验证成功，用户:', userInfo.userName || userInfo.username)
            
            // 修复：不要清除所有token，而是保存新的用户信息和token
            // this.clearAllTokens() // 注释掉这行，避免清除刚验证成功的token
            
            // 保存新的用户信息（包含token）
            await this.saveUserToLocal(userInfo)
            
            // 修复：延迟清除URL参数，确保后续API调用能正常进行
            // this.cleanUrlToken() // 暂时注释掉，避免影响后续API调用
            
            // 恢复token验证
            if (window.storageEventSync) {
              setTimeout(() => {
                window.storageEventSync.pauseTokenValidation = false
              }, 2000)
            }
            return userInfo
          } else {
            console.log('URL Token验证失败，token无效:', urlToken)
            // 不再清除无效的URL token，保留URL参数
            // this.cleanUrlToken()
          }
        } catch (error) {
          console.error('URL Token验证失败:', error)
          // 不再清除URL参数
          // this.cleanUrlToken()
        }
        
        // 恢复token验证
        if (window.storageEventSync) {
          window.storageEventSync.pauseTokenValidation = false
        }
      }
      
      // 优先级2：检查sessionStorage中的token
      const sessionToken = sessionStorage.getItem('accessToken')
      if (sessionToken) {
        console.log('验证Session Storage中的token:', sessionToken)
        try {
          const userInfo = await this.verifyToken(sessionToken)
          if (userInfo) {
            console.log('Session token验证成功，用户:', userInfo.userName || userInfo.username)
            return userInfo
          } else {
            console.log('Session token验证失败，清除无效token')
            this.clearSessionTokens()
          }
        } catch (error) {
          console.error('Session token验证失败:', error)
          this.clearSessionTokens()
        }
      }

      const localToken = localStorage.getItem('accessToken')
      if (localToken) {
        console.log('验证localStorage中的token:', localToken)
        try {
          const userInfo = await this.verifyToken(localToken)
          if (userInfo) {
            console.log('localStorage token验证成功，用户:', userInfo.userName || userInfo.username)
            return userInfo
          } else {
            console.log('localStorage token验证失败，清除无效token')
            this.clearLocalTokens()
          }
        } catch (error) {
          console.error('localStorage token验证失败:', error)
          this.clearLocalTokens()
        }
      }

      return null
      
      // 优先级3：尝试从统一用户平台获取新token
      // console.log('尝试从统一用户平台获取新token...')
      const platformToken = await this.getTokenFromExternalPlatform()
      if (platformToken) {
        // console.log('从外部平台获取到新token，验证中...')
        const userInfo = await this.verifyToken(platformToken)
        if (userInfo) {
          console.log('外部平台token验证成功，用户:', userInfo.userName || userInfo.username)
          await this.saveUserToLocal(userInfo)
          return userInfo
        }
      }
      
      // 优先级4：取消使用本地缓存的用户信息，确保页面右上角使用getUserTenants API的数据
      // const cachedUserInfo = this.getCachedUserInfo()
      // if (cachedUserInfo) {
      //   console.log('使用本地缓存的用户信息:', cachedUserInfo.userName || cachedUserInfo.username)
      //   return cachedUserInfo
      // }
      
      // console.log('未找到有效的用户登录状态，不返回本地缓存信息')
      return null
      
    } catch (error) {
      console.error('检查外部平台登录状态失败:', error)
      return null
    }
  }

  // 从外部平台获取token（模拟实现）
  async getExternalPlatformToken() {
    try {
      // 强制清除所有测试token
      const testTokens = ['c0c81bef2c934f829df667a202c99d1e', 'c34f010e8fc042099e9ac812da34f080']
      const currentSessionToken = sessionStorage.getItem('token')
      const currentLocalToken = localStorage.getItem('token')
      
      if (testTokens.includes(currentSessionToken)) {
        console.log('检测到测试token，清除Session Storage:', currentSessionToken)
        sessionStorage.removeItem('token')
      }
      
      if (testTokens.includes(currentLocalToken)) {
        console.log('检测到测试token，清除localStorage:', currentLocalToken)
        localStorage.removeItem('token')
      }
      
      // 1. 优先从Session Storage获取token（排除测试token）
      const sessionToken = sessionStorage.getItem('token') || 
                          sessionStorage.getItem('accessToken') || 
                          sessionStorage.getItem('userCenterToken')
      
      if (sessionToken && !testTokens.includes(sessionToken)) {
        console.log('从Session Storage获取到真实token:', sessionToken)
        return sessionToken
      }
      
      // 2. 从localStorage获取token（排除测试token）
      const localToken = localStorage.getItem('token') || 
                        localStorage.getItem('accessToken') || 
                        localStorage.getItem('userCenterToken')
      
      if (localToken && !testTokens.includes(localToken)) {
        console.log('从localStorage获取到真实token:', localToken)
        return localToken
      }
      
      // 3. 从URL参数获取accessToken（SSO跳转）
      const urlParams = new URLSearchParams(window.location.search)
      const urlAccessToken = urlParams.get('accessToken')
      if (urlAccessToken && !testTokens.includes(urlAccessToken)) {
        console.log('从URL参数获取到accessToken:', urlAccessToken)
        return urlAccessToken
      }
      
      // 4. 强制从外部平台获取真实token
      console.log('强制从外部平台获取真实token...')
      const platformToken = await this.getTokenFromExternalPlatform()
      if (platformToken && !testTokens.includes(platformToken)) {
        console.log('从外部平台获取到真实token:', platformToken)
        // 保存到本地Session Storage
        sessionStorage.setItem('token', platformToken)
        return platformToken
      }
      
      console.log('没有找到有效的真实token')
      return null
    } catch (error) {
      console.error('获取外部平台token失败:', error)
      return null
    }
  }

  // 从外部平台获取token
  async getTokenFromExternalPlatform() {
    try {
      // 方法1：通过URL参数获取（如果是从外部平台跳转过来的）
      const urlParams = new URLSearchParams(window.location.search)
      const urlToken = urlParams.get('accessToken') || urlParams.get('token')
      if (urlToken) {
        console.log('从URL参数获取到token:', urlToken)
        return urlToken
      }
      
      // 方法2：通过localStorage获取（如果之前已经同步过）
      const storedToken = localStorage.getItem('platformAccessToken')
      if (storedToken) {
        // console.log('从本地存储获取到平台token:', storedToken)
        return storedToken
      }
      
      // 方法3：强制从外部平台获取真实Session Storage
      console.log('强制从外部平台获取真实Session Storage...')
      const platformToken = await this.syncFromExternalPlatform()
      if (platformToken) {
        console.log('从外部平台同步到真实token:', platformToken)
        // 保存到平台token存储
        localStorage.setItem('platformAccessToken', platformToken)
        return platformToken
      }
      
      console.log('无法从外部平台获取真实token')
      return null
    } catch (error) {
      console.error('从外部平台获取token失败:', error)
      return null
    }
  }

  // 从外部平台同步Session Storage信息
  async syncFromExternalPlatform() {
    try {
      console.log('尝试从外部平台同步Session Storage信息...')
      
      // 由于跨域限制，我们使用更实用的方法
      // 方法1：通过URL参数获取（如果是从外部平台跳转过来的）
      const urlParams = new URLSearchParams(window.location.search)
      const urlToken = urlParams.get('accessToken') || urlParams.get('token')
      if (urlToken) {
        console.log('从URL参数获取到token:', urlToken)
        return urlToken
      }
      
      // 方法2：通过localStorage获取（如果之前已经同步过）
      const storedToken = localStorage.getItem('platformAccessToken')
      if (storedToken) {
        console.log('从本地存储获取到平台token:', storedToken)
        return storedToken
      }
      
      // 方法3：通过cookie获取（如果外部平台设置了cookie）
      const cookieToken = this.getTokenFromCookie()
      if (cookieToken) {
        console.log('从cookie获取到token:', cookieToken)
        return cookieToken
      }
      
      // 方法4：静默处理，不显示弹窗
      console.log('无法自动获取token，静默处理')
      
      return null
    } catch (error) {
      console.error('同步外部平台Session Storage失败:', error)
      return null
    }
  }

  // 通过iframe请求token
  requestTokenFromIframe(iframe) {
    return new Promise((resolve) => {
      const timeout = setTimeout(() => {
        console.log('iframe通信超时，尝试直接读取Session Storage')
        // 超时后尝试直接读取
        this.tryDirectAccess(iframe).then(resolve)
      }, 8000)
      
      // 监听来自iframe的消息
      const messageHandler = (event) => {
        console.log('收到iframe消息:', event.origin, event.data)
        if (event.origin !== 'http://oort.oortcloudsmart.com:21410') {
          return
        }
        
        if (event.data && event.data.type === 'TOKEN_RESPONSE') {
          clearTimeout(timeout)
          window.removeEventListener('message', messageHandler)
          console.log('收到外部平台token:', event.data.token)
          resolve(event.data.token)
        }
      }
      
      window.addEventListener('message', messageHandler)
      
      // 向iframe发送请求
      try {
        console.log('向iframe发送token请求...')
        iframe.contentWindow.postMessage({
          type: 'REQUEST_TOKEN',
          source: 'vlstream-ui'
        }, 'http://oort.oortcloudsmart.com:21410')
      } catch (e) {
        console.log('无法向iframe发送消息（跨域限制），尝试直接访问')
        clearTimeout(timeout)
        window.removeEventListener('message', messageHandler)
        this.tryDirectAccess(iframe).then(resolve)
      }
    })
  }

  // 显示手动获取token的提示（已禁用弹窗）
  showManualTokenPrompt() {
    console.log('=== 手动获取Token步骤（已禁用弹窗） ===')
    console.log('1. 打开外部平台: http://oort.oortcloudsmart.com:21410')
    console.log('2. 登录平台')
    console.log('3. 打开浏览器开发者工具 (F12)')
    console.log('4. 在Console中执行: sessionStorage.getItem("accessToken")')
    console.log('5. 复制返回的token值')
    console.log('6. 在测试页面中手动输入token')
    
    // 已禁用弹窗，只显示控制台日志
    console.log('弹窗功能已禁用，请查看控制台日志获取手动操作步骤')
  }

  // 自动从外部平台获取token（通过URL跳转）
  async autoSyncFromExternalPlatform() {
    try {
      console.log('开始自动同步外部平台token...')
      
      // 构建跳转URL，包含回调地址
      const currentUrl = encodeURIComponent(window.location.href)
      const platformUrl = `http://oort.oortcloudsmart.com:21410?redirect=${currentUrl}`
      
      console.log('跳转到外部平台:', platformUrl)
      window.location.href = platformUrl
      
      return true
    } catch (error) {
      console.error('自动同步失败:', error)
      return false
    }
  }

  // 从cookie中获取token
  getTokenFromCookie() {
    try {
      // 检查是否有外部平台设置的cookie
      const cookies = document.cookie.split(';')
      for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=')
        if (name === 'accessToken' || name === 'token' || name === 'userCenterToken') {
          return value
        }
      }
    } catch (error) {
      console.error('读取cookie失败:', error)
    }
    return null
  }

  // 检查URL中的token参数
  async checkUrlToken() {
    const urlParams = new URLSearchParams(window.location.search)
    const token = urlParams.get('token') || urlParams.get('accessToken')
    
    if (token) {
      try {
        console.log('检测到URL中的accessToken:', token)
        const userInfo = await this.verifyToken(token)
        if (userInfo) {
          console.log('URL Token验证成功，用户信息:', userInfo)
          // 保存用户信息到本地
          await this.saveUserToLocal(userInfo)
          // 清除URL中的token参数
          this.cleanUrlToken()
          return userInfo
        } else {
          console.log('URL Token验证失败，token无效:', token)
          // 不再清除URL参数
          // this.cleanUrlToken()
        }
      } catch (error) {
        console.error('URL Token验证失败:', error)
        // 不再清除URL参数
        // this.cleanUrlToken()
      }
    } else {
      // console.log('URL中没有找到accessToken参数')
    }
    return null
  }

  // 验证Token并获取完整用户信息
  async verifyToken(token) {
    try {
      // console.log('开始验证token:', token)
      
      const response = await verifyToken()
      
      console.log('verifyToken响应:', response)
      
      const apiResponse = normalizeApiResponse(response)
      
      if (apiResponse && (apiResponse.code === 200 || apiResponse.success === true)) {
        console.log('Token验证成功，基础用户信息:', apiResponse.data)

        return {
          ...apiResponse.data,
          accessToken: token,
          userName: apiResponse.data?.realName || apiResponse.data?.name || apiResponse.data?.account || apiResponse.data?.userName,
          loginId: apiResponse.data?.account || apiResponse.data?.loginId
        }
      } else {
        // 改进错误处理：检查是否是特定的错误类型
        const errorCode = apiResponse?.code || apiResponse?.data?.code
        const errorMsg = apiResponse?.msg || apiResponse?.data?.msg || apiResponse?.message
        
        console.log('Token验证失败，响应:', response)
        console.log('错误代码:', errorCode, '错误信息:', errorMsg)
        
        // 如果是400错误，可能是token格式问题，但不一定是无效token
        if (errorCode === 400) {
          console.log('⚠️ 收到400错误，可能是token格式问题，但不立即清除token')
          // 不立即返回null，给token一个机会
          return null
        }
        
        // 如果是其他错误，可能是网络问题或临时错误
        console.log('⚠️ 收到其他错误，可能是网络问题，不立即清除token')
        return null
      }
    } catch (error) {
      // console.error('Token验证请求失败:', error)
      
      // 检查是否是网络错误
      if (error.code === 'NETWORK_ERROR' || error.message?.includes('Network Error')) {
        console.log('⚠️ 网络错误，不立即清除token')
        return null
      }
      
      // 检查是否是超时错误
      if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
        console.log('⚠️ 请求超时，不立即清除token')
        return null
      }
      
      // 检查是否是400错误
      if (error.response?.status === 400) {
        // console.log('⚠️ 收到400错误，可能是token格式问题，但不立即清除token')
        return null
      }
      
      return null
    }
  }

  // 保存用户信息到本地
  async saveUserToLocal(userInfo) {
    try {
      // 检查userInfo是否包含有效的accessToken
      if (!userInfo || !userInfo.accessToken) {
        console.warn('⚠️ 用户信息不包含有效的accessToken，跳过保存')
        return
      }
      
      console.log('💾 开始保存用户信息到本地:', {
        userName: userInfo.userName || userInfo.username,
        tenantId: userInfo.tenantId,
        accessToken: userInfo.accessToken.substring(0, 8) + '...'
      })
      
      // 暂时注释掉后端API保存用户信息（API有问题）
      // await syncUserToLocal(userInfo)
      
      // 保存到localStorage和sessionStorage
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      localStorage.setItem('accessToken', userInfo.accessToken)
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
      sessionStorage.setItem('accessToken', userInfo.accessToken)
      
      // 验证保存是否成功
      const savedSessionToken = sessionStorage.getItem('accessToken')
      const savedLocalToken = localStorage.getItem('accessToken')
      
      console.log('✅ 用户信息已保存到本地，包含tenantId:', userInfo.tenantId)
      console.log('🔍 验证保存结果:', {
        sessionToken: savedSessionToken ? savedSessionToken.substring(0, 8) + '...' : 'null',
        localToken: savedLocalToken ? savedLocalToken.substring(0, 8) + '...' : 'null'
      })
    } catch (error) {
      console.error('❌ 保存用户信息失败:', error)
    }
  }

  // 清除URL中的token参数
  cleanUrlToken() {
    const url = new URL(window.location.href)
    // 修复：删除accessToken参数而不是token参数
    url.searchParams.delete('accessToken')
    url.searchParams.delete('token') // 也删除token参数以防万一
    window.history.replaceState({}, '', url.toString())
    console.log('已清除URL中的token参数')
  }

  // 清除Session Storage中的token
  clearSessionTokens() {
    try {
      console.log('🧹 清除Session Storage中的无效token')
      sessionStorage.removeItem('accessToken')
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('userCenterToken')
      sessionStorage.removeItem('userInfo')
      sessionStorage.removeItem('platformAccessToken')
      console.log('✅ Session Storage token已清除')
    } catch (error) {
      console.error('❌ 清除Session Storage token失败:', error)
    }
  }

  // 清除localStorage中的token
  clearLocalTokens() {
    try {
      console.log('🧹 清除localStorage中的无效token')
      localStorage.removeItem('accessToken')
      localStorage.removeItem('token')
      localStorage.removeItem('userCenterToken')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('platformAccessToken')
      console.log('✅ localStorage token已清除')
    } catch (error) {
      console.error('❌ 清除localStorage token失败:', error)
    }
  }

  // 清除所有存储中的token
  clearAllTokens() {
    this.clearSessionTokens()
    this.clearLocalTokens()
  }

  // 检查本地token是否有效
  async checkLocalToken() {
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      try {
        console.log('验证Session Storage中的token:', sessionToken)
        const verifiedUserInfo = await this.verifyToken(sessionToken)
        if (verifiedUserInfo) {
          console.log('Session Storage token验证成功')
          return verifiedUserInfo
        }
        console.log('Session Storage token验证失败，清除无效信息')
        this.clearSessionTokens()
      } catch (error) {
        console.error('Session Storage用户信息验证失败:', error)
        this.clearSessionTokens()
      }
    }

    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      try {
        console.log('验证localStorage中的token:', localToken)
        const verifiedUserInfo = await this.verifyToken(localToken)
        if (verifiedUserInfo) {
          console.log('localStorage token验证成功')
          return verifiedUserInfo
        }
        console.log('localStorage token验证失败，清除无效信息')
        this.clearLocalTokens()
      } catch (error) {
        console.error('localStorage用户信息验证失败:', error)
        this.clearLocalTokens()
      }
    }

    return null
  }

  // 登出
  logout() {
    this.clearAllTokens()
    window.location.href = '/'
  }

  // 手动设置新token（用于外部系统跳转）
  async setNewToken(token) {
    if (token) {
      try {
        console.log('🔍 验证新token有效性:', token.substring(0, 8) + '...')
        
        // 先验证token有效性
        const userInfo = await this.verifyToken(token)
        if (userInfo) {
          console.log('✅ 新token验证成功，保存到Session Storage')
          
          // 清除所有旧的token
          sessionStorage.removeItem('accessToken')
          sessionStorage.removeItem('token')
          localStorage.removeItem('accessToken')
          localStorage.removeItem('token')
          sessionStorage.removeItem('userInfo')
          localStorage.removeItem('userInfo')
          
          // 设置新token到Session Storage
          sessionStorage.setItem('token', token)
          sessionStorage.setItem('accessToken', token)
          
          // 保存用户信息
          await this.saveUserToLocal(userInfo)
          
          console.log('✅ 新token已设置到Session Storage:', token.substring(0, 8) + '...')
          return true
        } else {
          console.log('❌ 新token验证失败，不保存无效token')
          return false
        }
      } catch (error) {
        console.error('❌ 验证新token失败:', error)
        return false
      }
    }
    return false
  }

  // 获取当前token（优先使用URL中的token）
  getCurrentToken() {
    // 优先级：URL > sessionStorage > localStorage
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ auth.js getCurrentToken使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }
    
    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('token') || 
                        sessionStorage.getItem('accessToken') || 
                        sessionStorage.getItem('userCenterToken')
    if (sessionToken) {
      console.log('✅ auth.js getCurrentToken使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }
    
    // 3. 从localStorage获取
    const localToken = localStorage.getItem('token') || 
                      localStorage.getItem('accessToken') || 
                      localStorage.getItem('userCenterToken')
    if (localToken) {
      console.log('✅ auth.js getCurrentToken使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }
    
    console.log('❌ auth.js getCurrentToken没有找到任何token')
    return null
  }

  // 获取本地缓存的用户信息（优先使用sessionStorage）
  getCachedUserInfo() {
    try {
      // 优先使用sessionStorage中的用户信息
      const sessionUser = sessionStorage.getItem('userInfo')
      if (sessionUser) {
        return JSON.parse(sessionUser)
      }
      
      // 如果sessionStorage中没有，才使用localStorage中的用户信息
      const localUser = localStorage.getItem('userInfo')
      if (localUser) {
        return JSON.parse(localUser)
      }
    } catch (error) {
      console.error('解析缓存用户信息失败:', error)
    }
    return null
  }
}

// 创建并导出authManager实例
export const authManager = new AuthManager()
