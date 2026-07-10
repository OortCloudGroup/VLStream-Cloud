/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:46:36
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:46:36
* @Copyright aPaaS-front-team. All rights reserved.
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
    responseType: 'blob'
  })
}

// 分页查询流程分类列表
export function listCategory(query) {
  return commonFunc('/workflow/category/list', query, 'get')
}

// 查询流程全部分类列表
export function listAllCategory(query) {
  return commonFunc('/workflow/category/listAll', query, 'get')
}

// 新增流程分类
export function addCategory(data) {
  return commonFunc('/workflow/category', data, 'post')
}

// 修改流程分类
export function updateCategory(data) {
  return commonFunc('/workflow/category', data, 'put')
}

// 删除流程分类
export function deleteCategory(data) {
  return commonFunc('/workflow/category/' + data.categoryId, {}, 'delete')
}

// 新增表单
export function addForm(data) {
  return commonFunc('/workflow/form', data, 'post')
}

// 导出-表单
export function exportWorkflow(data) {
  return commonFuncB('/workflow/form/export', data, 'post')
}

// 修改表单
export function updateForm(data) {
  return commonFunc('/workflow/form', data, 'put')
}

// 删除表单
export function deleteForm(data) {
  return commonFunc('/workflow/form/' + data.formId, {}, 'delete')
}

// 表单详情
export function getForm(data) {
  return commonFunc('/workflow/form/' + data.formId, {}, 'get')
}

// 表单列表
export function listForm(query) {
  return commonFunc('/workflow/form/list', query, 'get')
}

// 模型列表
export function listModel(query) {
  return commonFunc('/workflow/model/list', query, 'get')
}

// 导出模型
export function exportModel(data) {
  return commonFuncB('/workflow/model/export', data, 'post')
}

// 新增模型
export function addModel(data) {
  return commonFunc('/workflow/model', data, 'post')
}

// 获取模型详情
export function getModel(data) {
  return commonFunc('/workflow/model/' + data.modelId, {}, 'get')
}

// 修改模型
export function updateModel(data) {
  return commonFunc('/workflow/model', data, 'put')
}

// 保存模型
export function saveModel(data) {
  return commonFunc('/workflow/model/save', data, 'post')
}

// 复制模型
export function copyModel(data) {
  return commonFunc('/workflow/model/copyModel', data, 'post')
}

// 设为最新模型
export function latestModel(data) {
  return commonFunc('/workflow/model/latest', data, 'post', true)
}

// 删除模型
export function deleteModel(data) {
  return commonFunc('/workflow/model', data, 'delete')
}

// 根据模型 ID 级联删除模型、流程图信息、所有模型版本、部署及其运行实例与历史数据
export function deleteModelCascade(data) {
  return commonFunc('/workflow/model/deleteModelCascade', data, 'delete')
}

// 部署模型
export function deployModel(data) {
  return commonFunc('/workflow/model/deploy', data, 'post', true)
}

// 获取流程模型图
export function getBpmnXml(data) {
  return commonFunc('/workflow/model/bpmnXml/' + data.modelId, {}, 'get')
}

// 获取流程模型历史列表
export function historyModel(data) {
  return commonFunc('/workflow/model/historyList', data, 'get')
}

// 部署列表
export function listDeploy(query) {
  return commonFunc('/workflow/deploy/list', query, 'get')
}

// 部署历史列表
export function listHistoryDeploy(query) {
  return commonFunc('/workflow/deploy/publishList', query, 'get')
}

// 删除流程部署
export function deleteDeploy(data) {
  return commonFunc('/workflow/deploy/' + data.deployId, {}, 'delete')
}

// 查询流程部署关联表单信息
export function getFormByDeployId(data) {
  return commonFunc('/workflow/deploy/form/' + data.deployId, {}, 'get')
}

// 修改部署状态
export function changeState(data) {
  return commonFunc('/workflow/deploy/changeState', data, 'put', true)
}

