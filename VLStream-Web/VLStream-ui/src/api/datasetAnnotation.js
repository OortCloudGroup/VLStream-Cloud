import request from '@/utils/request'

const call = async (datasetId, sampleId, method = 'get', data) => {
  const response = await request({ url: `/vlsData/projects/${datasetId}/samples/${sampleId}/annotation`, method, data })
  if (response.code !== 200 || response.success === false) throw new Error(response.msg || '标注操作失败')
  return response.data
}

export const getDatasetAnnotation = (datasetId, sampleId) => call(datasetId, sampleId)
export const saveDatasetAnnotation = (datasetId, sampleId, data) => call(datasetId, sampleId, 'put', data)
