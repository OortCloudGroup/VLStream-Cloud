/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// recordingcontrol APIinterface
export function recordApi(query) {
    return request({
        url: `/api/device/control/record/${query.deviceId}/${query.recordCmdStr}`,
        method: 'get',
        params: query
    })
}

// / APIinterface
export function guardApi(query) {
    return request({
        url: `/api/device/control/guard/${query.deviceId}/${query.guardCmdStr}`,
        method: 'get',
    })
}

