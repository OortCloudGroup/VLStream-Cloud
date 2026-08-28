/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { request } from '@/utils/service'
import { apaasServiceUrl } from '@/utils/apaasApiBase'

function commonFunc<T, K>(interfaceName: string, data: T, method: string, isParams = false, timeout = NaN) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request<K>({
    url: apaasServiceUrl('apaas-workflowforms', interfaceName),
    // url: '/workflow/' + interfaceName,
    method: method,
    ...params,
    timeout: timeout
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
    responseType: 'blob'
  })
}

// Query workflow list
export function listCategory(query) {
  return commonFunc('/workflow/category/list', query, 'get')
}

// Query workflow full list
export function listAllCategory(query) {
  return commonFunc('/workflow/category/listAll', query, 'get')
}

// Add workflow
export function addCategory(data) {
  return commonFunc('/workflow/category', data, 'post')
}

// Update workflow
export function updateCategory(data) {
  return commonFunc('/workflow/category', data, 'put')
}

// Delete workflow
export function deleteCategory(data) {
  return commonFunc('/workflow/category/' + data.categoryId, {}, 'delete')
}

// Add form
export function addForm(data) {
  return commonFunc('/workflow/form', data, 'post')
}

// Export -form
export function exportWorkflow(data) {
  return commonFuncB('/workflow/form/export', data, 'post')
}

// Update form
export function updateForm(data) {
  return commonFunc('/workflow/form', data, 'put')
}

// Delete form
export function deleteForm(data) {
  return commonFunc('/workflow/form/' + data.formId, {}, 'delete')
}

// form
export function getForm(data) {
  return commonFunc('/workflow/form/' + data.formId, {}, 'get')
}

// form
export function listForm(query) {
  return commonFunc('/workflow/form/list', query, 'get')
}

// model
export function listModel(query) {
  return commonFunc('/workflow/model/list', query, 'get')
}

// Export model
export function exportModel(data) {
  return commonFuncB('/workflow/model/export', data, 'post')
}

// Add model
export function addModel(data) {
  return commonFunc('/workflow/model', data, 'post')
}

// Get model
export function getModel(data) {
  return commonFunc('/workflow/model/' + data.modelId, {}, 'get')
}

// Update model
export function updateModel(data) {
  return commonFunc('/workflow/model', data, 'put')
}

// model
export function saveModel_a(data) {
  return commonFunc('/workflow/model/save', data, 'post')
}

// model
export function copyModel(data) {
  return commonFunc('/workflow/model/copyModel', data, 'post')
}

// to new model
export function latestModel(data) {
  return commonFunc('/workflow/model/latest', data, 'post', true)
}

// Delete model
export function deleteModel(data) {
  return commonFunc('/workflow/model', data, 'delete')
}

// model ID Delete model、workflow info、all model 、 instance and history data
export function deleteModelCascade(data) {
  return commonFunc('/workflow/model/deleteModelCascade', data, 'delete')
}

// model
export function deployModel(data) {
  return commonFunc('/workflow/model/deploy', data, 'post', true)
}

// Get workflowmodel
export function getBpmnXml(data) {
  return commonFunc('/workflow/model/bpmnXml/' + data.modelId, {}, 'get')
}

// Get workflowmodelhistory
export function historyModel(data) {
  return commonFunc('/workflow/model/historyList', data, 'get')
}

//
export function listDeploy(query) {
  return commonFunc('/workflow/deploy/list', query, 'get')
}

// history
export function listHistoryDeploy(query) {
  return commonFunc('/workflow/deploy/publishList', query, 'get')
}

// Delete workflow
export function deleteDeploy(data) {
  return commonFunc('/workflow/deploy/' + data.deployId, {}, 'delete')
}

// Query workflow forminfo
export function getFormByDeployId(data) {
  return commonFunc('/workflow/deploy/form/' + data.deployId, {}, 'get')
}

// Update
export function changeState(data) {
  return commonFunc('/workflow/deploy/changeState', data, 'put', true)
}

// Get workflow
export function getDeployBpmnXml(data) {
  return commonFunc('/workflow/deploy/bpmnXml/' + data.definitionId, {}, 'get')
}

