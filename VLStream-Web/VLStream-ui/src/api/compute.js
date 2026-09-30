import request from '@/utils/request'

export const listComputeNodes = () => request({ url: '/vlsCompute/nodes', method: 'get' })
export const saveComputeNode = (id, data) => request({
  url: `/vlsCompute/nodes${id ? `/${id}` : ''}`,
  method: id ? 'put' : 'post', data, sensitiveData: true
})
export const probeComputeNode = id => request({ url: `/vlsCompute/nodes/${id}/probe`, method: 'post' })
export const deleteComputeNode = id => request({ url: `/vlsCompute/nodes/${id}`, method: 'delete' })
export const startCloudTraining = (id, data) => request({ url: `/vlsCompute/trainings/${id}/start`, method: 'post', data })
