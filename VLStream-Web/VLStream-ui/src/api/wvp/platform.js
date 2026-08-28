/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Get
export function listPlatform(query) {
    return request({
        url: `/api/platform/query`,
        method: 'get',
        params: query
    })
}

// Check whether in
export function exitPlatform(serverGBId) {
    return request({
        url: `/api/platform/exit/${serverGBId}`,
        method: 'get',
    })
}

// Get info
export function serverConfig() {
    return request({
        url: `/api/platform/server_config`,
        method: 'get',
    })
}

// info
export function addPlatform(data) {
    return request({
        url: `/api/platform/add`,
        method: 'post',
        data: data
    })
}

// new info
export function updatePlatform(data) {
    return request({
        url: `/api/platform/update`,
        method: 'post',
        data: data
    })
}

// Delete info
export function delPlatform(id) {
    return request({
        url: `/api/platform/delete/${id}`,
        method: 'delete',
    })
}

// Push channel
export function pushChannel(id) {
    return request({
        url: `/api/platform/channel/push/${id}`,
        method: 'get',
    })
}

// Query list
export function queryChannelList(query) {
    return request({
        url: `/api/platform/channel/list`,
        method: 'get',
        params: query
    })
}

export function addChannel(data) {
    return request({
        url: `/api/platform/channel/add`,
        method: 'post',
        data: data
    })
}

export function delChannelForGB(data) {
    return request({
        url: `/api/platform/channel/remove`,
        method: 'delete',
        data: data
    })
}

export function addChannelByDevice(data) {
    return request({
        url: `/api/platform/channel/device/add`,
        method: 'post',
        data: data
    })
}

export function removeChannelByDevice(data) {
    return request({
        url: `/api/platform/channel/device/remove`,
        method: 'post',
        data: data
    })
}