//
// workflow
export function listProcess(data) {
  return commonFunc('/workflow/process/list', data, 'get')
}

// Query workflowform
export function getProcessForm(data) {
  return commonFunc('/workflow/process/getProcessForm', data, 'get')
}

// workflow ( workflow instance)
export function startProcess(data) {
  return commonFunc('/workflow/process/start', data, 'post')
}

// Delete workflow instance
export function deleteProcess(data) {
  return commonFunc('/workflow/process/instance/' + data.procInsId, {}, 'delete')
}

// Get workflow
export function getProcessBpmnXml(data) {
  return commonFunc('/workflow/process/bpmnXml/' + data.processDefId, {}, 'get')
}

// Get workflowmodelJSON
export function getBpmnJSON(data) {
  return commonFunc('/workflow/process/bpmnJson/' + data.processDefId, {}, 'get')
}

// workflow instance
export function detailProcess(data) {
  return commonFunc('/workflow/process/detail', data, 'get')
}

// workflow
export function listOwnProcess(data) {
  return commonFunc('/workflow/process/ownList', data, 'get')
}

// work order
export function ownWorkOrderList(data) {
  return commonFunc('/workorder/workorder/ownWorkOrderList', data, 'get')
}

// work order
export function todoWorkOrderList(data) {
  return commonFunc('/workorder/workorder/todoWorkOrderList', data, 'get')
}

// work order
export function claimWorkOrderList(data) {
  return commonFunc('/workorder/workorder/claimWorkOrderList', data, 'get')
}

// already work order
export function finishedWorkOrderList(data) {
  return commonFunc('/workorder/workorder/finishedWorkOrderList', data, 'get')
}

//
export function listTodoProcess(data) {
  return commonFunc('/workflow/process/todoList', data, 'get')
}

//
export function listClaimProcess(data) {
  return commonFunc('/workflow/process/claimList', data, 'get')
}

// already
export function listFinishedProcess(data) {
  return commonFunc('/workflow/process/finishedList', data, 'get')
}

//
export function listCopyProcess(data) {
  return commonFunc('/workflow/process/copyList', data, 'get')
}

//
export function stopProcess(data) {
  return commonFunc('/workflow/task/stopProcess', data, 'post')
}

// task
export function revokeProcess(data) {
  return commonFunc('/workflow/task/revokeProcess', data, 'post')
}

// role
export function listRole(data) {
  return commonFunc('/system/role/list', data, 'get')
}

// task
export function claimTask(data) {
  return commonFunc('/workflow/task/claim', data, 'post')
}

// / task
export function unClaimTask(data) {
  return commonFunc('/workflow/task/unClaim', data, 'post')
}

// json xml
export function jsonToXml(data) {
  return commonFunc('/model/download', data, 'post')
}

// workflow json
export function getJSONModel(data) {
  return commonFunc('/work/modeJson/' + data.modelId, '', 'get')
}

// task
export function completeTask(data) {
  return commonFunc('/workflow/task/complete', data, 'post')
}

// task
export function delegateTask(data) {
  return commonFunc('/workflow/task/delegate', data, 'post')
}

// task
export function transferTask(data) {
  return commonFunc('/workflow/task/transfer', data, 'post')
}

// task
export function returnTask(data) {
  return commonFunc('/workflow/task/return', data, 'post')
}

// task
export function returnList(data) {
  return commonFunc('/workflow/task/returnList', data, 'post')
}

// task
export function rejectTask(data) {
  return commonFunc('/workflow/task/reject', data, 'post')
}

// Get approvalnodeoperation
export function getApprovalButton(data) {
  return commonFunc('/system/dict/data/type/wf_approval_button', data, 'get')
}

// form
/* form - */ //
export function workflowAdd(data: any) {
  return commonFunc('/workflow/formSynthesis', data, 'POST')
}

/* form -Export */
export function workflowExport(data: any) {
  return commonFuncB('/workflow/formCategory/export', data, 'POST')
}

/* form - */ //
export function workflowEdit(data: any) {
  return commonFunc('/workflow/formSynthesis', data, 'PUT')
}

