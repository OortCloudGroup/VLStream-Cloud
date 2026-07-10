// 使用Cookie进行token同步
class CookieTokenSync {
  constructor() {
    this.cookieName = 'oort_access_token'
    this.userInfoCookieName = 'oort_user_info'
    this.checkInterval = null
    this.lastKnownToken = null
  }

  // 初始化
  init() {
    // 立即检查一次
    this.checkCookieToken()
    
    // 每5秒检查一次cookie变化
    this.checkInterval = setInterval(() => {
      this.checkCookieToken()
    }, 5000)
    
    console.log('🍪 Cookie token同步已初始化')
  }

  // 检查cookie中的token
  checkCookieToken() {
    const cookieToken = this.getCookie(this.cookieName)
    const cookieUserInfo = this.getCookie(this.userInfoCookieName)
    
    if (cookieToken && cookieToken !== this.lastKnownToken) {
      console.log('🔄 检测到cookie token变化:', cookieToken.substring(0, 8) + '...')
      
      this.lastKnownToken = cookieToken
      
      // 保存到本地存储
      sessionStorage.setItem('accessToken', cookieToken)
      sessionStorage.setItem('token', cookieToken)
      
      if (cookieUserInfo) {
        try {
          const userInfo = JSON.parse(decodeURIComponent(cookieUserInfo))
          sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
          localStorage.setItem('userInfo', JSON.stringify(userInfo))
          
          // 触发更新事件
          this.triggerUpdateEvent(cookieToken, userInfo)
        } catch (error) {
          console.error('❌ 解析cookie用户信息失败:', error)
        }
      }
    }
  }

  // 获取cookie值
  getCookie(name) {
    const value = `; ${document.cookie}`
    const parts = value.split(`; ${name}=`)
    if (parts.length === 2) {
      return parts.pop().split(';').shift()
    }
    return null
  }

  // 设置cookie
  setCookie(name, value, days = 7) {
    const expires = new Date()
    expires.setTime(expires.getTime() + (days * 24 * 60 * 60 * 1000))
    document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/;domain=.oortcloudsmart.com`
  }

  // 保存token到cookie
  saveTokenToCookie(token, userInfo) {
    this.setCookie(this.cookieName, token)
    if (userInfo) {
      this.setCookie(this.userInfoCookieName, encodeURIComponent(JSON.stringify(userInfo)))
    }
    console.log('💾 Token已保存到cookie')
  }

  // 触发更新事件
  triggerUpdateEvent(token, userInfo) {
    const event = new CustomEvent('cookieTokenUpdated', {
      detail: { token, userInfo }
    })
    window.dispatchEvent(event)
  }

  // 销毁
  destroy() {
    if (this.checkInterval) {
      clearInterval(this.checkInterval)
    }
  }
}

export const cookieTokenSync = new CookieTokenSync()