import axios from 'axios'
import { getModelHubAccessToken } from '@/utils/modelHubAuth'
import { MODEL_HUB_HEADERS } from '@/api/modelHubUser'

const PLATFORM_ORIGIN = 'https://workup-dev.myoumuamua.com:6433'
const NEW_API_BASE = import.meta.env.DEV
  ? '/bus/apaas-newapi'
  : `${PLATFORM_ORIGIN}/bus/apaas-newapi`

function getTenantId() {
  return (
    sessionStorage.getItem('modelHubTenantId') ||
    localStorage.getItem('modelHubTenantId') ||
    ''
  )
}

function getPlatformHeaders() {
  const accessToken = getModelHubAccessToken()
  const tenantId = getTenantId()
  return {
    ...MODEL_HUB_HEADERS,
    ...(accessToken ? { accesstoken: accessToken } : {}),
    ...(tenantId ? { tenantid: tenantId } : {}),
    'Cache-Control': 'no-store',
    Pragma: 'no-cache'
  }
}

function createRequest(baseURL, headers) {
  const request = axios.create({
    baseURL,
    timeout: 15000,
    headers
  })
  request.interceptors.response.use(
    (response) => response.data,
    (error) => Promise.reject(error)
  )
  return request
}

function unwrapData(response) {
  if (response?.success === false || response?.code === false) {
    throw new Error(response?.message || response?.msg || 'OortCloud 接口请求失败')
  }
  return response?.data !== undefined ? response.data : response
}

function normalizeModelBaseUrl(modelBaseUrl) {
  const configured = String(modelBaseUrl || '').trim().replace(/\/$/, '')
  if (!configured) return NEW_API_BASE
  if (import.meta.env.DEV && configured.startsWith(PLATFORM_ORIGIN)) {
    return configured.slice(PLATFORM_ORIGIN.length) || NEW_API_BASE
  }
  return configured
}

export async function getOortCloudTokenList() {
  const request = createRequest(NEW_API_BASE, getPlatformHeaders())
  const data = unwrapData(await request.get('/api/token/', {
    params: { p: 1, size: 10 }
  }))
  const items = Array.isArray(data) ? data : data?.items
  return (Array.isArray(items) ? items : [])
    .filter((item) => Number(item?.id) > 0)
    .map((item) => ({
      id: Number(item.id),
      name: item.name || `令牌 ${item.id}`,
      maskedKey: item.key || '',
      status: Number(item.status || 0),
      group: item.group || ''
    }))
}

export async function getOortCloudTokenKey(tokenId) {
  const request = createRequest(NEW_API_BASE, getPlatformHeaders())
  const data = unwrapData(await request.post(`/api/token/${tokenId}/key`))
  const value = String(data?.key || '').trim()
  if (!value) throw new Error('当前令牌未返回完整 Key')
  return value.startsWith('sk-') ? value : `sk-${value}`
}

export async function getOortCloudAccountStats() {
  const request = createRequest(NEW_API_BASE, getPlatformHeaders())
  return unwrapData(await request.get('/api/user/self')) || {}
}

export async function getOortCloudQuotaConfig() {
  const request = createRequest(NEW_API_BASE, getPlatformHeaders())
  return unwrapData(await request.get('/api/status')) || {}
}

export async function getOortCloudTokenUsage(apiKey, modelBaseUrl) {
  const request = createRequest(normalizeModelBaseUrl(modelBaseUrl), {
    Authorization: `Bearer ${apiKey}`,
    'X-Title': 'VLStream Cloud'
  })
  return unwrapData(await request.get('/api/usage/token')) || {}
}

export { NEW_API_BASE, PLATFORM_ORIGIN }
