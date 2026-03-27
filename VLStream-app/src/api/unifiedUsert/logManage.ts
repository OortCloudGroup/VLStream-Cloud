/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:47:24
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-03-22 19:04:59
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method = 'post', isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request < K >({
    url: config.URL + config.gateWay + 'apaas-log-manage' + interfaceName,
    method: method,
    ...params
  })
}

// 日志列表
export function logList(data) {
  return commonFunc('/api/v1/reportLogList', data)
}

// 群聊列表
export function groupList(data) {
  return commonFunc('/api/v1/roomList', data)
}

// 群聊消息列表
export function groupChatList(data) {
  return commonFunc('/api/v1/roomMsgList', data)
}

// 单聊消息列表
export function singleChatList(data) {
  return commonFunc('/api/v1/msgList', data)
}

// 用户与应用列表
export function userAndAppList(data) {
  return commonFunc('/api/v1/appMsgList', data)
}

// 应用使用统计
export function appUseStats(data) {
  return commonFunc('/api/v1/appUseStatistics', data)
}

// 应用使用人数
export function appUserTotal(data) {
  return commonFunc('/api/v1/appUserTotal', data)
}

// 图文消息统计
export function messageStats(data) {
  return commonFunc('/api/v1/appSendStatistics', data)
}

// user
export function myLoginHistroy(data) {
  return commonFunc('/api/v1/myLoginHistory', data)
}

export function myOpinionDel(data) {
  return commonFunc('/api/v1/myOpinionDel', data)
}

export function myOpinionList(data) {
  return commonFunc('/api/v1/myOpinionList', data)
}

export function myOpinionSave(data) {
  return commonFunc('/api/v1/myOpinionSave', data)
}

// 收藏列表
export function myCollectList(data) {
  return commonFunc('/api/v1/myCollectList', data)
}

export function myCollectDel(data) {
  return commonFunc('/api/v1/myCollectDel', data)
}

export function serviceLogList(data) {
  return commonFunc('/log/v1/list', data)
}

export function serviceLogDetail(data) {
  return commonFunc('/log/v1/detail', data)
}

// api调用统计
export function logServiceStatistics(data) {
  return commonFunc('/log/v1/service.statistics', data)
}

// api调用状态统计
export function logServiceStatus(data) {
  return commonFunc('/log/v1/status.statistics', data)
}

// qps统计
export function qpsStatistics(data) {
  return commonFunc('/log/v1/qps.statistics', data)
}

// 灰度发布统计
export function canaryStatistics(data) {
  return commonFunc('/log/v1/canary.statistics', data)
}

// 熔断访问统计
export function breakerStatistics(data) {
  return commonFunc('/log/v1/circuit_breaker.statistics', data)
}

// 调用异常统计
export function serverExceptionStatis(data) {
  return commonFunc('/log/v1/server_exception.statistics', data)
}

// beijing 旧总线的服务日志相关接口
// 日志详情
export function proxyBusLogDetail(data) {
  return commonFunc('/log/v1/proxybus_detail', data)
}

// 日志列表
export function proxyBusLogList(data) {
  return commonFunc('/log/v1/proxybus_list', data)
}

// api调用统计
export function proxyBusLogStatistics(data) {
  return commonFunc('/log/v1/proxybus_statistics', data)
}

export function exportLogTaskStart(data) {
  return commonFunc('/export/v1/task_start', data)
}

export function exportLogTaskStatus(data) {
  return commonFunc('/export/v1/task_status', data)
}

export function reportLogExport(data) {
  return commonFunc('/api/v1/reportLogExport', data)
}

// 告警规则
// 新增告警规则
export function alarmRuleAdd(data) {
  return commonFunc('/alertrule/v1/add', data)
}
// 获取分组名列表
export function alarmRuleInfo(data) {
  return commonFunc('/alertrule/v1/info', data)
}

// 查询告警规则列表
export function alarmRuleList(data) {
  return commonFunc('/alertrule/v1/list', data)
}

// 查询告警日志列表
export function alarmRuleLogList(data) {
  return commonFunc('/alertrule/v1/add', data)
}
