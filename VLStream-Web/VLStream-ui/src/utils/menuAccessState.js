import { shallowRef } from 'vue'

// A navigation validates the current identity before publishing its permissions.
// Never persist a permission snapshot across identities or tenant switches.
export const menuPermissions = shallowRef(null)
export const menuIsSystemAdmin = shallowRef(false)

export function setMenuPermissions(userInfo) {
  menuPermissions.value = Array.isArray(userInfo?.permissions) ? [...userInfo.permissions] : []
  menuIsSystemAdmin.value = userInfo?.isAdmin === true && String(userInfo?.user?.userId ?? userInfo?.userId ?? '') === '1'
}

export function clearMenuPermissions() {
  menuPermissions.value = null
  menuIsSystemAdmin.value = false
}
