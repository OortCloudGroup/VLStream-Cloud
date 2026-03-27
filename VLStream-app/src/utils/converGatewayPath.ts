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

export function converGatewayPath(url) {
  let newFirstLayerPath = '/' + Config.gateWay
  let newUrl = url.replace(/^https?:\/\/[^\/]+\/[^\/]+/, (match) => {
    return match.replace(/\/[^\/]+$/, newFirstLayerPath)
  })
  return newUrl
}

export function covertCurrentLocationURL(val) {
  if (!val || !val.includes('http')) {
    return val
  }
  const patterns = ['/wj1/', '/oortwj1/']
  for (const pattern of patterns) {
    const parts = val.split(pattern)
    if (parts.length === 2) {
      return Config.URL + Config.gateWay + pattern.slice(1) + parts[1]
    }
  }
  return val
}
