/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

// Get
export function getAllChild(query) {
    return request({
        url: `/api/region/base/child/list`,
        method: 'get',
        params: query
    })
}

// Query
export function queryForTree(query) {
    return request({
        url: `/api/region/tree/list`,
        method: 'get',
        params: query
    })
}

// new
export function updateRegion(data) {
    return request({
        url: `/api/region/update`,
        method: 'post',
        data: data
    })
}

//
export function addRegion(data) {
    return request({
        url: `/api/region/add`,
        method: 'post',
        data: data
    })
}

// Delete
export function deleteRegion(id) {
    return request({
        url: `/api/region/delete/${id}`,
        method: 'delete',
    })
}

