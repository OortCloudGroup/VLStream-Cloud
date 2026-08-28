/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

/**
 * deviceIDGet
 * @param {string} deviceId - deviceID
 * @returns {Promise}
 */
export const getTimeStrategy = (deviceId) => {
  return request({
    url: `/vlsTimeStrategy/${deviceId}`,
    method: 'get'
  })
}

/**
 * update time �?
 * @param {Object} timeStrategy - object
 * @returns {Promise}
 */
export const saveTimeStrategy = (timeStrategy) => {
  return request({
    url: '/vlsTimeStrategy',
    method: 'post',
    data: timeStrategy
  })
}

/**
 * deviceIDDelete
 * @param {string} deviceId - deviceID
 * @returns {Promise}
 */
export const deleteTimeStrategy = (deviceId) => {
  return request({
    url: `/vlsTimeStrategy/${deviceId}`,
    method: 'delete'
  })
}
