/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

// userinfo
export function syncUserToLocal(userInfo) {
  return request.post('/api/user/sync', {
    userId: userInfo.userId,
    tenantId: userInfo.tenantId,
    loginId: userInfo.loginId,
    userName: userInfo.userName,
    loginTime: userInfo.loginTime,
    loginIP: userInfo.loginIP,
    loginType: userInfo.login_type,
    client: userInfo.client,
    accessToken: userInfo.accessToken
  })
}

// Get userinfo
export function getLocalUserInfo(userId) {
  return request.get(`/api/user/${userId}`)
}

// new userinfo
export function updateLocalUserInfo(userId, userData) {
  return request.put(`/api/user/${userId}`, userData)
}

// APIobject
export const userSyncApi = {
  syncUser: syncUserToLocal,
  getUserInfo: getLocalUserInfo,
  updateUser: updateLocalUserInfo
}