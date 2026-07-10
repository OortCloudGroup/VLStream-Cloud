/**
 * apaas 接口基础路径，开发环境走 Vite /oort 代理
 */
import config from '@/config'
import { getToken } from '@/utils/cache/cookies'

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
  const prefix = getApaasGatewayPrefix()
  const svc = service.replace(/^\/+|\/+$/g, '')
  const normalizedPath = String(path || '').replace(/^\//, '')
  const serviceKey = svc.toLowerCase()
  if (import.meta.env.DEV
    && (serviceKey === 'apaas-workflowforms' || serviceKey === 'apaas-location-service')
    && import.meta.env.VITE_APAAS_WORKFLOWFORMS_DIRECT !== 'false') {
    return normalizedPath ? `/${normalizedPath}` : '/'
  }
  if (!normalizedPath) {
    return `${prefix}/${svc}`
  }
  return `${prefix}/${svc}/${normalizedPath}`
}

function resolveAuthToken(): string {
  return (
    getToken()
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
  get AccessToken() {
    return resolveAuthToken()
  },
  get tenantid() {
    return window.sessionStorage.getItem('tenantId') || ''
  },
  set tenantid(value: string) {
    window.sessionStorage.setItem('tenantId', value)
  }
}

export function openApaasWebPage(pagePath: string, query = '') {
  const base = config.URL.replace(/\/$/, '') + (config.frontURLStr || '/bus/apaas-web')
  const normalizedPath = pagePath.startsWith('/') ? pagePath : `/${pagePath}`
  const qs = query ? (query.startsWith('?') ? query : `?${query}`) : ''
  window.open(`${base}${normalizedPath}${qs}`, '_blank')
}
