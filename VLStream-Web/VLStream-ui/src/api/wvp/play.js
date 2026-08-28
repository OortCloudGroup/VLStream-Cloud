/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

//
export function playStop(deviceId, channelId) {
    return request({
        url: `/api/play/stop/${deviceId}/${channelId}`,
        method: 'get',
    })
}

