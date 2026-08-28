/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

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
  return request({
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
  return request({
    url: `/vlsContainerInstance/${id}`,
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
  return request({
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
  return request({
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
  return request({
    url: `/vlsContainerInstance/${id}`,
    method: 'delete'
  })
}

/**
 * Batch delete instance
 * @param {Array<number>} ids instanceIDarray
 */
export function batchDeleteContainerInstances(ids) {
  return request({
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
  return request({
    url: `/vlsContainerInstance/${id}/start`,
    method: 'post'
  })
}

/**
 * instance
 * @param {number} id instanceID
 */
export function stopContainerInstance(id) {
  return request({
    url: `/vlsContainerInstance/${id}/stop`,
    method: 'post'
  })
}

/**
 * instance
 * @param {number} id instanceID
 */
export function restartContainerInstance(id) {
  return request({
    url: `/vlsContainerInstance/${id}/restart`,
    method: 'post'
  })
}

/**
 * Get instance info
 */
export function getContainerInstanceStatistics() {
  return request({
    url: '/vlsContainerInstance/statistics',
    method: 'get'
  })
}

export function getGpuResourceSnapshot() {
  return request({
    url: '/vlsContainerInstance/resources',
    method: 'get'
  })
}

export function getContainerInstanceLogs(id, lines = 500) {
  return request({
    url: `/vlsContainerInstance/${id}/logs`,
    method: 'get',
    params: { lines }
  })
}

/**
 * Get in instance
 */
export function getRunningContainerInstances() {
  return request({
    url: '/vlsContainerInstance/running',
    method: 'get'
  })
}

/**
 * Get instance
 */
export function getErrorContainerInstances() {
  return request({
    url: '/vlsContainerInstance/error',
    method: 'get'
  })
}

/**
 * Get instance
 */
export function getUnhealthyContainerInstances() {
  return request({
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
  return request({
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
  return request({
    url: `/vlsContainerInstance/algorithm/${algorithmId}`,
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
  return request({
    url: `/vlsContainerInstance/${id}/monitoring`,
    method: 'put',
    data
  })
}
