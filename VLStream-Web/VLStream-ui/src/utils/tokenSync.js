/**
 * Token同步监听器
 * 监听URL参数中的token变化，实现即时同步
 */

import { authManager } from './auth.js'

class TokenSyncManager {
  constructor() {
    this.lastToken = null
    this.syncInterval = null
    this.isInitialized = false
  }

  /**
   * 初始化token同步
   */
  init() {
    if (this.isInitialized) return
    
    console.log('🔄 初始化Token同步监听器')
    
    // 立即检查一次
    this.checkAndSyncToken()
    
    // 设置定时检查（每5秒检查一次）
    this.syncInterval = setInterval(() => {
      this.checkAndSyncToken()
    }, 5000)
    
    // 监听页面可见性变化
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        console.log('📱 页面重新可见，检查token同步')
        this.checkAndSyncToken()
      }
    })
    
    // 监听storage变化（跨标签页同步）
    window.addEventListener('storage', (event) => {
      if (event.key === 'accessToken') {
        console.log('🔄 检测到storage变化，同步token')
        this.checkAndSyncToken()
      }
    })
    
    this.isInitialized = true
  }

  /**
   * 检查并同步token
   */
  async checkAndSyncToken() {
    try {
      // 获取URL中的token
      const urlToken = this.getTokenFromUrl()
      
      // 获取当前存储的token
      const currentToken = this.getCurrentStoredToken()
      
      console.log('🔍 Token检查:', {
        urlToken: urlToken ? urlToken.substring(0, 8) + '...' : 'null',
        currentToken: currentToken ? currentToken.substring(0, 8) + '...' : 'null'
      })
      
      // 如果URL中有新token且与当前token不同
      if (urlToken && urlToken !== currentToken) {
        console.log('🔄 检测到新token，开始同步')
        await this.syncNewToken(urlToken)
      }
      
      // 如果URL中没有token但存储中有token，检查token是否有效
      else if (!urlToken && currentToken) {
        console.log('🔍 检查当前token有效性')
        await this.validateCurrentToken(currentToken)
      }
      
      // 如果都没有token，尝试从其他地方获取
      else if (!urlToken && !currentToken) {
        console.log('🔍 尝试从其他地方获取token')
        await this.tryGetTokenFromOtherSources()
      }
      
      // 如果URL和存储中的token相同，不需要处理
      else if (urlToken && urlToken === currentToken) {
        console.log('✅ Token已同步，无需处理')
      }
      
    } catch (error) {
      console.error('❌ Token同步检查失败:', error)
    }
  }

  /**
   * 从URL中获取token
   */
  getTokenFromUrl() {
    const urlParams = new URLSearchParams(window.location.search)
    return urlParams.get('accessToken') || urlParams.get('token')
  }

  /**
   * 获取当前存储的token
   */
  getCurrentStoredToken() {
    // 优先级：URL > sessionStorage > localStorage
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ tokenSync使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }
    
    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      console.log('✅ tokenSync使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }
    
    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      console.log('✅ tokenSync使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }
    
    console.log('❌ tokenSync没有找到任何token')
    return null
  }

  /**
   * 同步新token
   */
  async syncNewToken(newToken) {
    try {
      console.log('🔄 开始同步新token')
      
      // 验证新token
      const isValid = await this.validateToken(newToken)
      
      if (isValid) {
        // 更新存储
        sessionStorage.setItem('accessToken', newToken)
        localStorage.setItem('accessToken', newToken)
        
        // 更新用户信息
        await authManager.checkExternalPlatformLogin()
        
        console.log('✅ Token同步成功')
        
        // 触发token更新事件
        this.triggerTokenUpdateEvent(newToken)
        
        // 清除URL中的token参数（可选）
        this.clearTokenFromUrl()
        
      } else {
        console.warn('⚠️ 新token验证失败')
        // 清除无效token
        sessionStorage.removeItem('accessToken')
        localStorage.removeItem('accessToken')
      }
      
    } catch (error) {
      console.error('❌ Token同步失败:', error)
    }
  }

  /**
   * 验证token有效性
   */
  async validateToken(token) {
    try {
      const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/verifyToken', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'requesttype': 'app',
          'appid': '6551b0147c4649a894e86bf8de248da4',
          'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
          'accesstoken': token
        },
        body: JSON.stringify({
          accessToken: token
        })
      })
      
      if (response.ok) {
        const result = await response.json()
        const isValid = result.code === 200 || result.success === true
        
        if (!isValid) {
          // 检查特定的错误类型
          const errorCode = result.code
          const errorMsg = result.msg || result.message
          
          if (errorCode === 400) {
            console.log('⚠️ 收到400错误，可能是token格式问题，但不立即标记为无效')
            return false
          }
        }
        
        return isValid
      } else {
        // 处理HTTP错误状态
        if (response.status === 400) {
          console.log('⚠️ 收到400错误，可能是token格式问题，但不立即标记为无效')
          return false
        } else if (response.status === 500) {
          console.log('⚠️ 服务器内部错误，不立即标记token为无效')
          return false
        } else {
          console.log('⚠️ HTTP错误状态:', response.status, '不立即标记token为无效')
          return false
        }
      }
    } catch (error) {
      console.error('Token验证失败:', error)
      
      // 检查是否是网络错误
      if (error.message?.includes('Network Error') || error.code === 'NETWORK_ERROR') {
        console.log('⚠️ 网络错误，不立即标记token为无效')
        return false
      }
      
      // 检查是否是超时错误
      if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
        console.log('⚠️ 请求超时，不立即标记token为无效')
        return false
      }
      
      return false
    }
  }

  /**
   * 验证当前token
   */
  async validateCurrentToken(token) {
    const isValid = await this.validateToken(token)
    
    if (!isValid) {
      console.warn('⚠️ 当前token已失效，清除token')
      sessionStorage.removeItem('accessToken')
      localStorage.removeItem('accessToken')
      
      // 触发token失效事件
      this.triggerTokenInvalidEvent()
    }
  }

  /**
   * 尝试从其他地方获取token
   */
  async tryGetTokenFromOtherSources() {
    // 检查cookie
    const cookieToken = this.getTokenFromCookie()
    if (cookieToken) {
      console.log('🍪 从cookie获取到token')
      await this.syncNewToken(cookieToken)
      return
    }
    
    // 检查sessionStorage中的其他token字段
    const sessionToken = sessionStorage.getItem('token') || sessionStorage.getItem('userToken')
    if (sessionToken) {
      console.log('📦 从sessionStorage其他字段获取到token')
      await this.syncNewToken(sessionToken)
      return
    }
    
    // 检查localStorage中的其他token字段
    const localToken = localStorage.getItem('token') || localStorage.getItem('userToken')
    if (localToken) {
      console.log('📦 从localStorage其他字段获取到token')
      await this.syncNewToken(localToken)
      return
    }
    
    // 检查用户信息对象中的token
    const userInfo = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo')
    if (userInfo) {
      try {
        const parsedUserInfo = JSON.parse(userInfo)
        if (parsedUserInfo.accessToken) {
          console.log('👤 从用户信息对象获取到token')
          await this.syncNewToken(parsedUserInfo.accessToken)
          return
        }
      } catch (error) {
        console.log('❌ 解析用户信息失败:', error.message)
      }
    }
    
    console.log('💡 未找到可用token')
  }

  /**
   * 从cookie获取token
   */
  getTokenFromCookie() {
    const cookies = document.cookie.split(';')
    for (const cookie of cookies) {
      const [name, value] = cookie.trim().split('=')
      if (name === 'accessToken' || name === 'token') {
        return value
      }
    }
    return null
  }

  /**
   * 清除URL中的token参数
   */
  clearTokenFromUrl() {
    const url = new URL(window.location.href)
    url.searchParams.delete('accessToken')
    url.searchParams.delete('token')
    
    // 使用replaceState避免在历史记录中留下痕迹
    window.history.replaceState({}, document.title, url.toString())
  }

  /**
   * 触发token更新事件
   */
  triggerTokenUpdateEvent(token) {
    const event = new CustomEvent('tokenUpdated', {
      detail: { token }
    })
    window.dispatchEvent(event)
  }

  /**
   * 触发token失效事件
   */
  triggerTokenInvalidEvent() {
    const event = new CustomEvent('tokenInvalid')
    window.dispatchEvent(event)
  }

  /**
   * 停止同步
   */
  stop() {
    if (this.syncInterval) {
      clearInterval(this.syncInterval)
      this.syncInterval = null
    }
    this.isInitialized = false
    console.log('🛑 Token同步监听器已停止')
  }

  /**
   * 手动触发同步
   */
  forceSync() {
    console.log('🔄 手动触发token同步')
    this.checkAndSyncToken()
  }
}

// 创建全局实例
export const tokenSyncManager = new TokenSyncManager()

// 自动初始化
if (typeof window !== 'undefined') {
  // 将实例暴露到全局window对象，方便调试
  window.tokenSyncManager = tokenSyncManager
  
  // 等待DOM加载完成后初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      tokenSyncManager.init()
    })
  } else {
    tokenSyncManager.init()
  }
} 