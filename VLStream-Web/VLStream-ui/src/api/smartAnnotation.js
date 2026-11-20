import request from '@/utils/request'

const call = async (path = '', method = 'get', data, params, signal) => {
  const response = await request({ url: `/vlsData/smart-annotation${path}`, method, data, params, signal })
  if (response.code !== 200 || response.success === false) throw new Error(response.msg || '智能标注操作失败')
  return response.data
}

export const listSmartTasks = params => call('', 'get', undefined, params)
export const smartOptions = (datasetId, forActiveLearning = false) =>
  call(`/options/${datasetId}`, 'get', undefined, forActiveLearning ? { forActiveLearning: true } : undefined)
export const createSmartTask = data => call('', 'post', data)
export const getSmartTask = id => call(`/${id}`)
export const smartPredictions = (id, params, signal) => call(`/${id}/predictions`, 'get', undefined, params, signal)
export const getSmartPrediction = (id, candidateId, signal) => call(`/${id}/predictions/${candidateId}`, 'get', undefined, undefined, signal)
export async function fetchSmartPredictionImage(id, candidateId, signal) {
  const blob = await request({
    url: `/vlsData/smart-annotation/${id}/predictions/${candidateId}/content`,
    method: 'get',
    responseType: 'blob',
    signal
  })
  if (!(blob instanceof Blob) || !blob.size || blob.type.includes('json')) {
    throw new Error('图片读取失败')
  }
  return blob
}
export const reviewSmartPrediction = (id, candidateId, data) => call(`/${id}/predictions/${candidateId}`, 'post', data)
export const confirmSmartBatch = (id, ids) => call(`/${id}/confirm`, 'post', { ids })
export const confirmAllSmartPredictions = id => call(`/${id}/confirm-all`, 'post')
export const nextSmartRound = id => call(`/${id}/next`, 'post')
export const finishSmartTask = id => call(`/${id}/finish`, 'post')
export const retrySmartTask = id => call(`/${id}/retry`, 'post')
export const cancelSmartTask = id => call(`/${id}/cancel`, 'post')
export const smartTaskLogs = id => call(`/${id}/logs`)
