/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:04:50
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 11:04:50
* @Copyright aPaaS-front-team. All rights reserved.
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
