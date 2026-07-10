export const SINGLE_TENANT_ID = '000000'
export const DEFAULT_USER_PASSWORD = 'Codex@123456'

/**
 * Reads the business payload from either RuoYi R<T> or a direct response object.
 */
export function payloadOf(response) {
  if (!response) return null
  if (Object.prototype.hasOwnProperty.call(response, 'data')) return response.data
  return response
}

/**
 * Converts page names from SpringBlade current/size to RuoYi pageNum/pageSize.
 */
export function toRuoyiPageParams(params = {}) {
  const { current, size, ...rest } = params || {}
  return {
    ...rest,
    pageNum: params.pageNum || current || 1,
    pageSize: params.pageSize || size || 10
  }
}

/**
 * Wraps a RuoYi TableDataInfo response in the SpringBlade page shape used by existing pages.
 */
export function toBladePage(response, mapper, pageParams = {}) {
  const rows = Array.isArray(response?.rows) ? response.rows : []
  const records = typeof mapper === 'function' ? rows.map(mapper) : rows
  return {
    ...response,
    success: response?.code === 200,
    data: {
      records,
      total: Number(response?.total || records.length),
      size: Number(pageParams.size || pageParams.pageSize || records.length || 10),
      current: Number(pageParams.current || pageParams.pageNum || 1)
    }
  }
}

/**
 * Wraps a RuoYi R<List<T>> response as the plain array payload expected by tree pages.
 */
export function toBladeList(response, mapper) {
  const payload = payloadOf(response)
  const rows = Array.isArray(payload) ? payload : []
  const data = typeof mapper === 'function' ? rows.map(mapper) : rows
  return {
    ...response,
    success: response?.code === 200,
    data
  }
}

/**
 * Wraps a RuoYi TableDataInfo response as a plain array payload for non-paginated tree pages.
 */
export function toBladeRows(response, mapper) {
  const rows = Array.isArray(response?.rows) ? response.rows : []
  const data = typeof mapper === 'function' ? rows.map(mapper) : rows
  return {
    ...response,
    success: response?.code === 200,
    data
  }
}

/**
 * Converts comma-separated, scalar, or array ids into a string array.
 */
export function toStringIds(value) {
  if (Array.isArray(value)) {
    return value.map(String).filter(Boolean)
  }
  if (value === undefined || value === null || value === '') {
    return []
  }
  return String(value).split(',').map((item) => item.trim()).filter(Boolean)
}

/**
 * Converts id inputs to numeric arrays for RuoYi fields declared as Long[].
 */
export function toNumberIds(value) {
  return toStringIds(value)
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item))
}

/**
 * Normalizes RuoYi status values to the legacy enabled=1 disabled=0 convention.
 */
export function toBladeStatus(status) {
  if (status === 0 || status === '1') return 0
  return 1
}

/**
 * Normalizes legacy status values to RuoYi normal=0 disabled=1.
 */
export function toRuoyiStatus(status) {
  if (status === 0 || status === '0') return '1'
  return '0'
}

/**
 * Converts sex values to the numeric form used by the current Element table template.
 */
export function toBladeSex(sex) {
  const value = Number(sex)
  return value === 1 || value === 2 ? value : 0
}

/**
 * Converts a local RuoYi SysUser row to the aliases used by VLStream system pages.
 */
export function mapRuoyiUser(user = {}, extra = {}) {
  const roleIds = extra.roleIds || (Array.isArray(user.roles) ? user.roles.map((role) => role.roleId) : [])
  const postIds = extra.postIds || []
  const account = user.loginId || user.userName || ''
  const displayName = user.nickName || user.userName || user.loginId || ''
  return {
    ...user,
    id: user.userId,
    userId: user.userId,
    account,
    name: displayName,
    realName: displayName,
    tenantId: user.tenantId || SINGLE_TENANT_ID,
    deptId: user.deptId || '',
    deptName: user.deptName || user.dept?.deptName || '',
    postId: user.postId || postIds[0] || '',
    postName: user.postName || user.postId || '',
    roleId: toStringIds(roleIds).join(','),
    roleName: Array.isArray(user.roles) ? user.roles.map((role) => role.roleName).filter(Boolean).join(',') : '',
    email: user.email || '',
    phone: user.phonenumber || '',
    sex: toBladeSex(user.sex),
    status: toBladeStatus(user.status)
  }
}

/**
 * Converts the legacy user form object into the local RuoYi SysUser payload.
 */
export function mapBladeUserToRuoyi(data = {}) {
  const roleIds = toStringIds(data.roleIds || data.roleId)
  const postIds = toNumberIds(data.postIds || data.postId)
  const userName = data.account || data.userName || ''
  const nickName = data.realName || data.name || data.nickName || userName
  const userId = data.userId || data.id
  return {
    userId,
    tenantId: data.tenantId || SINGLE_TENANT_ID,
    userName,
    nickName,
    loginId: data.account || userName,
    userType: data.userType || 'sys_user',
    deptId: data.deptId || undefined,
    deptName: data.deptName || undefined,
    postId: postIds.length ? String(postIds[0]) : data.postId || undefined,
    email: data.email || '',
    phonenumber: data.phone || data.phonenumber || '',
    sex: data.sex === undefined || data.sex === null || data.sex === '' ? '0' : String(data.sex),
    status: toRuoyiStatus(data.status),
    password: userId ? data.password : data.password || DEFAULT_USER_PASSWORD,
    roleIds,
    postIds
  }
}

