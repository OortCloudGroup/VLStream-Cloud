/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

//
export function playStop(query) {
    return request({
        url: `/api/playback/stop/${query.deviceId}/${query.channelId}/${query.streamId}`,
        method: 'get',
    })
}

// start
export function start(query) {
    return request({
        url: `/api/playback/start`,
        method: 'get',
        params: query,
    })
}

