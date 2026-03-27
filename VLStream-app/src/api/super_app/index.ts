/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:51
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-09-23 10:11:11
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'

import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: config.URL + config.gateWay + 'zc-smartcity-server/' + interfaceName,
    method: method,
    ...params
  })
}
/* 首页 */
export function systemNoticeGet(data: any) {
  return commonFunc('/app-api/system/notice/page', data, 'get')
}
/* 获得园林巡查巡查计划分页 */
export function gardenInspectionInspectionPlan(data: any) {
  return commonFunc('/app-api/smartCity/garden-inspection-inspection-plan/page', data, 'get')
}

// 获得园林养护记录分页
export function greeneryManagementRosteringScheduling(data) {
  return commonFunc('/app-api/smartCity/greenery-management-rostering-scheduling/page', data, 'get')
}
// 获得园林养护统计列表
export function greeneryManagementRosteringSchedulingOverViewList(data) {
  return commonFunc('/app-api/smartCity/greenery-management-rostering-scheduling/getOverViewList', data, 'get')
}
// 获得园林养护指令下发列表
export function greeneryManagementCommandList(data) {
  return commonFunc('app-api/smartCity/greenery-management-command/list', data, 'get')
}
// 获取养护人员
export function userSimpleList() {
  return commonFunc('app-api/system/user/simple-list', {}, 'get')
}
// 创建园林养护指令下发
export function greeneryManagementCommand(data) {
  return commonFunc('app-api/smartCity/greenery-management-command/create', data, 'post')
}
// 获得考核记录分页
export function smartCityAssessmentRecords(data) {
  return commonFunc('app-api/smartCity/assessment-records/page', data, 'get')
}
// 创建园林养护指令下发
export function greeneryManagementRecordCreate(data) {
  return commonFunc('/admin-api/smartCity/greenery-management-record/create', data, 'post')
}
// 创建问题整改信息
export function problemCorrectionProgressCreate(data) {
  return commonFunc('/app-api/smartCity/problem-correction-progress/create', data, 'post')
}
// 创建园林养护指令下发
export function gardenInspectionInspectionRecordCreate(data) {
  return commonFunc('/app-api/smartCity/garden-inspection-inspection-record/create', data, 'post')
}

// 创建事件处理记录
export function greeneryEmergencyRecordCreate(data) {
  return commonFunc('/admin-api/smartCity/greenery-management-emergency-record/create', data, 'post')
}

// 获取事件处理分页
export function greeneryEmergencyRecordPage(data) {
  return commonFunc('/admin-api/smartCity/greenery-management-emergency-record/page', data, 'get')
}

// 获取事件处理详情
export function greeneryEmergencyRecordDetails(data) {
  return commonFunc('/admin-api/smartCity/greenery-management-emergency-record/get', data, 'get')
}
