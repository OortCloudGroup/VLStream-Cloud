/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

// data.
export function getAnalysisRequestPage(params) {
  return request({ url: '/vlsAnalysisRequest/list', method: 'get', params })
}

// , after in successfully after successfully.
export function applyAnalysisRequest(data) {
  return request({ url: '/vlsAnalysisRequest/apply', method: 'post', data })
}

// new already .
export function updateAnalysisRequest(data) {
  return request({ url: '/vlsAnalysisRequest/update', method: 'post', data })
}
