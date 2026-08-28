/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

// algorithmmodelAPIservice

/**
 * Query algorithmmodel
 * @param {Object} params Query parameter
 */
export function getModelPage(params) {
  return request({
    url: '/vlsAlgorithmModel/page',
    method: 'get',
    data: params
  })
}

/**
 * IDQuery algorithmmodel
 * @param {Number} id modelID
 */
export function getModelById(id) {
  return request({
    url: `/vlsAlgorithmModel/${id}`,
    method: 'get'
  })
}

/**
 * algorithmmodel
 * @param {Object} data modeldata
 */
export function createModel(data) {
  return request({
    url: '/vlsAlgorithmModel/create',
    method: 'post',
    data
  })
}

/**
 * new algorithmmodel
 * @param {Object} data modeldata
 */
export function updateModel(data) {
  return request({
    url: '/vlsAlgorithmModel/update',
    method: 'post',
    data
  })
}

/**
 * Delete algorithmmodel
 * @param {Number} id modelID
 */
export function deleteModel(id) {
  return request({
    url: `/vlsAlgorithmModel/${id}`,
    method: 'delete'
  })
}

/**
 * Batch delete algorithmmodel
 * @param {Array} ids modelID
 */
export function batchDeleteModel(ids) {
  return request({
    url: '/vlsAlgorithmModel/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * algorithmIDQuery model list
 * @param {Number} algorithmId algorithmID
 */
export function getModelsByAlgorithmId(algorithmId) {
  return request({
    url: `/vlsAlgorithmModel/algorithm/${algorithmId}`,
    method: 'get'
  })
}

/**
 * trainingtaskIDQuery model list
 * @param {Number} trainingId trainingtaskID
 */
export function getModelsByTrainingId(trainingId) {
  return request({
    url: `/vlsAlgorithmModel/training/${trainingId}`,
    method: 'get'
  })
}

/**
 * Query model list
 * @param {String} status
 */
export function getModelsByStatus(status) {
  return request({
    url: `/vlsAlgorithmModel/status/${status}`,
    method: 'get'
  })
}

/**
 * model
 * @param {Number} id modelID
 */
export function publishModel(id) {
  return request({
    url: `/vlsAlgorithmModel/publish/${id}`,
    method: 'post'
  })
}

/**
 * model
 * @param {Number} id modelID
 */
export function unpublishModel(id) {
  return request({
    url: `/vlsAlgorithmModel/unpublish/${id}`,
    method: 'post'
  })
}

/**
 * model
 * @param {Array} ids modelID
 */
export function batchPublishModel(ids) {
  return request({
    url: '/vlsAlgorithmModel/batch-publish',
    method: 'post',
    data: ids
  })
}

/**
 * model
 * @param {Number} id modelID
 */
export function downloadModel(id) {
  return request({
    url: `/vlsAlgorithmModel/download/${id}`,
    method: 'get'
  })
}

/**
 * model
 * @param {Number} id modelID
 */
export function deployModel(id) {
  return request({
    url: `/vlsAlgorithmModel/deploy/${id}`,
    method: 'post'
  })
}

/**
 * Get model info
 */
export function getModelStatistics() {
  return request({
    url: '/vlsAlgorithmModel/statistics',
    method: 'get'
  })
}

/**
 * model and whether in
 * @param {String} modelName model
 * @param {String} version model
 * @param {Number} excludeId ID
 */
export function checkModelNameAndVersion(modelName, version, excludeId) {
  return request({
    url: '/vlsAlgorithmModel/check-name-version',
    method: 'get',
    params: {
      modelName,
      version,
      excludeId
    }
  })
}

/**
 * algorithmID and Query model
 * @param {Number} algorithmId algorithmID
 * @param {String} version
 */
export function getModelByAlgorithmIdAndVersion(algorithmId, version) {
  return request({
    url: `/vlsAlgorithmModel/algorithm/${algorithmId}/version/${version}`,
    method: 'get'
  })
}

/**
 * Get algorithm new model
 * @param {Number} algorithmId algorithmID
 */
export function getLatestModelByAlgorithmId(algorithmId) {
  return request({
    url: `/vlsAlgorithmModel/algorithm/${algorithmId}/latest`,
    method: 'get'
  })
}

/**
 * Query model
 * @param {Number} limit
 */
export function getPopularModels(limit = 10) {
  return request({
    url: '/vlsAlgorithmModel/popular',
    method: 'get',
    params: { limit }
  })
}

/**
 * Query model
 * @param {Number} createdBy ID
 */
export function countModelsByCreatedBy(createdBy) {
  return request({
    url: `/vlsAlgorithmModel/count/creator/${createdBy}`,
    method: 'get'
  })
}

/**
 * Get algorithmmodel
 */
export function getTotalModelSize() {
  return request({
    url: '/vlsAlgorithmModel/total-size',
    method: 'get'
  })
}