/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

export const getAlgorithmCatalogCategories = () => request({ url: '/vlsAlgorithmCatalog/categories', method: 'get' })
export const saveAlgorithmCatalogCategory = (data) => request({
  url: data.id ? `/vlsAlgorithmCatalog/categories/${data.id}` : '/vlsAlgorithmCatalog/categories',
  method: data.id ? 'put' : 'post', data
})
export const deleteAlgorithmCatalogCategories = (ids) => request({ url: '/vlsAlgorithmCatalog/categories', method: 'delete', data: ids })
export const getAlgorithmCatalogPage = (params) => request({ url: '/vlsAlgorithmCatalog/algorithms', method: 'get', params })
export const getAlgorithmCatalogSettings = () => request({ url: '/vlsAlgorithmCatalog/settings', method: 'get' })
export const saveAlgorithmCatalogSettings = (data) => request({ url: '/vlsAlgorithmCatalog/settings', method: 'put', data })

// ========== algorithm API ==========

/**
 * Query algorithm list
 * @param {Object} params Query parameter
 * @param {number} params.current current
 * @param {number} params.size
 * @param {string} params.name ( Query )
 * @param {string} params.repositoryType
 * @param {string} params.status
 */
export function getAlgorithmRepositoryPage(params) {
  return request({
    url: '/vlsAlgorithmRepository/page',
    method: 'get',
    params
  })
}

/**
 * Query all algorithm
 */
export function getEnabledAlgorithmRepositories() {
  return request({
    url: '/vlsAlgorithmRepository/enabled',
    method: 'get'
  })
}

/**
 * Query algorithm
 * @param {string} repositoryType
 */
export function getAlgorithmRepositoriesByType(repositoryType) {
  return request({
    url: `/vlsAlgorithmRepository/type/${repositoryType}`,
    method: 'get'
  })
}

/**
 * IDQuery algorithm
 * @param {number} id ID
 */
export function getAlgorithmRepositoryById(id) {
  return request({
    url: `/vlsAlgorithmRepository/${id}`,
    method: 'get'
  })
}

/**
 * algorithm
 * @param {Object} data data
 * @param {string} data.name
 * @param {string} data.remark remark
 * @param {string} data.repositoryType
 * @param {string} data.status
 */
export function createAlgorithmRepository(data) {
  return request({
    url: '/vlsAlgorithmRepository',
    method: 'post',
    data
  })
}

/**
 * new algorithm
 * @param {number} id ID
 * @param {Object} data new data
 */
export function updateAlgorithmRepository(id, data) {
  return request({
    url: `/vlsAlgorithmRepository/${id}`,
    method: 'put',
    data
  })
}

/**
 * Delete algorithm
 * @param {number} id ID
 */
export function deleteAlgorithmRepository(id) {
  return request({
    url: `/vlsAlgorithmRepository/${id}`,
    method: 'delete'
  })
}

/**
 * Batch delete algorithm
 * @param {number[]} ids ID
 */
