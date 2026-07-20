import { verifyToken } from '@/api/auth'

// 兼容 axios 原始响应与请求拦截器已经解包的 SpringBlade 响应。
const normalizeApiResponse = (response) => {
  if (response?.code !== undefined || response?.success !== undefined) return response
  return response?.data || response
}

export class AuthManager {
  /** 只检查本项目保存的 token，不再探测或跳转任何外部 SSO。 */
  async checkExternalPlatformLogin() {
    return this.checkLocalToken()
  }

  /** 兼容旧调用名，但只返回当前本地 token。 */
  async getTokenFromExternalPlatform() {
    return this.getCurrentToken()
  }

  /** 校验 URL 中的本地 token，成功后保存并移除查询参数。 */
  async checkUrlToken() {
    const url = new URL(window.location.href)
    const token = url.searchParams.get('accessToken') || url.searchParams.get('token')
    if (!token) return null

    const userInfo = await this.verifyToken(token)
    if (!userInfo) return null
    await this.saveUserToLocal(userInfo)
    this.cleanUrlToken()
    return userInfo
  }

  /** 使用本项目后端用户接口验证 token 并归一化用户字段。 */
  async verifyToken(token) {
    if (!token) return null
    try {
      const apiResponse = normalizeApiResponse(await verifyToken())
      if (!apiResponse || (apiResponse.code !== 200 && apiResponse.success !== true)) return null
      const data = apiResponse.data || {}
      const user = data.user || data
      return {
        ...user,
        ...data,
        accessToken: token,
        userName: user.realName || user.nickName || user.name || user.account || user.userName,
        loginId: user.account || user.loginId || user.userName,
        tenantId: user.tenantId || data.tenantId || '000000'
      }
    } catch (error) {
      console.warn('本地 token 验证失败:', error)
      return null
    }
  }

  /** 同步保存已由本项目后端验证的用户和 token。 */
  async saveUserToLocal(userInfo) {
    if (!userInfo?.accessToken) return
    const serialized = JSON.stringify(userInfo)
    sessionStorage.setItem('userInfo', serialized)
    sessionStorage.setItem('accessToken', userInfo.accessToken)
    localStorage.setItem('userInfo', serialized)
    localStorage.setItem('accessToken', userInfo.accessToken)
  }

  /** 移除 URL 中用于本地自动登录的 token 参数。 */
  cleanUrlToken() {
    const url = new URL(window.location.href)
    url.searchParams.delete('accessToken')
    url.searchParams.delete('token')
    window.history.replaceState({}, '', url.toString())
  }

  /** 清除会话级认证信息。 */
  clearSessionTokens() {
    ;['accessToken', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken']
      .forEach((key) => sessionStorage.removeItem(key))
  }

  /** 清除持久化认证信息。 */
  clearLocalTokens() {
    ;['accessToken', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken']
      .forEach((key) => localStorage.removeItem(key))
  }

  /** 清除全部本地认证信息。 */
  clearAllTokens() {
    this.clearSessionTokens()
    this.clearLocalTokens()
  }

  /** 校验会话或持久化存储中的本地 token。 */
  async checkLocalToken() {
    const sessionToken = sessionStorage.getItem('accessToken') || sessionStorage.getItem('token')
    if (sessionToken) {
      const userInfo = await this.verifyToken(sessionToken)
      if (userInfo) return userInfo
      this.clearSessionTokens()
    }

    const localToken = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (localToken) {
      const userInfo = await this.verifyToken(localToken)
      if (userInfo) return userInfo
      this.clearLocalTokens()
    }
    return null
  }

  /** 清理本地状态并回到本项目登录页。 */
  logout() {
    this.clearAllTokens()
    window.location.href = '/bus/vls-ui/login'
  }

  /** 验证并保存一个新的本地 token。 */
  async setNewToken(token) {
    const userInfo = await this.verifyToken(token)
    if (!userInfo) return false
    this.clearAllTokens()
    await this.saveUserToLocal(userInfo)
    return true
  }

  /** 按 URL、会话、持久化存储顺序获取当前本地 token。 */
  getCurrentToken() {
    const url = new URL(window.location.href)
    return url.searchParams.get('accessToken')
      || url.searchParams.get('token')
      || sessionStorage.getItem('accessToken')
      || sessionStorage.getItem('token')
      || localStorage.getItem('accessToken')
      || localStorage.getItem('token')
  }

  /** 获取缓存的本地用户信息。 */
  getCachedUserInfo() {
    try {
      const value = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo')
      return value ? JSON.parse(value) : null
    } catch (error) {
      console.warn('解析本地用户信息失败:', error)
      return null
    }
  }
}

export const authManager = new AuthManager()
