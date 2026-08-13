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
 * WVP 后端仍是权限最终判定方。未同步 WVP 权限清单时显示操作入口，
 * 接口若无权限会返回真实的 401/403，不在前端伪造授权结果。
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
