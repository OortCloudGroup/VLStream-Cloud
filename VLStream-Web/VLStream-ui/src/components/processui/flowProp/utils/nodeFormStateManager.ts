/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

/**
 * node form
 * to each workflownode formfield , full Store
 *
 * :
 * 1. each node formFiledList
 * 2. node data full
 * 3. new and
 * 4. Ref new
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
 * node formfield
 */
export interface NodeFormState {
  nodeId: string // node
  formFiledList: Ref<FormFieldInfo[]> // node formfield
  formKey: string // current formID
  updateFormFields: (fields: FormFieldInfo[]) => void // new field
  clearFormFields: () => void // null / empty field
}

/**
 * nodeform ( )
 */
const nodeFormStates = new Map<string, NodeFormState>()

/**
 * Get node form
 * @param nodeId - node
 * @return s node form object
 */
export const getOrCreateNodeFormState = (nodeId: string): NodeFormState => {
  // if already in node ,
  if (nodeFormStates.has(nodeId)) {
    return nodeFormStates.get(nodeId)!
  }

  // new node
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

  // node
  nodeFormStates.set(nodeId, nodeState)
  return nodeState
}

/**
 * node form (nodeDelete )
 * @param nodeId - node
 */
export const clearNodeFormState = (nodeId: string): void => {
  if (nodeFormStates.has(nodeId)) {
    const nodeState = nodeFormStates.get(nodeId)!
    nodeState.clearFormFields()
    nodeFormStates.delete(nodeId)
  }
}

/**
 * Get node formfield
 * @param nodeId - node
 * @return s formfieldarray
 */
export const getNodeFormFields = (nodeId: string): FormFieldInfo[] => {
  const nodeState = nodeFormStates.get(nodeId)
  return nodeState ? nodeState.formFiledList.value : []
}

/**
 * new node formfield
 * @param nodeId - node
 * @param fields - new field
 */
export const updateNodeFormFields = (nodeId: string, fields: FormFieldInfo[]): void => {
  const nodeState = getOrCreateNodeFormState(nodeId)
  nodeState.updateFormFields(fields)
}

/**
 * node form
 * @param nodeIds - nodeIDarray
 */
export const clearNodeFormStates = (nodeIds: string[]): void => {
  nodeIds.forEach(nodeId => clearNodeFormState(nodeId))
}

/**
 * Get all already node ( )
 */
export const getAllNodeFormStates = (): Map<string, NodeFormState> => {
  return new Map(nodeFormStates)
}
