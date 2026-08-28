/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { authRequest } from '@/utils/request'

const LOCAL_TENANT_ID = '000000'

/* * Get after current . */
export function getTenantMode() {
  return authRequest.get('/sso/v1/mode')
}

/* * token Sa-Token. */
export function exchangePlatformToken(accessToken: string, tenantId?: string) {
  return authRequest.post('/sso/v1/exchangeToken', {
    accessToken,
    ...(tenantId ? { tenantId } : {})
  }, {
    skipTokenAuth: true,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      accesstoken: accessToken,
      ...(tenantId ? { tenantid: tenantId } : {})
    }
  } as any)
}

/* * item SpringBlade , old store need to field. */
export async function loginSSO(data: Record<string, any>) {
  const response: any = await authRequest.post('/blade-auth/token', null, {
    params: {
      tenantId: LOCAL_TENANT_ID,
      username: data.username || data.loginId || data.account,
      password: data.password,
      grant_type: 'password',
      scope: 'all',
      type: 'account'
    },
    skipTokenAuth: true,
    useBladeClientAuth: true
  } as any)
  const payload = response?.data || response || {}
  const accessToken = payload.accessToken || payload.access_token || payload.token
  return {
    code: 200,
    success: true,
    data: {
      ...payload,
      accessToken,
      refreshToken: payload.refreshToken || payload.refresh_token || accessToken,
      tenantId: payload.tenantId || payload.tenant_id || LOCAL_TENANT_ID
    }
  }
}

/* * user Validate current in token. */
export function verifyToken() {
  return authRequest.post('/sso/v1/getUserInfo', {})
}

/* * Get current userinfo. */
export function getUserInfo() {
  return authRequest.post('/sso/v1/getUserInfo', {})
}

/* * Get info. */
export function getUserTenants() {
  return authRequest.post('/sso/v1/getUserTenants', {})
}

/* * Validate after , new token. */
export function switchTenant(tenantId: string) {
  return authRequest.post('/sso/v1/switchTenant', { tenantId })
}

/* * in new current token. */
export function refreshToken(data: Record<string, any>) {
  return authRequest.post('/sso/v1/refreshToken', data)
}

/* * current will . */
export async function logout() {
  const response: any = await authRequest.post('/blade-auth/logout')
  return response?.code === undefined ? { code: 200, data: response } : response
}

/* * old . */
export function getLoginCodeV2(data: Record<string, any>) {
  return loginSSO(data)
}

/* * old . */
export function fastLogin(data: Record<string, any>) {
  return loginSSO(data)
}

/* * user in Update . */
export function resetPassword(data: Record<string, any>) {
  return authRequest.put('/system/user/profile/updatePwd', null, {
    params: { oldPassword: data.oldPassword, newPassword: data.password }
  })
}
