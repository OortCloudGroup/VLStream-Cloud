/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest';

//
export function proxyPlay(data) {
    return request({
        url: '/dahua/zlmApi/proxyPlay',
        method: 'post',
        data: data
    })
}


export function stopProxy(id) {
    return request({
        url: '/dahua/zlmApi/stopProxy/' + id,
        method: 'get'
    })
}

