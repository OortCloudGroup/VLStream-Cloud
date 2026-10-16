/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

// Query trainingtask
export function getTrainingPage(params) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/page',
    method: 'get',
    params: params
  })
}

// Query trainingtask
export function getTrainingTask(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id),
    method: 'get'
  })
}

// Add trainingtask
export function addTrainingTask(data) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining',
    method: 'post',
    data: data
  })
}

// Update trainingtask
export function updateTrainingTask(data) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(data.id),
    method: 'put',
    data: data
  })
}

// Delete trainingtask
export function delTrainingTask(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id),
    method: 'delete'
  })
}

// starttrainingtask
export function startTraining(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id) + '/start',
    method: 'post'
  })
}

// starttrainingtask ( parameter)
export function startTrainingWithParams(id, params) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id) + '/start',
    method: 'post',
    params: params
  })
}

// trainingtask
export function stopTraining(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id) + '/stop',
    method: 'post'
  })
}

// Get traininglog
export function getTrainingLogs(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id) + '/logs',
    method: 'get'
  })
}

// Get training
export function getTrainingStatus(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id) + '/status',
    method: 'get'
  })
}

// Batch delete trainingtask
export function batchDeleteTraining(ids) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/batch',
    method: 'delete',
    data: ids
  })
}

// trainingtask
export function createTraining(data) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining',
    method: 'post',
    data: data
  })
}

// new trainingtask
export function updateTraining(data) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(data.id),
    method: 'put',
    data: data
  })
}

// Delete trainingtask
export function deleteTraining(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id),
    method: 'delete'
  })
}

// Convert model
export function convertModel(id) {
  return currentIdentityRequest({
    url: '/vlsAlgorithmTraining/' + computePathPart(id) + '/convert-model',
    method: 'post'
  })
}

export const getTrainingPublication = id => currentIdentityRequest({
  url: `/vlsAlgorithmTraining/${computePathPart(id)}/publication`, method: 'get'
})

export const retryTrainingPublication = id => currentIdentityRequest({
  url: `/vlsAlgorithmTraining/${computePathPart(id)}/publication/retry`, method: 'post'
})

export const downloadTrainingModel = (id, type) => currentIdentityRequest({
  url: `/vlsAlgorithmTraining/${computePathPart(id)}/download-model`, method: 'get',
  params: { type }, responseType: 'blob'
})
