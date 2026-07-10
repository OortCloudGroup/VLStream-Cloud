/**
 * 统一用户平台Token获取器
 * 直接从统一用户平台获取最新的有效token
 */

class UnifiedPlatformTokenFetcher {
  constructor() {
    this.unifiedPlatformUrl = 'http://oort.oortcloudsmart.com:21410/bus/apaas-web'
    this.apiBaseUrl = 'http://oort.oortcloudsmart.com:21410/bus/apaas-sso'
    this.appId = '6551b0147c4649a894e86bf8de248da4'
    this.secretKey = '58f9eeefc65f4b318204ba21f39a8861'
  }

  /**
   * 获取统一用户平台的最新token
   */
  async getLatestToken() {
    try {
      console.log('🔄 开始获取统一用户平台最新token')
      
      // 方法1：通过iframe获取
      const iframeToken = await this.getTokenFromIframe()
      if (iframeToken) {
        console.log('✅ 通过iframe获取到token')
        return iframeToken
      }
      
      // 方法2：通过API获取当前用户信息
      const apiToken = await this.getTokenFromAPI()
      if (apiToken) {
        console.log('✅ 通过API获取到token')
        return apiToken
      }
      
      // 方法3：通过localStorage获取
      const localStorageToken = await this.getTokenFromLocalStorage()
      if (localStorageToken) {
        console.log('✅ 通过localStorage获取到token')
        return localStorageToken
      }
      
      console.log('❌ 未能获取到统一用户平台token')
      return null
      
    } catch (error) {
      console.error('获取统一用户平台token失败:', error)
      return null
    }
  }

  /**
   * 通过iframe获取统一用户平台token
   */
  async getTokenFromIframe() {
    try {
      // 创建一个隐藏的iframe来访问统一用户平台
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = `${this.unifiedPlatformUrl}/desktopHome/index.html#/home`
      
      return new Promise((resolve) => {
        const timeout = setTimeout(() => {
          try {
            document.body.removeChild(iframe)
          } catch (e) {
            // iframe可能已经被移除
          }
          resolve(null)
        }, 10000) // 10秒超时
        
        iframe.onload = () => {
          try {
            // 尝试从iframe中获取token
            const iframeWindow = iframe.contentWindow
            // 优先从sessionStorage获取token（根据用户发现，统一用户平台使用sessionStorage）
            const iframeToken = iframeWindow.sessionStorage.getItem('token') ||
                               iframeWindow.sessionStorage.getItem('accessToken') ||
                               iframeWindow.localStorage.getItem('token') ||
                               iframeWindow.localStorage.getItem('accessToken')
            
            clearTimeout(timeout)
            try {
              document.body.removeChild(iframe)
            } catch (e) {
              // iframe可能已经被移除
            }
            resolve(iframeToken)
          } catch (error) {
            // 跨域限制，无法直接访问iframe内容
            console.log('⚠️ 跨域限制，无法从iframe获取token')
            clearTimeout(timeout)
            try {
              document.body.removeChild(iframe)
            } catch (e) {
              // iframe可能已经被移除
            }
            resolve(null)
          }
        }
        
        iframe.onerror = () => {
          console.log('⚠️ iframe加载失败')
          clearTimeout(timeout)
          try {
            document.body.removeChild(iframe)
          } catch (e) {
            // iframe可能已经被移除
          }
          resolve(null)
        }
        
        document.body.appendChild(iframe)
      })
    } catch (error) {
      console.error('通过iframe获取token失败:', error)
      return null
    }
  }

  /**
   * 通过API获取统一用户平台token
   */
  async getTokenFromAPI() {
    try {
      // 尝试使用当前token获取用户信息
      const currentToken = this.getCurrentToken()
      if (currentToken) {
        const response = await fetch(`${this.apiBaseUrl}/sso/v1/getUserInfo`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'requesttype': 'app',
            'appid': this.appId,
            'secretkey': this.secretKey,
            'accesstoken': currentToken
          },
          body: JSON.stringify({
            accessToken: currentToken
          })
        })
        
        if (response.ok) {
          const result = await response.json()
          if (result.data && result.data.accessToken) {
            return result.data.accessToken
          }
        }
      }
      
