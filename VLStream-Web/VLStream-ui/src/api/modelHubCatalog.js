import request from '@/utils/request'

function checked(response) {
  if (Number(response?.code) !== 200) throw new Error(response?.msg || response?.message || 'Model Hub 请求失败')
  return response.data
}
const headers = session => ({ 'X-Model-Hub-Token': session.accessToken, 'X-Model-Hub-Tenant': session.tenantId || '' })
export const getPublicHubModels = params => request({ url: '/vlsModelHub/models', method: 'get', params }).then(checked)
export const getHubCategories = () => request({ url: '/vlsModelHub/categories', method: 'post' }).then(checked)
export const getHubFiles = data => request({ url: '/vlsModelHub/files', method: 'post', data }).then(checked)
export async function downloadHubFile(data, session) {
  const blob = await request({ url: '/vlsModelHub/download', method: 'post', data, headers: headers(session), responseType: 'blob', timeout: 120000 })
  if (!(blob instanceof Blob)) throw new Error('模型下载响应格式不正确')
  if (blob.type.includes('json')) {
    const result = JSON.parse(await blob.text())
    throw new Error(result.msg || result.message || '模型下载失败')
  }
  return blob
}
