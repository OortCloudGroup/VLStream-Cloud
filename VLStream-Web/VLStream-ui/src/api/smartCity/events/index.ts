import { request } from '@/utils/service'
import { apaasServiceUrl } from '@/utils/apaasApiBase'

// function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
//   return request<K>({
//     url: config.URL + config.gateWay + 'apaas-sso/' + interfaceName,
//     method: method,
//     data: data
//   })
// }
function commonFuncB<T, K>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: apaasServiceUrl('apaas-location-service', interfaceName),
    method: method,
    data: data
  })
}

// 以下为 事件管理 接口
// 添加事件
export function addEvent(data) {
  return commonFuncB('task/v1/mytask_updata', data, 'post')
}

// 任务事件列表-事件类型过滤
export function event_item_list(data) {
  return commonFuncB('/task/v1/event_item_list', data, 'post')
}

// 任务事件类型删除
export function event_item_del(data) {
  return commonFuncB('/task/v1/event_item_del', data, 'post')
}

// 任务事件类型保存
export function event_item_save(data) {
  return commonFuncB('/task/v1/event_item_save', data, 'post')
}

// 任务事件列表
export function eventList(data) {
  return commonFuncB('task/v1/event_list', data, 'post')
}
// 添加任务事件反馈
export function addFeedback(data) {
  return commonFuncB('task/v1/event_back_add', data, 'post')
}
// 任务事件详情
export function eventDetail(data) {
  return commonFuncB('task/v1/event_info', data, 'post')
}
// 任务事件反馈列表
export function feedbackList(data) {
  return commonFuncB('task/v1/event_back_list', data, 'post')
}
// 任务分配
export function allocate(data) {
  return commonFuncB('task/v1/event_add_user', data, 'post')
}
// 删除任务事件
export function delEvent(data) {
  return commonFuncB('task/v1/event_del', data, 'post')
}
// 我的事件列表
export function myEventList(data) {
  return commonFuncB('task/v2/myevent_list', data, 'post')
}
// 我的事件列表
export function event_group_list(data) {
  return commonFuncB('task/v2/event_group_list', data, 'post')
}
// 保存事件分组配置
export function event_group_save(data) {
  return commonFuncB('task/v1/event_group_save', data, 'post')
}
// 删除事件分组配置
export function event_group_del(data) {
  return commonFuncB('/task/v1/event_group_del', data, 'post')
}
// 任务事件类型设置保存
export function event_item_setting_save(data) {
  return commonFuncB('/task/v1/event_item_setting_save', data, 'post')
}
// 设置事件类型状态
export function event_item_status(data) {
  return commonFuncB('/task/v1/event_item_status', data, 'post')
}
// 获取事件分组详情
export function event_group_info(data) {
  return commonFuncB('/task/v2/event_group_info', data, 'post')
}
// 获取区域/分组自动转工单配置
export function workflowConfigGet(data) {
  return commonFuncB('task/v1/workflowConfigGet', data, 'post')
}
// 保存区域/分组自动转工单配置
export function workflowConfigSet(data) {
  return commonFuncB('task/v1/workflowConfigSet', data, 'post')
}
// 保存部门用户转工单配置
export function eventGroupDeptuserSave(data) {
  return commonFuncB('task/v1/event_group_deptuser_save', data, 'post')
}
// 获取部门用户设置的转工单配置
export function eventFroupDeptuserList(data) {
  return commonFuncB('task/v1/event_group_deptuser_list', data, 'post')
}
// 设置部门用户转工单配置状态
export function eventGroupDeptuserStatus(data) {
  return commonFuncB('task/v1/event_group_deptuser_status', data, 'post')
}
// 更新事件分组设置
export function event_group_setting_save(data) {
  return commonFuncB('task/v2/event_group_setting_save', data, 'post')
}
// 更新事件分组自动转工单状态
export function event_group_status(data) {
  return commonFuncB('task/v2/event_group_status', data, 'post')
}
// 事件统计概览
export function eventStatistics(data) {
  return commonFuncB('task/v1/event_statistics', data, 'post')
}
