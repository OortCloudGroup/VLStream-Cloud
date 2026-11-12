/* SPDX-License-Identifier: MIT */
import request from '@/utils/request'
import { CanceledError } from 'axios'
import { authManager } from '@/utils/auth'
import { cancelOortCloudLoginIntent, getOortCloudAuthorizationRevision } from '@/utils/oortCloudLoginIntent.mjs'

let authorizationMutation = Promise.resolve()
function enqueueAuthorization(action) {
  const pending = authorizationMutation.then(action, action)
  authorizationMutation = pending.catch(() => {})
  return pending
}

export function getLlmProviders() {
  return request({ url: '/vlsLlmReview/providers', method: 'get' })
}

export function getOortCloudLlmAuthorization() {
  return request({ url: '/vlsLlmReview/oortcloud-authorization', method: 'get', silentError: true })
}

export function revokeOortCloudLlm() {
  cancelOortCloudLoginIntent()
  const identity = authManager.getIdentitySnapshot()
  // If a login request already reached the server, cancel after it completes.
  return enqueueAuthorization(() => requestForIdentity({ url: '/vlsLlmReview/oortcloud-authorization', method: 'delete' }, identity))
}

export function selectOortCloudLlmModel(modelName) {
  return requestForIdentity({ url: '/vlsLlmReview/oortcloud-model', method: 'put', data: { modelName } }, authManager.getIdentitySnapshot())
}

export function getLlmProviderModels(id) {
  return request({ url: `/vlsLlmReview/providers/${id}/models`, method: 'get', silentError: true })
}

export function authorizeOortCloudLlm(data, identity, isCurrent = () => true) {
  if (!identity?.token || !authManager.isIdentityCurrent(identity) || !isCurrent()) {
    throw new CanceledError('视觉复核授权账户已变化，请重新确认')
  }
  const revision = getOortCloudAuthorizationRevision()
  return enqueueAuthorization(() => requestForIdentity({ url: '/vlsLlmReview/oortcloud-authorization', method: 'post', data }, identity,
    () => revision === getOortCloudAuthorizationRevision() && isCurrent()))
}

function requestForIdentity(config, identity, isCurrent = () => true) {
  const snapshot = { ...identity }
  const requireCurrentIdentity = () => {
    if (!snapshot.token || !authManager.isIdentityCurrent(snapshot) || !isCurrent()) {
      throw new CanceledError('视觉复核授权账户已变化，请重新确认')
    }
  }
  requireCurrentIdentity()
  return request({
    ...config,
    silentError: true,
    localAuthToken: snapshot.token,
    platformAuthToken: snapshot.platformToken || '',
    // Axios transforms run after async request interceptors and before transport.
    // Recheck here so a late account/tenant switch cannot attach the old key to a new identity.
    transformRequest: [value => { requireCurrentIdentity(); return value }, ...[].concat(request.defaults.transformRequest || [])]
  })
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