/* form -Delete */
export function workflowDelete(data: any) {
  return commonFunc('/workflow/formCategory/' + data, '', 'DELETE')
}

// /* form -Batch delete */
// export function workflowDeleteBatch(data: any) {
//   return commonFunc('/manage/workflow/deleteBatch', data, 'DELETE')
// }

/* form - */
export function workflowList(data: any) {
  return commonFunc('/workflow/formCategory/list', data, 'GET')
}

/* form - */
export function formSynthesisList(data: any) {
  return commonFunc('/workflow/formSynthesis/list', data, 'GET')
}

/* form - */
export function formAppList(data: any) {
  return commonFunc('/system/formApp/list', data, 'GET')
}

/* form - -add */
export function formAppAdd(data: any) {
  return commonFunc('/system/formApp', data, 'POST')
}
/* form - -edit */
export function formAppEdit(data: any) {
  return commonFunc('/system/formApp', data, 'PUT')
}

/* form - -del */
export function formAppDel(data: any) {
  return commonFunc('/system/formApp/' + data, '', 'DELETE')
}

// work order
/* work order - */
export function addWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app', data, 'POST')
}

/* work order -Export */
export function exportWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app/export', data, 'POST')
}

/* work order - */
export function editWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app', data, 'PUT')
}

/* work order -Delete */
export function deleteWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app/' + data, '', 'DELETE')
}

/* work order - */
export function listWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app/list', data, 'GET')
}

//
/* - */
export function addwfApp(data: any) {
  return commonFunc('/wf/app', data, 'POST')
}

/* - */
export function editwfApp(data: any) {
  return commonFunc('/wf/app', data, 'PUT')
}

/* -Delete */
export function deletewfApp(data: any) {
  return commonFunc('/wf/app/' + data, '', 'DELETE')
}

/* - workflow */
export function listwfApp(data: any) {
  return commonFunc('/wf/app/list', data, 'GET')
}

// work orderworkflow
/* work orderworkflow- */
export function addSynthesis(data: any) {
  return commonFunc('/workorder/synthesis', data, 'POST')
}

/* work orderworkflow-Export */
export function exportSynthesis(data: any) {
  return commonFunc('/workorder/synthesis/export', data, 'POST')
}

/* work orderworkflow- */
export function editSynthesis(data: any) {
  return commonFunc('/workorder/synthesis', data, 'PUT')
}

/* work orderworkflow-Delete */
export function deleteSynthesis(data: any) {
  return commonFunc('/workorder/synthesis/' + data, '', 'DELETE')
}

/* work orderworkflow- */
export function listSynthesis(data: any) {
  return commonFunc('/workorder/synthesis/list', data, 'GET')
}

/* work orderworkflow- */
export function listSynthesisAll(data: any) {
  return commonFunc('/workorder/synthesis/listAll', data, 'GET')
}
// workflow
/* workflow- */
export function addwf(data: any) {
  return commonFunc('/wf/synthesis', data, 'POST')
}

/* workflow- */
export function editwf(data: any) {
  return commonFunc('/wf/synthesis', data, 'PUT')
}

/* workflow-Delete */
export function deletewf(data: any) {
  return commonFunc('/wf/synthesis/' + data, '', 'DELETE')
}

/* workflow- */
export function listwf(data: any) {
  return commonFunc('/wf/synthesis/list', data, 'GET')
}

/* workflow- */
export function listwfAll(data: any) {
  return commonFunc('/wf/synthesis/listAll', data, 'GET')
}

/* approvalworkflow */
export function wfApp(data: any) {
  return commonFunc('/wf/app/list', data, 'GET')
}
/* approvalworkflow */
export function todoList(data: any) {
  return commonFunc('/workflow/process/todoList', data, 'GET')
}
/* approvalworkflow already */
export function finishedList(data: any) {
  return commonFunc('/workflow/process/finishedList', data, 'GET')
}
/* approvalworkflow */
export function copyList(data: any) {
  return commonFunc('/workflow/process/copyList', data, 'GET')
}
/* approvalworkflow already */
export function ownList(data: any) {
  return commonFunc('/workflow/process/ownList', data, 'GET')
}

