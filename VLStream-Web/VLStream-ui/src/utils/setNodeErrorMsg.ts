/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2025-01-13 18:38:00
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-05-08 10:29:21
* @Copyright aPaaS-front-team. All rights reserved.
*/

// 统一处理各个节点的属性检查

import { useErrorMsgStoreHook } from '@/store/modules/useErrorMsg'
const errorStore = useErrorMsgStoreHook()

export const setFqrErrorMsg = (data, id) => {
  if (data.formKey) {
    errorStore.removeNodeError(id)
  } else {
    errorStore.addNodeError({ id: id, msg: ['请在发起节点选择表单'] })
  }
}

export const setCfqErrorMsg = (data, id) => {
  let errorMsg: string[] = []
  if (data.triggerType === undefined) {
    errorMsg.push('请设置触发器相关属性')
  } else if (data.triggerType === 1 && !data.url) {
    errorMsg.push('请填入请求URL')
  } else {
    errorMsg = []
  }
  if (errorMsg.length === 0) {
    errorStore.removeNodeError(id)
  } else {
    errorStore.addNodeError({ id: id, msg: errorMsg })
  }
}

export const setCsErrorMsg = (data, id) => {
  let errorMsg : string[] = []
  if (data.users === undefined) {
    errorMsg.push('请设置抄送相关属性')
  } else if (!data.users || data.users.length === 0) {
    errorMsg.push('请选择抄送人')
  } else {
    errorMsg = []
  }
  if (errorMsg.length === 0) {
    errorStore.removeNodeError(id)
  } else {
    errorStore.addNodeError({ id: id, msg: errorMsg })
  }
}

export const setTjErrorMsg = (data, id) => {
  let errorMsg : string[] = []
  if (data.property.group === undefined && !data.expression) {
    errorMsg.push('请设置条件相关属性')
  } else if (data.property.group && data.property.group.length === 0) {
    errorMsg.push('请添加条件组')
  } else if (data.property.group && data.property.group[0].conditions.length === 0) {
    errorMsg.push('请检查添加条件')
  } else {
    errorMsg = []
  }
  if (errorMsg.length === 0) {
    errorStore.removeNodeError(id)
  } else {
    errorStore.addNodeError({ id: id, msg: errorMsg })
  }
}

export const setYsclErrorMsg = (data, id) => {
  let errorMsg : string[] = []
  if (data.waitType === undefined) {
    errorMsg.push('请设置延时节点相关属性')
  } else if (data.waitType === 'duration' && data.delayDuration === 0) {
    errorMsg.push('请填入延时时间')
  } else if (data.waitType === 'date' && !data.timeDate) {
    errorMsg.push('请选择自动计算时间')
  } else {
    errorMsg = []
  }
  if (errorMsg.length === 0) {
    errorStore.removeNodeError(id)
  } else {
    errorStore.addNodeError({ id: id, msg: errorMsg })
  }
}

export const setApprovalErrorMsg = (data, id) => {
  let errorMsg: string[] = []
  if (data.approvalType === undefined) {
    errorMsg.push('请设置审批相关属性')
  } else if (data.approvalType === 1 && data.users?.length === 0) {
    errorMsg.push('请选择审批人')
  } else if (data.approvalType === 2 && data.dept?.length === 0) {
    errorMsg.push('请选择审批部门')
  } else if (data.approvalType === 4 && data.roles?.length === 0) {
    errorMsg.push('请选择审批角色')
  } else if (data.emptyApproType === 4 && data.emptyApproUser?.length === 0) {
    errorMsg.push('请选择审批人为空人员列表')
  } else {
    errorMsg = []
  }
  if (errorMsg.length === 0) {
    errorStore.removeNodeError(id)
  } else {
    errorStore.addNodeError({ id: id, msg: errorMsg })
  }
}
