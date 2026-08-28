/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-auth' + interfaceName,
    method: method,
    data: data
  })
}

//
export function authList(data) {
  return commonFunc('/admin/v1/authList', data)
}

// configuration whether need to Validate
export function authSetValid(data) {
  return commonFunc('/admin/v1/authSetValid', data)
}

// role
export function roleAuth(data) {
  return commonFunc('/admin/v1/roleAuth', data)
}

// role
export function roleAuthLike(data) {
  return commonFunc('/admin/v1/roleAuthLike', data)
}

// role
export function roleInfo(data) {
  return commonFunc('/admin/v1/roleInfo', data)
}

// role
export function roleList(data) {
  return commonFunc('/admin/v1/roleList', data)
}

// Delete role
export function roleRemove(data) {
  return commonFunc('/admin/v1/roleRemove', data)
}

// role
export function roleSave(data) {
  return commonFunc('/admin/v1/roleSave', data)
}

// role user
export function roleUserLike(data) {
  return commonFunc('/admin/v1/roleUserLike', data)
}

// role user
export function roleUserList(data) {
  return commonFunc('/admin/v1/roleUserList', data)
}

// user role
export function userRoleLike(data) {
  return commonFunc('/admin/v1/userRoleLike', data)
}

// user role
export function userRoleUnlike(data) {
  return commonFunc('/admin/v1/userRoleUnlike', data)
}

// roleDelete user
export function roleUserUnlike(data) {
  return commonFunc('/admin/v1/roleUserUnlike', data)
}

// service
export function serviceInfo(data) {
  return commonFunc('/admin/v1/serviceInfo', data)
}

// service
export function serviceList(data) {
  return commonFunc('/admin/v1/serviceList', data)
}

// userrole
export function userRole(data) {
  return commonFunc('/admin/v1/userRole', data)
}

// menu
// menu
export function menuInfo(data) {
  return commonFunc('/menu/v1/menuInfo', data)
}

// menu
export function menuList(data) {
  return commonFunc('/menu/v1/menuList', data)
}

// Delete menu
export function menuRemove(data) {
  return commonFunc('/menu/v1/menuRemove', data)
}

// menu
export function menuSave(data) {
  return commonFunc('/menu/v1/menuSave', data)
}

// Get menu
export function myAuth(data) {
  return commonFunc('/menu/v1/myAuth', data)
}

//
export function platformInfo(data) {
  return commonFunc('/menu/v1/platformInfo', data)
}

//
export function platformList(data) {
  return commonFunc('/menu/v1/platformList', data)
}

// Delete info
export function platformRemove(data) {
  return commonFunc('/menu/v1/platformRemove', data)
}

// info
export function platformSave(data) {
  return commonFunc('/menu/v1/platformSave', data)
}

// Set menuInitialize
export function platformSaveInitStatus(data) {
  return commonFunc('/menu/v1/platformSaveInitStatus', data)
}

// menu
export function roleAuthM(data) {
  return commonFunc('/menu/v1/roleAuth', data)
}

// menu
export function platformCopyMenu(data) {
  return commonFunc('/menu/v1/platformCopy', data)
}

// menu
export function roleAuthLink(data) {
  return commonFunc('/menu/v1/roleAuthLink', data)
}

//
export function jobAuth(data) {
  return commonFunc('/admin/v1/jobAuth', data)
}

//
export function jobList(data) {
  return commonFunc('/admin/v1/jobList', data)
}

// Delete
export function jobRemove(data) {
  return commonFunc('/admin/v1/jobRemove', data)
}

// role
export function jobRoleLike(data) {
  return commonFunc('/admin/v1/jobRoleLike', data)
}

// role
export function jobRoleList(data) {
  return commonFunc('/admin/v1/jobRoleList', data)
}

// role
export function jobRoleUnlike(data) {
  return commonFunc('/admin/v1/jobRoleUnlike', data)
}

//
export function jobSave(data) {
  return commonFunc('/admin/v1/jobSave', data)
}

// user
export function jobUserLike(data) {
  return commonFunc('/admin/v1/jobUserLike', data)
}

// Delete user
export function jobUserUnlike(data) {
  return commonFunc('/admin/v1/jobUserUnlike', data)
}

//
export function jobInfo(data) {
  return commonFunc('/admin/v1/jobInfo', data)
}

// role
export function roleJobLike(data) {
  return commonFunc('/admin/v1/roleJobLike', data)
}

// role
export function roleJobList(data) {
  return commonFunc('/admin/v1/roleJobList', data)
}

// role
export function roleJobUnlike(data) {
  return commonFunc('/admin/v1/roleJobUnlike', data)
}

// user
export function userJobLike(data) {
  return commonFunc('/admin/v1/userJobLike', data)
}

