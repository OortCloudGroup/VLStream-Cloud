import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

export const getDeviceModelCandidates = params => currentIdentityRequest({
  url: '/vlsModelDispatch/models', method: 'get', params
})

export const getDeviceModelTasks = deviceId => currentIdentityRequest({
  url: '/vlsModelDispatch/tasks', method: 'get', params: { deviceId, limit: 20 }
})

export const dispatchSelectedModel = (modelId, deviceId) => currentIdentityRequest({
  url: `/vlsModelDispatch/models/${computePathPart(modelId)}/dispatch`,
  method: 'post', params: { deviceId }, timeout: 120000
})

export const submitSavedModelAttempt = (modelId, deviceId, operationId) => currentIdentityRequest({
  url: `/vlsModelDispatch/models/${computePathPart(modelId)}/attempt`,
  method: 'post', params: { deviceId, operationId }, timeout: 120000
})

export const getSavedModelAttempt = (modelId, deviceId, operationId) => currentIdentityRequest({
  url: `/vlsModelDispatch/models/${computePathPart(modelId)}/attempt`,
  method: 'get', params: { deviceId, operationId }
})
