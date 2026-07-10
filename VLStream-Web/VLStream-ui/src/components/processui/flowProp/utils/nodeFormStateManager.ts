/**
 * 节点级别的表单状态管理工具
 * 为每个流程节点维护独立的表单字段列表，避免全局Store污染
 *
 * 特点：
 * 1. 每个节点有独立的formFiledList副本
 * 2. 节点之间的数据完全隔离
 * 3. 支持动态更新和重置
 * 4. 通过Ref保证响应式更新
 */

import { ref, Ref } from 'vue'

export interface FormFieldInfo {
  id: string
  name: string
  readonly: boolean
  hidden: boolean
  required: boolean
}

/**
 * 节点级别的表单字段状态
 */
export interface NodeFormState {
  nodeId: string // 节点唯一标识
  formFiledList: Ref<FormFieldInfo[]> // 该节点的表单字段列表
  formKey: string // 当前选择的表单ID
  updateFormFields: (fields: FormFieldInfo[]) => void // 更新字段列表
  clearFormFields: () => void // 清空字段列表
}

/**
 * 节点表单状态缓存（防止重复创建）
 */
const nodeFormStates = new Map<string, NodeFormState>()

/**
 * 获取或创建节点的表单状态
 * @param nodeId - 节点唯一标识
 * @returns 节点的表单状态对象
 */
export const getOrCreateNodeFormState = (nodeId: string): NodeFormState => {
  // 如果已存在该节点的状态，直接返回
  if (nodeFormStates.has(nodeId)) {
    return nodeFormStates.get(nodeId)!
  }

  // 创建新的节点状态
  const formFiledList = ref<FormFieldInfo[]>([])

  const nodeState: NodeFormState = {
    nodeId,
    formFiledList,
    formKey: '',
    updateFormFields: (fields: FormFieldInfo[]) => {
      formFiledList.value = fields
    },
    clearFormFields: () => {
      formFiledList.value = []
    }
  }

  // 缓存该节点状态
  nodeFormStates.set(nodeId, nodeState)
  return nodeState
}

/**
 * 清理节点的表单状态（节点删除时调用）
 * @param nodeId - 节点唯一标识
 */
export const clearNodeFormState = (nodeId: string): void => {
  if (nodeFormStates.has(nodeId)) {
    const nodeState = nodeFormStates.get(nodeId)!
    nodeState.clearFormFields()
    nodeFormStates.delete(nodeId)
  }
}

/**
 * 获取节点的表单字段列表
 * @param nodeId - 节点唯一标识
 * @returns 表单字段数组
 */
export const getNodeFormFields = (nodeId: string): FormFieldInfo[] => {
  const nodeState = nodeFormStates.get(nodeId)
  return nodeState ? nodeState.formFiledList.value : []
}

/**
 * 更新节点的表单字段列表
 * @param nodeId - 节点唯一标识
 * @param fields - 新的字段列表
 */
export const updateNodeFormFields = (nodeId: string, fields: FormFieldInfo[]): void => {
  const nodeState = getOrCreateNodeFormState(nodeId)
  nodeState.updateFormFields(fields)
}

/**
 * 批量清理多个节点的表单状态
 * @param nodeIds - 节点ID数组
 */
export const clearNodeFormStates = (nodeIds: string[]): void => {
  nodeIds.forEach(nodeId => clearNodeFormState(nodeId))
}

/**
 * 获取所有已缓存的节点状态（调试用）
 */
export const getAllNodeFormStates = (): Map<string, NodeFormState> => {
  return new Map(nodeFormStates)
}
