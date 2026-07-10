/**
 * 自动跨系统Token同步器
 * 实现真正的自动跨系统token同步，无需手动操作
 */

class AutoCrossSystemSync {
  constructor() {
    this.syncInterval = null
    this.isInitialized = false
    this.lastUnifiedToken = null
    this.checkInterval = 5000 // 5秒检查一次
    this.unifiedPlatformUrl = 'http://oort.oortcloudsmart.com:21410/bus/apaas-web/desktopHome/index.html#/home'
    this.apiBaseUrl = 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso'
    this.appId = 'vls-ui'
    this.secretKey = 'vls-ui-secret'
    this.iframe = null
    this.iframeCheckInterval = null
    
    // 智能验证缓存
    this.tokenValidationCache = new Map() // token验证结果缓存
    this.failedTokenCache = new Set() // 失败token缓存
    this.lastValidationTime = 0 // 上次验证时间
    this.validationCooldown = 30000 // 验证冷却时间30秒
    this.maxFailedAttempts = 3 // 最大失败次数
    this.failedAttempts = new Map() // token失败次数记录
    
    // 防重复运行
    this.isRunning = false // 防止重复运行
    this.lastCheckTime = 0 // 上次检查时间
    this.minCheckInterval = 2000 // 最小检查间隔2秒
  }

