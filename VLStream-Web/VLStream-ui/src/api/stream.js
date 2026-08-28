/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

/**
 * HLS Convert
 * @param {Object} data - Convert data
 * @param {string} data.deviceId - deviceID
 * @param {string} data.rtspUrl - RTSP
 * @param {string} data.quality - Convert (low/medium/high)
 */
export function startHLSStream(data) {
  return request({
    url: '/api/stream/start-hls',
    method: 'post',
    data
  })
}

/**
 * HLS Convert
 * @param {Object} data - data
 * @param {string} data.deviceId - deviceID
 */
export function stopHLSStream(data) {
  return request({
    url: '/api/stream/stop-hls',
    method: 'post',
    data
  })
}

/**
 * Get info
 */
export function getActiveStreams() {
  return request({
    url: '/api/stream/active',
    method: 'get'
  })
}

/**
 * device whether
 * @param {string} deviceId - deviceID
 */
export function checkStreamStatus(deviceId) {
  return request({
    url: `/api/stream/check/${deviceId}`,
    method: 'get'
  })
}

/**
 * all Convert (administrator can )
 */
export function stopAllStreams() {
  return request({
    url: '/api/stream/stop-all',
    method: 'post'
  })
}