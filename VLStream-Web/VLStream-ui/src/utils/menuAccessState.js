import { shallowRef } from 'vue'

// A navigation validates the current identity before publishing its permissions.
// Never persist a permission snapshot across identities or tenant switches.
export const menuPermissions = shallowRef(null)
export const menuIsSystemAdmin = shallowRef(false)
export const navigationUser = shallowRef(null)

export function setMenuPermissions(userInfo) {
  menuPermissions.value = Array.isArray(userInfo?.permissions) ? [...userInfo.permissions] : []
  menuIsSystemAdmin.value = userInfo?.isAdmin === true
  const user = userInfo?.user
  navigationUser.value = userInfo ? {
    userName: userInfo.userName || userInfo.loginId || '',
    userId: String(userInfo.userId ?? user?.userId ?? userInfo.id ?? ''),
    tenantId: String(userInfo.tenantId ?? user?.tenantId ?? ''),
    photo: userInfo.avatar || user?.avatar || ''
  } : null
}

export function clearMenuPermissions() {
  menuPermissions.value = null
  menuIsSystemAdmin.value = false
  navigationUser.value = null
}
