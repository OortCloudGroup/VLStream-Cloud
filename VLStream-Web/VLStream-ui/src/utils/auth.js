/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { verifyToken } from '@/api/auth'
import { sessionValue } from '@/utils/requestIdentity.mjs'
import { exchangePlatformToken, getTenantMode, switchTenant, validatePlatformSession } from '@/api/system/localAuth'
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

// All AuthManager instances share identity changes (layout, router and login).
let identityRevision = 0

export class AuthManager {
  constructor() {
    this.validatedPlatformToken = ''
  }

  invalidatePendingIdentity() {
    identityRevision++
    this.validatedPlatformToken = ''
  }

  getIdentitySnapshot() {
    return {
      revision: identityRevision,
      token: this.getCurrentToken(),
      platformToken: this.getPlatformToken(),
      tenantId: sessionValue(['tenantId', 'tenant_id'])
    }
  }

  isIdentityCurrent(identity, allowCleared = false) {
    const current = this.getIdentitySnapshot()
    return current.revision === identity.revision && ['token', 'platformToken', 'tenantId']
      .every(key => current[key] === identity[key] || (allowCleared && !current[key]))
  }

  /* * after Process token Validate . */
  async checkExternalPlatformLogin() {
    const { accessToken: urlToken } = extractAuthCallbackParams(window.location.href)
    if (urlToken) {
      return this.checkUrlToken()
    }
    const identity = this.getIdentitySnapshot()
    const userInfo = await this.checkLocalToken()
    if (!userInfo || !this.isIdentityCurrent(identity)) return null

    const tenantMode = await this.getTenantMode()
    if (!this.isIdentityCurrent(identity)) return null
    if (tenantMode !== 'multi') return userInfo

    const platformToken = this.getPlatformToken()
    if (!platformToken) return null
    if (this.validatedPlatformToken === platformToken) return userInfo

    try {
      const response = normalizeApiResponse(await validatePlatformSession())
      if (!this.isIdentityCurrent(identity)) return null
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
    this.clearAllTokens()
    this.validatedPlatformToken = ''

    // URL 传入的是统一平台令牌；无论单租户还是多租户，都必须先换取 VLS 本地会话。
    // single 仅禁止租户切换，不应禁止平台账号授权登录。
    const userInfo = await this.exchangePlatformToken(token, tenantId || undefined)
    if (!userInfo) return null
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
  saveUserToLocal(userInfo) {
    if (!userInfo?.accessToken) return
    const current = this.getIdentitySnapshot()
    if (current.token !== userInfo.accessToken || current.platformToken !== (userInfo.platformAccessToken || '')
      || current.tenantId !== String(userInfo.tenantId || '')) this.invalidatePendingIdentity()
    ;['wvpAccessToken'].forEach(key => { sessionStorage.removeItem(key); localStorage.removeItem(key) })
    const serialized = JSON.stringify(userInfo)
    sessionStorage.setItem('userInfo', serialized)
    sessionStorage.setItem('accessToken', userInfo.accessToken)
    localStorage.setItem('userInfo', serialized)
    localStorage.setItem('accessToken', userInfo.accessToken)
    if (userInfo.platformAccessToken) {
      sessionStorage.setItem('platformAccessToken', userInfo.platformAccessToken)
      localStorage.setItem('platformAccessToken', userInfo.platformAccessToken)
    } else {
      clearPlatformAccessToken()
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
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'wvpAccessToken', 'tenantId', 'tenant_id']
      .forEach((key) => sessionStorage.removeItem(key))
    // Keep this tab explicitly signed out; another tab's persisted login must not become its fallback.
    sessionStorage.setItem('userInfo', 'null')
  }

  /* * info. */
  clearLocalTokens() {
    ;['accessToken', 'access_token', 'token', 'userCenterToken', 'userInfo', 'platformAccessToken', 'wvpAccessToken', 'apaas_token', 'tenantId', 'tenant_id']
      .forEach((key) => localStorage.removeItem(key))
  }

  /* * full info. */
  clearAllTokens() {
    this.invalidatePendingIdentity()
    this.clearSessionTokens()
    this.clearLocalTokens()
  }

  clearIdentityTokens(identity) {
    if (!this.isIdentityCurrent(identity, true)) return
    const matches = storage => [
      [storage.getItem('accessToken') || storage.getItem('token'), identity.token],
      [storage.getItem('platformAccessToken'), identity.platformToken],
      [storage.getItem('tenantId') || storage.getItem('tenant_id'), identity.tenantId]
    ].every(([value, expected]) => !value || value === expected)
    const clearSession = matches(sessionStorage), clearLocal = matches(localStorage)
    this.invalidatePendingIdentity()
    if (clearSession) this.clearSessionTokens()
    if (clearLocal) this.clearLocalTokens()
  }

  /* * Validate will in token. */
  async checkLocalToken() {
    const identity = this.getIdentitySnapshot()
    const token = this.getCurrentToken()
    if (!token) return null
    const userInfo = await this.verifyToken(token)
    if (userInfo) return this.isIdentityCurrent(identity) ? userInfo : null
    this.clearIdentityTokens(identity)
    return null
  }

  /* * item . */
  logout() {
    this.clearAllTokens()
    window.location.href = '/bus/vls-ui/login'
  }

  /* * new token. */
  async setNewToken(token, session = {}, identity = this.getIdentitySnapshot()) {
    if (!this.isIdentityCurrent(identity)) return false
    // Use the identity returned by the trusted switch endpoint, including a rotated platform token.
    const platformToken = session.platformAccessToken || this.getPlatformToken()
    const tenantId = session.tenantId ? String(session.tenantId) : ''
    this.clearAllTokens()
    this.validatedPlatformToken = ''
    if (platformToken) {
      sessionStorage.setItem('platformAccessToken', platformToken)
      localStorage.setItem('platformAccessToken', platformToken)
    }
    sessionStorage.setItem('accessToken', token)
    localStorage.setItem('accessToken', token)
    if (tenantId) {
      sessionStorage.setItem('tenantId', tenantId)
      localStorage.setItem('tenantId', tenantId)
    }
    const pendingIdentity = this.getIdentitySnapshot()
    const userInfo = await this.verifyToken(token)
    if (!userInfo || (tenantId && String(userInfo.tenantId) !== tenantId)) {
      // A current 401 interceptor may already have removed some of this identity.
      this.clearIdentityTokens(pendingIdentity)
      return false
    }
    if (!this.isIdentityCurrent(pendingIdentity)) return false
    this.saveUserToLocal({ ...userInfo, platformAccessToken: platformToken || undefined })
    return true
  }

  async switchTenant(tenantId, identity = this.getIdentitySnapshot()) {
    if (!this.isIdentityCurrent(identity)) return false
    this.invalidatePendingIdentity()
    const pendingIdentity = this.getIdentitySnapshot()
    const response = normalizeApiResponse(await switchTenant(tenantId))
    if (!this.isIdentityCurrent(pendingIdentity)) return false
    if (response?.code !== 200 || !response?.data?.accessToken) {
      throw new Error(response?.msg || '新租户会话校验失败')
    }
    return this.setNewToken(response.data.accessToken, response.data, pendingIdentity)
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
    this.invalidatePendingIdentity()
    const identity = this.getIdentitySnapshot()
    try {
      const response = normalizeApiResponse(await exchangePlatformToken(platformToken, tenantId))
      if (!this.isIdentityCurrent(identity)) return null
      if (response?.code !== 200 || !response?.data?.accessToken) return null
      const data = response.data
      const user = data.user || {}
      const userInfo = {
        ...user,
        ...data,
        accessToken: data.accessToken,
        platformAccessToken: data.platformAccessToken || platformToken,
        userName: data.userName || user.nickName || user.userName,
        loginId: data.account || user.loginId || user.userName,
        tenantId: data.tenantId || user.tenantId
      }
      this.clearAllTokens()
      this.saveUserToLocal(userInfo)
      this.validatedPlatformToken = userInfo.platformAccessToken
      clearPlatformLoginPending()
      this.cleanUrlToken()
      return userInfo
    } catch (error) {
      if (!this.isIdentityCurrent(identity, true)) return null
      logSafeRequestError('平台换票失败', error)
      this.validatedPlatformToken = ''
      this.clearIdentityTokens(identity)
      clearPlatformLoginPending()
      return null
    }
  }

  /* * Get current VLS local session token. Platform tokens must never authenticate VLS APIs. */
  getCurrentToken() {
    return sessionValue(['accessToken', 'token'])
  }

  /* * Get the OortCloud platform token used only for platform APIs and token exchange. */
  getPlatformToken() {
    return sessionValue(['platformAccessToken'])
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
