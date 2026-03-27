import { request } from '@/utils/service'

import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false, type = 1) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  let url
  if (type === 1) {
    url = config.URL + config.gateWay + 'apaas-location-service/' + interfaceName
  } else {
    url = config.URL + config.gateWay + 'apaas-sso/' + interfaceName
  }
  return request<K>({
    url: url,
    method: method,
    ...params
  })
}
// 以下为 巡查管理 相关接口

// 获取我的任务列表
export function taskList(data) {
  return commonFunc('task/v2/list', data, 'post')
}
export function myTaskList(data) {
  return commonFunc('task/v2/mytask_list', data, 'post')
}

// 添加任务
export function addTask(data) {
  return commonFunc('task/v2/add', data, 'post')
}
// 任务详情
export function taskDetails(data) {
  return commonFunc('task/v2/info', data, 'post')
}
// 我的任务详情
export function myTaskDetails(data) {
  return commonFunc('task/v2/mytask_info', data, 'post')
}
// 获取任务打卡详情
export function clockInDetails(data) {
  return commonFunc('task/v2/task_checkin_info', data, 'post')
}
// 获取我的任务打卡详情
export function myClockInDetails(data) {
  return commonFunc('task/v2/mytask_get_checkin', data, 'post')
}
// 获取任务打卡列表
export function clockInList(data) {
  return commonFunc('task/v2/task_checkin_list', data, 'post')
}
// 获取任务打卡轨迹
export function trajectory(data) {
  return commonFunc('task/v2/task_checkin_report', data, 'post')
}
