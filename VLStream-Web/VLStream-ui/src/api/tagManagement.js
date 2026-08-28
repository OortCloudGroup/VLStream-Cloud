/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

// API

/**
 * Query info
 * @param {Object} params - Query parameter
 * @param {number} params.current - current �?
 * @param {number} params.size -
 * @param {string} params.keyword - �?
 * @param {string} params.categoryType -
 * @param {number} params.level - layer
 * @param {number} params.parentId - ID
 */
export function getTagManagementPage(params) {
  return request({
    url: '/vlsTagManagement/page',
    method: 'get',
    params
  })
}

/**
 * Get ( )
 */
export function getTagTree() {
  return request({
    url: '/vlsTagManagement/tree',
    method: 'get'
  })
}

/**
 * Get
 * @param {number} id - ID
 */
export function getTagById(id) {
  return request({
    url: `/vlsTagManagement/${id}`,
    method: 'get'
  })
}

/**
 *
 * @param {Object} data - data
 */
export function createTag(data) {
  return request({
    url: '/vlsTagManagement',
    method: 'post',
    data
  })
}

/**
 * new
 * @param {number} id - ID
 * @param {Object} data - new data
 */
export function updateTag(id, data) {
  return request({
    url: `/vlsTagManagement/${id}`,
    method: 'put',
    data
  })
}

/**
 * Delete
 * @param {number} id - ID
 */
export function deleteTag(id) {
  return request({
    url: `/vlsTagManagement/${id}`,
    method: 'delete'
  })
}

/**
 * Batch delete
 * @param {Array} ids - IDarray
 */
export function batchDeleteTags(ids) {
  return request({
    url: '/vlsTagManagement/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * Get info
 */
export function getTagStatistics() {
  return request({
    url: '/vlsTagManagement/statistics',
    method: 'get'
  })
}

/**
 * Get
 * @param {number} id - ID
 */
export function getTagUsageStats(id) {
  return request({
    url: `/vlsTagManagement/${id}/stats`,
    method: 'get'
  })
}

/**
 * whether
 * @param {string} tagName -
 * @param {number} parentId - ID ( )
 * @param {number} excludeId - ID ( , )
 */
export function checkTagNameDuplicate(tagName, parentId = null, excludeId = null) {
  const params = { tagName }
  if (parentId !== null) params.parentId = parentId
  if (excludeId !== null) params.excludeId = excludeId

  return request({
    url: '/vlsTagManagement/check-name',
    method: 'get',
    params
  })
}

/**
 * Get all sub
 * @param {number} parentId - ID
 */
export function getChildTags(parentId) {
  return request({
    url: `/vlsTagManagement/${parentId}/children`,
    method: 'get'
  })
}

/**
 * new
 * @param {number} id - ID
 * @param {number} newParentId - new ID
 */
export function moveTag(id, newParentId) {
  return request({
    url: `/vlsTagManagement/${id}/move`,
    method: 'put',
    data: { newParentId }
  })
}

// device API

/**
 * Get device all
 * @param {number} deviceId - deviceID
 */
export function getDeviceTags(deviceId) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'get'
  })
}

/**
 * Get all device
 * @param {number} tagId - ID
 */
export function getTagDevices(tagId) {
  return request({
    url: `/api/device-tag-relation/tag/${tagId}/devices`,
    method: 'get'
  })
}

/**
 * to device
 * @param {number} deviceId - deviceID
 * @param {number} tagId - ID
 * @param {string} createdBy - creator
 */
export function addDeviceTag(deviceId, tagId, createdBy = 'system') {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tag/${tagId}`,
    method: 'post',
    params: { createdBy }
  })
}

/**
 * to device
 * @param {number} deviceId - deviceID
 * @param {Array} tagIds - IDarray
 * @param {string} createdBy - creator
 */
export function addDeviceTags(deviceId, tagIds, createdBy = 'system') {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'post',
    data: tagIds,
    params: { createdBy }
  })
}

/**
 * device
 * @param {number} deviceId - deviceID
 * @param {number} tagId - ID
 */
export function removeDeviceTag(deviceId, tagId) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tag/${tagId}`,
    method: 'delete'
  })
}

/**
 * device
 * @param {number} deviceId - deviceID
 * @param {Array} tagIds - IDarray
 */
export function removeDeviceTags(deviceId, tagIds) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'delete',
    data: tagIds
  })
}

/**
 * new device all
 * @param {number} deviceId - deviceID
 * @param {Array} tagIds - IDarray
 * @param {string} createdBy - creator
 */
export function updateDeviceTags(deviceId, tagIds, createdBy = 'system') {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/tags`,
    method: 'put',
    data: tagIds,
    params: { createdBy }
  })
}

/**
 * Delete device all
 * @param {number} deviceId - deviceID
 */
export function removeAllDeviceTags(deviceId) {
  return request({
    url: `/api/device-tag-relation/device/${deviceId}/all-tags`,
    method: 'delete'
  })
}

/**
 * Delete all device
 * @param {number} tagId - ID
 */
export function removeAllTagDevices(tagId) {
  return request({
    url: `/api/device-tag-relation/tag/${tagId}/all-devices`,
    method: 'delete'
  })
}

/**
 * device whether in
 * @param {number} deviceId - deviceID
 * @param {number} tagId - ID
 */
export function checkDeviceTagRelation(deviceId, tagId) {
  return request({
    url: '/api/device-tag-relation/check-relation',
    method: 'get',
    params: {
      deviceId,
      tagId
    }
  })
}
