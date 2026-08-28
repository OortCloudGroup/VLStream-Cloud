/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { request } from '@/utils/service'

/* * Query current user . */
export function myOpinionList(data: Record<string, unknown>) {
  return request({
    url: '/api/v1/myOpinionList',
    method: 'post',
    data
  })
}

/* * current user . */
export function myOpinionSave(data: Record<string, unknown>) {
  return request({
    url: '/api/v1/myOpinionSave',
    method: 'post',
    data
  })
}

/* * Delete current user . */
export function myOpinionDel(data: Record<string, unknown>) {
  return request({
    url: '/api/v1/myOpinionDel',
    method: 'post',
    data
  })
}
