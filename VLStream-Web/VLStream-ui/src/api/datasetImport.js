import request from '@/utils/request'

const call = async (url, method = 'get', data, params, extra = {}) => {
  const result = await request({ url: `/vlsData${url}`, method, data, params, sensitiveData: url.startsWith('/sources'), ...extra })
  if (result.code !== 200 || result.success === false) throw new Error(result.msg || '操作失败')
  return result.data
}
export const importCapabilities = () => call('/import-capabilities')
export const beginDatasetUpload = data => call('/uploads', 'post', data)
export const uploadDatasetPart = (id, part, file, sha256, signal, onUploadProgress) => {
  const data = new FormData(); data.append('file', file); data.append('sha256', sha256)
  return call(`/uploads/${id}/parts/${part}`, 'put', data, undefined, { headers: { 'Content-Type': 'multipart/form-data' }, signal, timeout: 120000, onUploadProgress })
}
export const completeDatasetUpload = id => call(`/uploads/${id}/complete`, 'post')
export const listDatasetImports = datasetId => call('/imports', 'get', undefined, { datasetId })
export const retryDatasetImport = id => call(`/imports/${id}/retry`, 'post')
export const cancelDatasetImport = id => call(`/uploads/${id}`, 'delete')
export const listDatasetSources = params => call('/sources', 'get', undefined, params)
export const saveDatasetSource = (id, data) => call(id ? `/sources/${id}` : '/sources', id ? 'put' : 'post', data)
export const deleteDatasetSource = id => call(`/sources/${id}`, 'delete')
export const browseDatasetSource = (id, path, token) => call(`/sources/${id}/files`, 'get', undefined, { path, token })
export const importRemoteDataset = data => call('/imports/remote', 'post', data)
export const extractVideoFrames = data => call('/video-frames', 'post', data)
export const getVideoFrameOrigins = (datasetId, sampleId) => call(`/datasets/${datasetId}/samples/${sampleId}/video-origins`)
