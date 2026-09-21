/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

const devicePath = deviceId => encodeURIComponent(String(deviceId || '').trim())

export const getIpcTunnelStatus = deviceId => request({
  url: `/vlsTunnel/devices/${devicePath(deviceId)}`,
  method: 'get',
  silentError: true
})

export const createIpcTunnelAccessSession = deviceId => request({
  url: `/vlsTunnel/devices/${devicePath(deviceId)}/access-sessions`,
  method: 'post',
  sensitiveData: true
})

export const issueIpcTunnelEnrollment = deviceId => request({
  url: `/vlsTunnel/devices/${devicePath(deviceId)}/enrollments`,
  method: 'post',
  sensitiveData: true
})

export const changeIpcTunnelDesiredState = (deviceId, state) => request({
  url: `/vlsTunnel/devices/${devicePath(deviceId)}/desired-state`,
  method: 'post',
  params: { state }
})
