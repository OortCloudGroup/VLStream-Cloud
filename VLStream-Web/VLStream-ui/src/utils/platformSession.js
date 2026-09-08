/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

const PLATFORM_LOGIN_URL = import.meta.env.VITE_PLATFORM_LOGIN_URL || '/bus/apaas-web/loginPage/index.html'
const PLATFORM_APP_NAME = import.meta.env.VITE_PLATFORM_APP_NAME || 'VLStream'
const TENANT_MODE_KEY = 'vlsTenantMode'
const PLATFORM_LOGIN_PENDING_KEY = 'vlsPlatformLoginPendingAt'
const LOGIN_PENDING_MAX_AGE = 30 * 1000

export const saveTenantMode = mode => {
  const normalizedMode = mode === 'multi' ? 'multi' : 'single'
  sessionStorage.setItem(TENANT_MODE_KEY, normalizedMode)
  return normalizedMode
}

export const isMultiTenantMode = () => sessionStorage.getItem(TENANT_MODE_KEY) === 'multi'

export const clearPlatformAccessToken = () => {
  sessionStorage.removeItem('platformAccessToken')
  localStorage.removeItem('platformAccessToken')
}

export const isPlatformTokenFailure = data => {
  const code = Number(data?.code)
  const message = String(data?.msg || data?.message || '').toLowerCase()
  return code === 4004
    || message.includes('无效的accesstoken')
    || message.includes('accesstoken无效')
    || message.includes('token失效')
}

export const buildPlatformLoginUrl = (returnPath = window.location.pathname + window.location.search + window.location.hash) => {
  const loginUrl = new URL(PLATFORM_LOGIN_URL, window.location.origin)
  const appBaseUrl = new URL(import.meta.env.BASE_URL, window.location.origin)
  const rawReturnPath = String(returnPath || '/')
  const redirectUrl = rawReturnPath.startsWith(appBaseUrl.pathname)
    ? new URL(rawReturnPath, window.location.origin)
    : new URL(rawReturnPath.replace(/^\/+/, ''), appBaseUrl)
  redirectUrl.searchParams.delete('accessToken')
  redirectUrl.searchParams.delete('access_token')
  redirectUrl.searchParams.delete('token')
  loginUrl.searchParams.set('appname', PLATFORM_APP_NAME)
  loginUrl.searchParams.set('redirect_uri', redirectUrl.toString())
  if (!loginUrl.hash) loginUrl.hash = '/'
  return loginUrl.toString()
}

export const redirectToPlatformLogin = returnPath => {
  const now = Date.now()
  const pendingAt = Number(sessionStorage.getItem(PLATFORM_LOGIN_PENDING_KEY) || 0)
  if (pendingAt > 0 && now - pendingAt < LOGIN_PENDING_MAX_AGE) return false
  sessionStorage.setItem(PLATFORM_LOGIN_PENDING_KEY, String(now))
  clearPlatformAccessToken()
  window.location.replace(buildPlatformLoginUrl(returnPath))
  return true
}

export const clearPlatformLoginPending = () => sessionStorage.removeItem(PLATFORM_LOGIN_PENDING_KEY)
