/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'
import {
  mapBladeUserToRuoyi,
  mapRuoyiUser,
  toBladePage,
  toRuoyiPageParams,
  toStringIds
} from './ruoyiCompat'

/**
 * Query user list
 * @param {Object} params Query parameter, current (current ), size ( ), account ( ), realName ( ) etc.
 * @return s {Promise} Promise object
 */
export function getUserList(params) {
  const ruoyiParams = {
    ...toRuoyiPageParams(params),
    userName: params?.account || params?.userName,
    nickName: params?.realName || params?.nickName || params?.name,
    phonenumber: params?.phone || params?.phonenumber
  }
  return request({ url: '/system/user/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladePage(response, mapRuoyiUser, params))
}

/**
 * Query ID user
 * @param {Object} params parameter, id
 * @return s {Promise} Promise object
 */
export function getUserDetail(params) {
  const userId = params?.id || params?.userId
  return request({ url: `/system/user/${encodeURIComponent(userId)}`, method: 'get' })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: mapRuoyiUser(response?.data?.user || {}, {
        roleIds: response?.data?.roleIds || [],
        postIds: response?.data?.postIds || []
      })
    }))
}

/**
 * new user ( interface)
 * @param {Object} data user data, after User field
 * @return s {Promise} operation Promise object
 */
export function submitUser(data) {
  return request({ url: '/system/user', method: 'post', data: mapBladeUserToRuoyi(data) })
}

/**
 * new userdata
 * @param {Object} data new user , user ID
 * @return s {Promise} operation Promise object
 */
export function updateUser(data) {
  return request({ url: '/system/user', method: 'put', data: mapBladeUserToRuoyi(data) })
}

/**
 * Delete user
 * @param {String} ids user ID
 * @return s {Promise} operation Promise object
 */
export function removeUsers(ids) {
  return request({ url: `/system/user/${encodeURIComponent(ids)}`, method: 'delete' })
}

/**
 * user role
 * @param {String} userIds user ID
 * @param {String} roleIds role ID
 * @return s {Promise} operation Promise object
 */
export function grantUserRoles(userIds, roleIds) {
  const userId = toStringIds(userIds)[0]
  return request({
    url: '/system/user/authRole',
    method: 'put',
    params: { userId, roleIds: toStringIds(roleIds).join(',') }
  })
}

/**
 * user
 * @param {String} userIds user ID
 * @return s {Promise} operation of the Promise object
 */
export function resetUserPassword(userIds) {
  const userId = toStringIds(userIds)[0]
  return request({
    url: '/system/user/resetPwd',
    method: 'put',
    data: { userId, password: 'Codex@123456' }
  })
}

/**
 * user
 * @param {String} userIds user ID
 * @return s {Promise} operation Promise object
 */
export function unlockUsers(userIds) {
  const userId = toStringIds(userIds)[0]
  return request({
    url: '/system/user/changeStatus',
    method: 'put',
    data: { userId, status: '0' }
  })
}
