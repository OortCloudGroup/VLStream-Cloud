/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

/* * Process Cookie */


export const getToken = () => {
  // pcweb2.0 in sessionStorage token
  let tmeoBToken = ''
  if (sessionStorage.getItem('tempObj')) {
    try {
      const tempObj = JSON.parse(sessionStorage.getItem('tempObj') || '')
      tmeoBToken = tempObj.token
    } catch (error) {
      tmeoBToken = ''
    }
  }
  return sessionStorage.getItem('token') || tmeoBToken || localStorage.getItem('apaas_token') || ''
}

export const setToken = (token: string) => {
  sessionStorage.setItem('token', token)
  //
  localStorage.setItem('apaas_token', token)
}

export const removeToken = () => {
  sessionStorage.setItem('token', '')
  sessionStorage.setItem('tempObj', '')
  //
  localStorage.setItem('apaas_token', '')
  localStorage.setItem('refresh_token', '')
  // parameter null / empty
  window.location.search = ''
}

// Get new token
export const getRefreshToken = () => {
  return localStorage.getItem('refresh_token') || ''
}

// Set new token
export const setRefreshToken = (refreshToken)=>{
   return localStorage.setItem('refresh_token', refreshToken)
}
