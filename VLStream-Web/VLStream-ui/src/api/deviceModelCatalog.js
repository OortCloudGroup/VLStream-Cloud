import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

export const getDeviceModelCandidates = params => currentIdentityRequest({
  url: '/vlsModelDispatch/models', method: 'get', params
})

export const dispatchSelectedModel = (modelId, deviceId) => currentIdentityRequest({
  url: `/vlsModelDispatch/models/${computePathPart(modelId)}/dispatch`,
  method: 'post', params: { deviceId }, timeout: 120000
})
