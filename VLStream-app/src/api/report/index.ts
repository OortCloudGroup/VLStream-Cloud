/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:51
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-11-21 14:36:48
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
    url: config.URL + config.gateWay + 'apaas-report' + interfaceName,
    method: method,
    ...params
  })
}

/* report_data_source-添加 */
export function reportDataSourceAdd(data: any) {
  return commonFunc('/manage/reportDataSource/add', data, 'POST')
}

/* report_data_source-编辑 */
export function reportDataSourceEdit(data: any) {
  return commonFunc('/manage/reportDataSource/edit', data, 'POST')
}

/* report_data_source-删除 */
export function reportDataSourceDelete(data: any) {
  return commonFunc('/manage/reportDataSource/delete', data, 'DELETE')
}

/* report_data_source-批量删除 */
export function reportDataSourceDeleteBatch(data: any) {
  return commonFunc('/manage/reportDataSource/deleteBatch', data, 'DELETE')
}

/* report_data_source-列表 */
export function reportDataSourceList(data: any) {
  return commonFunc('/manage/reportDataSource/list', data, 'GET')
}

// 综合报表
/* 综合报表-添加 */
export function reportStrideAppAdd(data: any) {
  return commonFunc('/manage/reportStrideApp/add', data, 'POST')
}

/* 综合报表-编辑 */
export function reportStrideAppEdit(data: any) {
  return commonFunc('/manage/reportStrideApp/edit', data, 'POST')
}

/* 综合报表-删除 */
export function reportStrideAppDelete(data: any) {
  return commonFunc('/manage/reportStrideApp/delete', data, 'DELETE')
}

/* 综合报表-批量删除 */
export function reportStrideAppDeleteBatch(data: any) {
  return commonFunc('/manage/reportStrideApp/deleteBatch', data, 'DELETE')
}

/* 综合报表-列表 */
export function reportStrideAppList(data: any) {
  return commonFunc('/manage/reportStrideApp/rootList', data, 'GET')
}

// 应用报表
/* 应用报表-添加 */
export function reportAppAdd(data: any) {
  return commonFunc('/manage/reportApp/add', data, 'POST')
}

/* 应用报表-编辑 */
export function reportAppEdit(data: any) {
  return commonFunc('/manage/reportApp/edit', data, 'POST')
}

/* 应用报表-删除 */
export function reportAppDelete(data: any) {
  return commonFunc('/manage/reportApp/delete', data, 'DELETE')
}

/* 应用报表-列表 */
export function reportAppList(data: any) {
  return commonFunc('/manage/reportApp/list', data, 'GET')
}
/* 应用报表-通过id查询 */
export function reportAppQuery(data: any) {
  return commonFunc('/manage/reportApp/queryById', data, 'GET')
}

// 统计- 报表总数
export function fixeNumberCount(data: any) {
  return commonFunc('/manage/reportTemplate/fixeNumberCount', data, 'GET')
}

// 统计- 报表新增数量
export function NoFixeNumberCount(data: any) {
  return commonFunc('/manage/reportTemplate/NoFixeNumberCount', data, 'GET')
}

// 统计图表- 用户报表使用情况
export function sysUsage(data: any) {
  return commonFunc('/sys/log/usage', data, 'GET')
}

// 统计图表- 用户报表使用人数
export function sysUsageC(data: any) {
  return commonFunc('/sys/log/usage/count', data, 'GET')
}

// 统计图表- 综合报表使用情况
export function sysStride(data: any) {
  return commonFunc('/sys/log/stride', data, 'GET')
}// 统计图表- 应用报表使用情况
export function sysApp(data: any) {
  return commonFunc('/sys/log/app', data, 'GET')
}

// 统计图表- 角色使用差异
export function sysDifference(data: any) {
  return commonFunc('/sys/log/role/difference', data, 'GET')
}

// 统计图表- 部门使用情况
export function sysDept(data: any) {
  return commonFunc('/sys/log/dept/usage', data, 'GET')
}
