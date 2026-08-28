/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'
import {
  mapBladeMenuToRuoyi,
  mapRuoyiMenu,
  toBladeList,
  toStringIds
} from './ruoyiCompat'

/**
 * Get all menu and button ( before Convert to )
 * @param {Object} params Query parameter
 * @return s {Promise} Promise object
 */
export function getMenuList(params) {
  const ruoyiParams = {
    ...params,
    menuName: params?.name || params?.menuName,
    perms: params?.code || params?.perms
  }
  return request({ url: '/system/menu/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladeList(response, mapRuoyiMenu))
}

/**
 * Get menu ( button)
 * @param {Object} params Query parameter
 * @return s {Promise} Promise object
 */
export function getMenuOnlyList(params) {
  return getMenuList(params)
}

/**
 * Get menu data
 * @param {Object} params Query parameter
 * @return s {Promise} Promise object
 */
export function getMenuTree(params) {
  return request({ url: '/system/menu/treeselect', method: 'get', params })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: response?.data || []
    }))
}

/**
 * Get role ( menu 、data 、interface )
 * @param {Object} params Query parameter
 * @return s {Promise} Promise object
 */
export function getGrantTree(params) {
  return request({ url: '/system/menu/treeselect', method: 'get', params })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: {
        menu: response?.data || [],
        dataScope: [],
        apiScope: []
      }
    }))
}

/**
 * Get rolecurrent already in ID
 * @param {String} roleIds role ID
 * @return s {Promise} already in key
 */
export function getRoleTreeKeys(roleIds) {
  const roleId = toStringIds(roleIds)[0]
  return request({ url: `/system/menu/roleMenuTreeselect/${encodeURIComponent(roleId)}`, method: 'get' })
    .then((response) => ({
      ...response,
      success: response?.code === 200,
      data: {
        menu: response?.data?.checkedKeys || [],
        dataScope: [],
        apiScope: []
      }
    }))
}

/**
 * Add new menu、button item
 * @param {Object} data menu data
 * @return s {Promise} operation Promise object
 */
export function submitMenu(data) {
  const payload = mapBladeMenuToRuoyi(data)
  return request({
    url: '/system/menu',
    method: payload.menuId ? 'put' : 'post',
    data: payload
  })
}

/**
 * Delete menu
 * @param {String} ids menu ID
 * @return s {Promise} operation Promise object
 */
export function removeMenus(ids) {
  const menuIds = toStringIds(ids)
  if (menuIds.length <= 1) {
    return request({ url: `/system/menu/${encodeURIComponent(menuIds[0] || ids)}`, method: 'delete' })
  }
  return Promise.all(menuIds.map((menuId) => request({ url: `/system/menu/${encodeURIComponent(menuId)}`, method: 'delete' })))
    .then(() => ({ code: 200, success: true, msg: '操作成功', data: true }))
}
