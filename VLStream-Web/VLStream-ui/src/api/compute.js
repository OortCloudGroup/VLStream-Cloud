import request from '@/utils/request'
import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

export const listComputeNodes = () => currentIdentityRequest({ url: '/vlsCompute/nodes', method: 'get' })
export const getComputeNode = id => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}`, method: 'get' })
const platformProof = accessToken => ({ requirePlatformIdentity: true, platformAuthToken: accessToken || '', sensitiveData: true, silentError: true })
export const listComputeMarket = () => currentIdentityRequest({ url: '/vlsCompute/market', method: 'get' })
export const getComputeAuthorizationMode = () => request({ url: '/sso/v1/mode', method: 'get', sensitiveData: true, silentError: true })
export const checkComputePlatformAuthorization = accessToken => request({ url: '/vlsCompute/platform-authorization', method: 'get', ...platformProof(accessToken) })
export const refreshComputeHealth = (id, platformToken) => platformToken === undefined
  ? currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/health-refresh`, method: 'post', timeout: 240000 })
  : request({ url: `/vlsCompute/nodes/${computePathPart(id)}/health-refresh`, method: 'post', timeout: 240000, ...platformProof(platformToken) })
export const listMyTrainingTasks = params => currentIdentityRequest({ url: '/vlsCompute/my-trainings', method: 'get', params })
export const recordComputeObservation = (id, data) => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/observation`, method: 'put', data })
export const listComputeSelection = () => currentIdentityRequest({ url: '/vlsCompute/selection', method: 'get' })
export const getComputeQueueSnapshot = id => currentIdentityRequest({ url: `/vlsCompute/selection/${computePathPart(id)}/queue`, method: 'get', timeout: 35000, silentError: true })
export const publishComputeOffer = (id, data) => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/offer`, method: 'put', data })
export const requestComputePower = (id, action, platformToken) => platformToken === undefined ? currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/power/${computePathPart(action)}`, method: 'post' }) : request({ url: `/vlsCompute/nodes/${computePathPart(id)}/power/${computePathPart(action)}`, method: 'post', ...platformProof(platformToken) })
export const claimComputeInstance = (id, platformToken) => request({ url: `/vlsCompute/nodes/${computePathPart(id)}/claim`, method: 'post', ...platformProof(platformToken) })
export const releaseComputeClaim = (id, platformToken) => request({ url: `/vlsCompute/nodes/${computePathPart(id)}/release-claim`, method: 'post', ...platformProof(platformToken) })
export const requestPublicComputePower = (id, action, platformToken) => request({ url: `/vlsCompute/public/nodes/${computePathPart(id)}/power/${computePathPart(action)}`, method: 'post', ...platformProof(platformToken) })
export const savePublicComputeIdentity = (id, data) => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/public-identity`, method: 'put', data, sensitiveData: true })
export const saveComputeProviderSettings = (id, data) => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/provider-settings`, method: 'put', data, sensitiveData: true })
export const refreshComputeProviderState = (id, recover = false) => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/provider-state`, method: 'post', params: { recover } })
export const refreshPublicComputeProviderState = (id, recover, platformToken) => request({ url: `/vlsCompute/public/nodes/${computePathPart(id)}/provider-state`, method: 'post', params: { recover }, ...platformProof(platformToken) })
export const queryComputeCostObservation = id => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/cost-observation`, method: 'post' })
export const saveComputeNode = (id, data) => currentIdentityRequest({
  url: `/vlsCompute/nodes${id ? `/${computePathPart(id)}` : ''}`,
  method: id ? 'put' : 'post', data, sensitiveData: true
})
export const probeComputeNode = id => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/probe`, method: 'post' })
export const deleteComputeNode = id => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}`, method: 'delete' })
export const saveOwnComputeNode = (id, data) => currentIdentityRequest({
  url: `/vlsCompute/my-nodes${id ? `/${computePathPart(id)}` : ''}`,
  method: id ? 'put' : 'post', data, sensitiveData: true
})
export const probeOwnComputeNode = id => currentIdentityRequest({ url: `/vlsCompute/my-nodes/${computePathPart(id)}/probe`, method: 'post' })
export const deleteOwnComputeNode = id => currentIdentityRequest({ url: `/vlsCompute/my-nodes/${computePathPart(id)}`, method: 'delete' })
export const startCloudTraining = (id, data, platformToken) => platformToken === undefined ? currentIdentityRequest({ url: `/vlsCompute/trainings/${computePathPart(id)}/start`, method: 'post', data }) : request({ url: `/vlsCompute/trainings/${computePathPart(id)}/start`, method: 'post', data, ...platformProof(platformToken) })
export const listProviderInventory = id => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/provider-inventory`, method: 'get', sensitiveData: true, silentError: true })
export const syncProviderInventory = id => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/provider-inventory/sync`, method: 'post', sensitiveData: true, silentError: true })
export const refreshProviderInventory = (id, key) => currentIdentityRequest({ url: `/vlsCompute/nodes/${computePathPart(id)}/provider-inventory/${computePathPart(key)}/refresh`, method: 'post', sensitiveData: true, silentError: true })
