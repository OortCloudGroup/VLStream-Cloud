import { request } from '@/utils/service'

import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false, type = 1) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  let url
  if (type === 1) {
    url = config.URL + config.gateWay + 'zc-smartcity-server/' + interfaceName
  } else {
    url = config.URL + config.gateWay + 'apaas-location-service' + interfaceName
  }
  return request<K>({
    url: url,
    method: method,
    ...params
  })
}

// 获得园林养护任务分页
export function maintenanceList(data) {
  return commonFunc('admin-api/smartCity/greenery-management-task/page', data, 'get')
}
// 获得园林养护任务
export function maintainDetails(data) {
  return commonFunc('admin-api/smartCity/greenery-management-task/get', data, 'get')
}
// 获得园林养护统计列表
export function statisticList(data) {
  return commonFunc('admin-api/smartCity/greenery-management-task/getOverViewList', data, 'get')
}
// 获得园林养护应急事件处理分页
export function eventList(data) {
  return commonFunc('admin-api/smartCity/greenery-management-emergency-incident/page', data, 'get')
}

// 创建园林养护任务
export function addMaintenance(data) {
  return commonFunc('admin-api/smartCity/greenery-management-task/create', data, 'post')
}
// 字典
export function dictionary(data) {
  return commonFunc('admin-api/system/dict-data/page', data, 'get', true)
}

// 获得园林养护任务分页（任务下发）
export function addTask(data) {
  return commonFunc('admin-api/smartCity/greenery-management-task/updateUser', data, 'post', true)
}
// 创建园林养护应急事件处理
export function addEvent(data) {
  return commonFunc('admin-api/smartCity/greenery-management-emergency-incident/create', data, 'post')
}
// 获得园林养护记录分页
export function maintainRecordList(data) {
  return commonFunc('admin-api/smartCity/greenery-management-record/page', data, 'get')
}
// 获得园林养护记录
export function maintainRecord(data) {
  return commonFunc('admin-api/smartCity/greenery-management-record/get', data, 'get')
}
// 工牌设备获取历史轨迹
export function trajectory(data) {
  return commonFunc('/task/v1/device_position_list', data, 'post', false, 2)
}
// 获得园林我的养护任务分页
export function myMaintenanceList(data) {
  return commonFunc('admin-api/smartCity/greenery-management-task/myPage', data, 'get')
}
// 获得园林我的养护任务分页
export function myComMaintenanceList(data) {
  return commonFunc('admin-api/smartCity/greenery-management-common-task/myPage', data, 'get')
}
// 获得园林我的养护任务分页
export function myEventList(data) {
  return commonFunc('admin-api/smartCity/greenery-management-emergency-incident/myPage', data, 'get')
}
// 获得园林我的养护任务分页
export function getComTask(data) {
  return commonFunc('admin-api/smartCity/greenery-management-common-task/page', data, 'get')
}
// 获得园林我的养护任务分页
export function createComTask(data) {
  return commonFunc('admin-api/smartCity/greenery-management-common-task/create', data, 'post')
}
// 获得园林我的养护任务分页
export function comTaskDetails(data) {
  return commonFunc('admin-api/smartCity/greenery-management-common-task/get', data, 'get')
}
// 获得园林我的养护任务分页
export function getEmerDetails(data) {
  return commonFunc('admin-api/smartCity/greenery-management-emergency-incident/get', data, 'get')
}
