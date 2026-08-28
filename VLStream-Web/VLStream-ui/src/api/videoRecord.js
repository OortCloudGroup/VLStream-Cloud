/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

/**
 * record API
 */

// Query record
export function getVideoRecordPage(params) {
  return request({
    url: '/vlsVideoRecord/page',
    method: 'get',
    params
  })
}

// IDGet record
export function getVideoRecordById(id) {
  return request({
    url: `/vlsVideoRecord/${id}`,
    method: 'get'
  })
}

// record
export function createVideoRecord(data) {
  return request({
    url: '/vlsVideoRecord',
    method: 'post',
    data
  })
}

// new record
export function updateVideoRecord(id, data) {
  return request({
    url: `/vlsVideoRecord/${id}`,
    method: 'put',
    data
  })
}

// Delete record
export function deleteVideoRecord(id) {
  return request({
    url: `/vlsVideoRecord/${id}`,
    method: 'delete'
  })
}

// Batch delete record
export function batchDeleteVideoRecords(ids) {
  return request({
    url: '/vlsVideoRecord/batch',
    method: 'delete',
    data: ids
  })
}

// start
export function startRecording(deviceId, deviceName = '设备', duration = 600, quality = 'medium') {
  return request({
    url: `/vlsVideoRecord/start`,
    method: 'post',
    params: {
      deviceId,
      deviceName,
      duration,
      quality
    }
  })
}

//
export function stopRecording(recordId) {
  return request({
    url: `/vlsVideoRecord/stop/${recordId}`,
    method: 'post'
  })
}

// Get
export function getRecordingStatus(deviceId) {
  return request({
    url: `/vlsVideoRecord/status/${deviceId}`,
    method: 'get'
  })
}

// Get info
export function getRecordingStatistics() {
  return request({
    url: '/vlsVideoRecord/statistics',
    method: 'get'
  })
}

// Get device record
export function getDeviceRecords(deviceId, params) {
  return request({
    url: `/vlsVideoRecord/device/${deviceId}`,
    method: 'get',
    params
  })
}

//
export function downloadRecordFile(id) {
  return request({
    url: `/vlsVideoRecord/${id}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

//
export function previewRecordFile(id) {
  return request({
    url: `/vlsVideoRecord/${id}/preview`,
    method: 'get'
  })
}

// Get
export function getRecordFile(filePath) {
  return request({
    url: `/vlsVideoRecord/file/${encodeURIComponent(filePath)}`,
    method: 'get'
  })
}

// Get
export function getRecordThumbnail(filePath) {
  return request({
    url: `/vlsVideoRecord/thumbnail/${encodeURIComponent(filePath)}`,
    method: 'get'
  })
}
