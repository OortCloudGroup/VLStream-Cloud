/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

//
export function probe(query){
    return request({
        url: '/onvif/service/getInfo',
        method: 'get',
        params: query
    })
}

