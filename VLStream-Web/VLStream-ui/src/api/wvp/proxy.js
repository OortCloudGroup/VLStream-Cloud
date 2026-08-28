/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Get
export function listProxy(query) {
    return request({
        url: `/api/proxy/list`,
        method: 'get',
        params: query
    })
}

// Get ffmpeg.cmd
export function getFFmpegCMDs(query) {
    return request({
        url: `/api/proxy/ffmpeg_cmd/list`,
        method: 'get',
        params: query
    })
}

// new
export function updateProxy(data) {
    return request({
        url: `/api/proxy/update`,
        method: 'post',
        data: data
    })
}

// Add
export function addProxy(data) {
    return request({
        url: `/api/proxy/add`,
        method: 'post',
        data: data
    })
}

// Delete
export function deleteProxy(id) {
    return request({
        url: `/api/proxy/delete/${id}`,
        method: 'delete',
    })
}

//
export function stopProxy(id) {
    return request({
        url: `/api/proxy/stop/${id}`,
        method: 'post',
    })
}

//
export function start(query) {
    return request({
        url: `/api/proxy/start`,
        method: 'get',
        params: query
    })
}

