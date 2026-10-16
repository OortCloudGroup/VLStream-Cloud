/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

// algorithmmodelAPIservice

/**
 * Query algorithmmodel
 * @param {Object} params Query parameter
 */
export function getModelPage(params) {
  return currentIdentityRequest({
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
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/${computePathPart(id)}`,
    method: 'get'
  })
}

/**
 * algorithmmodel
 * @param {Object} data modeldata
 */
export function createModel(data) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmModel/create',
    method: 'post',
    data
  })
}

/** Import an externally trained YOLOv8 PT and its matching data.yaml. */
export function importModelFiles(formData, onUploadProgress) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmModel/import',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

/** Import one PT and its class YAML from a ZIP archive. */
export function importModelArchive(formData, onUploadProgress) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmModel/import-zip',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

/**
 * new algorithmmodel
 * @param {Object} data modeldata
 */
export function updateModel(data) {
  return currentIdentityRequest({
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
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/${computePathPart(id)}`,
    method: 'delete'
  })
}

/**
 * Batch delete algorithmmodel
 * @param {Array} ids modelID
 */
export function batchDeleteModel(ids) {
  return currentIdentityRequest({
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
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/algorithm/${computePathPart(algorithmId)}`,
    method: 'get'
  })
}

/**
 * trainingtaskIDQuery model list
 * @param {Number} trainingId trainingtaskID
 */
export function getModelsByTrainingId(trainingId) {
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/training/${computePathPart(trainingId)}`,
    method: 'get'
  })
}

/**
 * Query model list
 * @param {String} status
 */
export function getModelsByStatus(status) {
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/status/${computePathPart(status)}`,
    method: 'get'
  })
}

/**
 * model
 * @param {Number} id modelID
 */
export function publishModel(id) {
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/publish/${computePathPart(id)}`,
    method: 'post'
  })
}

/**
 * model
 * @param {Number} id modelID
 */
export function unpublishModel(id) {
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/unpublish/${computePathPart(id)}`,
    method: 'post'
  })
}

/**
 * model
 * @param {Array} ids modelID
 */
export function batchPublishModel(ids) {
  return currentIdentityRequest({
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
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/download/${computePathPart(id)}`,
    method: 'get'
  })
}

export const downloadModelFile = (id, type) => currentIdentityRequest({
  url: `/vlsAlgorithmModel/${computePathPart(id)}/download-file`, method: 'get',
  params: { type }, responseType: 'blob'
})

/**
 * model
 * @param {Number} id modelID
 */
export function deployModel(id) {
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/deploy/${computePathPart(id)}`,
    method: 'post'
  })
}

/**
 * Get model info
 */
export function getModelStatistics() {
  return currentIdentityRequest({
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
  return currentIdentityRequest({
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
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/algorithm/${computePathPart(algorithmId)}/version/${computePathPart(version)}`,
    method: 'get'
  })
}

/**
 * Get algorithm new model
 * @param {Number} algorithmId algorithmID
 */
export function getLatestModelByAlgorithmId(algorithmId) {
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/algorithm/${computePathPart(algorithmId)}/latest`,
    method: 'get'
  })
}

/**
 * Query model
 * @param {Number} limit
 */
export function getPopularModels(limit = 10) {
  return currentIdentityRequest({
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
  return currentIdentityRequest({
    url: `/vlsAlgorithmModel/count/creator/${computePathPart(createdBy)}`,
    method: 'get'
  })
}

/**
 * Get algorithmmodel
 */
export function getTotalModelSize() {
  return currentIdentityRequest({
    url: '/vlsAlgorithmModel/total-size',
    method: 'get'
  })
}
