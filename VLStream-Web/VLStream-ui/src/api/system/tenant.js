/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'
import { SINGLE_TENANT_ID } from './ruoyiCompat'

/**
 * Query list
 * @param {Object} params current, size, tenantId, tenantName, contactNumber etc.parameter
 * @return s {Promise} data
 */
export function getTenantList(params) {
  return request({ url: '/blade-system/tenant/list', method: 'get', params })
}

/**
 * Get ( )
 * @param {Object} params parameter
 * @return s {Promise} item data
 */
export function getTenantSelect(params) {
  return request({ url: '/blade-system/tenant/select', method: 'get', params })
}

/**
 * Add Update data
 * @param {Object} data data
 * @return s {Promise} Promise object
 */
export function submitTenant(data) {
  return Promise.resolve({
    code: 200,
    success: true,
    msg: '单租户模式已固定默认租户',
    data: { ...(data || {}), tenantId: SINGLE_TENANT_ID }
  })
}

/**
 * Delete
 * @param {String} ids tenant ID
 * @return s {Promise} Promise object
 */
export function removeTenants(ids) {
  return Promise.resolve({
    code: 200,
    success: true,
    msg: '单租户模式不删除默认租户',
    data: true
  })
}
