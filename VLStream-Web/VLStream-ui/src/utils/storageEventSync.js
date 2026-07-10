// 使用Storage事件监听同步token
class StorageEventSync {
  constructor() {
    this.isInitialized = false
    this.requestInterval = null
    this.platformCheckInterval = null
    this.lastRequestTime = 0
    this.requestTimeout = 5000 // 5秒超时
    this.pauseTokenValidation = false // 添加暂停验证标志
  }

  // 初始化
  init() {
    if (this.isInitialized) return
    
    // 在初始化时验证并清理无效token
    this.validateAndCleanTokens()
    
    // 监听storage事件
    window.addEventListener('storage', this.handleStorageChange.bind(this))
    
    // 监听来自其他窗口的token请求
    window.addEventListener('storage', this.handleTokenRequest.bind(this))
    
    // 监听来自统一用户平台的postMessage
    window.addEventListener('message', this.handleUnifiedPlatformMessage.bind(this))
    
    // 立即请求一次token
    this.requestTokenFromOtherWindows()
    
    // 每10秒请求一次token（避免频繁请求）
    this.requestInterval = setInterval(() => {
      this.requestTokenFromOtherWindows()
    }, 10000)
    
    // 添加定时检查统一用户平台token
    this.platformCheckInterval = setInterval(() => {
      this.checkPlatformToken()
    }, 30000) // 每30秒检查一次
    
    // 监听页面焦点变化
    window.addEventListener('focus', () => {
      console.log('🔍 页面获得焦点，检查token更新...')
      this.checkPlatformToken()
    })
    
    // 监听页面可见性变化
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        console.log('🔍 页面变为可见，检查token更新...')
        this.checkPlatformToken()
      }
    })
    
    this.isInitialized = true
    console.log('📡 Storage事件token同步已初始化（包含平台检查和跨域通信）')
  }

  // 请求其他窗口的token
  requestTokenFromOtherWindows() {
    const now = Date.now()
    const requestId = 'token_request_' + now + '_' + Math.random().toString(36).substr(2, 9)
    
    // 发送token请求信号
    const requestData = {
      type: 'REQUEST_TOKEN',
      requestId: requestId,
      timestamp: now,
      origin: window.location.origin,
      requester: 'VLStream-ui'
    }
    
    try {
      localStorage.setItem(requestId, JSON.stringify(requestData))
      console.log('📡 已发送token请求:', requestId)
      
      // 设置超时清理
      setTimeout(() => {
        try {
          localStorage.removeItem(requestId)
        } catch (e) {
          // 忽略清理错误
        }
      }, this.requestTimeout)
      
    } catch (error) {
      console.error('❌ 发送token请求失败:', error)
    }
  }

  // 处理token请求（作为token提供者）
  handleTokenRequest(event) {
    if (!event.key || !event.key.startsWith('token_request_')) return
    if (!event.newValue) return
    
    try {
      const requestData = JSON.parse(event.newValue)
      if (requestData.type === 'REQUEST_TOKEN' && requestData.requester !== 'VLStream-ui') {
        // 其他窗口请求token，我们提供token
        this.respondWithToken(requestData.requestId)
      }
    } catch (error) {
      console.error('❌ 处理token请求失败:', error)
    }
  }

  // 验证并清理无效token
  async validateAndCleanTokens() {
    try {
      const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
      const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
      
      // 如果sessionStorage和localStorage的token不同，优先使用sessionStorage
      if (sessionToken && localToken && sessionToken !== localToken) {
        console.log('🧹 检测到token不一致，清理localStorage中的旧token')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('token')
        localStorage.setItem('accessToken', sessionToken)
        localStorage.setItem('token', sessionToken)
      }
      
      // 如果只有localStorage有token，同步到sessionStorage
      if (!sessionToken && localToken) {
        console.log('🔄 将localStorage中的token同步到sessionStorage')
        sessionStorage.setItem('accessToken', localToken)
        sessionStorage.setItem('token', localToken)
      }
      
    } catch (error) {
      console.error('❌ 验证和清理token失败:', error)
    }
  }

  // 处理token验证失败
  handleTokenValidationFailure() {
    try {
      console.log('🚫 Token验证失败，但暂时不清理token（避免误删新token）')
      
      // 暂时注释掉token清理逻辑，避免误删新token
      // // 清理sessionStorage中的token
      // sessionStorage.removeItem('accessToken')
      // sessionStorage.removeItem('token')
      // 
      // // 清理localStorage中的token（但保留用户信息）
      // localStorage.removeItem('accessToken')
      // localStorage.removeItem('token')
      
      // 只请求新的token，不清理现有token
      this.requestTokenFromOtherWindows()
      
      console.log('🔄 已请求新token，但保留现有token')
    } catch (error) {
      console.error('❌ 处理token验证失败时出错:', error)
    }
  }

  // 响应token请求
  respondWithToken(requestId) {
    try {
      // 严格按优先级获取token：sessionStorage > localStorage
      const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
      const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
      
      // 优先使用sessionStorage中的token
      const token = sessionToken || localToken
      const userInfo = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo')
      
      if (token) {
        // 如果token来自localStorage但sessionStorage为空，先同步到sessionStorage
        if (!sessionToken && localToken) {
          sessionStorage.setItem('accessToken', localToken)
          sessionStorage.setItem('token', localToken)
          console.log('🔄 已将localStorage中的token同步到sessionStorage')
        }
        
        const responseId = 'token_response_' + requestId.replace('token_request_', '')
        const responseData = {
          type: 'TOKEN_RESPONSE',
          token: token,
          userInfo: userInfo ? JSON.parse(userInfo) : null,
          timestamp: Date.now(),
          responderId: 'unified_platform',
          source: sessionToken ? 'sessionStorage' : 'localStorage'
        }
        
        localStorage.setItem(responseId, JSON.stringify(responseData))
        console.log('📡 已响应token请求:', responseId, `token来源: ${responseData.source}`, token.substring(0, 8) + '...')
        
        // 设置清理定时器
        setTimeout(() => {
          try {
            localStorage.removeItem(responseId)
          } catch (e) {
            // 忽略清理错误
          }
        }, 3000)
      } else {
        console.log('⚠️ 没有找到有效的token来响应请求')
      }
    } catch (error) {
      console.error('❌ 响应token请求失败:', error)
    }
  }

  // 保存token
  saveToken(token, userInfo) {
    try {
      // 优先保存到sessionStorage
      sessionStorage.setItem('accessToken', token)
      sessionStorage.setItem('token', token)
      
      // 同步到localStorage（用于跨标签页同步）
      localStorage.setItem('accessToken', token)
      localStorage.setItem('token', token)
      
      if (userInfo) {
        const userInfoStr = typeof userInfo === 'string' ? userInfo : JSON.stringify(userInfo)
        sessionStorage.setItem('userInfo', userInfoStr)
        localStorage.setItem('userInfo', userInfoStr)
      }
      
      console.log('💾 Token已保存到本地存储（sessionStorage优先）')
      
      // 触发更新事件
      this.triggerUpdateEvent(token, userInfo)
      
    } catch (error) {
      console.error('❌ 保存token失败:', error)
    }
  }

  // 获取当前有效token
  getCurrentToken() {
    // 优先级：URL > sessionStorage > localStorage
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ storageEventSync使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }
    
    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      console.log('✅ storageEventSync使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }
    
    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      console.log('✅ storageEventSync使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }
    
    console.log('❌ storageEventSync没有找到任何token')
    return null
  }

  // 触发更新事件
  triggerUpdateEvent(token, userInfo) {
    const event = new CustomEvent('storageEventTokenUpdated', {
      detail: { token, userInfo }
    })
    window.dispatchEvent(event)
  }

  // 检查统一用户平台的token
  // 检查统一用户平台的token
  checkPlatformToken() {
    try {
      console.log('🔍 检查统一用户平台token更新...')
      
      // 检查是否有新的token在sessionStorage中
      const currentToken = this.getCurrentToken()
      const platformToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
      
      if (platformToken && platformToken !== currentToken) {
        console.log('🆕 发现新的平台token，正在同步...', platformToken.substring(0, 8) + '...')
        
        // 获取用户信息
        const userInfo = sessionStorage.getItem('userInfo')
        
        // 保存新token
        this.saveToken(platformToken, userInfo ? JSON.parse(userInfo) : null)
        
        return true
      } else if (!platformToken && !currentToken) {
        console.log('⚠️ 未找到任何token，尝试主动获取')
        this.requestTokenFromOtherWindows()
        
        // 尝试通过iframe获取统一用户平台的token
        this.fetchTokenFromUnifiedPlatform()
        
      } else if (currentToken) {
        // 验证当前token是否仍然有效
        this.validateCurrentToken(currentToken)
      }
      
      return false
    } catch (error) {
      console.error('❌ 检查平台token失败:', error)
      return false
    }
  }

  // 通过iframe获取统一用户平台的token
  fetchTokenFromUnifiedPlatform() {
    try {
      console.log('🔗 尝试通过API获取统一用户平台token...')
      
      // 方法1：尝试通过正确的API调用方式获取token
      this.fetchTokenViaAPI()
      
      // 方法2：如果API失败，尝试通过iframe访问统一用户平台主页面
      this.fetchTokenViaIframe()
      
    } catch (error) {
      console.error('❌ 获取统一用户平台token失败:', error)
      // 如果所有方法都失败，重定向到登录页面
      setTimeout(() => {
        this.redirectToLogin()
      }, 5000)
    }
  }

  // 通过API获取token
  async fetchTokenViaAPI() {
    try {
      const currentToken = this.getCurrentToken()
      if (!currentToken) {
        console.log('⚠️ 没有当前token，无法通过API获取')
        return false
      }
      
      // 使用正确的API端点和请求方式
      const response = await fetch('http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/getUserInfo', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'requesttype': 'app',
          'appid': '6551b0147c4649a894e86bf8de248da4',
          'secretkey': '58f9eeefc65f4b318204ba21f39a8861',
          'accesstoken': currentToken
        },
        body: JSON.stringify({
          accessToken: currentToken
        })
      })
      
      if (response.ok) {
        const result = await response.json()
        if (result.data && result.data.accessToken) {
          console.log('🎉 通过API获取到新token:', result.data.accessToken.substring(0, 8) + '...')
          this.saveToken(result.data.accessToken, result.data)
          return true
        }
      } else {
        console.log('⚠️ API调用失败:', response.status, response.statusText)
      }
      
      return false
    } catch (error) {
      console.error('❌ API获取token失败:', error)
      return false
    }
  }

  // 通过iframe获取token（访问主页面而不是API端点）
  fetchTokenViaIframe() {
    try {
      console.log('🔗 尝试通过iframe访问统一用户平台主页面...')
      
      // 创建隐藏iframe，访问统一用户平台的主页面而不是API端点
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = 'http://oort.oortcloudsmart.com:21410/bus/apaas-web' // 访问主页面
      
      // 设置超时
      const timeout = setTimeout(() => {
        if (iframe.parentNode) {
          document.body.removeChild(iframe)
        }
        console.log('⏰ iframe获取token超时')
      }, 10000)
      
      // 监听iframe加载完成
      iframe.onload = () => {
        try {
          // 发送postMessage请求token
          const iframeWindow = iframe.contentWindow
          if (iframeWindow) {
            iframeWindow.postMessage({
              type: 'REQUEST_TOKEN',
              origin: window.location.origin,
              timestamp: Date.now()
            }, 'http://oort.oortcloudsmart.com:21410')
            
            console.log('📤 已向统一用户平台主页面发送token请求')
          }
        } catch (error) {
          console.error('❌ 访问iframe失败:', error)
        }
        
        // 清理
        clearTimeout(timeout)
        setTimeout(() => {
          if (iframe.parentNode) {
            document.body.removeChild(iframe)
          }
        }, 5000)
      }
      
      iframe.onerror = () => {
        console.error('❌ iframe加载失败')
        clearTimeout(timeout)
        if (iframe.parentNode) {
          document.body.removeChild(iframe)
        }
      }
      
      document.body.appendChild(iframe)
      
    } catch (error) {
      console.error('❌ iframe方法失败:', error)
    }
  }

  // 尝试从URL获取token
  tryGetTokenFromUrl() {
    try {
      const urlParams = new URLSearchParams(window.location.search)
      const urlToken = urlParams.get('token') || urlParams.get('accessToken')
      
      if (urlToken) {
        console.log('🔗 从URL获取到token:', urlToken.substring(0, 8) + '...')
        this.saveToken(urlToken, null)
        
        // 清理URL中的token参数
        const newUrl = new URL(window.location)
        newUrl.searchParams.delete('token')
        newUrl.searchParams.delete('accessToken')
        window.history.replaceState({}, '', newUrl)
        
        return true
      }
      
      return false
    } catch (error) {
      console.error('❌ 从URL获取token失败:', error)
      return false
    }
  }

  // 验证当前token
  // 验证当前token
  async validateCurrentToken(token) {
    // 检查是否暂停验证
    if (this.pauseTokenValidation) {
      console.log('⏸️ Token验证已暂停，跳过验证')
      return true
    }
    
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
        const isValid = (result.data && result.data.code === 200) || 
                       (result.code === 200) || 
                       (result.success === true)
        
        if (!isValid) {
          // 检查错误类型，避免因"被挤下线"而清理新token
          const errorCode = result.data?.code || result.code
          const errorMsg = result.data?.msg || result.msg
          
          if (errorCode === 4004 && errorMsg && errorMsg.includes('被挤下线')) {
            console.log('⚠️ 用户在其他地方登录，但不清理token（可能是新用户）')
            return false
          } else if (errorCode === 400) {
            console.log('⚠️ 收到400错误，可能是token格式问题，但不立即清除token')
            return false
          } else {
            console.log('🚫 当前token验证失败，错误信息:', errorMsg)
            console.log('🚫 当前token验证失败，但暂时不清理token（避免误删新token）')
            // 暂时注释掉token清理调用
            // this.handleTokenValidationFailure()
          }
        } else {
          console.log('✅ 当前token仍然有效')
        }
        
        return isValid
      } else {
        // 处理HTTP错误状态
        if (response.status === 400) {
          console.log('⚠️ 收到400错误，可能是token格式问题，但不立即清除token')
          return false
        } else if (response.status === 500) {
          console.log('⚠️ 服务器内部错误，不立即清除token')
          return false
        } else {
          console.log('⚠️ HTTP错误状态:', response.status, '不立即清除token')
          return false
        }
      }
    } catch (error) {
      console.error('❌ Token验证异常:', error)
      
      // 检查是否是网络错误
      if (error.message?.includes('Network Error') || error.code === 'NETWORK_ERROR') {
        console.log('⚠️ 网络错误，不立即清除token')
        return false
      }
      
      // 检查是否是超时错误
      if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
        console.log('⚠️ 请求超时，不立即清除token')
        return false
      }
      
      console.log('⚠️ 其他验证异常，不立即清除token')
      return false
    }
  }

  // 处理storage变化（接收token响应）
  handleStorageChange(event) {
    // 处理token响应
    if (event.key && event.key.startsWith('token_response_') && event.newValue) {
      try {
        const responseData = JSON.parse(event.newValue)
        if (responseData.type === 'TOKEN_RESPONSE' && responseData.token) {
          console.log('🔄 通过Storage事件收到token:', responseData.token.substring(0, 8) + '...')
          
          // 检查token是否与当前不同
          const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
          if (responseData.token !== currentToken) {
            // 保存新token
            this.saveToken(responseData.token, responseData.userInfo)
          }
          
          // 清理响应数据
          setTimeout(() => {
            try {
              localStorage.removeItem(event.key)
            } catch (e) {
              // 忽略清理错误
            }
          }, 1000)
        }
      } catch (error) {
        console.error('❌ 解析token响应失败:', error)
      }
    }
    
    // 处理直接的token更新事件
    if (event.key === 'shared_token_update' && event.newValue) {
      try {
        const updateData = JSON.parse(event.newValue)
        if (updateData.token) {
          console.log('🔄 检测到共享token更新:', updateData.token.substring(0, 8) + '...')
          this.saveToken(updateData.token, updateData.userInfo)
        }
      } catch (error) {
        console.error('❌ 处理共享token更新失败:', error)
      }
    }
  }

  // 广播token更新（供统一用户平台调用）
  broadcastTokenUpdate(token, userInfo) {
    try {
      const updateData = {
        token: token,
        userInfo: userInfo,
        timestamp: Date.now(),
        source: 'unified_platform'
      }
      
      localStorage.setItem('shared_token_update', JSON.stringify(updateData))
      
      // 清理更新数据
      setTimeout(() => {
        try {
          localStorage.removeItem('shared_token_update')
        } catch (e) {
          // 忽略清理错误
        }
      }, 2000)
      
      console.log('📡 已广播token更新')
    } catch (error) {
      console.error('❌ 广播token更新失败:', error)
    }
  }

  // 处理来自统一用户平台的postMessage
  handleUnifiedPlatformMessage(event) {
    try {
      // 验证消息来源
      if (event.origin !== 'http://oort.oortcloudsmart.com:21410') {
        return
      }
      
      const data = event.data
      if (!data || typeof data !== 'object') {
        return
      }
      
      console.log('📨 收到统一用户平台消息:', data)
      
      // 处理token响应
      if (data.type === 'TOKEN_RESPONSE' && data.token) {
        console.log('🎉 从统一用户平台获取到token:', data.token.substring(0, 8) + '...')
        
        // 检查是否为新token
        const currentToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
        if (data.token !== currentToken) {
          console.log('🔄 检测到新用户token，立即更新并暂停验证')
          
          // 立即暂停token验证循环，避免新token被清理
          this.pauseTokenValidation = true
          
          // 清除旧的用户信息
          sessionStorage.removeItem('userInfo')
          localStorage.removeItem('userInfo')
          
          // 保存新token
          this.saveToken(data.token, data.userInfo)
          
          // 立即验证新token并更新用户信息
          this.handleNewTokenReceived(data.token, data.userInfo)
          
          // 延迟恢复token验证，给新token充分时间生效
          setTimeout(() => {
            this.pauseTokenValidation = false
            console.log('🔄 恢复token验证循环')
          }, 8000) // 延长到8秒
        }
      }
      
      // 处理错误响应
      if (data.type === 'TOKEN_ERROR') {
        console.log('❌ 统一用户平台token获取失败:', data.error)
        this.handleTokenError(data.error)
      }
      
    } catch (error) {
      console.error('❌ 处理统一用户平台消息失败:', error)
    }
  }

  // 处理新token接收
  async handleNewTokenReceived(token, userInfo) {
    try {
      console.log('🚀 开始处理新token并更新页面信息')
      
      // 1. 清除旧的用户信息
      sessionStorage.removeItem('userInfo')
      localStorage.removeItem('userInfo')
      
      // 2. 验证新token
      const isValid = await this.validateNewToken(token)
      if (!isValid) {
        console.error('❌ 新token验证失败')
        return
      }
      
      // 3. 触发页面刷新事件
      this.triggerPageRefresh()
      
    } catch (error) {
      console.error('❌ 处理新token失败:', error)
    }
  }

  // 验证新token
  async validateNewToken(token) {
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
        const isValid = result.code === 200
        
        if (isValid) {
          console.log('✅ 新token验证成功，用户信息:', result.data)
          // 保存验证后的用户信息
          if (result.data) {
            const userInfo = {
              ...result.data,
              accessToken: token
            }
            sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
            localStorage.setItem('userInfo', JSON.stringify(userInfo))
          }
          return true
        } else {
          console.error('❌ 新token验证失败:', result)
          return false
        }
      } else {
        console.error('❌ 新token验证请求失败:', response.status)
        return false
      }
    } catch (error) {
      console.error('❌ 新token验证异常:', error)
      return false
    }
  }

  // 触发页面刷新
  triggerPageRefresh() {
    try {
      console.log('🔄 触发页面信息刷新')
      
      // 发送自定义事件通知页面更新
      const event = new CustomEvent('userTokenUpdated', {
        detail: {
          timestamp: Date.now(),
          source: 'unified_platform'
        }
      })
      window.dispatchEvent(event)
      
      // 如果页面有全局的authManager，触发重新加载
      if (window.authManager && typeof window.authManager.checkExternalPlatformLogin === 'function') {
        setTimeout(() => {
          window.authManager.checkExternalPlatformLogin()
        }, 500)
      }
      
      // 刷新页面（作为备选方案）
      setTimeout(() => {
        if (confirm('检测到用户信息已更新，是否刷新页面以显示最新信息？')) {
          window.location.reload()
        }
      }, 1000)
      
    } catch (error) {
      console.error('❌ 触发页面刷新失败:', error)
    }
  }

  // 处理token错误
  handleTokenError(error) {
    console.error('❌ Token获取错误:', error)
    
    // 显示更合适的错误提示
    const message = '获取用户信息失败，请检查网络连接或联系管理员'
    
    if (window.ElMessage) {
      window.ElMessage.error(message)
    } else {
      alert(message)
    }
  }

  // 重定向到登录页面
  redirectToLogin() {
    try {
      console.log('🔄 重定向到登录页面...')
      const loginUrl = 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso/sso/v1/login?redirect=' + encodeURIComponent(window.location.href)
      window.location.href = loginUrl
    } catch (error) {
      console.error('❌ 重定向失败:', error)
    }
  }

  // 销毁
  destroy() {
    if (this.requestInterval) {
      clearInterval(this.requestInterval)
      this.requestInterval = null
    }
    
    if (this.platformCheckInterval) {
      clearInterval(this.platformCheckInterval)
      this.platformCheckInterval = null
    }
    
    window.removeEventListener('storage', this.handleStorageChange.bind(this))
    window.removeEventListener('storage', this.handleTokenRequest.bind(this))
    
    this.isInitialized = false
    console.log('📡 Storage事件token同步已销毁')
  }
} // 添加这个右花括号来结束类定义

export const storageEventSync = new StorageEventSync()

