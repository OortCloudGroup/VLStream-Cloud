/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { verifyToken } from '@/api/auth'
import { exchangePlatformToken, getTenantMode, validatePlatformSession } from '@/api/system/localAuth'
import { extractAuthCallbackParams, stripAuthCallbackParams } from '@/utils/authCallbackParams.mjs'
import { logSafeRequestError } from '@/utils/safeRequestError.mjs'
import {
  clearPlatformAccessToken,
  clearPlatformLoginPending,
  redirectToPlatformLogin,
  saveTenantMode
} from '@/utils/platformSession'

// axios and already SpringBlade .
const normalizeApiResponse = (response) => {
  if (response?.code !== undefined || response?.success !== undefined) return response
  return response?.data || response
}

export class AuthManager {
  constructor() {
    this.validatedPlatformToken = ''
  }

  /* * after Process token Validate . */
  async checkExternalPlatformLogin() {
    const { accessToken: urlToken } = extractAuthCallbackParams(window.location.href)
    if (urlToken) {
      return this.checkUrlToken()
    }
    const userInfo = await this.checkLocalToken()
    if (!userInfo) return null

    const tenantMode = await this.getTenantMode()
    if (tenantMode !== 'multi') return userInfo

    const platformToken = this.getPlatformToken()
    if (!platformToken) return null
    if (this.validatedPlatformToken === platformToken) return userInfo

    try {
      const response = normalizeApiResponse(await validatePlatformSession())
      if (response?.code !== 200 && response?.success !== true) return null
      this.validatedPlatformToken = platformToken
      clearPlatformLoginPending()
      return userInfo
    } catch (error) {
      logSafeRequestError('多租户平台会话校验失败', error)
      return null
    }
  }

  /* * old , only current token. */
  async getTokenFromExternalPlatform() {
    return this.getPlatformToken()
  }

  /* * Validate URL in token, successfully after Query parameter. */
  async checkUrlToken(href = window.location.href) {
    const url = new URL(href)
    const { accessToken: token, tenantId } = extractAuthCallbackParams(url.toString())
    this.cleanUrlToken() // Strip before any request; failure/cancellation must not leave URL/history credentials.
    if (!token) return null

    // URL 传入的是统一平台令牌；无论单租户还是多租户，都必须先换取 VLS 本地会话。
    // single 仅禁止租户切换，不应禁止平台账号授权登录。
    const userInfo = await this.exchangePlatformToken(token, tenantId || undefined)
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
      logSafeRequestError('本地会话验证失败', error)
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
    const current = window.location.href, clean = stripAuthCallbackParams(current)
    if (clean !== current) window.history.replaceState({}, '', clean)
  }

  /* * will info. */
  clearSessionTokens() {
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'tenantId', 'tenant_id']
      .forEach((key) => sessionStorage.removeItem(key))
  }

  /* * info. */
  clearLocalTokens() {
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'apaas_token', 'tenantId', 'tenant_id']
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
      return saveTenantMode(response?.data?.tenantType === 'multi' ? 'multi' : 'single')
    } catch (error) {
      logSafeRequestError('获取租户模式失败，按既有单租户处理', error)
      return saveTenantMode('single')
    }
  }

  async exchangePlatformToken(platformToken, tenantId) {
    this.cleanUrlToken()
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
      this.validatedPlatformToken = platformToken
      clearPlatformLoginPending()
      this.cleanUrlToken()
      return userInfo
    } catch (error) {
      logSafeRequestError('平台换票失败', error)
      this.validatedPlatformToken = ''
      clearPlatformAccessToken()
      clearPlatformLoginPending()
      return null
    }
  }

  /* * Get current VLS local session token. Platform tokens must never authenticate VLS APIs. */
  getCurrentToken() {
    return sessionStorage.getItem('accessToken')
      || sessionStorage.getItem('token')
      || localStorage.getItem('accessToken')
      || localStorage.getItem('token')
      || localStorage.getItem('apaas_token')
  }

  /* * Get the OortCloud platform token used only for platform APIs and token exchange. */
  getPlatformToken() {
    const { accessToken } = extractAuthCallbackParams(window.location.href)
    return accessToken
      || sessionStorage.getItem('platformAccessToken')
      || localStorage.getItem('platformAccessToken')
  }

  getUrlTenantId(url = new URL(window.location.href)) {
    return extractAuthCallbackParams(url.toString()).tenantId || undefined
  }

  hasPlatformCallbackToken(url = new URL(window.location.href)) {
    return Boolean(extractAuthCallbackParams(url.toString()).accessToken)
  }

  redirectToPlatformLogin(returnPath) {
    return redirectToPlatformLogin(returnPath)
  }

  /* * Get userinfo. */
  getCachedUserInfo() {
    try {
      const value = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo')
      return value ? JSON.parse(value) : null
    } catch (error) {
      logSafeRequestError('解析本地用户信息失败', error)
      return null
    }
  }
}

export const authManager = new AuthManager()
