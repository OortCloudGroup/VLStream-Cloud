/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:45:18
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-04-12 17:06:18
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, type = true) {
  let params = type ? { params: data } : { data: data }
  return request<K>({
    // url: 'http://192.168.88.56:8099' + interfaceName,
    url: config.URL + config.gateWay + 'apaas-newsservice' + interfaceName,
    method: method,
    ...params
  })
}

// 新闻
export function new_list(data) {
  return commonFunc('/api/v1/newservice/new_list', data, 'POST')
}
// 新闻详情
export function new_Detail(data) {
  return commonFunc('/api/v1/newservice/detail', data, 'POST')
}
// 发布新闻
export function push_new(data) {
  return commonFunc('/api/v1/newservice/new_save', data, 'POST', false)
}

// 编辑新闻
export function edit_new(data) {
  return commonFunc('/api/v1/newservice/new_edit', data, 'POST', false)
}

// 审核新闻
export function check_new(data) {
  return commonFunc('/api/v1/newservice/check', data, 'POST')
}