/* task in interface */
export function aggregation(data: any) {
  return commonFunc('/workflow/aggregation/FormAndAppId', data, 'GET')
}

/* Query model and workflow list */
export function processCategoryList(data: any) {
  return commonFunc('/system/processCategory/list', data, 'GET')
}

/* model list */
export function ModelAndCategoryInfoList(data: any) {
  return commonFunc('/system/processCategory/ModelAndCategoryInfoList', data, 'GET')
}

/* Query model and workflow add */
export function processCategoryAdd(data: any) {
  return commonFunc('/system/processCategory', data, 'POST')
}

/* Query model and workflow del */
export function processCategoryDel(data: any) {
  return commonFunc('/system/processCategory/' + data, '', 'DELETE')
}

/* Query model and workflow edit */
export function processCategoryEdit(data: any) {
  return commonFunc('/system/processCategory', data, 'PUT')
}

/* approval -Query workflow user - list */
export function smartList(data: any) {
  return commonFunc('/workflow/process/smart/list', data, 'GET')
}

/* Update */
export function updateSortOrderBatch(data: any) {
  return commonFunc('/system/processCategory/updateSortOrderBatch', data, 'PUT')
}

/* approval-Update */
export function updateSortOrlerBatch(data: any) {
  return commonFunc('/workflow/model/updateSortOrlerBatch', data, 'PUT')
}

/* list - sub */
export function selectGroupWithModels(data: any) {
  return commonFunc('/system/processGroup/selectGroupWithModels', data, 'GET')
}

/* list */
export function processGroupList(data: any) {
  return commonFunc('/system/processGroup/list', data, 'GET')
}

/* add */
export function processGroupAdd(data: any) {
  return commonFunc('/system/processGroup', data, 'POST')
}

/* del */
export function processGroupDel(data: any) {
  return commonFunc('/system/processGroup/' + data, '', 'DELETE')
}

/* edit */
export function processGroupEdit(data: any) {
  return commonFunc('/system/processGroup', data, 'PUT')
}

/*  */
export function processGroupSort(data: any) {
  return commonFunc('/system/processGroup/updateSortOrderBatch', data, 'PUT')
}

/* model */
export function modelSort(data: any) {
  return commonFunc('/workflow/model/updateSortOrderBatch', data, 'post')
}

/* group -Query workflow list */
export function workflowProcessGroupList(data: any) {
  return commonFunc('/workflow/process/group/list', data, 'GET')
}

// approval-Export workflowrecord
export function wpSmartOwnExport(data: any) {
  return commonFunc('/workflow/process/smartOwnExport', data, 'POST')
}

// Export record -
export function operlogList(data: any) {
  return commonFunc('/monitor/operlog/list', data, 'GET')
}

// Export record - Export
export function operlogExport(data: any) {
  return commonFuncB('/monitor/operlog/export', data, 'POST')
}

// Get workflow record -
export function MOProcessList(data: any) {
  return commonFunc('/monitor/operlog/ProcessList', data, 'POST')
}

// approval -
export function WPHandoverList(data: any) {
  return commonFunc('/workflow/process/handoverList', data, 'GET')
}

// data
export function WPHandover(data: any) {
  return commonFunc('/workflow/process/handover', data, 'POST', false, 1000 * 50)
}

// Get record
export function MOTransferList(data: any) {
  return commonFunc('/monitor/operlog/transferList', data, 'POST')
}

// workflowapproval -add
export function systemWorkflowAdd(data: any) {
  return commonFunc('/workflow/rule', data, 'POST')
}

// workflowapproval -del
export function systemWorkflowDel(data: any) {
  return commonFunc('/workflow/rule/' + data, '', 'DELETE')
}

// workflowapproval -edit
export function systemWorkflowEdit(data: any) {
  return commonFunc('/workflow/rule', data, 'PUT')
}

// workflowapproval -list
export function systemWorkflowRule(data: any) {
  return commonFunc('/workflow/rule/list', data, 'GET')
}

// configuration workflow -list
export function timeOutRuleList(data: any) {
  return commonFunc('/workflow/rule/timeOutRuleList', data, 'GET')
}
