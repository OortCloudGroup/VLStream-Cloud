/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import axios from 'axios'
import { getPlatformAccessToken } from '@/utils/request'

const PLATFORM_APP_ID = import.meta.env.VITE_PLATFORM_APP_ID || ''
const PLATFORM_SECRET_KEY = import.meta.env.VITE_PLATFORM_SECRET_KEY || ''

function platformServiceUrl(service, path = '') {
  const base = String(import.meta.env.VITE_PLATFORM_API_BASE || '').replace(/\/$/, '')
  const normalizedService = String(service || '').replace(/^\/+|\/+$/g, '')
  const normalizedPath = String(path || '').replace(/^\/+/, '')
  return `${base}/bus/${normalizedService}${normalizedPath ? `/${normalizedPath}` : ''}`
}

function responseData(response) {
  const body = response?.data || {}
  if (body.code !== undefined && Number(body.code) !== 200) {
    throw new Error(body.msg || body.message || '平台接口请求失败')
  }
  return body.data ?? body
}

function platformHeaders() {
  const accessToken = getPlatformAccessToken()
  const tenantId = sessionStorage.getItem('tenantId') || localStorage.getItem('tenantId') || ''
  return {
    Accept: 'application/json, text/plain, */*',
    requesttype: 'app',
    ...(accessToken ? { accesstoken: accessToken, Authorization: `Bearer ${accessToken}` } : {}),
    ...(PLATFORM_APP_ID ? { appid: PLATFORM_APP_ID } : {}),
    ...(PLATFORM_SECRET_KEY ? { secretkey: PLATFORM_SECRET_KEY } : {}),
    ...(tenantId ? { tenantid: tenantId } : {})
  }
}

/* * Get userinfo, VLS sub user . */
export async function getPlatformHeaderUser() {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'sso/v1/getUserInfo'),
    { accessToken: getPlatformAccessToken() },
    { headers: platformHeaders(), timeout: 10000 }
  )
  const payload = responseData(response) || {}
  return payload.userInfo || payload.user || payload
}

export async function getPlatformApps(userId = '') {
  const accessToken = getPlatformAccessToken()
  const response = await axios.post(
    platformServiceUrl('apaas-admin-platform', 'client/module/mypclist'),
    { accessToken, uuid: userId, pageNum: 1, pageSize: 999 },
    { headers: platformHeaders(), timeout: 10000 }
  )
  const payload = responseData(response) || {}
  return (payload.list || []).flatMap(item => item.app_list || [])
}

export async function getPlatformAccounts(isMoreAccount = 0) {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'sso/v1/userAccountCenter'),
    { accessToken: getPlatformAccessToken(), is_more_account: isMoreAccount },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return responseData(response) || []
}

export async function switchPlatformAccount(account) {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'sso/v1/login'),
    {
      accessToken: getPlatformAccessToken(),
      tenant_id: account?.user?.tenant?.tenant_id,
      user_id: account?.user?.user_id,
      indet_type: account?.type
    },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return response?.data
}

export async function getPlatformTenant() {
  const tenantId = sessionStorage.getItem('tenantId') || localStorage.getItem('tenantId') || ''
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'sso/v1/getTenant'),
    { accessToken: getPlatformAccessToken(), tenant_id: tenantId },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return responseData(response) || {}
}

export async function getPlatformUserTenants() {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'sso/v1/getUserTenants'),
    { accessToken: getPlatformAccessToken(), isUniqueId: 1 },
    { headers: platformHeaders(), timeout: 10000 }
  )
  const payload = responseData(response) || {}
  return payload.list || []
}

export async function getPlatformTenantDetail(tenantId) {
  const response = await axios.post(
    platformServiceUrl('apaas-user', 'tenant/v1/info'),
    { accessToken: getPlatformAccessToken(), tenant_id: tenantId },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return responseData(response) || {}
}

export async function verifyPlatformToken() {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'sso/v1/verifyToken'),
    { accessToken: getPlatformAccessToken() },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return response?.data
}

export async function markPlatformMessagesRead() {
  const response = await axios.post(
    platformServiceUrl('apaas-unified-msg', 'msg/v1/instatmsg/batchstatus'),
    { accessToken: getPlatformAccessToken(), status: 1 },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return response?.data
}

export async function getPlatformMessageInfo(messageId) {
  const response = await axios.get(
    platformServiceUrl('apaas-unified-msg', 'msg/v1/instatmsg/info'),
    {
      params: { accessToken: getPlatformAccessToken(), req_id: messageId },
      headers: platformHeaders(),
      timeout: 10000
    }
  )
  return responseData(response) || {}
}

export async function markPlatformMessageRead(messageId) {
  const response = await axios.post(
    platformServiceUrl('apaas-unified-msg', 'msg/v1/instatmsg/status'),
    { accessToken: getPlatformAccessToken(), req_id: Number(messageId), status: 1 },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return response?.data
}

export async function logoutPlatform() {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'sso/v1/logout'),
    { accessToken: getPlatformAccessToken() },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return response?.data
}

/* * Get aPaaS in not . */
export async function getPlatformMessages(status = 0, page = 1, pageSize = 6) {
  const response = await axios.get(
    platformServiceUrl('apaas-unified-msg', 'msg/v1/instatmsg/list'),
    {
      params: { accessToken: getPlatformAccessToken(), status, page, pageSize },
      headers: platformHeaders(),
      timeout: 10000
    }
  )
  const payload = responseData(response) || {}
  return {
    count: Number(payload.count || 0),
    total: Number(payload.total || payload.count || 0),
    list: (payload.list || []).map((item, index) => ({
      id: item.id || item.msg_id || index,
      title: item.msg_title || item.title || item.msg_content || item.content || '平台通知',
      content: item.msg_content || item.content || '',
      time: item.created_at || item.create_time || item.send_time || ''
    }))
  }
}

export async function getPlatformCurrentIndustry(userId, tenantId) {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'userSceneManage/v1/getLastIndustry'),
    { accessToken: getPlatformAccessToken(), tenant_id: tenantId, entity_id: userId },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return responseData(response) || {}
}

export async function getPlatformIndustryList(tenantId) {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'userSceneManage/v1/getMyIndustryList'),
    { accessToken: getPlatformAccessToken(), tenant_id: tenantId },
    { headers: platformHeaders(), timeout: 10000 }
  )
  const payload = responseData(response) || {}
  return payload.list || payload
}

export async function savePlatformIndustry(item, type, tenantId) {
  const response = await axios.post(
    platformServiceUrl('apaas-sso', 'userSceneManage/v1/saveLastIndustry'),
    {
      accessToken: getPlatformAccessToken(),
      industry_id: item?.industry_id,
      set_type: type,
      tenant_id: tenantId
    },
    { headers: platformHeaders(), timeout: 10000 }
  )
  return response?.data
}
