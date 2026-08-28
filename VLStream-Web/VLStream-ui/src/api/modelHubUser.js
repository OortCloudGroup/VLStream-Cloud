/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import axios from 'axios'
import { getModelHubAccessToken } from '@/utils/modelHubAuth'

/**
 * Model Hub SSO
 * correct : {platform}/bus/apaas-sso/sso/v1/getUserInfo
 */
const PLATFORM_SSO_BASE = import.meta.env.DEV
  ? '/bus/apaas-sso'
  : 'https://workup-dev.myoumuamua.com:6433/bus/apaas-sso'

export const MODEL_HUB_HEADERS = {
  'Content-Type': 'application/json',
  accept: 'application/json, text/plain, */*',
  requesttype: 'app',
  appid: '08e3168bd56a4e75ae3d5dee63db0657',
  secretkey: '32e3ca224aa741fbb1362d33070bca2f'
}

function createModelHubRequest(accessToken) {
  const token = accessToken || getModelHubAccessToken()
  const tenantId =
    sessionStorage.getItem('modelHubTenantId') ||
    localStorage.getItem('modelHubTenantId') ||
    ''

  const headers = {
    ...MODEL_HUB_HEADERS,
    ...(token ? { accesstoken: token } : {}),
    ...(tenantId ? { tenantid: tenantId } : {})
  }

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
export function getModelHubUserInfo(data = {}) {
  const accessToken = data.accessToken || getModelHubAccessToken()
  const request = createModelHubRequest(accessToken)
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

/* * userinfo */
export function editModelHubUser(data = {}) {
  const accessToken = data.accessToken || getModelHubAccessToken()
  const request = createModelHubRequest(accessToken)
  return request.post('/sso/v1/userEdit', {
    ...data,
    accessToken
  })
}
