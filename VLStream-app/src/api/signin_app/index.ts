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
    url: config.URL + config.gateWay + 'apaas-signservice' + interfaceName,
    method: method,
    ...params
  })
}

// 列表 获取今日签到的列表
export function list(data) {
  return commonFunc('/api/v1/signserviceservice/list', data, 'post', false)
}

// 前端
export function sign(data) {
  return commonFunc('/api/v1/signserviceservice/sign', data, 'post', false)
}

// 早到排行榜
export function rank(data) {
  return commonFunc('/api/v1/signserviceservice/rank', data, 'post', false)
}

// 获取当天上班时间以及打卡时间
export function work_time(data) {
  return commonFunc('/api/v1/signserviceservice/work_time', data, 'post', false)
}

// 获取当天上班时间以及打卡时间
export function appro(data) {
  return commonFunc('/api/v1/signserviceservice/appro', data, 'post', false)
}