export function batchDeleteAlgorithmRepositories(ids) {
  return request({
    url: '/vlsAlgorithmRepository/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * new
 * @param {number} id ID
 * @param {string} status new
 */
export function updateAlgorithmRepositoryStatus(id, status) {
  return request({
    url: `/vlsAlgorithmRepository/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * new
 * @param {number[]} ids ID
 * @param {string} status new
 */
export function batchUpdateAlgorithmRepositoryStatus(ids, status) {
  return request({
    url: '/vlsAlgorithmRepository/batch/status',
    method: 'put',
    data: ids,
    params: { status }
  })
}

/**
 * algorithm
 */
export function countAlgorithmRepositories() {
  return request({
    url: '/vlsAlgorithmRepository/count',
    method: 'get'
  })
}

/**
 * new algorithm
 * @param {number} id ID
 */
export function refreshAlgorithmCount(id) {
  return request({
    url: `/vlsAlgorithmRepository/${id}/refresh-count`,
    method: 'put'
  })
}

// ========== algorithm API ==========

/**
 * Query algorithm list
 * @param {Object} params Query parameter
 * @param {number} params.current current
 * @param {number} params.size
 * @param {number} params.repositoryId ID
 * @param {string} params.name algorithm ( Query )
 * @param {string} params.category algorithm
 * @param {string} params.type algorithm
 * @param {string} params.deployStatus
 */
export function getAlgorithmPage(params) {
  return request({
    url: '/vlsAlgorithm/page',
    method: 'get',
    params
  })
}

/**
 * IDQuery algorithm list
 * @param {number} repositoryId ID
 */
export function getAlgorithmsByRepositoryId(repositoryId) {
  return request({
    url: `/vlsAlgorithm/repository/${repositoryId}`,
    method: 'get'
  })
}

/**
 * Query algorithm list
 * @param {string} category algorithm
 */
export function getAlgorithmsByCategory(category) {
  return request({
    url: `/vlsAlgorithm/category/${category}`,
    method: 'get'
  })
}

/**
 * IDQuery algorithm
 * @param {number} id algorithmID
 */
export function getAlgorithmById(id) {
  return request({
    url: `/vlsAlgorithm/${id}`,
    method: 'get'
  })
}

/**
 * algorithm
 * @param {Object} data algorithmdata
 * @param {number} data.repositoryId ID
 * @param {string} data.name algorithm
 * @param {string} data.category algorithm
 * @param {string} data.type algorithm
 * @param {string} data.description algorithm
 * @param {string} data.version algorithm
 * @param {string} data.inputFormat
 * @param {string} data.outputFormat
 * @param {number} data.gpuRequired whether need to GPU
 */
export function createAlgorithm(data) {
  return request({
    url: '/vlsAlgorithm',
    method: 'post',
    data
  })
}

/**
 * new algorithm
 * @param {number} id algorithmID
 * @param {Object} data new data
 */
export function updateAlgorithm(id, data) {
  return request({
    url: `/vlsAlgorithm/${id}`,
    method: 'put',
    data
  })
}

/**
 * Delete algorithm
 * @param {number} id algorithmID
 */
export function deleteAlgorithm(id) {
  return request({
    url: `/vlsAlgorithm/${id}`,
    method: 'delete'
  })
}

/**
 * Batch delete algorithm
 * @param {number[]} ids algorithmID
 */
export function batchDeleteAlgorithms(ids) {
  return request({
    url: '/vlsAlgorithm/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * new
 * @param {number} id algorithmID
 * @param {string} deployStatus
 */
export function updateAlgorithmDeployStatus(id, deployStatus) {
  return request({
    url: `/vlsAlgorithm/${id}/deploy-status`,
    method: 'put',
    params: { deployStatus }
  })
}

/**
 * new
 * @param {number[]} ids algorithmID
 * @param {string} deployStatus
 */
export function batchUpdateAlgorithmDeployStatus(ids, deployStatus) {
  return request({
    url: '/vlsAlgorithm/batch/deploy-status',
    method: 'put',
    data: ids,
    params: { deployStatus }
  })
}

/**
 * algorithm device
 * @param {number} algorithmId algorithmID
 * @param {number[]} deviceIds deviceID
 */
export function deployAlgorithmToDevices(algorithmId, deviceIds) {
  return request({
    url: `/vlsAlgorithm/${algorithmId}/deploy`,
    method: 'post',
    data: deviceIds
  })
}

/**
 * algorithm
 * @param {number} algorithmId algorithmID
 * @param {Object} testData data ( )
 */
export function evaluateAlgorithm(algorithmId) {
  return request({
    url: `/vlsAlgorithm/${algorithmId}/evaluate`,
    method: 'post'
  })
}

/**
 * Get algorithm
 */
export function getAlgorithmCategoryStatistics() {
  return request({
    url: '/vlsAlgorithm/statistics/category',
    method: 'get'
  })
}

/**
 * Get algorithm
 */
export function getAlgorithmTypeStatistics() {
  return request({
    url: '/vlsAlgorithm/statistics/type',
    method: 'get'
  })
}

/**
 * Get
 */
export function getAlgorithmDeployStatusStatistics() {
  return request({
    url: '/vlsAlgorithm/statistics/deploy-status',
    method: 'get'
  })
}

/**
 * algorithm
 * @param {number} repositoryId ID
 */
export function countAlgorithmsByRepositoryId(repositoryId) {
  return request({
    url: `/vlsAlgorithm/count/repository/${repositoryId}`,
    method: 'get'
  })
}
