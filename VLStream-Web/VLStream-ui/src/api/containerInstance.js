/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'
import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

/**
 * instance APIinterface
 *
 * @author VLStream Team
 * @since 1.0.0
 */

/**
 * Query instance
 * @param {Object} params Query parameter
 * @param {number} params.current current
 * @param {number} params.size
 * @param {string} params.name instance ( Query )
 * @param {string} params.status instance
 * @param {number} params.algorithmId algorithmID
 * @param {string} params.healthStatus
 * @param {string} params.startTime create timestart
 * @param {string} params.endTime create timefinish
 */
export function getContainerInstancePage(params) {
  return currentIdentityRequest({
    url: '/vlsContainerInstance/page',
    method: 'get',
    params
  })
}

/**
 * Get instance
 * @param {number} id instanceID
 */
export function getContainerInstanceById(id) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/${computePathPart(id)}`,
    method: 'get'
  })
}

/**
 * instance
 * @param {Object} data instanceinfo
 * @param {string} data.name instance
 * @param {string} data.image
 * @param {number} data.algorithmId algorithmID
 * @param {string} data.cpuLimit CPU
 * @param {string} data.memoryLimit
 * @param {string} data.gpuLimit GPU
 * @param {string} data.description
 * @param {string} data.envVariables variableJSON
 * @param {string} data.portMappings JSON
 */
export function createContainerInstance(data) {
  return currentIdentityRequest({
    url: '/vlsContainerInstance',
    method: 'post',
    data
  })
}

/**
 * new instance
 * @param {Object} data instanceinfo
 * @param {number} data.id instanceID
 * @param {string} data.name instance
 * @param {string} data.description
 * @param {string} data.envVariables variableJSON
 * @param {string} data.portMappings JSON
 */
export function updateContainerInstance(data) {
  return currentIdentityRequest({
    url: '/vlsContainerInstance',
    method: 'put',
    data
  })
}

/**
 * Delete instance
 * @param {number} id instanceID
 */
export function deleteContainerInstance(id) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/${computePathPart(id)}`,
    method: 'delete'
  })
}

/**
 * Batch delete instance
 * @param {Array<number>} ids instanceIDarray
 */
export function batchDeleteContainerInstances(ids) {
  return currentIdentityRequest({
    url: '/vlsContainerInstance/batch',
    method: 'delete',
    data: { ids }
  })
}

/**
 * instance
 * @param {number} id instanceID
 */
export function startContainerInstance(id) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/${computePathPart(id)}/start`,
    method: 'post'
  })
}

/**
 * instance
 * @param {number} id instanceID
 */
export function stopContainerInstance(id) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/${computePathPart(id)}/stop`,
    method: 'post'
  })
}

/**
 * instance
 * @param {number} id instanceID
 */
export function restartContainerInstance(id) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/${computePathPart(id)}/restart`,
    method: 'post'
  })
}

/**
 * Get instance info
 */
export function getContainerInstanceStatistics() {
  return currentIdentityRequest({
    url: '/vlsContainerInstance/statistics',
    method: 'get'
  })
}

  export function getGpuResourceSnapshot(options = {}) {
  return request({
    url: '/vlsContainerInstance/resources',
      method: 'get',
      signal: options.signal,
      silentError: options.silentError
  })
}

export function getContainerInstanceLogs(id, lines = 500) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/${computePathPart(id)}/logs`,
    method: 'get',
    params: { lines }
  })
}

/**
 * Get in instance
 */
export function getRunningContainerInstances() {
  return currentIdentityRequest({
    url: '/vlsContainerInstance/running',
    method: 'get'
  })
}

/**
 * Get instance
 */
export function getErrorContainerInstances() {
  return currentIdentityRequest({
    url: '/vlsContainerInstance/error',
    method: 'get'
  })
}

/**
 * Get instance
 */
export function getUnhealthyContainerInstances() {
  return currentIdentityRequest({
    url: '/vlsContainerInstance/unhealthy',
    method: 'get'
  })
}

/**
 * instance whether in
 * @param {string} name instance
 * @param {number} excludeId instanceID ( )
 */
export function checkContainerInstanceName(name, excludeId = null) {
  return currentIdentityRequest({
    url: '/vlsContainerInstance/check-name',
    method: 'get',
    params: { name, excludeId }
  })
}

/**
 * algorithmIDGet instance
 * @param {number} algorithmId algorithmID
 */
export function getContainerInstancesByAlgorithm(algorithmId) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/algorithm/${computePathPart(algorithmId)}`,
    method: 'get'
  })
}

/**
 * new instance data
 * @param {number} id instanceID
 * @param {Object} data data
 * @param {number} data.cpuUsage CPU
 * @param {number} data.memoryUsage
 * @param {number} data.gpuUsage GPU
 */
export function updateContainerInstanceMonitoring(id, data) {
  return currentIdentityRequest({
    url: `/vlsContainerInstance/${computePathPart(id)}/monitoring`,
    method: 'put',
    data
  })
}
