import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

export const submitModelBatch = data => currentIdentityRequest({
  url: '/vlsModelDispatch/batches', method: 'post', data, timeout: 180000
})
export const getModelBatch = operationId => currentIdentityRequest({
  url: `/vlsModelDispatch/batches/${computePathPart(operationId)}`, method: 'get'
})
export const getRecentModelBatches = () => currentIdentityRequest({ url: '/vlsModelDispatch/batches', method: 'get' })
export const retryModelBatch = (operationId, data) => currentIdentityRequest({
  url: `/vlsModelDispatch/batches/${computePathPart(operationId)}/retry`, method: 'post', data, timeout: 180000
})
