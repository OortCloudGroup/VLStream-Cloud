/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

// device APIinterface

/**
 * Get device ( )
 * @param {Object} params - Query parameter
 * @param {number} params.current - current
 * @param {number} params.size -
 * @param {string} params.keyword - (device deviceID�? * @param {string} params.status - device �? * @param {string} params.tag -
 */
export function getDeviceList(params) {
  console.log('API调用 getDeviceList，参数', params)
  return request({
    url: '/vlsDeviceInfo/page',
    method: 'get',
    params
  })
}

/**
 * Get device
 * @param {number} id - deviceID
 */
export function getDeviceById(id) {
  return request({
    url: `/vlsDeviceInfo/${id}`,
    method: 'get'
  })
}

/**
 * device
 * @param {Object} data - deviceinfo
 */
export function createDevice(data) {
  return request({
    url: '/vlsDeviceInfo',
    method: 'post',
    data
  })
}

/**
 * new device
 * @param {number} id - deviceID
 * @param {Object} data - deviceinfo
 */
export function updateDevice(id, data) {
  return request({
    url: `/vlsDeviceInfo/${id}`,
    method: 'put',
    data
  })
}

/**
 * Delete device
 * @param {number} id - deviceID
 */
export function deleteDevice(id) {
  return request({
    url: `/vlsDeviceInfo/${id}`,
    method: 'delete'
  })
}

/**
 * Batch delete device
 * @param {Array} ids - deviceIDarray
 */
export function batchDeleteDevices(ids) {
  return request({
    url: '/vlsDeviceInfo/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * Get device info
 */
export function getDeviceStatistics() {
  return request({
    url: '/vlsDeviceInfo/statistics',
    method: 'get'
  })
}

/**
 * Get device �? */
export function getDeviceTree() {
  return request({
    url: '/vlsDeviceInfo/tree',
    method: 'get'
  })
}

/**
 * device
 * @param {number} id - deviceID
 */
export function testDeviceConnection(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/test`,
    method: 'post'
  })
}

/**
 * new device �? * @param {number} id - deviceID
 */
export function refreshDeviceStatus(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/refresh`,
    method: 'post'
  })
}

/**
 * new device �? * @param {Array} ids - deviceIDarray
 */
export function batchRefreshDevices(ids) {
  return request({
    url: '/vlsDeviceInfo/batch/refresh',
    method: 'post',
    data: { ids }
  })
}

/**
 * Get device
 */
export function getDeviceTypeStatistics() {
  return request({
    url: '/vlsDeviceInfo/type-statistics',
    method: 'get'
  })
}

/**
 * Get all �? */
export function getDeviceTags() {
  return request({
    url: '/vlsDeviceInfo/tags',
    method: 'get'
  })
}

/**
 * PTZcontrol -
 * @param {number} id - deviceID
 * @param {string} direction - : up, down, left, right
 * @param {number} speed - �?-8�? */
export function ptzMove(id, direction, speed = 4) {
  return request({
    url: `/vlsDeviceInfo/${id}/ptz/move`,
    method: 'post',
    data: { direction, speed }
  })
}

/**
 * PTZcontrol -
 * @param {number} id - deviceID
 */
export function ptzStop(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/ptz/stop`,
    method: 'post'
  })
}

/**
 * PTZcontrol -
 * @param {number} id - deviceID
 * @param {string} action - : zoom_in, zoom_out
 * @param {number} speed - �?-8�? */
export function ptzZoom(id, action, speed = 4) {
  return request({
    url: `/vlsDeviceInfo/${id}/ptz/zoom`,
    method: 'post',
    data: { action, speed }
  })
}

/**
 * Get device �? * @param {number} id - deviceID
 */
export function getDeviceStreamInfo(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/stream`,
    method: 'get'
  })
}

/**
 * Export device
 * @param {Object} params - Query parameter
 */
export function exportDevices(params) {
  return request({
    url: '/vlsDeviceInfo/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * Import device
 * @param {FormData} formData - form �? */
export function importDevices(formData) {
  return request({
    url: '/vlsDeviceInfo/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * algorithm (algorithmID + WVPdevice )
 * @param {number} algorithmId algorithmID
 * @param {string} deviceIdsStr WVPdevice
 * @param {string} modelType model : pt/onnx/rknn/int8-rknn/om
 */
export function dispatchAlgorithmToDevices(algorithmId, deviceIdsStr, modelType = 'om') {
  return request({
    url: `/vlsDeviceInfo/${algorithmId}/algorithms`,
    method: 'post',
    params: { deviceIds: deviceIdsStr, modelType }
  })
}

/**
 * VLS after ZLMediaKit .
 * RTSP/RTMP after from devicerecord , service.
 */
export function createDevicePreview(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/preview`,
    method: 'post'
  })
}
