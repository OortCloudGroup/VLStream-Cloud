/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:51
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-11-20 16:03:33
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

function commonFuncB<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-report' + interfaceName,
    method: method,
    data: data,
    responseType: 'blob'
  })
}

/* report_template-添加 */
export function reportTemplateAdd(data: any) {
  return commonFunc('/manage/reportTemplate/add', data, 'POST')
}

/* 报表模板-发布 */
export function reportTemplatePublish(data: any) {
  return commonFunc('/manage/reportTemplate/publish', data, 'POST')
}

/* report_template-编辑 */
export function reportTemplateEdit(data: any) {
  return commonFunc('/manage/reportTemplate/edit', data, 'POST')
}

/* report_template-删除 */
export function reportTemplateDelete(data: any) {
  return commonFunc('/manage/reportTemplate/delete', data, 'DELETE')
}

/* report_template-批量删除 */
export function reportTemplateDeleteBatch(data: any) {
  return commonFunc('/manage/reportTemplate/deleteBatch', data, 'DELETE')
}

/* report_template-列表 */
export function reportTemplateList(data: any) {
  return commonFunc('/manage/reportTemplate/list', data, 'POST')
}

// 报表组
/* 报表组-添加 */
export function reportGroupAdd(data: any) {
  return commonFunc('/manage/reportGroup/add', data, 'POST')
}

// /* 报表组-编辑 */
export function reportGroupEdit(data: any) {
  return commonFunc('/manage/reportGroup/edit', data, 'POST')
}

// /* 报表组-删除 */
export function reportGroupDel(data: any) {
  return commonFunc('/manage/reportGroup/delete', data, 'DELETE')
}

// /* 报表组-删除 */
export function reportGroupDels(data: any) {
  return commonFunc('/manage/reportGroup/deleteBatch', data, 'DELETE')
}

/* 报表组-列表 */
export function reportGroupList(data: any) {
  return commonFunc('/manage/reportGroup/list', data, 'GET')
}

// 报表组详情
export function reportGroupDetail(data: any) {
  return commonFunc('/manage/reportGroup/queryById', data, 'GET')
}

// 获取报表组下面的分类（报表）
export function queryReportGroupTypeByMainId(data: any) {
  return commonFunc('/manage/reportGroup/queryReportGroupTypeByMainId', data, 'GET')
}

// 通过报表组获取url
export function queryReportTypeTemplate(data: any) {
  return commonFunc('/manage/reportTypeTemplate/queryReportTypeTemplate', data, 'POST')
}

// report-group-type-controller-删除报表组中的报表类型
export function reportGroupTypeDeleteType(data: any) {
  return commonFunc('/manage/reportGroupType/deleteType', data, 'DELETE')
}

// report-group-type-controller-添加报表组中的报表类型
export function reportGroupTypeAddType(data: any) {
  return commonFunc('/manage/reportGroupType/addType', data, 'POST')
}

// 报表组与报表类型关联表
/* 报表组与报表类型关联表-添加 */
export function typeTemplateAdd(data: any) {
  return commonFunc('/manage/reportTypeTemplate/add', data, 'POST')
}

// /* 报表组与报表类型关联表-编辑 */
export function typeTemplateEdit(data: any) {
  return commonFunc('/manage/reportTypeTemplate/edit', data, 'POST')
}

// /* 报表组与报表类型关联表-删除 */
export function typeTemplateDel(data: any) {
  return commonFunc('/manage/reportTypeTemplate/delete', data, 'DELETE')
}

// /* 报表组与报表类型关联表-删除 */
export function typeTemplateDels(data: any) {
  return commonFunc('/manage/reportTypeTemplate/deleteBatch', data, 'DELETE')
}

/* 报表组与报表类型关联表-列表 */
export function typeTemplateList(data: any) {
  return commonFunc('/manage/reportTypeTemplate/list', data, 'GET')
}

/* 查询报表组，报表类型下的，多个报表 */
export function repTypeReportTypeTemplate(data: any) {
  return commonFunc('/manage/reportTypeTemplate/queryReportTypeTemplate', data, 'POST')
}

/* 11报表模版-导出路径 */
export function TempExpUrl(data: any) {
  return commonFunc('/manage/reportTemplate/expUrl/' + data, '', 'GET')
}

/* 导出excel */
export function manageExportXls(data: any) {
  return commonFuncB('/manage/reportTemplate/exportXls?' + data, '', 'GET')
}

/* 导入excel */
export function manageImportExcel(data: any) {
  return commonFunc('/manage/reportTemplate/importExcel', data, 'POST')
}

/* 设计跳转链接-获取token */
export function manageGetToken(data: any) {
  return commonFunc('/manage/reportTemplate/getToken', data, 'GET')
}

/* 报表统计-报表使用记录 */
export function sysQueryLogList(data: any) {
  return commonFunc('/sys/log/queryLogList', data, 'POST')
}

/* 报表统计-登录记录 */
export function statsLogin(data: any) {
  return commonFunc('/stats/login', data, 'GET')
}

/* 报表统计-报表使用统计 */
export function sysQueryUsageStats(data: any) {
  return commonFunc('/sys/log/queryUsageStats', data, 'POST')
}
