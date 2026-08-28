/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import wvpRequest from '@/utils/wvpRequest'

export function getConfigKey(configKey) {
  return wvpRequest({
    url: `/system/config/configKey/${encodeURIComponent(configKey)}`,
    method: 'get'
  })
}
