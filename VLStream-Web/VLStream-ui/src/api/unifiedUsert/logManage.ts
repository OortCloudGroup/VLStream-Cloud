/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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

function commonFunc2<T, K>(interfaceName: string, data: T, method = 'post', isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request < K >({
    url: config.URL + config.gateWay + 'zc-smartcity-server/app-api' + interfaceName,
    method: method,
    ...params
  })
}

function commonFunc3<T, K>(interfaceName: string, data: T, method = 'post', isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request < K >({
    url: config.URL + config.gateWay + 'zc-smartcity-server/admin-api' + interfaceName,
    method: method,
    ...params
  })
}

// log
export function logList(data) {
  return commonFunc('/api/v1/reportLogList', data)
}

//
export function groupList(data) {
  return commonFunc('/api/v1/roomList', data)
}

//
export function groupChatList(data) {
  return commonFunc('/api/v1/roomMsgList', data)
}

//
export function singleChatList(data) {
  return commonFunc('/api/v1/msgList', data)
}

// user and
export function userAndAppList(data) {
  return commonFunc('/api/v1/appMsgList', data)
}

//
export function appUseStats(data) {
  return commonFunc('/api/v1/appUseStatistics', data)
}

//
export function appUserTotal(data) {
  return commonFunc('/api/v1/appUserTotal', data)
}

//
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

//
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

// api
export function logServiceStatistics(data) {
  return commonFunc('/log/v1/service.statistics', data)
}

// api
export function logServiceStatus(data) {
  return commonFunc('/log/v1/status.statistics', data)
}

// qps
export function qpsStatistics(data) {
  return commonFunc('/log/v1/qps.statistics', data)
}

//
export function canaryStatistics(data) {
  return commonFunc('/log/v1/canary.statistics', data)
}

//
export function breakerStatistics(data) {
  return commonFunc('/log/v1/circuit_breaker.statistics', data)
}

//
export function serverExceptionStatis(data) {
  return commonFunc('/log/v1/server_exception.statistics', data)
}

// beijing old servicelogrelatedinterface
// log
export function proxyBusLogDetail(data) {
  return commonFunc('/log/v1/proxybus_detail', data)
}

// log
export function proxyBusLogList(data) {
  return commonFunc('/log/v1/proxybus_list', data)
}

// api
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

// alarm / alert
// Add alarm / alert
export function alarmRuleAdd(data) {
  return commonFunc('/alertrule/v1/add', data)
}
// Get group
export function alarmRuleInfo(data) {
  return commonFunc('/alertrule/v1/info', data)
}

// Query alarm / alert list
export function alarmRuleList(data) {
  return commonFunc('/alertrule/v1/list', data)
}

// Query alarm / alertlog list
export function alarmRuleLogList(data) {
  return commonFunc('/alertrule/v1/add', data)
}

// info
export function createProblemCorrectionProgress(data) {
  return commonFunc2('/smartCity/problem-correction-progress/create', data, 'post')
}

// task
export function getGardenInspectionTask(data) {
  return commonFunc3('/smartCity/garden-inspection-task/get', data, 'get')
}

// eventProcess
export function getGreeneryManagementEmergencyIncident(data) {
  return commonFunc3('/smartCity/greenery-management-emergency-incident/get', data, 'get')
}

// task
export function getGreeneryManagementTask(data) {
  return commonFunc3('/smartCity/greenery-management-task/get', data, 'get')
}

//
export function getProblemCorrectionProgress(data) {
  return commonFunc3('/smartCity/problem-correction-progress/get', data, 'get')
}

//
export function pageProblemCorrectionProgress(data) {
  return commonFunc3('/smartCity/problem-correction-progress/page', data, 'get')
}
