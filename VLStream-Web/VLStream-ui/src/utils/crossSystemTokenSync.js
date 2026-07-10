/**
 * 跨系统Token同步器
 * 实现VLStream-ui和统一用户平台之间的token同步
 */

class CrossSystemTokenSync {
  constructor() {
    this.syncInterval = null
    this.isInitialized = false
    this.lastUnifiedToken = null
    this.checkInterval = 3000 // 每3秒检查一次
  }

  /**
   * 初始化跨系统token同步
   */
  init() {
    if (this.isInitialized) return
    
    console.log('🔄 初始化跨系统Token同步器')
    
    // 立即检查一次
    this.checkAndSyncCrossSystemToken()
    
    // 设置定时检查
    this.syncInterval = setInterval(() => {
      this.checkAndSyncCrossSystemToken()
    }, this.checkInterval)
    
    // 监听页面可见性变化
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        console.log('📱 页面重新可见，检查跨系统token同步')
        this.checkAndSyncCrossSystemToken()
      }
    })
    
    // 监听storage变化（跨标签页同步）
    window.addEventListener('storage', (event) => {
      if (event.key === 'accessToken' || event.key === 'token') {
        console.log('🔄 检测到storage变化，检查跨系统token同步')
        this.checkAndSyncCrossSystemToken()
      }
    })
    
    this.isInitialized = true
  }

  /**
   * 检查并同步跨系统token
   */
  async checkAndSyncCrossSystemToken() {
    try {
      // 获取当前系统的token
      const currentToken = this.getCurrentSystemToken()
      
      // 获取统一用户平台的token
      const unifiedToken = await this.getUnifiedPlatformToken()
      
      console.log('🔍 跨系统Token检查:', {
        currentToken: currentToken ? currentToken.substring(0, 8) + '...' : 'null',
        unifiedToken: unifiedToken ? unifiedToken.substring(0, 8) + '...' : 'null'
      })
      
      // 如果统一用户平台有新token且与当前token不同
      if (unifiedToken && unifiedToken !== currentToken) {
        console.log('🔄 检测到统一用户平台新token，开始同步')
        
        // 判断token来源
        const urlParams = new URLSearchParams(window.location.search)
        const urlToken = urlParams.get('accessToken') || urlParams.get('token')
        const source = urlToken === unifiedToken ? 'url' : 'api'
        
        await this.syncUnifiedToken(unifiedToken, source)
      }
      
      // 如果当前系统有token但统一用户平台没有，检查是否需要同步
      else if (currentToken && !unifiedToken) {
        console.log('🔍 检查当前token是否需要同步到统一用户平台')
        await this.validateAndSyncCurrentToken(currentToken)
      }
      
      // 如果都没有token，尝试从其他地方获取
      else if (!currentToken && !unifiedToken) {
        console.log('🔍 尝试从其他地方获取token')
        await this.tryGetTokenFromOtherSources()
      }
      
      // 如果token相同，验证token有效性
      else if (currentToken && unifiedToken && currentToken === unifiedToken) {
        console.log('🔍 检测到token相同，验证有效性')
        const isValid = await this.validateToken(currentToken)
        
        if (isValid) {
          console.log('✅ 跨系统Token已同步且有效')
          this.lastUnifiedToken = unifiedToken
        } else {
          console.log('⚠️ 跨系统Token相同但无效，需要重新获取')
          // 清除无效token
          sessionStorage.removeItem('accessToken')
          localStorage.removeItem('accessToken')
          sessionStorage.removeItem('token')
          localStorage.removeItem('token')
          
          // 触发token失效事件
          this.triggerTokenInvalidEvent()
          
          // 尝试获取新的有效token
          await this.tryGetTokenFromOtherSources()
        }
      }
      
    } catch (error) {
      console.error('❌ 跨系统Token同步检查失败:', error)
    }
  }

  /**
   * 获取当前系统的token
   */
  getCurrentSystemToken() {
    // 优先级：URL > sessionStorage > localStorage
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ crossSystemTokenSync使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }
    
    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      console.log('✅ crossSystemTokenSync使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }
    
    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      console.log('✅ crossSystemTokenSync使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }
    
    console.log('❌ crossSystemTokenSync没有找到任何token')
    return null
  }

  /**
   * 获取统一用户平台的token
   */
  async getUnifiedPlatformToken() {
    try {
      // 方法1：检查URL参数中是否有来自统一用户平台的token
      const urlParams = new URLSearchParams(window.location.search)
      const urlToken = urlParams.get('accessToken') || urlParams.get('token')
      
      if (urlToken) {
        console.log('🔗 从URL参数获取到统一用户平台token')
        return urlToken
      }
      
      // 方法2：尝试通过iframe获取统一用户平台的token
      const iframeToken = await this.getTokenFromUnifiedPlatformIframe()
      if (iframeToken) {
        console.log('🖼️ 从iframe获取到统一用户平台token')
        return iframeToken
      }
      
      // 方法3：通过API获取统一用户平台的当前用户信息
      const currentToken = this.getCurrentSystemToken()
      if (currentToken) {
        const userInfo = await this.getUnifiedUserInfo(currentToken)
        if (userInfo && userInfo.accessToken) {
          console.log('👤 从统一用户平台API获取到token')
          return userInfo.accessToken
        }
      }
      
      // 方法4：尝试从localStorage获取统一用户平台的token
      const localStorageToken = this.getTokenFromUnifiedPlatformLocalStorage()
      if (localStorageToken) {
        console.log('💾 从localStorage获取到统一用户平台token')
        return localStorageToken
      }
      
      return null
    } catch (error) {
      console.error('获取统一用户平台token失败:', error)
      return null
    }
  }

  /**
   * 获取统一用户平台的用户信息
   */
  async getUnifiedUserInfo(token) {
    try {
      const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/getUserInfo', {
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
        return result.data
      }
      
      return null
    } catch (error) {
      console.error('获取统一用户平台用户信息失败:', error)
      return null
    }
  }

  /**
   * 同步统一用户平台的token
   */
  async syncUnifiedToken(unifiedToken, source = 'api') {
    try {
      console.log('🔄 开始同步统一用户平台token，来源:', source)
      
      // 如果token来自URL参数，直接同步（因为URL参数通常是从统一用户平台直接传递的）
      if (source === 'url') {
        console.log('🔗 检测到URL参数token，直接同步')
        
        // 更新当前系统的token存储
        sessionStorage.setItem('accessToken', unifiedToken)
        localStorage.setItem('accessToken', unifiedToken)
        sessionStorage.setItem('token', unifiedToken)
        localStorage.setItem('token', unifiedToken)
        
        // 更新用户信息
        await this.updateUserInfo(unifiedToken)
        
        console.log('✅ URL参数token同步成功')
        
        // 触发token更新事件
        this.triggerTokenUpdateEvent(unifiedToken)
        
        // 清除URL中的token参数
        this.clearTokenFromUrl()
        
        this.lastUnifiedToken = unifiedToken
        return true
        
      } else {
        // 对于其他来源的token，进行验证
        console.log('🔍 验证token有效性')
        const isValid = await this.validateToken(unifiedToken)
        
        if (isValid) {
          // 更新当前系统的token存储
          sessionStorage.setItem('accessToken', unifiedToken)
          localStorage.setItem('accessToken', unifiedToken)
          sessionStorage.setItem('token', unifiedToken)
          localStorage.setItem('token', unifiedToken)
          
          // 更新用户信息
          await this.updateUserInfo(unifiedToken)
          
          console.log('✅ 统一用户平台token同步成功')
          
          // 触发token更新事件
          this.triggerTokenUpdateEvent(unifiedToken)
          
          // 清除URL中的token参数
          this.clearTokenFromUrl()
          
          this.lastUnifiedToken = unifiedToken
          return true
          
        } else {
          console.warn('⚠️ 统一用户平台token验证失败')
          return false
        }
      }
      
    } catch (error) {
      console.error('❌ 统一用户平台token同步失败:', error)
      return false
    }
  }

  /**
   * 验证并同步当前token到统一用户平台
   */
  async validateAndSyncCurrentToken(currentToken) {
    try {
      console.log('🔍 验证当前token是否需要同步到统一用户平台')
      
      // 验证当前token在统一用户平台是否有效
      const isValid = await this.validateToken(currentToken)
      
      if (isValid) {
        console.log('✅ 当前token在统一用户平台有效，无需同步')
        this.lastUnifiedToken = currentToken
      } else {
        console.warn('⚠️ 当前token在统一用户平台无效，需要重新获取')
        // 清除无效token
        sessionStorage.removeItem('accessToken')
        localStorage.removeItem('accessToken')
        sessionStorage.removeItem('token')
        localStorage.removeItem('token')
        
        // 触发token失效事件
        this.triggerTokenInvalidEvent()
      }
      
    } catch (error) {
      console.error('❌ 验证当前token失败:', error)
    }
  }

  /**
   * 更新用户信息
   */
  async updateUserInfo(token) {
    try {
      // 获取用户信息
      const userInfo = await this.getUnifiedUserInfo(token)
      
      if (userInfo) {
        // 更新用户信息存储
        sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        
        console.log('✅ 用户信息更新成功:', userInfo.userName)
      }
      
    } catch (error) {
      console.error('❌ 更新用户信息失败:', error)
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
   * 尝试从其他地方获取token
   */
  async tryGetTokenFromOtherSources() {
          // 检查cookie
      const cookieToken = this.getTokenFromCookie()
      if (cookieToken) {
        console.log('🍪 从cookie获取到token')
        await this.syncUnifiedToken(cookieToken, 'cookie')
        return
      }
      
      // 检查用户信息对象中的token
      const userInfo = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo')
      if (userInfo) {
        try {
          const parsedUserInfo = JSON.parse(userInfo)
          if (parsedUserInfo.accessToken) {
            console.log('👤 从用户信息对象获取到token')
            await this.syncUnifiedToken(parsedUserInfo.accessToken, 'userInfo')
            return
          }
        } catch (error) {
          console.log('❌ 解析用户信息失败:', error.message)
        }
      }
    
    console.log('💡 未找到可用token')
  }

  /**
   * 通过iframe获取统一用户平台的token
   */
  async getTokenFromUnifiedPlatformIframe() {
    try {
      // 创建一个隐藏的iframe来访问统一用户平台
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = 'http://oort.oortcloudsmart.com:21410/bus/apaas-web/desktopHome/index.html#/home'
      
      return new Promise((resolve) => {
        const timeout = setTimeout(() => {
          document.body.removeChild(iframe)
          resolve(null)
        }, 5000) // 5秒超时
        
        iframe.onload = () => {
          try {
            // 尝试从iframe中获取token
            const iframeWindow = iframe.contentWindow
            const iframeToken = iframeWindow.localStorage.getItem('accessToken') || 
                               iframeWindow.localStorage.getItem('token') ||
                               iframeWindow.sessionStorage.getItem('accessToken') ||
                               iframeWindow.sessionStorage.getItem('token')
            
            clearTimeout(timeout)
            document.body.removeChild(iframe)
            resolve(iframeToken)
          } catch (error) {
            // 跨域限制，无法直接访问iframe内容
            clearTimeout(timeout)
            document.body.removeChild(iframe)
            resolve(null)
          }
        }
        
        document.body.appendChild(iframe)
      })
    } catch (error) {
      console.error('通过iframe获取token失败:', error)
      return null
    }
  }

  /**
   * 从localStorage获取统一用户平台的token
   */
  getTokenFromUnifiedPlatformLocalStorage() {
    try {
      // 尝试从可能的统一用户平台token存储位置获取
      const possibleKeys = [
        'unified_accessToken',
        'unified_token',
        'apaas_accessToken',
        'apaas_token',
        'sso_accessToken',
        'sso_token'
      ]
      
      for (const key of possibleKeys) {
        const token = localStorage.getItem(key) || sessionStorage.getItem(key)
        if (token) {
          return token
        }
      }
      
      return null
    } catch (error) {
      console.error('从localStorage获取统一用户平台token失败:', error)
      return null
    }
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
    window.history.replaceState({}, document.title, url.toString())
  }

  /**
   * 触发token更新事件
   */
  triggerTokenUpdateEvent(token) {
    const event = new CustomEvent('crossSystemTokenUpdated', {
      detail: { token, source: 'unified-platform' }
    })
    window.dispatchEvent(event)
  }

  /**
   * 触发token失效事件
   */
  triggerTokenInvalidEvent() {
    const event = new CustomEvent('crossSystemTokenInvalid')
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
    console.log('🛑 跨系统Token同步器已停止')
  }

  /**
   * 手动触发同步
   */
  forceSync() {
    console.log('🔄 手动触发跨系统token同步')
    this.checkAndSyncCrossSystemToken()
  }
}

// 创建全局实例
export const crossSystemTokenSync = new CrossSystemTokenSync()

// 自动初始化
if (typeof window !== 'undefined') {
  // 将实例暴露到全局window对象，方便调试
  window.crossSystemTokenSync = crossSystemTokenSync
  
  // 等待DOM加载完成后初始化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      crossSystemTokenSync.init()
    })
  } else {
    crossSystemTokenSync.init()
  }
} 