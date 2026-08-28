/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { verifyToken } from '@/api/auth'
import { exchangePlatformToken, getTenantMode } from '@/api/system/localAuth'

// axios and already SpringBlade .
const normalizeApiResponse = (response) => {
  if (response?.code !== undefined || response?.success !== undefined) return response
  return response?.data || response
}

export class AuthManager {
  /* * after Process token Validate . */
  async checkExternalPlatformLogin() {
    const mode = await this.getTenantMode()
    const url = new URL(window.location.href)
    const urlToken = url.searchParams.get('accessToken') || url.searchParams.get('access_token') || url.searchParams.get('token')
    if (mode === 'multi' && urlToken) {
      return this.exchangePlatformToken(urlToken, this.getUrlTenantId(url))
    }
    return this.checkLocalToken()
  }

  /* * old , only current token. */
  async getTokenFromExternalPlatform() {
    return this.getCurrentToken()
  }

  /* * Validate URL in token, successfully after Query parameter. */
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

  /* * item after userinterface token userfield. */
  async verifyToken(token) {
    if (!token) return null
    try {
      const apiResponse = normalizeApiResponse(await verifyToken(token))
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

  /* * already item after user and token. */
  async saveUserToLocal(userInfo) {
    if (!userInfo?.accessToken) return
    const serialized = JSON.stringify(userInfo)
    sessionStorage.setItem('userInfo', serialized)
    sessionStorage.setItem('accessToken', userInfo.accessToken)
    localStorage.setItem('userInfo', serialized)
    localStorage.setItem('accessToken', userInfo.accessToken)
    if (userInfo.platformAccessToken) {
      sessionStorage.setItem('platformAccessToken', userInfo.platformAccessToken)
      localStorage.setItem('platformAccessToken', userInfo.platformAccessToken)
    }
    if (userInfo.tenantId) {
      sessionStorage.setItem('tenantId', userInfo.tenantId)
      localStorage.setItem('tenantId', userInfo.tenantId)
    }
  }

  /* * URL in token parameter. */
  cleanUrlToken() {
    const url = new URL(window.location.href)
    url.searchParams.delete('accessToken')
    url.searchParams.delete('access_token')
    url.searchParams.delete('token')
    url.searchParams.delete('tenantId')
    url.searchParams.delete('tenant_id')
    window.history.replaceState({}, '', url.toString())
  }

  /* * will info. */
  clearSessionTokens() {
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'tenantId', 'tenant_id']
      .forEach((key) => sessionStorage.removeItem(key))
  }

  /* * info. */
  clearLocalTokens() {
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'tenantId', 'tenant_id']
      .forEach((key) => localStorage.removeItem(key))
  }

  /* * full info. */
  clearAllTokens() {
    this.clearSessionTokens()
    this.clearLocalTokens()
  }

  /* * Validate will in token. */
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

  /* * item . */
  logout() {
    this.clearAllTokens()
    window.location.href = '/bus/vls-ui/login'
  }

  /* * new token. */
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
        platformAccessToken: platformToken,
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

  /* * URL、 will 、 Get current token. */
  getCurrentToken() {
    const url = new URL(window.location.href)
    return url.searchParams.get('accessToken')
      || url.searchParams.get('access_token')
      || url.searchParams.get('token')
      || sessionStorage.getItem('platformAccessToken')
      || localStorage.getItem('platformAccessToken')
      || sessionStorage.getItem('accessToken')
      || sessionStorage.getItem('token')
      || localStorage.getItem('accessToken')
      || localStorage.getItem('token')
  }

  getUrlTenantId(url = new URL(window.location.href)) {
    return url.searchParams.get('tenantId') || url.searchParams.get('tenant_id') || undefined
  }

  /* * Get userinfo. */
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
