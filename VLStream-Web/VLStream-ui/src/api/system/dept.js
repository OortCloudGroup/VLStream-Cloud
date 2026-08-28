/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'
import {
  mapBladeDeptToRuoyi,
  mapRuoyiDept,
  toBladeList,
  toStringIds
} from './ruoyiCompat'

/**
 * Get department
 * @param {Object} params parameter, deptName, fullName, tenantId etc.
 * @return s {Promise} department data
 */
export function getDeptList(params) {
  const ruoyiParams = {
    ...params,
    deptName: params?.deptName || params?.fullName
  }
  return request({ url: '/system/dept/list', method: 'get', params: ruoyiParams })
    .then((response) => toBladeList(response, mapRuoyiDept))
}

/**
 * Get department data
 * @param {Object} params tenantId etc. parameter
 * @return s {Promise} department data
 */
export function getDeptTree(params) {
  return request({ url: '/system/dept/list', method: 'get', params })
    .then((response) => toBladeList(response, mapRuoyiDept))
}

/**
 * Add Update departmentdata
 * @param {Object} data department object
 * @return s {Promise} Promise object
 */
export function submitDept(data) {
  const payload = mapBladeDeptToRuoyi(data)
  return request({
    url: '/system/dept',
    method: payload.deptId ? 'put' : 'post',
    data: payload
  })
}

/**
 * Delete department
 * @param {String} ids department ID
 * @return s {Promise} Promise object
 */
export function removeDepts(ids) {
  const deptIds = toStringIds(ids)
  if (deptIds.length <= 1) {
    return request({ url: `/system/dept/${encodeURIComponent(deptIds[0] || ids)}`, method: 'delete' })
  }
  return Promise.all(deptIds.map((deptId) => request({ url: `/system/dept/${encodeURIComponent(deptId)}`, method: 'delete' })))
    .then(() => ({ code: 200, success: true, msg: '操作成功', data: true }))
}
