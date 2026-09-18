import request from '@/utils/request'

export const getDeviceModelCandidates = params => request({
  url: '/vlsModelDispatch/models', method: 'get', params
})

export const dispatchSelectedModel = (modelId, deviceId) => request({
  url: `/vlsModelDispatch/models/${encodeURIComponent(modelId)}/dispatch`,
  method: 'post', params: { deviceId }, timeout: 120000
})
