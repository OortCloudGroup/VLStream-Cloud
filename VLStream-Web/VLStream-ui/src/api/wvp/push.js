/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Get
export function listPush(query) {
    return request({
        url: `/api/push/list`,
        method: 'get',
        params: query
    })
}

// info
export function addPush(data) {
    return request({
        url: `/api/push/add`,
        method: 'post',
        data: data
    })
}

// new info
export function updatePush(data) {
    return request({
        url: `/api/push/update`,
        method: 'post',
        data: data
    })
}

// Delete
export function removePush(id) {
    return request({
        url: `/api/push/remove/${id}`,
        method: 'post',
    })
}

//
export function start(query) {
    return request({
        url: `/api/push/start`,
        method: 'get',
        params: query
    })
}

// idstart
export function startPlay(stream) {
    return request({
        url: `/api/push/startPlay/${stream}`,
        method: 'get',
    })
}
