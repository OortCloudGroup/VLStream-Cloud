import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-location-service' + interfaceName,
    method: method,
    data: data
  })
}

// 任务事件-add
export function event_add(data) {
  return commonFunc('/task/v1/event_add', data, 'post')
}

// 任务事件-del
export function event_del(data) {
  return commonFunc('/task/v1/event_del', data, 'post')
}

// 任务事件-info
export function event_info(data) {
  return commonFunc('/task/v1/event_info', data, 'post')
}

// 任务事件-list
export function event_list(data) {
  return commonFunc('/task/v1/event_list', data, 'post')
}

// 任务事件人员-add
export function event_add_user(data) {
  return commonFunc('/task/v1/event_add_user', data, 'post')
}

// 任务事件人员-del
export function event_del_user(data) {
  return commonFunc('/task/v1/event_del_user', data, 'post')
}

// 任务事件反馈-add
export function event_back_add(data) {
  return commonFunc('/task/v1/event_back_add', data, 'post')
}

// 任务事件反馈-list
export function event_back_list(data) {
  return commonFunc('/task/v1/event_back_list', data, 'post')
}

// 我的事件-list
export function myevent_list(data) {
  return commonFunc('/task/v2/myevent_list', data, 'post')
}

// 我的事件-反馈-list
export function myevent_back_list(data) {
  return commonFunc('/task/v2/myevent_back_list', data, 'post')
}

