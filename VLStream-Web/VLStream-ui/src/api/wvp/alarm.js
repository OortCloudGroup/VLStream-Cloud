/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

// Query list
export function listAlarm(query) {
  return request({
    url: '/api/alarm/all',
    method: 'get',
    params: query
  })
}

// Delete
export function delAlarm(alarmIds) {
  return request({
    url: '/api/alarm/delete/' + alarmIds,
    method: 'delete'
  })
}

