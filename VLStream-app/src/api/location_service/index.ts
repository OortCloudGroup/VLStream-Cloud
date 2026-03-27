import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-location-service/' + interfaceName,
    method: method,
    ...params
  })
}

// 用户上图
export function deviceMap2(data) {
  return commonFunc('task/v2/device_map', data, 'post')
}

// 工牌历史轨迹
export function gpHistory(data) {
  return commonFunc('task/v1/device_position_list', data, 'post')
}

// 元数据历史轨迹
export function definitionDataTrack(data) {
  return commonFunc('definition/v1/definitionDataTrack', data, 'post')
}
