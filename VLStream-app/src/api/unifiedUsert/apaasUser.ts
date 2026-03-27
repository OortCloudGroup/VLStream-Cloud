/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:47:04
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-04-26 18:55:29
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post', isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-user' + interfaceName,
    method: method,
    ...params
  })
}

// tenant
// 租户添加
export function tenantAdd(data) {
  return commonFunc('/tenant/v1/add', data)
}

// 租户删除
export function tenantDel(data) {
  return commonFunc('/tenant/v1/delete', data)
}

// 租户编辑
export function tenantEdit(data) {
  return commonFunc('/tenant/v1/edit', data)
}

// 租户详情
export function tenantInfo(data) {
  return commonFunc('/tenant/v1/info', data)
}

// 租户列表
export function tenantList(data) {
  return commonFunc('/tenant/v1/list', data)
}

// 租户管理员重置密码
export function resetAdminPassword(data) {
  return commonFunc('/tenant/v1/resetAdminPassword', data)
}

// 租户状态设置
export function tenantSetStatus(data) {
  return commonFunc('/tenant/v1/setStatus', data)
}

// 添加租户管理员
export function addTenantAdmin(data) {
  return commonFunc('/tenant/v1/addAdminUser', data)
}

// 获取租户管理员
export function getTenantAdmin(data) {
  return commonFunc('/tenant/v1/getAdminUser', data)
}

// 移除租户管理员
export function removeTenantAdmin(data) {
  return commonFunc('/tenant/v1/removeAdminUser', data)
}

// user
// 部门添加
export function deptAdd(data) {
  return commonFunc('/user/v1/deptAdd', data)
}

// 部门删除
export function deptDelete(data) {
  return commonFunc('/user/v1/deptDelete', data)
}

// 部门编辑
export function deptEdit(data) {
  return commonFunc('/user/v1/deptEdit', data)
}

// 部门详情
export function deptInfo(data) {
  return commonFunc('/user/v1/deptInfo', data)
}

// 部门列表
export function deptList(data) {
  return commonFunc('/user/v1/deptList', data)
}

// 部门排序调整
export function deptSortShit(data) {
  return commonFunc('/user/v1/deptSortShit', data)
}

// 用户添加
export function userAdd(data) {
  return commonFunc('/user/v1/userAdd', data)
}

// 用户删除
export function userDelete(data) {
  return commonFunc('/user/v1/userDelete', data)
}

// 用户编辑
export function userEdit(data) {
  return commonFunc('/user/v1/userEdit', data)
}

// 用户详情
export function userV1Info(data) {
  return commonFunc('/user/v1/userInfo', data)
}

// 用户列表
export function userList(data) {
  return commonFunc('/user/v1/userList', data)
}

// 组织部门列表
export function deptUserList(data) {
  return commonFunc('/user/v1/deptUserList', data)
}

// 用户部门排序
export function userDeptSortShift(data) {
  return commonFunc('/user/v1/userDeptSortShift', data)
}

// 用户关联部门
export function userDeptLike(data) {
  return commonFunc('/user/v1/userDeptLike', data)
}

// 用户取消关联部门
export function userUnlikeDept(data) {
  return commonFunc('/user/v1/userDeptUnlike', data)
}

// 用户密码重置
export function resetUserPassword(data) {
  return commonFunc('/user/v1/resetUserPassword', data)
}

// 审核管理-租户审核列表
export function tenantCheckList(data) {
  return commonFunc('/tenant/v1/checkList', data)
}

// 审核管理-租户审核详情
export function tenantCheckInfo(data) {
  return commonFunc('/tenant/v1/checkInfo', data)
}

// 审核管理-租户审核
export function tenantcheck(data) {
  return commonFunc('/tenant/v1/check', data)
}

// 审核管理-部门审核列表
export function deptCheckList(data) {
  return commonFunc('/user/v1/deptCheckList', data)
}

// 审核管理-部门审核详情
export function deptCheckInfo(data) {
  return commonFunc('/user/v1/deptCheckInfo', data)
}

// 审核管理-部门审核
export function deptcheck(data) {
  return commonFunc('/user/v1/deptCheck', data)
}

// 审核管理-用户审核列表
export function userCheckList(data) {
  return commonFunc('/user/v1/userCheckList', data)
}

// 审核管理-用户审核详情
export function userCheckInfo(data) {
  return commonFunc('/user/v1/userCheckInfo', data)
}

// 审核管理-用户审核
export function usercheck(data) {
  return commonFunc('/user/v1/userCheck', data)
}

// 审核设置列表
export function checksetList(data) {
  return commonFunc('/checkset/v1/list', data)
}

// 审核设置编辑
export function checksetEdit(data) {
  return commonFunc('/checkset/v1/edit', data)
}

// 部门失效-失效
export function deptSetStatus(data) {
  return commonFunc('/user/v1/deptSetStatus', data)
}

// 用户失效-失效
export function uSetStatus(data) {
  return commonFunc('/user/v1/userSetStatus', data)
}

// 门户 通过流程id获取审核信息
export function getCheckInfoByProcinsId(data) {
  return commonFunc('/checkset/v1/getCheckInfoByProcinsId', data, 'POST')
}

// 修改顶级租户名称
export function updateHighTenantName(data) {
  return commonFunc('/tenant/v1/nameEdit ', data, 'POST')
}
