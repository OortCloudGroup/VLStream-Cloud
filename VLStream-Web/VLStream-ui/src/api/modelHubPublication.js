import request from '@/utils/request'
import { captureComputeIdentity, computePathPart } from '@/utils/computeIdentityRequest'

function publicationError(data, fallback) {
  const error = new Error(data?.msg || data?.message || fallback || '开发广场操作失败')
  const code = Number(data?.code)
  if (Number.isFinite(code)) error.code = code
  return error
}

async function call(kind, id, suffix = '', method = 'get', data, session) {
  if (!['model', 'dataset'].includes(kind)) throw new Error('资源类型无效')
  const identity = captureComputeIdentity()
  identity.assertCurrent()
  let response
  try {
    response = await request({
      url: `/vlsModelHub/publications/${kind}/${computePathPart(id)}${suffix}`,
      method, data, sensitiveData: true, silentError: true, timeout: 180000,
      headers: session ? { 'X-Model-Hub-Token': session.accessToken, 'X-Model-Hub-Tenant': session.tenantId || '' } : {}
    })
    identity.assertCurrent()
  } catch (error) {
    identity.assertCurrent()
    throw publicationError(error?.data || error?.response?.data, error?.message)
  }
  if (Number(response?.code) !== 200 || response?.success === false) throw publicationError(response)

  return response.data
}
export const getPublication = (kind, id) => call(kind, id)
export const setPublicationFeatured = (kind, id, featured) => call(kind, id, '/featured', 'put', { featured })
export const publishToModelHub = (kind, id, data, session) => call(kind, id, '/publish', 'post', data, session)

export const reconcilePublication = (kind, id, session) => call(kind, id, '/reconcile', 'post', {}, session)
export const resumePublication = (kind, id, session) => call(kind, id, '/resume', 'post', {}, session)
