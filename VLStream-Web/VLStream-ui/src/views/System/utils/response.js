/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

/**
 * Get SpringBlade interface R<T> data (Payload)
 * @param {Object} response after object
 * @return s {*} response.data response
 */
export function getPayload(response) {
  if (!response) return null
  if (Object.prototype.hasOwnProperty.call(response, 'data')) return response.data
  return response
}

/**
 * from SpringBlade in Get datarecordarray (records)
 * @param {Object} response after object, R<IPage> IPage
 * @return s {Array} recordarray, null / empty array
 */
export function getRecords(response) {
  const payload = getPayload(response)
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(response?.records)) return response.records
  return []
}

/**
 * from SpringBlade in Get (total)
 * @param {Object} response after object
 * @return s {Number}
 */
export function getTotal(response) {
  const payload = getPayload(response)
  return Number(payload?.total ?? response?.total ?? 0)
}

/**
 * parentId data
 * @param {Array} list data
 * @param {Array} rootParentIds to node parentId collection
 * @return s {Array} after
 */
export function buildTree(list, rootParentIds = [0, '0', null, undefined]) {
  const source = Array.isArray(list) ? list : []
  const nodeMap = new Map()

  // : all node children null / empty array
  source.forEach((item) => {
    nodeMap.set(String(item.id), { ...item, children: Array.isArray(item.children) ? item.children : [] })
  })

  const roots = []

  // : node parent roots
  nodeMap.forEach((node) => {
    const parentId = node.parentId
    const parent = nodeMap.get(String(parentId))
    if (parent && parent.id !== node.id) {
      parent.children.push(node)
    } else if (rootParentIds.includes(parentId)) {
      roots.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/**
 * node, after property to Element Plus table property label/children
 * @param {Array} nodes nodearray
 * @return s {Array} after nodearray
 */
export function normalizeTree(nodes) {
  const list = Array.isArray(nodes) ? nodes : []
  return list.map((node) => {
    const label = node.label || node.title || node.name || node.roleName || node.deptName || node.scopeName || node.menuName || node.fullName || node.postName || node.tenantName || node.code || node.id
    return {
      ...node,
      label,
      children: normalizeTree(node.children)
    }
  })
}

/**
 * in ID collection, , after SpringBlade comma-separated parameter
 * @param {Array} rows in dataarray
 * @return s {String} ID
 */
export function joinIds(rows) {
  return (Array.isArray(rows) ? rows : [])
    .map((row) => row?.id)
    .filter((id) => id !== undefined && id !== null && id !== '')
    .join(',')
}

/**
 * Validate SpringBlade after /Delete /Update operation whether successfully
 * @param {Object} response object
 * @return s {Boolean} true represents operationsuccessfully
 */
export function isSuccess(response) {
  if (!response) return false
  if (response.success === true) return true
  if (response.code === 200) return true
  return response.data === true
}
