/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

/**
 * apaas interface , Vite /oort
 */
import config from '@/config'
import { getToken } from '@/utils/cache/cookies'

function getLocalBackendPrefix(): string {
  const configuredBase = import.meta.env.VITE_API_BASE_URL
  if (configuredBase !== undefined && configuredBase !== '') {
    return String(configuredBase).replace(/\/$/, '')
  }
  return import.meta.env.DEV ? '' : '/bus/apaas-vls-server'
}

export function getApaasGatewayPrefix(): string {
  const envPrefix = import.meta.env.VITE_APAAS_GATEWAY_PREFIX
  if (envPrefix !== undefined && envPrefix !== '') {
    return String(envPrefix).replace(/\/$/, '')
  }
  if (import.meta.env.DEV) {
    return '/oort'
  }
  const url = String(config.URL || '').replace(/\/$/, '')
  const gateWay = String(config.gateWay || '').replace(/^\/+|\/+$/g, '')
  const prodBase = import.meta.env.VITE_APAAS_API_BASE || `${url}/${gateWay}`
  return String(prodBase).replace(/\/$/, '')
}

export function apaasServiceUrl(service: string, path = ''): string {
  const svc = service.replace(/^\/+|\/+$/g, '')
  const normalizedPath = String(path || '').replace(/^\//, '')
  const serviceKey = svc.toLowerCase()

  // Workflow and work-order APIs are hosted by the current Java backend.
  if (serviceKey === 'apaas-workflowforms') {
    const localBackendPrefix = getLocalBackendPrefix()
    return normalizedPath ? `${localBackendPrefix}/${normalizedPath}` : `${localBackendPrefix}/`
  }

  if (import.meta.env.DEV
    && serviceKey === 'apaas-location-service'
    && import.meta.env.VITE_APAAS_WORKFLOWFORMS_DIRECT !== 'false') {
    return normalizedPath ? `/${normalizedPath}` : '/'
  }

  const prefix = getApaasGatewayPrefix()
  if (!normalizedPath) {
    return `${prefix}/${svc}`
  }
  return `${prefix}/${svc}/${normalizedPath}`
}

function resolveAuthToken(): string {
  const urlParams = new URLSearchParams(window.location.search)
  return (
    urlParams.get('accessToken')
    || urlParams.get('accesstoken')
    || sessionStorage.getItem('platformAccessToken')
    || localStorage.getItem('platformAccessToken')
    || getToken()
    || localStorage.getItem('accessToken')
    || localStorage.getItem('apaas_token')
    || sessionStorage.getItem('token')
    || sessionStorage.getItem('accessToken')
    || ''
  )
}

export const apaasRequestHeaders: Record<string, any> = {
  get authorization() {
    return resolveAuthToken()
  },
  get accesstoken() {
    return resolveAuthToken()
  },
  get tenantid() {
    return window.sessionStorage.getItem('tenantId') || ''
  },
  set tenantid(value: string) {
    window.sessionStorage.setItem('tenantId', value)
  }
}

function getPlatformWebBase() {
  const configuredBase = import.meta.env.VITE_PLATFORM_WEB_BASE
  if (configuredBase) {
    return String(configuredBase).replace(/\/$/, '')
  }

  const loginUrl = import.meta.env.VITE_PLATFORM_LOGIN_URL
  if (loginUrl) {
    try {
      const parsed = new URL(loginUrl)
      const appRoot = parsed.pathname.split('/loginPage/')[0]
      return `${parsed.origin}${appRoot}`.replace(/\/$/, '')
    } catch (error) {
      console.warn('平台登录地址配置无效，使用默认前端地址:', error)
    }
  }

  return config.URL.replace(/\/$/, '') + (config.frontURLStr || '/bus/apaas-web')
}

export function openApaasWebPage(pagePath: string, query = '', targetWindow = '_blank', windowFeatures = '') {
  const base = getPlatformWebBase()
  const normalizedPath = pagePath.startsWith('/') ? pagePath : `/${pagePath}`
  const qs = query ? (query.startsWith('?') ? query : `?${query}`) : ''
  const target = normalizedPath.startsWith('/bus/')
    ? `${new URL(base).origin}${normalizedPath}`
    : `${base}${normalizedPath}`
  window.open(`${target}${qs}`, targetWindow, windowFeatures)
}
