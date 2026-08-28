/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'
import {
  mapBladeRoleToRuoyi,
  mapRuoyiRole,
  toBladeRows,
  toStringIds
} from './ruoyiCompat'

/**
 * Query role list
 * @param {Object} params Query parameter, roleName (role name), roleAlias (role ), tenantId (tenant ID) etc.
 * @return s {Promise} Promise object
 */
export function getRoleList(params) {
  const ruoyiParams = {
    ...params,
    roleKey: params?.roleAlias || params?.roleKey
  }
  return request({ url: '/system/role/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladeRows(response, mapRuoyiRole))
}

/**
 * Query role
 * @param {Object} params Query parameter, tenantId
 * @return s {Promise} Promise object
 */
export function getRoleTree(params) {
  const ruoyiParams = {
    ...params,
    roleKey: params?.roleAlias || params?.roleKey
  }
  return request({ url: '/system/role/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladeRows(response, mapRuoyiRole))
}

/**
 * Get role role
 * @param {String|Number} roleId role ID
 * @return s {Promise} Promise object
 */
export function getRoleTreeById(roleId) {
  return request({ url: `/system/role/${encodeURIComponent(roleId)}`, method: 'get' })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: mapRuoyiRole(response?.data || {})
    }))
}

/**
 * Update roledata
 * @param {Object} data role data
 * @return s {Promise} operation Promise object
 */
export function submitRole(data) {
  const payload = mapBladeRoleToRuoyi(data)
  return request({
    url: '/system/role',
    method: payload.roleId ? 'put' : 'post',
    data: payload
  })
}

/**
 * Delete role
 * @param {String} ids role ID
 * @return s {Promise} operation Promise object
 */
export function removeRoles(ids) {
  return request({ url: `/system/role/${encodeURIComponent(ids)}`, method: 'delete' })
}

/**
 * role ( menu 、data 、interface )
 * @param {Object} data , roleIds, menuIds, dataScopeIds, apiScopeIds etc.field
 * @return s {Promise} operation Promise object
 */
export function grantRole(data) {
  const roleId = toStringIds(data?.roleIds)[0]
  return request({ url: `/system/role/${encodeURIComponent(roleId)}`, method: 'get' })
    .then((response) => {
      const role = mapBladeRoleToRuoyi({
        ...response?.data,
        id: roleId,
        menuIds: data?.menuIds
      })
      return request({ url: '/system/role', method: 'put', data: role })
    })
}
