import request from '@/utils/request'
import { SINGLE_TENANT_ID, toRuoyiStatus } from './ruoyiCompat'

const DEFAULT_PAGE_SIZE = 999

function compact(value) {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined && item !== null && item !== ''))
}

function asArray(value) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  if (value === undefined || value === null || value === '') return []
  return String(value).split(',').map((item) => item.trim()).filter(Boolean)
}

function normalizeStatus(status) {
  if (status === undefined || status === null || status === '') return undefined
  return toRuoyiStatus(status)
}

function pickRows(response) {
  if (Array.isArray(response?.rows)) return response.rows
  if (Array.isArray(response?.data?.list)) return response.data.list
  if (Array.isArray(response?.data)) return response.data
  if (Array.isArray(response)) return response
  return []
}

function directoryList(list, extra = {}) {
  return {
    code: 200,
    success: true,
    msg: '操作成功',
    data: {
      list,
      total: list.length,
      ...extra
    }
  }
}

function idOf(value) {
  if (value === undefined || value === null) return ''
  return String(value)
}

export function mapDirectoryUser(user = {}) {
  const userId = idOf(user.userId || user.user_id || user.id)
  const loginId = user.loginId || user.userName || user.account || user.login_id || ''
  const userName = user.nickName || user.realName || user.name || user.userName || loginId || userId
  const deptId = idOf(user.deptId || user.dept_id || user.dept?.deptId)
  const deptName = user.deptName || user.dept_name || user.dept?.deptName || ''
  const phone = user.phonenumber || user.phone || user.user_detail?.phone || ''
  return {
    ...user,
    id: userId,
    userId,
    user_id: userId,
    account: loginId,
    login_id: loginId,
    userName: user.userName || loginId,
    user_name: userName,
    nickName: user.nickName || userName,
    realName: user.realName || userName,
    name: userName,
    deptId,
    dept_id: deptId,
    deptName,
    dept_name: deptName,
    tenantId: user.tenantId || SINGLE_TENANT_ID,
    tenant_id: user.tenantId || SINGLE_TENANT_ID,
    photo: user.photo || user.avatar || '',
    avatar: user.avatar || user.photo || '',
    phone,
    phonenumber: phone,
    user_detail: {
      ...(user.user_detail || {}),
      phone
    },
    unique_login_id: {
      ...(user.unique_login_id || {}),
      login_id: loginId
    },
    dept_list: user.dept_list || (deptId ? [{ dept_id: deptId, deptinfo: { dept_name: deptName } }] : [])
  }
}

export function mapDirectoryDept(dept = {}) {
  const deptId = idOf(dept.deptId || dept.dept_id || dept.id)
  const deptCode = idOf(dept.deptCode || dept.dept_code || deptId)
  const parentId = idOf(dept.parentId || dept.parent_id || '0')
  const deptName = dept.deptName || dept.dept_name || dept.fullName || dept.label || dept.name || deptId
  return {
    ...dept,
    id: deptId,
    deptId,
    dept_id: deptId,
    deptCode,
    dept_code: deptCode,
    parentId,
    parent_id: parentId,
    deptName,
    dept_name: deptName,
    fullName: dept.fullName || deptName,
    label: dept.label || deptName,
    name: dept.name || deptName,
    tenantId: dept.tenantId || SINGLE_TENANT_ID,
    tenant_id: dept.tenantId || SINGLE_TENANT_ID,
    dept_photo: dept.dept_photo || '',
    son_dept: Array.isArray(dept.son_dept) ? dept.son_dept : [],
    users: Array.isArray(dept.users) ? dept.users : []
  }
}

export function mapDirectoryRole(role = {}) {
  const roleId = idOf(role.roleId || role.role_id || role.id || role.ruuid)
  const roleName = role.roleName || role.role_name || role.name || roleId
  const roleKey = role.roleKey || role.roleAlias || role.role_alias || ''
  return {
    ...role,
    id: roleId,
    roleId,
    role_id: roleId,
    ruuid: roleId,
    name: roleName,
    roleName,
    role_name: roleName,
    roleKey,
    roleAlias: roleKey,
    tenantId: role.tenantId || SINGLE_TENANT_ID,
    tenant_id: role.tenantId || SINGLE_TENANT_ID
  }
}

