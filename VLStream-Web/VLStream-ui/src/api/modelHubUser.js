/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import axios from 'axios'
import { clearModelHubAuth, getModelHubAccessToken, getPlatformOrigin } from '@/utils/modelHubAuth'
import { applyPlatformGatewayHeaders } from '@/utils/request'

/**
 * Model Hub SSO
 * correct : {platform}/bus/apaas-sso/sso/v1/getUserInfo
 */
const PLATFORM_SSO_BASE = import.meta.env.DEV
  ? '/bus/apaas-sso'
  : `${getPlatformOrigin()}/bus/apaas-sso`

export const MODEL_HUB_HEADERS = {
  'Content-Type': 'application/json',
  accept: 'application/json, text/plain, */*',
  requesttype: 'app',
  appid: '08e3168bd56a4e75ae3d5dee63db0657',
  secretkey: '32e3ca224aa741fbb1362d33070bca2f'
}

export function getModelHubRequestHeaders(session) {
  const token = session ? session.accessToken : getModelHubAccessToken()
  const tenantId = session ? session.tenantId : (
    sessionStorage.getItem('modelHubTenantId') || localStorage.getItem('modelHubTenantId') || '')
  const headers = {
    ...(session?.usesPlatformSession ? { 'Content-Type': 'application/json', accept: MODEL_HUB_HEADERS.accept } : MODEL_HUB_HEADERS),
    ...(token ? { accesstoken: token } : {}),
    ...(tenantId ? { tenantid: tenantId } : {})
  }
  if (session?.usesPlatformSession) applyPlatformGatewayHeaders({ headers })
  return headers
}

function createModelHubRequest(accessToken, session) {
  const headers = getModelHubRequestHeaders(session)
  if (accessToken) headers.accesstoken = accessToken
  const instance = axios.create({
    baseURL: PLATFORM_SSO_BASE,
    timeout: 15000,
    headers
  })

  instance.interceptors.response.use(
    (response) => response.data,
    (error) => Promise.reject(error)
  )

  return instance
}

/* * Get userinfo */
export function getModelHubUserInfo(data = {}, session) {
  const accessToken = session ? session.accessToken : (data.accessToken || getModelHubAccessToken())
  const request = createModelHubRequest(accessToken, session)
  return request.post('/sso/v1/getUserInfo', {
    accessToken,
    desensitize: data.desensitize !== undefined ? data.desensitize : true
  })
}

/* * exit OortCloud , VLStream will . */
export function logoutModelHubUser() {
  const accessToken = getModelHubAccessToken()
  const request = createModelHubRequest(accessToken)
  return request.post('/sso/v1/logout', { accessToken })
}

export async function logoutModelHubSession() {
  try {
    await logoutModelHubUser()
  } finally {
    clearModelHubAuth()
  }
}

/* * userinfo */
export function editModelHubUser(data = {}) {
  const accessToken = data.accessToken || getModelHubAccessToken()
  const request = createModelHubRequest(accessToken)
  return request.post('/sso/v1/userEdit', {
    ...data,
    accessToken
  })
}
