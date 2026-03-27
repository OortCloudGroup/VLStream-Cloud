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

function commonFunc<T, K>(interfaceName: string, data: T, method: string, type = false) {
  let params = type ? { params: data } : { data: data }
  return request<K>({
    // url: 'http://192.168.88.56:8099' + interfaceName,
    url: config.URL + config.gateWay + 'apaas-sso/' + interfaceName,
    method: method,
    ...params
  })
}

// 部门用户APP活跃榜
export function AppStatistics(data) {
  return commonFunc('rank/v1/app.statistics', data, 'POST')
}

// 上报app使用情况
export function AppUse(data) {
  return commonFunc('rank/v1/app.use', data, 'POST')
}

// 部门活跃榜
export function deptRank(data) {
  return commonFunc('rank/v1/login.dept', data, 'POST')
}

// 用户活跃榜
export function userRank(data) {
  return commonFunc('rank/v1/login.user', data, 'POST')
}

// 部门应用榜
export function appDept(data) {
  return commonFunc('rank/v1/app.dept', data, 'POST')
}

// 用户应用榜
export function appUser(data) {
  return commonFunc('rank/v1/app.user', data, 'POST')
}
