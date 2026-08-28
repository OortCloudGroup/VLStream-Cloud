/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

// deviceconfigurationQuery APIinterface
export function configDownloadApi(query) {
    return request({
        url: `/api/device/config/query/${query.deviceId}/${query.configType}`,
        method: 'get',
        params: query
    })
}

