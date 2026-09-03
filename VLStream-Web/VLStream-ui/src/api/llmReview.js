/* SPDX-License-Identifier: MIT */
import request from '@/utils/request'

export function getLlmProviders() {
  return request({ url: '/vlsLlmReview/providers', method: 'get' })
}

export function createLlmProvider(data) {
  return request({ url: '/vlsLlmReview/providers', method: 'post', data })
}

export function updateLlmProvider(id, data) {
  return request({ url: `/vlsLlmReview/providers/${id}`, method: 'put', data })
}

export function deleteLlmProvider(id) {
  return request({ url: `/vlsLlmReview/providers/${id}`, method: 'delete' })
}

export function testLlmProvider(id, data) {
  return request({ url: `/vlsLlmReview/providers/${id}/test`, method: 'post', data })
}

export function getAlgorithmLlmReviewConfig(algorithmId) {
  return request({ url: `/vlsLlmReview/algorithms/${algorithmId}/config`, method: 'get' })
}

export function saveAlgorithmLlmReviewConfig(algorithmId, data) {
  return request({ url: `/vlsLlmReview/algorithms/${algorithmId}/config`, method: 'put', data })
}

export function getLlmReviewTasks(params) {
  return request({ url: '/vlsLlmReview/tasks', method: 'get', params })
}

export function getLlmReviewTask(id) {
  return request({ url: `/vlsLlmReview/tasks/${id}`, method: 'get' })
}

export function submitLlmManualDecision(id, decision) {
  return request({
    url: `/vlsLlmReview/tasks/${id}/manual-decision`,
    method: 'post',
    data: { decision }
  })
}

export function getEventMediaViewUrl(mediaId) {
  return request({ url: `/vlsDeviceMedia/${mediaId}/view-url`, method: 'get', params: { seconds: 300 } })
}
