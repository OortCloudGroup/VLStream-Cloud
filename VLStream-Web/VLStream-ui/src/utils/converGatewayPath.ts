/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import Config from '@/config'

function getGatewayPath() {
  const gateWay = String(Config.gateWay || '').replace(/^\/+|\/+$/g, '')
  return gateWay ? `/${gateWay}` : ''
}

function getBaseURL() {
  return String(Config.URL || '').replace(/\/+$/g, '')
}

export function converGatewayPath(url) {
  if (!url) return url

  const gatewayPath = getGatewayPath()
  return url.replace(/^(https?:\/\/[^\/]+)(\/.*)$/i, (match, origin, path) => {
    return `${origin}${gatewayPath}${path}`
  })
}

export function covertCurrentLocationURL(val) {
  if (!val || !val.includes('http')) {
    return val
  }
  const patterns = ['/wj1/', '/oortwj1/']
  for (const pattern of patterns) {
    const parts = val.split(pattern)
    if (parts.length === 2) {
      return `${getBaseURL()}${getGatewayPath()}${pattern}${parts[1]}`
    }
  }
  return val
}