async function loadUsersByIds(ids) {
  const users = await Promise.all(ids.map((id) => request({
    url: `/system/user/${encodeURIComponent(id)}`,
    method: 'get'
  }).then((response) => response?.data?.user || response?.data || null).catch(() => null)))
  return users.filter(Boolean).map(mapDirectoryUser)
}

async function loadDeptsByIds(ids) {
  const depts = await Promise.all(ids.map((id) => request({
    url: `/system/dept/${encodeURIComponent(id)}`,
    method: 'get'
  }).then((response) => response?.data || null).catch(() => null)))
  return depts.filter(Boolean).map(mapDirectoryDept)
}

export async function getUserList(params = {}) {
  const ids = asArray(params.user_id || params.userId || params.id)
  if (ids.length > 0) {
    return directoryList(await loadUsersByIds(ids))
  }

  const keyword = params.keyword || params.user_name || params.name || params.realName || params.nickName
  const response = await request({
    url: '/system/user/list',
    method: 'get',
    params: compact({
      pageNum: params.pageNum || params.current || params.page || 1,
      pageSize: params.pageSize || params.size || params.pagesize || DEFAULT_PAGE_SIZE,
      userName: params.account || params.userName || params.login_id,
      nickName: keyword,
      deptId: params.dept_id || params.deptId,
      status: normalizeStatus(params.status)
    })
  })
  const list = pickRows(response).map(mapDirectoryUser)
  return directoryList(list, { total: Number(response?.total || list.length) })
}

export async function getDeptList(params = {}) {
  const ids = asArray(params.dept_id || params.deptId || params.id)
  if (ids.length > 0) {
    return directoryList(await loadDeptsByIds(ids))
  }

  const keyword = params.keyword || params.dept_name || params.deptName || params.fullName || params.name
  const response = await request({
    url: '/system/dept/list',
    method: 'get',
    params: compact({
      deptName: keyword,
      parentId: params.parent_id || params.parentId,
      status: normalizeStatus(params.status)
    })
  })
  return directoryList(pickRows(response).map(mapDirectoryDept))
}

function buildDeptUserTree(depts, users) {
  const nodeById = new Map()
  const roots = []

  depts.map(mapDirectoryDept).forEach((dept) => {
    nodeById.set(dept.dept_id, { ...dept, son_dept: [], users: [] })
  })

  users.map(mapDirectoryUser).forEach((user) => {
    const node = nodeById.get(user.dept_id)
    if (node) node.users.push(user)
  })

  nodeById.forEach((node) => {
    const parent = nodeById.get(node.parent_id)
    if (parent && parent.dept_id !== node.dept_id) {
      parent.son_dept.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

export async function getDeptUser(params = {}) {
  const [deptResponse, userResponse] = await Promise.all([
    getDeptList(params),
    getUserList({ ...params, page: 1, pagesize: DEFAULT_PAGE_SIZE, pageSize: DEFAULT_PAGE_SIZE })
  ])
  const depts = deptResponse.data.list || []
  const users = userResponse.data.list || []
  return directoryList(buildDeptUserTree(depts, users), { deptList: depts, userList: users })
}

export function deptUserList(params = {}) {
  return getUserList(params)
}

export async function roleList(params = {}) {
  const response = await request({
    url: '/system/role/list',
    method: 'get',
    params: compact({
      pageNum: params.pageNum || params.page || 1,
      pageSize: params.pageSize || params.pagesize || DEFAULT_PAGE_SIZE,
      roleName: params.roleName || params.name,
      roleKey: params.roleAlias || params.roleKey
    })
  })
  const list = pickRows(response).map(mapDirectoryRole)
  return directoryList(list, { total: Number(response?.total || list.length) })
}

export function usedSet() {
  return Promise.resolve({ code: 200, success: true, msg: '操作成功', data: true })
}

export function usedDel() {
  return Promise.resolve({ code: 200, success: true, msg: '操作成功', data: true })
}

export function usedGet() {
  return Promise.resolve(directoryList([]))
}

export function addressUserList() {
  return Promise.resolve(directoryList([]))
}

export function addressList() {
  return Promise.resolve(directoryList([]))
}

export function tagList() {
  return Promise.resolve(directoryList([]))
}

export function tagUserList() {
  return Promise.resolve(directoryList([]))
}
