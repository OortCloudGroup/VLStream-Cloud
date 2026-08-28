/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Query group
export function queryForTree(query) {
    return request({
        url: `/api/group/tree/list`,
        method: 'get',
        params: query
    })
}

// new group
export function updateGroup(data) {
    return request({
        url: `/api/group/update`,
        method: 'post',
        data: data
    })
}

// group
export function addGroup(data) {
    return request({
        url: `/api/group/add`,
        method: 'post',
        data: data
    })
}

// Delete group
export function deleteGroup(id) {
    return request({
        url: `/api/group/delete/${id}`,
        method: 'delete',
    })
}

