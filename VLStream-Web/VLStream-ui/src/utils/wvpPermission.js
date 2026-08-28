/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

const readPermissions = () => {
  const raw = sessionStorage.getItem('wvpPermissions') || localStorage.getItem('wvpPermissions')
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

/**
 * WVP after is . not WVP operation ,
 * interface will 401/403, in before .
 */
export function checkPermi(requiredPermissions) {
  const permissions = readPermissions()
  if (!permissions) return true
  if (permissions.includes('*:*:*')) return true
  return requiredPermissions.some((permission) => permissions.includes(permission))
}

export function checkRole(requiredRoles) {
  const raw = sessionStorage.getItem('wvpRoles') || localStorage.getItem('wvpRoles')
  if (!raw) return true
  try {
    const roles = JSON.parse(raw)
    return Array.isArray(roles) && (roles.includes('admin') || requiredRoles.some((role) => roles.includes(role)))
  } catch {
    return true
  }
}