      return null
    } catch (error) {
      console.error('通过API获取token失败:', error)
      return null
    }
  }

  /**
   * 通过localStorage获取统一用户平台token
   */
  async getTokenFromLocalStorage() {
    try {
      // 尝试从可能的统一用户平台token存储位置获取
      // 根据用户发现，统一用户平台使用sessionStorage存储token
      const possibleKeys = [
        'token', // 优先检查sessionStorage.token
        'accessToken',
        'unified_token',
        'unified_accessToken',
        'apaas_token',
        'apaas_accessToken',
        'sso_token',
        'sso_accessToken'
      ]
      
      // 优先从sessionStorage获取（统一用户平台使用sessionStorage）
      for (const key of possibleKeys) {
        const token = sessionStorage.getItem(key) || localStorage.getItem(key)
        if (token) {
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
      console.error('通过localStorage获取token失败:', error)
      return null
    }
  }

  /**
   * 获取当前token
   */
  getCurrentToken() {
    // 优先级：URL > sessionStorage > localStorage
    // 1. 优先从URL参数获取
    const urlParams = new URLSearchParams(window.location.search)
    const urlToken = urlParams.get('accessToken') || urlParams.get('token')
    if (urlToken) {
      console.log('✅ unifiedPlatformTokenFetcher使用URL token:', urlToken.substring(0, 8) + '...')
      return urlToken
    }
    
    // 2. 从sessionStorage获取
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      console.log('✅ unifiedPlatformTokenFetcher使用sessionStorage token:', sessionToken.substring(0, 8) + '...')
      return sessionToken
    }
    
    // 3. 从localStorage获取
    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      console.log('✅ unifiedPlatformTokenFetcher使用localStorage token:', localToken.substring(0, 8) + '...')
      return localToken
    }
    
    console.log('❌ unifiedPlatformTokenFetcher没有找到任何token')
    return null
  }

  /**
   * 验证token有效性
   */
  async validateToken(token) {
    try {
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
   * 打开统一用户平台获取新token
   */
  openUnifiedPlatform() {
    console.log('🔗 打开统一用户平台获取新token')
    console.log('请在新窗口中登录统一用户平台，然后复制新token')
    
    // 打开统一用户平台
    const platformUrl = `${this.unifiedPlatformUrl}/desktopHome/index.html#/home`
    window.open(platformUrl, '_blank', 'width=1200,height=800')
    
    // 显示获取token的说明
    this.showTokenInstructions()
  }

  /**
   * 显示获取token的说明
   */
  showTokenInstructions() {
    const instructions = `
🎯 获取统一用户平台新token的步骤：

1. 在新打开的窗口中登录统一用户平台
2. 登录成功后，按F12打开开发者工具
3. 在Console中运行以下命令获取token：
   console.log('token:', sessionStorage.getItem('token'))
4. 复制输出的token值
5. 将token添加到VLStream-ui的URL参数中：
   http://oort.oortcloudsmart.com:21410/bus/vls-ui?accessToken=你的token值
6. 刷新VLStream-ui页面，观察是否自动同步

💡 提示：统一用户平台使用sessionStorage存储token，所以优先检查：
   console.log('token:', sessionStorage.getItem('token'))
   如果上面没有，再尝试：
   console.log('accessToken:', sessionStorage.getItem('accessToken'))
   或者
   console.log('token:', localStorage.getItem('token'))
    `
    
    console.log(instructions)
    
    // 也可以显示一个提示框
    if (confirm('是否显示获取token的详细说明？')) {
      alert(instructions)
    }
  }

  /**
   * 手动输入token
   */
  async manualInputToken() {
    const token = prompt('请输入从统一用户平台获取的token:')
    if (token && token.trim()) {
      console.log('🔗 手动输入token:', token.substring(0, 8) + '...')
      
      // 验证token有效性
      const isValid = await this.validateToken(token.trim())
      if (isValid) {
        console.log('✅ 手动输入的token有效')
        return token.trim()
      } else {
        console.log('❌ 手动输入的token无效')
        alert('输入的token无效，请检查后重试')
        return null
      }
    }
    return null
  }
}

// 创建全局实例
export const unifiedPlatformTokenFetcher = new UnifiedPlatformTokenFetcher()

// 将实例暴露到全局window对象，方便调试
if (typeof window !== 'undefined') {
  window.unifiedPlatformTokenFetcher = unifiedPlatformTokenFetcher
} 