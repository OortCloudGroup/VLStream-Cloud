/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { request } from '@/utils/service'
import { getBaseURL } from '@/utils/request'

const localApiBaseUrl = getBaseURL().replace(/\/$/, '')

function localApiUrl(path: string) {
  return `${localApiBaseUrl}/${path.replace(/^\/+/, '')}`
}

/* * Query current user . */
export function myOpinionList(data: Record<string, unknown>) {
  return request({
    url: localApiUrl('/api/v1/myOpinionList'),
    method: 'post',
    data
  })
}

/* * current user . */
export function myOpinionSave(data: Record<string, unknown>) {
  return request({
    url: localApiUrl('/api/v1/myOpinionSave'),
    method: 'post',
    data
  })
}

/* * Delete current user . */
export function myOpinionDel(data: Record<string, unknown>) {
  return request({
    url: localApiUrl('/api/v1/myOpinionDel'),
    method: 'post',
    data
  })
}