  /**
   * 初始化自动跨系统token同步
   */
  init() {
    if (this.isInitialized) {
      console.log('🔄 自动跨系统Token同步器已初始化')
      return
    }

    console.log('🔄 初始化自动跨系统Token同步器')
    this.isInitialized = true

    // 监听页面可见性变化
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        console.log('📱 页面变为可见，检查token同步')
        this.checkAndSyncCrossSystemToken()
      }
    })

    // 监听storage变化
    window.addEventListener('storage', (event) => {
      if (event.key === 'accessToken' || event.key === 'token') {
        console.log('🔄 检测到storage变化，立即检查跨系统token同步')
        this.checkAndSyncCrossSystemToken()
      }
    })

    // 监听URL变化
    this.watchUrlChanges()

    // 定期检查token同步
    this.syncInterval = setInterval(() => {
      this.checkAndSyncCrossSystemToken()
    }, this.checkInterval)

    // 启动iframe监听
    this.startIframeMonitoring()

    // 立即执行一次检查
    this.checkAndSyncCrossSystemToken()
  }

  /**
   * 监听URL变化
   */
  watchUrlChanges() {
    let currentUrl = window.location.href

    // 使用MutationObserver监听URL变化
    const observer = new MutationObserver(() => {
      if (window.location.href !== currentUrl) {
        currentUrl = window.location.href
        console.log('🔗 URL发生变化，检查token同步')
        this.checkAndSyncCrossSystemToken()
      }
    })

    observer.observe(document, { subtree: true, childList: true })

    // 监听popstate事件
    window.addEventListener('popstate', () => {
      console.log('🔗 浏览器前进后退，检查token同步')
      this.checkAndSyncCrossSystemToken()
    })
  }

  /**
   * 启动iframe监听统一用户平台token变化
   */
  startIframeMonitoring() {
    try {
      console.log('🖼️ 启动iframe监听统一用户平台token变化')
      
      // 创建隐藏的iframe
      this.iframe = document.createElement('iframe')
      this.iframe.src = this.unifiedPlatformUrl
      this.iframe.style.display = 'none'
      this.iframe.style.width = '0'
      this.iframe.style.height = '0'
      this.iframe.style.border = 'none'
      
      // 监听iframe加载完成
      this.iframe.onload = () => {
        console.log('🖼️ iframe加载完成，开始监听token变化')
        this.startIframeTokenCheck()
      }
      
      // 添加到页面
      document.body.appendChild(this.iframe)
      
    } catch (error) {
      console.error('❌ 启动iframe监听失败:', error)
    }
  }

  /**
   * 开始iframe token检查
   */
  startIframeTokenCheck() {
    if (this.iframeCheckInterval) {
      clearInterval(this.iframeCheckInterval)
    }
    
    // 每3秒检查一次iframe中的token
    this.iframeCheckInterval = setInterval(() => {
      this.checkIframeToken()
    }, 3000)
  }

  /**
   * 检查iframe中的token
   */
  async checkIframeToken() {
    try {
      if (!this.iframe || !this.iframe.contentWindow) {
        return
      }

      // 尝试从iframe中获取token
      const iframeWindow = this.iframe.contentWindow
      const iframeToken = iframeWindow.sessionStorage.getItem('token') ||
                         iframeWindow.sessionStorage.getItem('accessToken') ||
                         iframeWindow.localStorage.getItem('token') ||
                         iframeWindow.localStorage.getItem('accessToken')
      
      // 过滤掉无效的token值
      if (iframeToken && 
          iframeToken !== 'undefined' && 
          iframeToken !== 'null' && 
          iframeToken.trim() !== '' &&
          iframeToken !== this.lastUnifiedToken) {
        
        console.log('🖼️ 检测到iframe中token变化:', iframeToken.substring(0, 8) + '...')
        
        // 验证token有效性
        const isValid = await this.validateToken(iframeToken)
        if (isValid) {
          console.log('✅ iframe中token验证成功，开始同步')
          await this.syncToken(iframeToken, 'iframe')
        } else {
          console.log('❌ iframe中token验证失败')
        }
      }
    } catch (error) {
      // iframe跨域错误是正常的，不打印错误
      // console.log('🖼️ iframe检查跳过（跨域限制）')
    }
  }

  /**
   * 检查并同步跨系统token
   */
  async checkAndSyncCrossSystemToken() {
    // 防重复运行检查
    if (this.isRunning) {
      console.log('⏳ 同步检查正在运行中，跳过本次检查')
      return
    }
    
    const now = Date.now()
    if (now - this.lastCheckTime < this.minCheckInterval) {
      console.log('⏳ 检查间隔太短，跳过本次检查')
      return
    }
    
    this.isRunning = true
    this.lastCheckTime = now
    
    try {
      console.log('🔍 自动检查跨系统token同步')
      
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
        console.log('🔄 检测到统一用户平台新token，开始自动同步')
        await this.syncToken(unifiedToken, 'auto')
      }
      
      // 如果当前系统有token但统一用户平台没有，检查是否需要同步
      else if (currentToken && !unifiedToken) {
        console.log('🔍 检查当前token是否需要同步到统一用户平台')
        await this.validateAndSyncCurrentToken(currentToken)
      }
      
      // 如果都没有token，尝试从其他地方获取（但避免重复处理）
      else if (!currentToken && !unifiedToken) {
        console.log('🔍 尝试从其他地方获取token')
        await this.tryGetTokenFromOtherSources()
      }
      
      // 如果token相同，验证有效性
      else if (currentToken && unifiedToken && currentToken === unifiedToken) {
        console.log('🔍 检测到token相同，验证有效性')
        const isValid = await this.validateToken(currentToken)
        
        if (isValid) {
          console.log('✅ 跨系统Token已同步且有效')
          this.lastUnifiedToken = unifiedToken
        } else {
          console.log('⚠️ 跨系统Token相同但验证失败')
          
          // 检查token是否真的无效
          if (currentToken && currentToken !== 'undefined' && currentToken !== 'null') {
            console.log('🔍 token看起来是有效的，可能是网络问题，不立即清除')
            // 不立即清除，可能是网络问题或临时验证失败
          } else {
            console.log('🧹 token确实是无效的，清除token')
            // 只清除无效token
            this.clearInvalidToken()
            
            // 尝试获取新的有效token
            await this.tryGetTokenFromOtherSources()
          }
        }
      }
      
    } catch (error) {
      console.error('❌ 自动跨系统Token同步检查失败:', error)
    } finally {
      // 确保运行状态正确
      this.isRunning = false
    }
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
      
      // 方法2：通过API获取统一用户平台的当前用户信息
      const currentToken = this.getCurrentSystemToken()
      if (currentToken) {
        const userInfo = await this.getUnifiedUserInfo(currentToken)
        if (userInfo && userInfo.accessToken) {
          console.log('👤 从统一用户平台API获取到token')
          return userInfo.accessToken
        }
      }
      
      // 方法3：尝试从localStorage获取统一用户平台的token
      const localStorageToken = await this.getTokenFromLocalStorage()
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
   * 从localStorage获取统一用户平台的token
   */
  async getTokenFromLocalStorage() {
    try {
      // 优先从sessionStorage获取（统一用户平台使用sessionStorage）
      const possibleKeys = ['token', 'accessToken']
      
      for (const key of possibleKeys) {
        const token = sessionStorage.getItem(key) || localStorage.getItem(key)
        
        // 过滤掉无效的token值
        if (token && 
            token !== 'undefined' && 
            token !== 'null' && 
            token.trim() !== '') {
          
          console.log(`🔍 找到token: ${key} = ${token.substring(0, 8)}...`)
          // 验证token有效性
          const isValid = await this.validateToken(token)
          if (isValid) {
            console.log(`✅ token验证成功: ${key}`)
            return token
          } else {
            console.log(`❌ token验证失败: ${key}`)
          }
        }
      }
      
      return null
    } catch (error) {
      console.error('从localStorage获取token失败:', error)
      return null
    }
  }

  /**
   * 同步token到当前系统
   */
  async syncToken(token, source = 'auto') {
    try {
      console.log(`🔄 开始同步token，来源: ${source}`)
      
      // 验证token有效性
      const isValid = await this.validateToken(token)
      if (!isValid) {
        console.log('❌ token验证失败，跳过同步')
        return false
      }
      
      // 更新本地存储
      sessionStorage.setItem('accessToken', token)
      localStorage.setItem('accessToken', token)
      sessionStorage.setItem('token', token)
      localStorage.setItem('token', token)
      
      // 更新用户信息
      await this.updateUserInfo(token)
      
      // 触发token更新事件
      this.triggerTokenUpdateEvent(token, source)
      
      // 清除URL中的token参数
      this.clearTokenFromUrl()
      
      console.log('✅ token同步成功')
      this.lastUnifiedToken = token
      
      return true
      
    } catch (error) {
      console.error('❌ token同步失败:', error)
      return false
    }
  }

  /**
   * 验证并同步当前token
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
        console.log('⚠️ 当前token在统一用户平台验证失败')
        
        // 检查当前token是否真的是无效的
        const currentTokenCheck = this.getCurrentSystemToken()
        if (currentTokenCheck && currentTokenCheck !== 'undefined' && currentTokenCheck !== 'null') {
          console.log('🔍 当前token看起来是有效的，可能是网络问题，不立即清除')
          // 不立即清除，可能是网络问题或临时验证失败
          // 检查是否在失败缓存中
          if (this.failedTokenCache.has(currentTokenCheck)) {
            console.log('🚫 token在失败缓存中，但保留用户信息')
          } else {
            console.log('💡 token可能暂时验证失败，保留token和用户信息')
          }
        } else {
          console.log('🧹 当前token确实是无效的，清除token')
          // 只清除无效token，不清除用户信息
          this.clearInvalidToken()
        }
        
        // 不触发token失效事件，避免清空用户界面
        // this.triggerTokenInvalidEvent()
      }
      
    } catch (error) {
      console.error('❌ 验证当前token失败:', error)
    }
  }

  /**
   * 尝试从其他地方获取token
   */
  async tryGetTokenFromOtherSources() {
    // 检查cookie
    const cookieToken = this.getTokenFromCookie()
    if (cookieToken && !this.failedTokenCache.has(cookieToken)) {
      console.log('🍪 从cookie获取到token')
      await this.syncToken(cookieToken, 'cookie')
      return
    }
    
    // 检查用户信息对象中的token
    const userInfo = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo')
    if (userInfo) {
      try {
        const parsedUserInfo = JSON.parse(userInfo)
        if (parsedUserInfo.accessToken && 
            parsedUserInfo.accessToken !== 'undefined' && 
            parsedUserInfo.accessToken !== 'null' && 
            parsedUserInfo.accessToken.trim() !== '' &&
            !this.failedTokenCache.has(parsedUserInfo.accessToken)) {
          console.log('👤 从用户信息对象获取到token')
          await this.syncToken(parsedUserInfo.accessToken, 'userInfo')
          return
        } else if (parsedUserInfo.accessToken) {
          console.log('🚫 用户信息中的token已在失败缓存中或无效，跳过')
        }
      } catch (error) {
        console.log('❌ 解析用户信息失败:', error.message)
      }
    }
    
    console.log('💡 未找到可用token或所有token都已验证失败')
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
   * 获取统一用户平台的用户信息
   */
  async getUnifiedUserInfo(token) {
    try {
      const response = await fetch(`${this.apiBaseUrl}/sso/v1/getUserInfo`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'requesttype': 'app',
          'appid': this.appId,
          'secretkey': this.secretKey,
          'accesstoken': token
        },
        body: JSON.stringify({
          accessToken: token
        })
      })
      
      if (response.ok) {
        const result = await response.json()
        if (result.code === 200 || result.success === true) {
          return result.data || result
        }
      }
      
      return null
    } catch (error) {
      console.error('获取统一用户平台用户信息失败:', error)
      return null
    }
  }

  /**
   * 验证token有效性
   */
  async validateToken(token) {
    if (!token) {
      return false
    }
    
    // 检查是否在失败缓存中
    if (this.failedTokenCache.has(token)) {
      console.log('🚫 token在失败缓存中，跳过验证:', token.substring(0, 8) + '...')
      return false
    }
    
    // 检查失败次数
    const failedCount = this.failedAttempts.get(token) || 0
    if (failedCount >= this.maxFailedAttempts) {
      console.log('🚫 token失败次数过多，加入失败缓存:', token.substring(0, 8) + '...')
      this.failedTokenCache.add(token)
      return false
    }
    
    // 检查验证冷却时间
    const now = Date.now()
    if (now - this.lastValidationTime < this.validationCooldown) {
      console.log('⏳ 验证冷却中，跳过验证')
      // 冷却期间不返回false，而是返回缓存结果或true
      if (this.tokenValidationCache.has(token)) {
        const cachedResult = this.tokenValidationCache.get(token)
        console.log('💾 冷却期间使用缓存验证结果:', token.substring(0, 8) + '...', cachedResult)
        return cachedResult
      }
      // 如果没有缓存，冷却期间暂时认为token有效，避免错误清除
      console.log('💡 冷却期间无缓存，暂时认为token有效')
      return true
    }
    
    // 检查缓存
    if (this.tokenValidationCache.has(token)) {
      const cachedResult = this.tokenValidationCache.get(token)
      console.log('💾 使用缓存验证结果:', token.substring(0, 8) + '...', cachedResult)
      return cachedResult
    }
    
    try {
      console.log('🔍 开始验证token:', token.substring(0, 8) + '...')
      this.lastValidationTime = now
      
      const response = await fetch(`${this.apiBaseUrl}/sso/v1/verifyToken`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'requesttype': 'app',
          'appid': this.appId,
          'secretkey': this.secretKey,
          'accesstoken': token
        },
        body: JSON.stringify({
          accessToken: token
        })
      })
      
      let isValid = false
      if (response.ok) {
        const result = await response.json()
        isValid = result.code === 200 || result.success === true
        
        // 改进：检查特定的错误类型
        if (!isValid) {
          const errorCode = result.code
          const errorMsg = result.msg || result.message
          
          if (errorCode === 400) {
            console.log('⚠️ 收到400错误，可能是token格式问题，但不立即标记为无效')
            // 不立即增加失败次数，给token一个机会
            this.tokenValidationCache.set(token, false)
            return false
          }
        }
      } else {
        // 处理HTTP错误状态
        if (response.status === 400) {
          console.log('⚠️ 收到400错误，可能是token格式问题，但不立即标记为无效')
          this.tokenValidationCache.set(token, false)
          return false
        } else if (response.status === 500) {
          console.log('⚠️ 服务器内部错误，不立即标记token为无效')
          return false
        } else {
          console.log('⚠️ HTTP错误状态:', response.status, '不立即标记token为无效')
          return false
        }
      }
      
      // 缓存验证结果
      this.tokenValidationCache.set(token, isValid)
      
      // 更新失败次数
      if (!isValid) {
        const newFailedCount = failedCount + 1
        this.failedAttempts.set(token, newFailedCount)
        console.log(`❌ token验证失败 (${newFailedCount}/${this.maxFailedAttempts}):`, token.substring(0, 8) + '...')
        
        if (newFailedCount >= this.maxFailedAttempts) {
          this.failedTokenCache.add(token)
        }
      } else {
        console.log('✅ token验证成功:', token.substring(0, 8) + '...')
        this.failedAttempts.delete(token) // 清除失败记录
      }
      
      return isValid
    } catch (error) {
      console.error('❌ Token验证异常:', error)
      
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
      
      // 更新失败次数
      const newFailedCount = failedCount + 1
      this.failedAttempts.set(token, newFailedCount)
      
      if (newFailedCount >= this.maxFailedAttempts) {
        this.failedTokenCache.add(token)
      }
      
      return false
    }
  }

  /**
   * 获取当前系统token
   */
  getCurrentSystemToken() {
    // 优先级：URL > sessionStorage > localStorage
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ autoCrossSystemSync使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }
    
    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      console.log('✅ autoCrossSystemSync使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }
    
    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      console.log('✅ autoCrossSystemSync使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }
    
    console.log('❌ autoCrossSystemSync没有找到任何token')
    return null
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
    window.history.replaceState({}, '', url.toString())
  }

  /**
   * 清除无效token
   */
  clearInvalidToken() {
    console.log('🧹 清除无效token，保留用户信息')
    
    // 只清除token，不清除用户信息
    sessionStorage.removeItem('accessToken')
    localStorage.removeItem('accessToken')
    sessionStorage.removeItem('token')
    localStorage.removeItem('token')
    
    // 不清除用户信息，让用户保持登录状态
    // sessionStorage.removeItem('userInfo')
    // localStorage.removeItem('userInfo')
  }

  /**
   * 清理验证缓存
   */
  clearValidationCache() {
    this.tokenValidationCache.clear()
    this.failedTokenCache.clear()
    this.failedAttempts.clear()
    this.lastValidationTime = 0
    console.log('🧹 验证缓存已清理')
  }

  /**
   * 重置失败记录
   */
  resetFailedAttempts() {
    this.failedAttempts.clear()
    this.failedTokenCache.clear()
    console.log('🔄 失败记录已重置')
  }

  /**
   * 触发token更新事件
   */
  triggerTokenUpdateEvent(token, source) {
    const event = new CustomEvent('autoCrossSystemTokenUpdated', {
      detail: { token, source }
    })
    window.dispatchEvent(event)
    console.log('📡 触发token更新事件:', source)
  }

  /**
   * 触发token失效事件
   */
  triggerTokenInvalidEvent() {
    const event = new CustomEvent('autoCrossSystemTokenInvalid')
    window.dispatchEvent(event)
    console.log('📡 触发token失效事件')
  }

  /**
   * 停止同步
   */
  stop() {
    console.log('🛑 停止自动跨系统Token同步')
    
    if (this.syncInterval) {
      clearInterval(this.syncInterval)
      this.syncInterval = null
    }
    
    if (this.iframeCheckInterval) {
      clearInterval(this.iframeCheckInterval)
      this.iframeCheckInterval = null
    }
    
    if (this.iframe && this.iframe.parentNode) {
      this.iframe.parentNode.removeChild(this.iframe)
      this.iframe = null
    }
    
    this.isInitialized = false
  }

  /**
   * 强制同步
   */
  forceSync() {
    console.log('🔄 强制触发token同步')
    this.checkAndSyncCrossSystemToken()
  }
}

// 自动初始化
if (typeof window !== 'undefined') {
  const autoCrossSystemSync = new AutoCrossSystemSync()
  autoCrossSystemSync.init()
} 