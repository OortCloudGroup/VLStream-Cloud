/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:00
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:46:00
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-admin-platform/' + interfaceName,
    method: method,
    data: data
  })
}
function commonFuncS<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-deployment-service/' + interfaceName,
    method: method,
    data: data
  })
}
function commonFuncK<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-kongmanage/' + interfaceName,
    method: method,
    data: data
  })
}
// 应用市场（应用列表）
export function appmarket(data) {
  return commonFunc('admin-platform/appmarket', data)
}

// 获取分类列表
export function classifylist(data) {
  return commonFunc('admin-platform/classifylist', data)
}

// 服务-列表
export function manageServicelist(data) {
  return commonFuncK('/manage/v1/servicelist', data)
}

// 服务-所属分类列表
export function classifyListService(data) {
  return commonFuncS('v1/classify/classifylist', data)
}
