/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */
import request from '@/utils/wvpRequest'

export const listDevices = params => request({ url: '/ehome/devices', params })
export const getStatus = () => request({ url: '/ehome/status' })
export const getChannels = id => request({ url: `/ehome/devices/${id}/channels`, timeout: 15000 })
export const updateDevice = (id, data) => request({ url: `/ehome/devices/${id}`, method: 'put', data })
export const startPreview = (id, data) => request({ url: `/ehome/devices/${id}/preview`, method: 'post', data, timeout: 30000 })
export const stopPreview = id => request({ url: `/ehome/previews/${id}`, method: 'delete' })
export const keepPreview = id => request({ url: `/ehome/previews/${id}/heartbeat`, method: 'put' })