// 获取部署流程图
export function getDeployBpmnXml(data) {
  return commonFunc('/workflow/deploy/bpmnXml/' + data.definitionId, {}, 'get')
}

// 办公管理
// 流程管理
export function listProcess(data) {
  return commonFunc('/workflow/process/list', data, 'get')
}

// 查询流程表单
export function getProcessForm(data) {
  return commonFunc('/workflow/process/getProcessForm', data, 'get')
}

// 发起流程（部署流程实例）
export function startProcess(data) {
  return commonFunc('/workflow/process/start', data, 'post')
}

// 删除流程实例
export function deleteProcess(data) {
  return commonFunc('/workflow/process/instance/' + data.procInsId, {}, 'delete')
}

// 获取流程图
export function getProcessBpmnXml(data) {
  return commonFunc('/workflow/process/bpmnXml/' + data.processDefId, {}, 'get')
}

// 获取流程模型JSON
export function getBpmnJSON(data) {
  return commonFunc('/workflow/process/bpmnJson/' + data.processDefId, {}, 'get')
}

// 流程实例详情
export function detailProcess(data) {
  return commonFunc('/workflow/process/detail', data, 'get')
}

// 我发起的流程
export function listOwnProcess(data) {
  return commonFunc('/workflow/process/ownList', data, 'get')
}

// 我的工单
export function ownWorkOrderList(data) {
  return commonFunc('/workorder/workorder/ownWorkOrderList', data, 'get')
}

// 待办工单
export function todoWorkOrderList(data) {
  return commonFunc('/workorder/workorder/todoWorkOrderList', data, 'get')
}

// 可接工单
export function claimWorkOrderList(data) {
  return commonFunc('/workorder/workorder/claimWorkOrderList', data, 'get')
}

// 已办工单
export function finishedWorkOrderList(data) {
  return commonFunc('/workorder/workorder/finishedWorkOrderList', data, 'get')
}

// 我的待办
export function listTodoProcess(data) {
  return commonFunc('/workflow/process/todoList', data, 'get')
}

// 我的待签
export function listClaimProcess(data) {
  return commonFunc('/workflow/process/claimList', data, 'get')
}

// 我的已办
export function listFinishedProcess(data) {
  return commonFunc('/workflow/process/finishedList', data, 'get')
}

// 抄送我的
export function listCopyProcess(data) {
  return commonFunc('/workflow/process/copyList', data, 'get')
}

// 取消申请
export function stopProcess(data) {
  return commonFunc('/workflow/task/stopProcess', data, 'post')
}

// 撤回任务
export function revokeProcess(data) {
  return commonFunc('/workflow/task/revokeProcess', data, 'post')
}

// 角色列表
export function listRole(data) {
  return commonFunc('/system/role/list', data, 'get')
}

// 签收任务
export function claimTask(data) {
  return commonFunc('/workflow/task/claim', data, 'post')
}

// 取消认领/签收任务
export function unClaimTask(data) {
  return commonFunc('/workflow/task/unClaim', data, 'post')
}

// json 转 xml
export function jsonToXml(data) {
  return commonFunc('/model/download', data, 'post')
}

// 保存流程图json
export function getJSONModel(data) {
  return commonFunc('/work/modeJson/' + data.modelId, '', 'get')
}

// 完成任务
export function completeTask(data) {
  return commonFunc('/workflow/task/complete', data, 'post')
}

// 委派任务
export function delegateTask(data) {
  return commonFunc('/workflow/task/delegate', data, 'post')
}

// 转办任务
export function transferTask(data) {
  return commonFunc('/workflow/task/transfer', data, 'post')
}

// 退回任务
export function returnTask(data) {
  return commonFunc('/workflow/task/return', data, 'post')
}

// 可退回任务列表
export function returnList(data) {
  return commonFunc('/workflow/task/returnList', data, 'post')
}

// 拒绝任务
export function rejectTask(data) {
  return commonFunc('/workflow/task/reject', data, 'post')
}

