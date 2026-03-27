/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:36
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-04-09 23:43:22
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'
// import config from '@/config'
import { getToken } from '@/utils/cache/cookies'
function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  config.headers.authorization = getToken()
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-workflowforms' + interfaceName,
    // url: '/workflow/' + interfaceName,
    method: method,
    ...params
    // headers: {
    //   'authorization': getToken()
    // }
  })
}

function commonFuncB<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-workflowforms' + interfaceName,
    method: method,
    data: data,
    // responseType: 'blob'
    responseType: 'arraybuffer'
  })
}

function commonFunc2<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-workflowforms' + interfaceName,
    method: method,
    data: data,
    responseType: 'blob'
  })
}

// 新增规则树
export function addTree(data) {
  return commonFunc('/rule/tree', data, 'post')
}

// 导出规则树列表
export function exportTree(data) {
  return commonFunc('/rule/tree/export', data, 'post', true)
}

// 获取规则树列表
export const getTreeList = (data) => {
  return commonFunc('/rule/tree/list', data, 'get')
}

// 删除规则树
export const deleteTree = (data) => {
  return commonFunc('/rule/tree/' + data.id, data, 'delete')
}

// 获取规则树详细信息
export const getTreeInfo = (data) => {
  return commonFunc('/rule/tree/' + data, '', 'get')
}

// 修改规则树
export const updateTree = (data) => {
  return commonFunc('/rule/tree', data, 'put')
}

// 规则列表
export const listRule = (data) => {
  return commonFunc('/rule/list/list', data, 'get')
}

// 新增规则
export const addRule = (data) => {
  return commonFunc('/rule/list/add', data, 'post')
}

// 删除规则
export const deleteRule = (data) => {
  return commonFunc('/rule/list/delete/' + data, '', 'delete')
}

// 修改规则列
export const updateRule = (data) => {
  return commonFunc('/rule/list/edit', data, 'put')
}

// 查询规则列
export const getRule = (data) => {
  return commonFunc('/rule/list/' + data, '', 'get')
}

// 导入规则列表
export const exportImportData = (data) => {
  return commonFunc('/rule/list/importData', data, 'post')
}

// 导出规则-模板
export const exportTemplate = (data) => {
  return commonFunc2('/rule/list/exportTemplate', data, 'post')
}

// 导出规则列表
export const exportRule = (data) => {
  return commonFunc2('/rule/list/export', data, 'post')
}

// 工单管理
/* 工单管理-添加 */
export function addWorkorder(data: any) {
  return commonFunc('/workorder/workorder', data, 'POST')
}

/* 工单管理-编辑 */
export function editWorkorder(data: any) {
  return commonFunc('/workorder/workorder', data, 'PUT')
}

/* 工单管理-删除 */
export function delWorkorder(data: any) {
  return commonFunc('/workorder/workorder/' + data, '', 'DELETE')
}

/* 工单管理-列表 */
export function listWorkorder(data: any) {
  return commonFunc('/workorder/workorder/list', data, 'GET')
}

/* 工单管理-详细 */
export function infoWorkorder(data: any) {
  return commonFunc('/workorder/workorder/' + data, '', 'GET')
}

/* 工单管理-导出 */
export function exportWorkorder(data: any) {
  return commonFunc('/workorder/workorder/export', data, 'POST')
}

/* 打印工单 */
export function workorderExportPdf(data: any) {
  return commonFuncB('/workorder/workorder/exportPdf', data, 'POST')
}

/* 查询即时工单列表 */
export function workorderImmediateList(data: any) {
  return commonFunc('/workorder/workorder/ImmediateList', data, 'GET')
}

/* 查询循环工单列表 */
export function workorderLoopList(data: any) {
  return commonFunc('/workorder/workorder/LoopList', data, 'GET')
}
