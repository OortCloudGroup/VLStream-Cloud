/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Query recording
export function openRtpServer(query) {
    return request({
        url: `/api/cloud/record/list`,
        method: 'get',
        params: query
    })
}

// Get
export function getPlayUrlPath(query) {
    return request({
        url: `/api/cloud/record/play/path`,
        method: 'get',
        params: query
    })
}

// Query list
export function listRecord(query) {
    return request({
        url: `/api/record/plan/query`,
        method: 'get',
        params: query
    })
}

// Add
export function addRecord(data) {
    return request({
        url: `/api/record/plan/add`,
        method: 'post',
        data: data
    })
}

// new
export function updateRecord(data) {
    return request({
        url: `/api/record/plan/update`,
        method: 'post',
        data: data
    })
}

// Get
export function getRecord(id) {
    return request({
        url: `/api/record/plan/get/${id}`,
        method: 'get',
    })
}

// Delete
export function deleteRecord(id) {
    return request({
        url: `/api/record/plan/delete/${id}`,
        method: 'delete',
    })
}

// Query channel list
export function listPlanRecord(query) {
    return request({
        url: `/api/record/plan/channel/list`,
        method: 'get',
        params: query
    })
}

// channel
export function link(data) {
    return request({
        url: `/api/record/plan/link`,
        method: 'post',
        data: data
    })
}

// Query recording
export function listDateRecord(query) {
    return request({
        url: `/api/cloud/record/date/list`,
        method: 'get',
        params: query
    })
}