// 获取审批节点操作的列表-流程设计器查按钮字典：wf_approval_button
export function getApprovalButton() {
  return Promise.resolve({
    code: 200,
    msg: '操作成功',
    data: [
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 45,
        dictSort: 1,
        dictLabel: '通过',
        dictValue: '0',
        dictType: 'wf_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '工单审批通过',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 46,
        dictSort: 2,
        dictLabel: '委派',
        dictValue: '1',
        dictType: 'wf_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批委派',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 47,
        dictSort: 3,
        dictLabel: '转办',
        dictValue: '2',
        dictType: 'wf_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批转办',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 48,
        dictSort: 4,
        dictLabel: '退回',
        dictValue: '3',
        dictType: 'wf_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批退回',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 49,
        dictSort: 5,
        dictLabel: '拒绝',
        dictValue: '4',
        dictType: 'wf_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批拒绝',
        default: true
      }
    ]
  })
}

// 获取审批节点操作的列表-工单设计器查按钮字典：wk_approval_button
export function getApprovalButton_wk() {
  return Promise.resolve({
    code: 200,
    msg: '操作成功',
    data: [
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 34,
        dictSort: 1,
        dictLabel: '通过',
        dictValue: '0',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批通过',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 35,
        dictSort: 2,
        dictLabel: '委派',
        dictValue: '1',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批委派',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 36,
        dictSort: 3,
        dictLabel: '转办',
        dictValue: '2',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批转办',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 37,
        dictSort: 4,
        dictLabel: '退回',
        dictValue: '3',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批退回',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2024-04-19 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 38,
        dictSort: 5,
        dictLabel: '拒绝',
        dictValue: '4',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批拒绝',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2025-02-22 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 39,
        dictSort: 6,
        dictLabel: '指派',
        dictValue: '5',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批指派',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2025-02-22 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 40,
        dictSort: 7,
        dictLabel: '接单',
        dictValue: '6',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批接单',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2025-02-22 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 41,
        dictSort: 8,
        dictLabel: '退单',
        dictValue: '7',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批退单',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2025-02-22 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 42,
        dictSort: 9,
        dictLabel: '提交回访记录',
        dictValue: '8',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批提交回访记录',
        default: true
      },
      {
        createBy: 'admin',
        createTime: '2025-02-22 14:21:30',
        updateBy: '',
        updateTime: null,
        dictCode: 43,
        dictSort: 10,
        dictLabel: '验收通过',
        dictValue: '9',
        dictType: 'wk_approval_button',
        cssClass: ' ',
        listClass: null,
        isDefault: 'Y',
        status: '0',
        remark: '流程审批验收通过',
        default: true
      }
    ]
  })
}

// 表单分类
/* 表单分类-添加 */ // 综合
export function workflowAdd(data: any) {
  return commonFunc('/workflow/formSynthesis', data, 'POST')
}

/* 表单分类-导出 */
export function workflowExport(data: any) {
  return commonFuncB('/workflow/formCategory/export', data, 'POST')
}

/* 表单分类-编辑 */ // 综合
export function workflowEdit(data: any) {
  return commonFunc('/workflow/formSynthesis', data, 'PUT')
}

/* 表单分类-删除 */
export function workflowDelete(data: any) {
  return commonFunc('/workflow/formCategory/' + data, '', 'DELETE')
}

// /* 表单分类-批量删除 */
// export function workflowDeleteBatch(data: any) {
//   return commonFunc('/manage/workflow/deleteBatch', data, 'DELETE')
// }

/* 表单分类-列表 */
export function workflowList(data: any) {
  return commonFunc('/workflow/formCategory/list', data, 'GET')
}

/* 表单分类-综合分类 */
export function formSynthesisList(data: any) {
  return commonFunc('/workflow/formSynthesis/list', data, 'GET')
}

/* 表单分类-应用分类 */
export function formAppList(data: any) {
  return commonFunc('/system/formApp/list', data, 'GET')
}

