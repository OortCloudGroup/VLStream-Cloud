/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Get channelinfo
export function getCommonChannel(id) {
    return request({
        url: `/api/common/channel/one/${id}`,
        method: 'get',
    })
}

// Get channelinfo
export function getIndustryCodeList() {
    return request({
        url: `/api/common/channel/industry/list`,
        method: 'get',
    })
}


// Get channel
export function getDeviceTypeList() {
    return request({
        url: `/api/common/channel/type/list`,
        method: 'get',
    })
}

// Get
export function getNetworkIdentificationTypeList() {
    return request({
        url: `/api/common/channel/network/identification/list`,
        method: 'get',
    })
}

// channel
export function resetChannel(id) {
    return request({
        url: `/api/common/channel/reset/${id}`,
        method: 'post',
    })
}

// Update channelinfo
export function updateChannelData(data) {
    return request({
        url: `/api/common/channel/update`,
        method: 'post',
        data: data,
    })
}

// notificationdevice
export function sendDevicePush(params) {
    return request({
        url: `/api/play/start/${params.deviceId}/${params.channelId}`,
        method: 'get',
        timeout: 10000 * 6
    })
}

// Add channelinfo
export function addChannelData(data) {
    return request({
        url: `/api/common/channel/add`,
        method: 'post',
        data: data,
    })
}

// Get channel
export function queryListByCivilCode(query) {
    return request({
        url: `/api/common/channel/civilcode/list`,
        method: 'get',
        params: query
    })
}


// ParentIdGet channel
export function queryListByParentId(query) {
    return request({
        url: `/api/common/channel/parent/list`,
        method: 'get',
        params: query
    })
}

// Get info
export function getServerMediaInfo(query) {
    return request({
        url: `/api/server/media_server/media_info`,
        method: 'get',
        params: query
    })
}

// control
export function getPtzCamera(url, query) {
    return request({
        url: '/api/front-end/ptz/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}

// control -
export function getFocusCamera(url, query) {
    return request({
        url: '/api/front-end/fi/focus/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}
// control -
export function getIrIsCamera(url, query) {
    return request({
        url: '/api/front-end/fi/iris/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}
// control - Query
export function gotoPresetList(url) {
    return request({
        url: '/api/front-end/preset/query/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get'
    })
}
// control - Add
export function getAddPreset(url, query) {
    return request({
        url: '/api/front-end/preset/add/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}
// control -
export function callPreset(url, query) {
    return request({
        url: '/api/front-end/preset/call/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}
// control - Delete
export function deletePreset(url, query) {
    return request({
        url: '/api/front-end/preset/delete/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control - Add
export function GetAddCruisePoint(url, query) {
    return request({
        url: '/api/front-end/cruise/point/add/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control - Delete
export function GetDeleteCruisePoint(url, query) {
    return request({
        url: '/api/front-end/cruise/point/delete/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control - Set
export function GetSetSpeed(url, query) {
    return request({
        url: '/api/front-end/scan/set/speed/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control - Set
export function GetSetScanLeft(url, query) {
    return request({
        url: '/api/front-end/scan/set/left/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control - start
export function GetStartScan(url, query) {
    return request({
        url: '/api/front-end/scan/start/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}


// control -
export function GetStopScan(url, query) {
    return request({
        url: '/api/front-end/scan/stop/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control - Set
export function GetSetScanRight(url, query) {
    return request({
        url: '/api/front-end/scan/set/right/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control -
export function GetPtzWiper(url, query) {
    return request({
        url: '/api/front-end/wiper/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control - can
export function GetPtzSwitch(url, query) {
    return request({
        url: '/api/front-end/auxiliary/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// control -
export function GetBroadcast(url, query) {
    return request({
        url: '/api/play/broadcast/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}

// channel
export function addChannelToRegion(data) {
    return request({
        url: `/api/common/channel/region/add`,
        method: 'post',
        data: data
    })
}

// Delete channel
export function deleteChannelToRegion(data) {
    return request({
        url: `/api/common/channel/region/delete`,
        method: 'post',
        data: data
    })
}

// Delete channel
export function deleteChannelToGroup(data) {
    return request({
        url: `/api/common/channel/group/delete`,
        method: 'post',
        data: data
    })
}

// channelinfo
export function addChannelToGroup(data) {
    return request({
        url: `/api/common/channel/group/add`,
        method: 'post',
        data: data
    })
}


