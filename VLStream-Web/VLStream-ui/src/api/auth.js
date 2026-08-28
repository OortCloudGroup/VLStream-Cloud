/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { authRequest } from '../utils/request.js'

// SpringBlade interfaceGet accessToken.
export function loginUser(params) {
  return authRequest.post('/blade-auth/token', null, {
    params,
    skipTokenAuth: true,
    useBladeClientAuth: true
  })
}

// VLS will token Get after userinfo.
export function getUserInfo(localToken) {
  return authRequest.get('/blade-system/user/info', {
    ...(localToken ? { localAuthToken: localToken } : {})
  })
}

// Validate token current userinterface, old user in .
export function verifyToken(localToken) {
  return getUserInfo(localToken)
}

// SpringBlade interface, current after to .
export function logoutUser() {
  return authRequest.post('/blade-auth/logout')
}
