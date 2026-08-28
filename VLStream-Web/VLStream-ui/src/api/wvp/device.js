/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

// Query device
export function listDevice(query) {
    return request({
        url: '/api/device/query/devices',
        method: 'get',
        params: query
    })
}

// Query device
export function deviceList(query) {
    return request({
        url: '/api/device/query/deviceList',
        method: 'get',
        params: query
    })
}

// Update data
export function updateTransport(data) {
    return request({
        url: `/api/device/query/transport/${data.deviceId}/${data.streamMode}`,
        method: 'post',
    })
}

// /
export function subscribeCatalog(data) {
    return request({
        url: `/api/device/query/subscribe/catalog/${data.id}/${data.cycle}`,
        method: 'post',
    })
}

// /
export function subscribeMobilePosition(data) {
    return request({
        url: `/api/device/query/subscribe/mobile-position/${data.id}/${data.cycle}/${data.interval}`,
        method: 'post',
    })
}

// IDQuery device
export function getDeviceById(deviceId) {
    return request({
        url: `/api/device/query/devices/${deviceId}`,
        method: 'get',
    })
}

// new deviceinfo
export function updateDevice(data) {
    return request({
        url: `/api/device/query/device/update/`,
        method: 'post',
        data: data
    })
}

// device
export function deleteDevice(deviceId) {
    return request({
        url: `/api/device/query/devices/${deviceId}/delete`,
        method: 'delete',
    })
}

// device
export function batchDeleteDevice(deviceId) {
    return request({
        url: `/api/device/query/devices/batchDelete/${deviceId}`,
        method: 'delete',
    })
}


// Query
export function syncStatus(deviceId) {
    return request({
        url: `/api/device/query/${deviceId}/sync_status`,
        method: 'post',
    })
}

// devicechannel
export function devicesSync(deviceId) {
    return request({
        url: `/api/device/query/devices/${deviceId}/sync`,
        method: 'post',
    })
}

// Query device
export function listDeviceChannel(query) {
    return request({
        url: `/api/device/query/devices/channels`,
        method: 'get',
        params: query
    })
}

// Update channel
export function changeAudio(data) {
    return request({
        url: `/api/device/query/channel/audio`,
        method: 'post',
        data: data
    })
}

// Update channel
export function updateChannelStreamIdentification(data) {
    return request({
        url: `/api/device/query/channel/stream/identification/update/`,
        method: 'post',
        data: data
    })
}

// Query sub channel
export function subChannels(query) {
    return request({
        url: `/api/device/query/sub_channels/channels`,
        method: 'get',
        params: query
    })
}

