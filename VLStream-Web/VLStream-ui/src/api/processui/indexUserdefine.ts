/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { request } from '@/utils/service'
import { apaasServiceUrl } from '@/utils/apaasApiBase'
function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: apaasServiceUrl('apaas-workflowforms', interfaceName),
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
    url: apaasServiceUrl('apaas-workflowforms', interfaceName),
    method: method,
    data: data,
    // responseType: 'blob'
    responseType: 'arraybuffer'
  })
}

function commonFunc2<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: apaasServiceUrl('apaas-workflowforms', interfaceName),
    method: method,
    data: data,
    responseType: 'blob'
  })
}

// Add
export function addTree(data) {
  return commonFunc('/rule/tree', data, 'post')
}

// Export
export function exportTree(data) {
  return commonFunc('/rule/tree/export', data, 'post', true)
}

// Get
export const getTreeList = (data) => {
  return commonFunc('/rule/tree/list', data, 'get')
}

// Delete
export const deleteTree = (data) => {
  return commonFunc('/rule/tree/' + data.id, data, 'delete')
}

// Get info
export const getTreeInfo = (data) => {
  return commonFunc('/rule/tree/' + data, '', 'get')
}

// Update
export const updateTree = (data) => {
  return commonFunc('/rule/tree', data, 'put')
}

//
export const listRule = (data) => {
  return commonFunc('/rule/list/list', data, 'get')
}

// Add
export const addRule = (data) => {
  return commonFunc('/rule/list/add', data, 'post')
}

// Delete
export const deleteRule = (data) => {
  return commonFunc('/rule/list/delete/' + data, '', 'delete')
}

// Update
export const updateRule = (data) => {
  return commonFunc('/rule/list/edit', data, 'put')
}

// Query
export const getRule = (data) => {
  return commonFunc('/rule/list/' + data, '', 'get')
}

// Import
export const exportImportData = (data) => {
  return commonFunc('/rule/list/importData', data, 'post')
}

// Export -
export const exportTemplate = (data) => {
  return commonFunc2('/rule/list/exportTemplate', data, 'post')
}

// Export
export const exportRule = (data) => {
  return commonFunc2('/rule/list/export', data, 'post')
}

// work order
/* work order - */
export function addWorkorder(data: any) {
  return commonFunc('/workorder/workorder', data, 'POST')
}

/* work order - */
export function editWorkorder(data: any) {
  return commonFunc('/workorder/workorder', data, 'PUT')
}

/* work order -Delete */
export function delWorkorder(data: any) {
  return commonFunc('/workorder/workorder/' + data, '', 'DELETE')
}

/* work order - */
export function listWorkorder(data: any) {
  return commonFunc('/workorder/workorder/list', data, 'GET')
}

/* work order - */
export function infoWorkorder(data: any) {
  return commonFunc('/workorder/workorder/' + data, '', 'GET')
}

/* work order -Export */
export function exportWorkorder(data: any) {
  return commonFunc('/workorder/workorder/export', data, 'POST')
}

/* work order */
export function workorderExportPdf(data: any) {
  return commonFuncB('/workorder/workorder/exportPdf', data, 'POST')
}

/* Query work order list */
export function workorderImmediateList(data: any) {
  return commonFunc('/workorder/workorder/ImmediateList', data, 'GET')
}

/* Query loopwork order list */
export function workorderLoopList(data: any) {
  return commonFunc('/workorder/workorder/LoopList', data, 'GET')
}
/* approval */
export function workflowProcessList(data: any) {
  return commonFunc('/workflow/process/list', data, 'GET')
}
/* fieldconfiguration */
export function workflowFieldFieldCodes(data: any) {
  return commonFunc('/workflow/field/field-codes', data, 'GET')
}
/* Update fieldconfiguration */
export function workflowFieldFieldCodesPost(data: any) {
  return commonFunc('/workflow/field/field-codes', data, 'POST')
}
