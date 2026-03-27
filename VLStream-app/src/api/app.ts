/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:50:22
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-11-15 10:52:49
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

// 应用市场（应用列表）
export function appmarket(data) {
  return commonFunc('admin-platform/appmarket', data)
}

// 获取分类列表
export function classifylist(data) {
  return commonFunc('admin-platform/classifylist', data)
}
