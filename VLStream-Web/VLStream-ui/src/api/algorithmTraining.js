/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

// Query trainingtask
export function getTrainingPage(params) {
  return request({
    url: '/vlsAlgorithmTraining/page',
    method: 'get',
    params: params
  })
}

// Query trainingtask
export function getTrainingTask(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id,
    method: 'get'
  })
}

// Add trainingtask
export function addTrainingTask(data) {
  return request({
    url: '/vlsAlgorithmTraining',
    method: 'post',
    data: data
  })
}

// Update trainingtask
export function updateTrainingTask(data) {
  return request({
    url: '/vlsAlgorithmTraining/' + data.id,
    method: 'put',
    data: data
  })
}

// Delete trainingtask
export function delTrainingTask(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id,
    method: 'delete'
  })
}

// starttrainingtask
export function startTraining(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/start',
    method: 'post'
  })
}

// starttrainingtask ( parameter)
export function startTrainingWithParams(id, params) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/start',
    method: 'post',
    params: params
  })
}

// trainingtask
export function stopTraining(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/stop',
    method: 'post'
  })
}

// Get traininglog
export function getTrainingLogs(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/logs',
    method: 'get'
  })
}

// Get training
export function getTrainingStatus(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/status',
    method: 'get'
  })
}

// Batch delete trainingtask
export function batchDeleteTraining(ids) {
  return request({
    url: '/vlsAlgorithmTraining/batch',
    method: 'delete',
    data: ids
  })
}

// trainingtask
export function createTraining(data) {
  return request({
    url: '/vlsAlgorithmTraining',
    method: 'post',
    data: data
  })
}

// new trainingtask
export function updateTraining(data) {
  return request({
    url: '/vlsAlgorithmTraining/' + data.id,
    method: 'put',
    data: data
  })
}

// Delete trainingtask
export function deleteTraining(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id,
    method: 'delete'
  })
}

// Convert model
export function convertModel(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/convert-model',
    method: 'post'
  })
}