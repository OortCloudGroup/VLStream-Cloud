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
 * 即时治理列表
 */
export function getVlsMobileSceneGovernanceImmediateList(data) {
  return commonFunc('/vlsMobileSceneGovernance/immediate/list', data, 'GET')
}

/**
 * 循环治理列表
 */
export function getVlsMobileSceneGovernanceloopList(data) {
  return commonFunc('/vlsMobileSceneGovernance/loop/list', data, 'GET')
}

/**
 * 新增即时治理
 */
export function saveVlsMobileSceneGovernanceImmediate(data) {
  return commonFunc('/vlsMobileSceneGovernance/immediate/save', data, 'POST')
}

/**
 * 新增循环治理
 */
export function saveVlsMobileSceneGovernanceLoop(data) {
  return commonFunc('/vlsMobileSceneGovernance/loop/save', data, 'POST')
}
