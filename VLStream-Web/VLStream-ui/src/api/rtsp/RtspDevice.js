/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Query rtspdevice list
export function listRtspDevice(query) {
    return request({
        url: '/rtsp/RtspDevice/list',
        method: 'get',
        params: query
    })
}

// Query rtspdevice list
export function rtspDeviceList(query) {
  return request({
    url: '/rtsp/RtspDevice/rtspDeviceList',
    method: 'get',
    params: query
  })
}


// Query rtspdevice
export function getRtspDevice(id) {
    return request({
        url: '/rtsp/RtspDevice/' + id,
        method: 'get'
    })
}

// Add rtspdevice
export function addRtspDevice(data) {
    return request({
        url: '/rtsp/RtspDevice',
        method: 'post',
        data: data
    })
}

// Update rtspdevice
export function updateRtspDevice(data) {
    return request({
        url: '/rtsp/RtspDevice',
        method: 'put',
        data: data
    })
}

// Delete rtspdevice
export function delRtspDevice(id) {
    return request({
        url: '/rtsp/RtspDevice/' + id,
        method: 'delete'
    })
}

export function addDetection(data) {
    return request({
        url: '/rtsp/RtspDevice/addDetection',
        method: 'post',
        data: data
    })
}

export function stopDetection(data) {
    return request({
        url: '/rtsp/RtspDevice/stopDetection',
        method: 'post',
        data: data
    })
}

export function alarmClockRtspDevice(query) {
    return request({
        url: '/rtsp/RtspDevice/alarmClock',
        method: 'get',
        params: query
    })
}