// user
export function userJobUnlike(data) {
  return commonFunc('/admin/v1/userJobUnlike', data)
}

// user
export function userJobList(data) {
  return commonFunc('/admin/v1/userJobList', data)
}

//
//
export function postAuth(data) {
  return commonFunc('/admin/v1/postAuth', data)
}

//
export function postInfo(data) {
  return commonFunc('/admin/v1/postInfo', data)
}

// Get
export function postList(data) {
  return commonFunc('/admin/v1/postList', data)
}

// Delete
export function postRemove(data) {
  return commonFunc('/admin/v1/postRemove', data)
}

// role
export function postRoleLike(data) {
  return commonFunc('/admin/v1/postRoleLike', data)
}

// role
export function postRoleUnlike(data) {
  return commonFunc('/admin/v1/postRoleUnlike', data)
}

// role
export function postRoleList(data) {
  return commonFunc('/admin/v1/postRoleList', data)
}

//
export function postSave(data) {
  return commonFunc('/admin/v1/postSave', data)
}

// user
export function postUserLike(data) {
  return commonFunc('/admin/v1/postUserLike', data)
}

// user
export function postUserUnlike(data) {
  return commonFunc('/admin/v1/postUserUnlike', data)
}

// user
export function postUserList(data) {
  return commonFunc('/admin/v1/postUserList', data)
}

// role
export function rolePostLike(data) {
  return commonFunc('/admin/v1/rolePostLike', data)
}

// role
export function rolePostUnlike(data) {
  return commonFunc('/admin/v1/rolePostUnlike', data)
}

// role
export function rolePostList(data) {
  return commonFunc('/admin/v1/rolePostList', data)
}

// user
export function userPostLike(data) {
  return commonFunc('/admin/v1/userPostLike', data)
}

// user
export function userPostUnlike(data) {
  return commonFunc('/admin/v1/userPostUnlike', data)
}

// user
export function userPostList(data) {
  return commonFunc('/admin/v1/userPostList', data)
}

// roledata
export function roleDataList(data) {
  return commonFunc('/admin/v1/roleDataList', data)
}

export function roleDataLike(data) {
  return commonFunc('/admin/v1/roleDataLike', data)
}

export function roleDataUnlike(data) {
  return commonFunc('/admin/v1/roleDataUnlike', data)
}

// can
export function roleFunctionAdd(data) {
  return commonFunc('/admin/v1/roleFunctionAdd', data)
}

// can Delete
export function roleFunctionDel(data) {
  return commonFunc('/admin/v1/roleFunctionDel', data)
}

// can
export function roleFunctionEdit(data) {
  return commonFunc('/admin/v1/roleFunctionEdit', data)
}

// can
export function roleFunctionInfo(data) {
  return commonFunc('/admin/v1/roleFunctionInfo', data)
}

// can
export function roleFunctionList(data) {
  return commonFunc('/admin/v1/roleFunctionList', data)
}

// role
export function roleAppLike(data) {
  return commonFunc('/admin/v1/roleAppLike', data)
}

// role
export function roleAppList(data) {
  return commonFunc('/admin/v1/roleAppList', data)
}

// role
export function roleAppUnlike(data) {
  return commonFunc('/admin/v1/roleAppUnlike', data)
}

// Initialize
export function initAdd(data) {
  return commonFunc('/admin/v1/initAdd', data)
}

// Initialize Delete
export function initDel(data) {
  return commonFunc('/admin/v1/initDel', data)
}

// Initialize
export function initEdit(data) {
  return commonFunc('/admin/v1/initEdit', data)
}

// Initialize
export function initInfo(data) {
  return commonFunc('/admin/v1/initInfo', data)
}

// Initialize
export function initList(data) {
  return commonFunc('/admin/v1/initList', data)
}

// Initialize full
export function initListAll(data) {
  return commonFunc('/admin/v1/initListAll', data)
}

// Initialize
export function initSetStatus(data) {
  return commonFunc('/admin/v1/initSetStatus', data)
}

// Get Initialize data
export function initShow(data) {
  return commonFunc('/admin/v1/initShow', data)
}

// startInitialize data
export function initStart(data) {
  return commonFunc('/admin/v1/initStart', data)
}

// Get roledata interface
export function roleDataTable(data) {
  return commonFunc('/admin/v1/roleDataTable', data)
}

// role data
export function roleDataTableLike(data) {
  return commonFunc('/admin/v1/roleDataTableLike', data)
}

// Get data
export function dataTableList(data) {
  return commonFunc('/admin/v1/dataTableList', data)
}

// Get data
export function dataTableInfo(data) {
  return commonFunc('/admin/v1/dataTableInfo', data)
}

