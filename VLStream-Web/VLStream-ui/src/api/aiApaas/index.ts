/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { request } from '@/utils/service'
import { apaasServiceUrl } from '@/utils/apaasApiBase'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: apaasServiceUrl('apaas-ai', interfaceName),
    method: method,
    data: data
  })
}

// ai Generate
export function textCompletion(data) {
  return commonFunc('api/v1/text_completion', data, 'post')
}

// ai Generate
export function imageGeneration(data) {
  return commonFunc('api/v1/text_img', data, 'post')
}

// Get AI
export function getTextImage(data) {
  return commonFunc('api/v1/text_img_state', data, 'post')
}

// Get base64
export function textImageDownload(data) {
  return commonFunc('api/v1/text_img_download', data, 'post')
}