/**
 * Converts a local RuoYi SysRole row to legacy role table and tree aliases.
 */
export function mapRuoyiRole(role = {}) {
  return {
    ...role,
    id: role.roleId,
    roleId: role.roleId,
    parentId: role.parentId || '0',
    roleAlias: role.roleKey || '',
    label: role.roleName || role.roleKey || role.roleId,
    sort: role.roleSort,
    tenantId: role.tenantId || SINGLE_TENANT_ID,
    status: toBladeStatus(role.status)
  }
}

/**
 * Converts the legacy role form object into the local RuoYi SysRole payload.
 */
export function mapBladeRoleToRuoyi(data = {}) {
  return {
    roleId: data.roleId || data.id,
    tenantId: data.tenantId || SINGLE_TENANT_ID,
    roleName: data.roleName || '',
    roleKey: data.roleKey || data.roleAlias || '',
    roleSort: data.roleSort || data.sort || 1,
    dataScope: data.dataScope || '1',
    status: toRuoyiStatus(data.status),
    remark: data.remark || '',
    menuIds: toNumberIds(data.menuIds),
    deptIds: toStringIds(data.deptIds)
  }
}

/**
 * Converts a local RuoYi SysDept row to legacy department table and tree aliases.
 */
export function mapRuoyiDept(dept = {}) {
  return {
    ...dept,
    id: dept.deptId,
    deptId: dept.deptId,
    parentId: dept.parentId || '0',
    tenantId: dept.tenantId || SINGLE_TENANT_ID,
    fullName: dept.fullName || dept.deptName || '',
    label: dept.deptName || dept.fullName || dept.deptId,
    sort: dept.orderNum,
    status: toBladeStatus(dept.status)
  }
}

/**
 * Converts the legacy department form object into the local RuoYi SysDept payload.
 */
export function mapBladeDeptToRuoyi(data = {}) {
  return {
    deptId: data.deptId || data.id,
    tenantId: data.tenantId || SINGLE_TENANT_ID,
    parentId: data.parentId || '0',
    deptName: data.deptName || data.fullName || '',
    orderNum: data.orderNum || data.sort || 1,
    status: toRuoyiStatus(data.status),
    remark: data.remark || ''
  }
}

/**
 * Converts RuoYi menu type letters to the numeric category used by legacy forms.
 */
export function toBladeMenuCategory(menuType) {
  if (menuType === 'F') return 2
  if (menuType === 'C') return 1
  return 0
}

/**
 * Converts legacy numeric menu category to RuoYi menu type letters.
 */
export function toRuoyiMenuType(category) {
  if (category === 2 || category === '2') return 'F'
  if (category === 1 || category === '1') return 'C'
  return 'M'
}

/**
 * Converts a local RuoYi SysMenu row to legacy menu table aliases.
 */
export function mapRuoyiMenu(menu = {}) {
  return {
    ...menu,
    id: menu.menuId,
    menuId: menu.menuId,
    parentId: menu.parentId || '0',
    name: menu.menuName || '',
    code: menu.perms || menu.path || '',
    alias: menu.menuName || '',
    category: toBladeMenuCategory(menu.menuType),
    action: menu.perms || '',
    source: menu.icon || '',
    sort: menu.orderNum,
    isOpen: menu.visible === '0' ? 1 : 2,
    label: menu.menuName || menu.path || menu.menuId,
    status: toBladeStatus(menu.status)
  }
}

/**
 * Converts the legacy menu form object into the local RuoYi SysMenu payload.
 */
export function mapBladeMenuToRuoyi(data = {}) {
  return {
    menuId: data.menuId || data.id,
    parentId: data.parentId || 0,
    menuName: data.menuName || data.name || data.alias || '',
    path: data.path || data.code || '',
    component: data.component,
    isFrame: data.isOpen === 1 || data.isOpen === true ? '0' : '1',
    menuType: toRuoyiMenuType(data.category),
    perms: data.perms || data.action || data.code || '',
    icon: data.icon || data.source || '',
    orderNum: data.orderNum || data.sort || 1,
    visible: '0',
    status: toRuoyiStatus(data.status),
    remark: data.remark || ''
  }
}

/**
 * Converts a local RuoYi SysPost row to legacy post table and selector aliases.
 */
export function mapRuoyiPost(post = {}) {
  return {
    ...post,
    id: post.postId,
    postId: post.postId,
    tenantId: post.tenantId || SINGLE_TENANT_ID,
    category: post.category || 3,
    label: post.postName || post.postCode || post.postId,
    sort: post.postSort,
    status: toBladeStatus(post.status)
  }
}

/**
 * Converts the legacy post form object into the local RuoYi SysPost payload.
 */
export function mapBladePostToRuoyi(data = {}) {
  return {
    postId: data.postId || data.id,
    tenantId: data.tenantId || SINGLE_TENANT_ID,
    postCode: data.postCode || '',
    postName: data.postName || '',
    postSort: data.postSort || data.sort || 1,
    status: toRuoyiStatus(data.status),
    remark: data.remark || ''
  }
}
