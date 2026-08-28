/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { request } from '@/utils/service'

// function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
//   return request<K>({
//     method: method,
//     data: data
//   })
// }
/**
 * current Java after in event interface, apaas-location-service.
 */
function commonFuncB<T, K>(interfaceName: string, data: T, method = 'post') {
  const normalizedPath = String(interfaceName || '').replace(/^\/+/, '')
  return request<K>({
    url: `/${normalizedPath}`,
    method: method,
    data: removeLegacyAccessToken(data)
  })
}

/**
 * taskinterface , can token .
 */
function removeLegacyAccessToken<T>(data: T): T {
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return data
  }
  const { accessToken: _legacyAccessToken, ...payload } = data as Record<string, unknown>
  return payload as T
}

// to event interface
// event
export function addEvent(data) {
  return commonFuncB('task/v1/mytask_updata', data, 'post')
}

// taskevent -event
export function event_item_list(data) {
  return commonFuncB('/task/v1/event_item_list', data, 'post')
}

// taskevent Delete
export function event_item_del(data) {
  return commonFuncB('/task/v1/event_item_del', data, 'post')
}

// taskevent
export function event_item_save(data) {
  return commonFuncB('/task/v1/event_item_save', data, 'post')
}

// taskevent
export function eventList(data) {
  return commonFuncB('task/v1/event_list', data, 'post')
}
// taskevent
export function addFeedback(data) {
  return commonFuncB('task/v1/event_back_add', data, 'post')
}
// taskevent
export function eventDetail(data) {
  return commonFuncB('task/v1/event_info', data, 'post')
}
// taskevent
export function feedbackList(data) {
  return commonFuncB('task/v1/event_back_list', data, 'post')
}
// task
export function allocate(data) {
  return commonFuncB('task/v1/event_add_user', data, 'post')
}
// Delete taskevent
export function delEvent(data) {
  return commonFuncB('task/v1/event_del', data, 'post')
}
// event
export function myEventList(data) {
  return commonFuncB('task/v2/myevent_list', data, 'post')
}
// event
export function event_group_list(data) {
  return commonFuncB('task/v2/event_group_list', data, 'post')
}
// Get V2 group ,
export function event_group_tree(data) {
  return commonFuncB('task/v2/event_group_tree', data, 'post')
}
// V2 、group ( and main full )
export function event_group_save_v2(data) {
  return commonFuncB('task/v2/event_group_save', data, 'post')
}
// Delete V2 、group ( Delete sub node)
export function event_group_delete_v2(data) {
  return commonFuncB('task/v2/event_group_delete', data, 'post')
}
// eventgroupconfiguration
export function event_group_save(data) {
  return commonFuncB('task/v1/event_group_save', data, 'post')
}
// Delete eventgroupconfiguration
export function event_group_del(data) {
  return commonFuncB('/task/v1/event_group_del', data, 'post')
}
// taskevent Set
export function event_item_setting_save(data) {
  return commonFuncB('/task/v1/event_item_setting_save', data, 'post')
}
// Set event
export function event_item_status(data) {
  return commonFuncB('/task/v1/event_item_status', data, 'post')
}
// Get eventgroup
export function event_group_info(data) {
  return commonFuncB('/task/v2/event_group_info', data, 'post')
}
// Get /group work orderconfiguration
export function workflowConfigGet(data) {
  return commonFuncB('task/v1/workflowConfigGet', data, 'post')
}
// /group work orderconfiguration
export function workflowConfigSet(data) {
  return commonFuncB('task/v1/workflowConfigSet', data, 'post')
}
// departmentuser work orderconfiguration
export function eventGroupDeptuserSave(data) {
  return commonFuncB('task/v1/event_group_deptuser_save', data, 'post')
}
// Get departmentuserSet work orderconfiguration
export function eventFroupDeptuserList(data) {
  return commonFuncB('task/v1/event_group_deptuser_list', data, 'post')
}
// Set departmentuser work orderconfiguration
export function eventGroupDeptuserStatus(data) {
  return commonFuncB('task/v1/event_group_deptuser_status', data, 'post')
}
// new eventgroupSet
export function event_group_setting_save(data) {
  return commonFuncB('task/v2/event_group_setting_save', data, 'post')
}
// new eventgroup work order
export function event_group_status(data) {
  return commonFuncB('task/v2/event_group_status', data, 'post')
}
// event
export function eventStatistics(data) {
  return commonFuncB('task/v1/event_statistics', data, 'post')
}
