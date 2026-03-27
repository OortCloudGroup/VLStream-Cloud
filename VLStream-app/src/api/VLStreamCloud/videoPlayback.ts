/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:38:14
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-01-23 14:35:59
* @Copyright aPaaS-front-team. All rights reserved.
*/
// import { request } from '@/utils/service'
// import config from '@/config'
// function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
//   return request<K>({
//     url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
//     method: method,
//     data: data
//   })
// }

// export function getDeviceList(data) {
//   return commonFunc('/vlsDeviceInfo/page', data, 'GET')
// }

import { request } from '@/utils/service'
import config from '@/config'

// 公共请求函数：统一URL拼接、请求方式、入参处理，泛型支持类型约束
function commonFunc<T, K>(interfaceName: string, data: T, method = 'post', isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request < K >({
    url: config.URL + config.gateWay + 'vls-server' + interfaceName,
    method: method,
    ...params
  })
}

/**
 * 视频回放查询
 */
export function getPlayback(data) {
  return commonFunc('/vlsVideoRecord/playback', data, 'GET')
}

/**
 * 获取视频回放流
 * @param {number|string} recordId - 录制记录ID
 * @param {Object} params - 请求参数，包含 accessToken
 */
export function getPlaybackStream(recordId, params = {}) {
  return request({
    url: config.URL + config.gateWay + 'vls-server/vlsVideoRecord/stream/' + recordId,
    method: 'GET',
    params: params,
    responseType: 'blob'
  })
}