/* 表单分类-应用分类-add */
export function formAppAdd(data: any) {
  return commonFunc('/system/formApp', data, 'POST')
}
/* 表单分类-应用分类-edit */
export function formAppEdit(data: any) {
  return commonFunc('/system/formApp', data, 'PUT')
}

/* 表单分类-应用分类-del */
export function formAppDel(data: any) {
  return commonFunc('/system/formApp/' + data, '', 'DELETE')
}

// 应用工单分类
/* 应用工单分类-添加 */
export function addWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app', data, 'POST')
}

/* 应用工单分类-导出 */
export function exportWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app/export', data, 'POST')
}

/* 应用工单分类-编辑 */
export function editWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app', data, 'PUT')
}

/* 应用工单分类-删除 */
export function deleteWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app/' + data, '', 'DELETE')
}

/* 应用工单分类-列表 */
export function listWorkOrder(data: any) {
  return commonFunc('/WorkOrder/app/list', data, 'GET')
}

// 应用通用分类
/* 应用通用分类-添加 */
export function addwfApp(data: any) {
  return commonFunc('/wf/app', data, 'POST')
}

/* 应用通用分类-编辑 */
export function editwfApp(data: any) {
  return commonFunc('/wf/app', data, 'PUT')
}

/* 应用通用分类-删除 */
export function deletewfApp(data: any) {
  return commonFunc('/wf/app/' + data, '', 'DELETE')
}

/* 应用分类-通用流程 */
export function listwfApp(data: any) {
  return commonFunc('/wf/app/list', data, 'GET')
}

// 综合工单流程
/* 综合工单流程-添加 */
export function addSynthesis(data: any) {
  return commonFunc('/workorder/synthesis', data, 'POST')
}

/* 综合工单流程-导出 */
export function exportSynthesis(data: any) {
  return commonFunc('/workorder/synthesis/export', data, 'POST')
}

/* 综合工单流程-编辑 */
export function editSynthesis(data: any) {
  return commonFunc('/workorder/synthesis', data, 'PUT')
}

/* 综合工单流程-删除 */
export function deleteSynthesis(data: any) {
  return commonFunc('/workorder/synthesis/' + data, '', 'DELETE')
}

/* 综合工单流程-列表 */
export function listSynthesis(data: any) {
  return commonFunc('/workorder/synthesis/list', data, 'GET')
}

/* 综合工单流程-列表 */
export function listSynthesisAll(data: any) {
  return commonFunc('/workorder/synthesis/listAll', data, 'GET')
}
// 综合通用流程
/* 综合通用流程-添加 */
export function addwf(data: any) {
  return commonFunc('/wf/synthesis', data, 'POST')
}

/* 综合通用流程-编辑 */
export function editwf(data: any) {
  return commonFunc('/wf/synthesis', data, 'PUT')
}

/* 综合通用流程-删除 */
export function deletewf(data: any) {
  return commonFunc('/wf/synthesis/' + data, '', 'DELETE')
}

/* 综合通用流程-列表 */
export function listwf(data: any) {
  return commonFunc('/wf/synthesis/list', data, 'GET')
}

/* 综合通用流程-列表 */
export function listwfAll(data: any) {
  return commonFunc('/wf/synthesis/listAll', data, 'GET')
}

/* 审批流程应用 */
export function wfApp(data: any) {
  return commonFunc('/wf/app/list', data, 'GET')
}
/* 审批流程管理 待办 */
export function todoList(data: any) {
  return commonFunc('/workflow/process/todoList', data, 'GET')
}
/* 审批流程管理 已办 */
export function finishedList(data: any) {
  return commonFunc('/workflow/process/finishedList', data, 'GET')
}
/* 审批流程管理 抄送 */
export function copyList(data: any) {
  return commonFunc('/workflow/process/copyList', data, 'GET')
}
/* 审批流程管理 已发起 */
export function ownList(data: any) {
  return commonFunc('/workflow/process/ownList', data, 'GET')
}

/* 任务中心 聚合接口 */
export function aggregation(data: any) {
  return commonFunc('/workflow/aggregation/FormAndAppId', data, 'GET')
}
