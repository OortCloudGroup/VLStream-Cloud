import { verifyToken } from '@/api/auth'
import { exchangePlatformToken, getTenantMode } from '@/api/system/localAuth'

// 兼容 axios 原始响应与请求拦截器已经解包的 SpringBlade 响应。
const normalizeApiResponse = (response) => {
  if (response?.code !== undefined || response?.success !== undefined) return response
  return response?.data || response
}

export class AuthManager {
  /** 根据后端模式处理平台换票或本地 token 校验。 */
  async checkExternalPlatformLogin() {
    const mode = await this.getTenantMode()
    const url = new URL(window.location.href)
    const urlToken = url.searchParams.get('accessToken') || url.searchParams.get('access_token') || url.searchParams.get('token')
    if (mode === 'multi' && urlToken) {
      return this.exchangePlatformToken(urlToken, this.getUrlTenantId(url))
    }
    return this.checkLocalToken()
  }

  /** 兼容旧调用名，但只返回当前本地 token。 */
  async getTokenFromExternalPlatform() {
    return this.getCurrentToken()
  }

  /** 校验 URL 中的本地 token，成功后保存并移除查询参数。 */
  async checkUrlToken() {
    const url = new URL(window.location.href)
    const token = url.searchParams.get('accessToken') || url.searchParams.get('access_token') || url.searchParams.get('token')
    if (!token) return null

    const mode = await this.getTenantMode()
    const userInfo = mode === 'multi'
      ? await this.exchangePlatformToken(token, this.getUrlTenantId(url))
      : await this.verifyToken(token)
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
    if (userInfo.tenantId) {
      sessionStorage.setItem('tenantId', userInfo.tenantId)
      localStorage.setItem('tenantId', userInfo.tenantId)
    }
  }

  /** 移除 URL 中用于本地自动登录的 token 参数。 */
  cleanUrlToken() {
    const url = new URL(window.location.href)
    url.searchParams.delete('accessToken')
    url.searchParams.delete('access_token')
    url.searchParams.delete('token')
    url.searchParams.delete('tenantId')
    url.searchParams.delete('tenant_id')
    window.history.replaceState({}, '', url.toString())
  }

  /** 清除会话级认证信息。 */
  clearSessionTokens() {
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'tenantId', 'tenant_id']
      .forEach((key) => sessionStorage.removeItem(key))
  }

  /** 清除持久化认证信息。 */
  clearLocalTokens() {
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'tenantId', 'tenant_id']
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
    this.clearAllTokens()
    sessionStorage.setItem('accessToken', token)
    localStorage.setItem('accessToken', token)
    const userInfo = await this.verifyToken(token)
    if (!userInfo) {
      this.clearAllTokens()
      return false
    }
    await this.saveUserToLocal(userInfo)
    return true
  }

  async getTenantMode() {
    try {
      const response = normalizeApiResponse(await getTenantMode())
      return response?.data?.tenantType === 'multi' ? 'multi' : 'single'
    } catch (error) {
      console.warn('获取租户模式失败，按单租户处理:', error?.message)
      return 'single'
    }
  }

  async exchangePlatformToken(platformToken, tenantId) {
    try {
      const response = normalizeApiResponse(await exchangePlatformToken(platformToken, tenantId))
      if (response?.code !== 200 || !response?.data?.accessToken) return null
      const data = response.data
      const user = data.user || {}
      const userInfo = {
        ...user,
        ...data,
        accessToken: data.accessToken,
        userName: data.userName || user.nickName || user.userName,
        loginId: data.account || user.loginId || user.userName,
        tenantId: data.tenantId || user.tenantId
      }
      this.clearAllTokens()
      await this.saveUserToLocal(userInfo)
      this.cleanUrlToken()
      return userInfo
    } catch (error) {
      console.warn('平台 token 换票失败:', error?.response?.data?.msg || error?.message)
      return null
    }
  }

  /** 按 URL、会话、持久化存储顺序获取当前本地 token。 */
  getCurrentToken() {
    const url = new URL(window.location.href)
    return url.searchParams.get('accessToken')
      || url.searchParams.get('access_token')
      || url.searchParams.get('token')
      || sessionStorage.getItem('accessToken')
      || sessionStorage.getItem('token')
      || localStorage.getItem('accessToken')
      || localStorage.getItem('token')
  }

  getUrlTenantId(url = new URL(window.location.href)) {
    return url.searchParams.get('tenantId') || url.searchParams.get('tenant_id') || undefined
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
