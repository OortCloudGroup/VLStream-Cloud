/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

/**
 * Query data list
 * @param {Object} params current, size, scopeName, resourceCode etc.parameter
 * @return s {Promise} data
 */
export function getDataScopeList(params) {
  return request({ url: '/blade-system/data-scope/list', method: 'get', params })
}

/**
 * Get data info
 * @param {Object} params parameter, id
 * @return s {Promise} data Promise object
 */
export function getDataScopeDetail(params) {
  return Promise.resolve({ code: 200, success: true, msg: '操作成功', data: null, params })
}

/**
 * new data configuration
 * @param {Object} data data configurationobject
 * @return s {Promise} operation Promise object
 */
export function submitDataScope(data) {
  return Promise.resolve({
    code: 500,
    success: false,
    msg: '当前后端未提供数据权限范围维护接口',
    data
  })
}

/**
 * Delete data configuration
 * @param {String} ids data ID
 * @return s {Promise} operation Promise object
 */
export function removeDataScopes(ids) {
  return Promise.resolve({
    code: 500,
    success: false,
    msg: '当前后端未提供数据权限范围删除接口',
    data: ids
  })
}
