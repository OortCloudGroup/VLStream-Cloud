/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

export const getPlatformLogos = () => request.get('/system/platform-logo')

export const getCurrentPlatformLogo = () => request.get('/system/platform-logo/current')

export const createPlatformLogo = (file, description) => {
  const data = new FormData()
  data.append('file', file)
  data.append('description', description || '')
  return request.post('/system/platform-logo', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updatePlatformLogo = (id, file, description) => {
  const data = new FormData()
  if (file) data.append('file', file)
  data.append('description', description || '')
  return request.put(`/system/platform-logo/${encodeURIComponent(id)}`, data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const activatePlatformLogo = id => request.put(
  id == null
    ? '/system/platform-logo/default/active'
    : `/system/platform-logo/${encodeURIComponent(id)}/active`
)

export const removePlatformLogo = id => request.delete(
  `/system/platform-logo/${encodeURIComponent(id)}`
)
