import request from '@/utils/request'

const base = '/vlsData/projects'
const call = async (url, method = 'get', data, params) => {
  const result = await request({ url: base + url, method, data, params })
  if (result.code !== 200 || result.success === false) throw new Error(result.msg || '操作失败')
  return result.data
}

export const listDataProjects = params => call('', 'get', undefined, params)
export const getDataProject = id => call(`/${id}`)
export const saveDataProject = (id, data) => call(id ? `/${id}` : '', id ? 'put' : 'post', data)
export const listDataSamples = (id, params) => call(`/${id}/samples`, 'get', undefined, params)
export const getDataSample = (id, sampleId) => call(`/${id}/samples/${sampleId}`)
export const editDataSample = (id, sampleId, data) => call(`/${id}/samples/${sampleId}`, 'put', data)
export const batchDataSamples = (id, data) => call(`/${id}/samples/batch`, 'post', data)
export const checkDataSamples = (id, ids) => call(`/${id}/samples/check`, 'post', ids)
export const reviewDataSamples = (id, count) => call(`/${id}/samples/review`, 'get', undefined, { count })
export const dataStatistics = id => call(`/${id}/statistics`)
export const splitDataSamples = (id, data) => call(`/${id}/split`, 'post', data)
export const listDataVersions = id => call(`/${id}/versions`)
export const saveDataVersion = (id, data) => call(`/${id}/versions`, 'post', data)
export const getDataVersion = (id, versionId) => call(`/${id}/versions/${versionId}`)
export const compareDataVersion = (id, versionId, to) => call(`/${id}/versions/${versionId}/compare`, 'get', undefined, { to })
export const restoreDataVersion = (id, versionId) => call(`/${id}/versions/${versionId}/restore`, 'post')
export const importDataSamples = async (id, files, source, archive) => {
  const form = new FormData()
  files.forEach(file => form.append(archive ? 'file' : 'files', file))
  form.append('source', source)
  const result = await request({ url: `${base}/${id}/samples/${archive ? 'import-archive' : 'import'}`, method: 'post', data: form, headers: { 'Content-Type': 'multipart/form-data' } })
  if (result.code !== 200 || result.success === false) throw new Error(result.msg || '导入失败')
  return result.data
}
export const exportDataSamples = (id, params, versionId) => request({ url: `${base}/${id}/${versionId ? `versions/${versionId}/export` : 'samples/export'}`, method: 'get', params, responseType: 'blob' })
export const publishDataTraining = id => request({ url: `/vlsAlgorithmAnnotation/${id}/save-dataset`, method: 'post' })
