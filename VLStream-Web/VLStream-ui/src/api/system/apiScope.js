/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

/**
 * Query interface list
 * @param {Object} params current, size, scopeName, resourceCode etc.parameter
 * @return s {Promise} interface
 */
export function getApiScopeList(params) {
  return request({ url: '/blade-system/api-scope/list', method: 'get', params })
}

/**
 * Get interface info
 * @param {Object} params parameter, id
 * @return s {Promise} interface Promise object
 */
export function getApiScopeDetail(params) {
  return Promise.resolve({ code: 200, success: true, msg: '操作成功', data: null, params })
}

/**
 * new interface configuration
 * @param {Object} data interface configurationobject
 * @return s {Promise} operation Promise object
 */
export function submitApiScope(data) {
  return Promise.resolve({
    code: 500,
    success: false,
    msg: '当前后端未提供接口权限范围维护接口',
    data
  })
}

/**
 * Delete interface configuration
 * @param {String} ids interface ID
 * @return s {Promise} operation Promise object
 */
export function removeApiScopes(ids) {
  return Promise.resolve({
    code: 500,
    success: false,
    msg: '当前后端未提供接口权限范围删除接口',
    data: ids
  })
}
