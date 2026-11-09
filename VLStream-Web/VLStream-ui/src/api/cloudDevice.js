import request from '@/utils/request'
import wvpRequest from '@/utils/wvpRequest'
import { authManager } from '@/utils/auth'
import { CanceledError } from 'axios'
const call = async (client, config) => {
  const identity = authManager.getIdentitySnapshot()
  const check = () => { if (!authManager.isIdentityCurrent(identity)) throw new CanceledError('登录身份已变化') }
  check()
  const result = await client({ ...config, localAuthToken: identity.token, silentError: true,
    transformRequest: [value => { check(); return value }, ...[].concat(client.defaults.transformRequest || [])] })
  check(); return result?.data ?? result
}
export const cloudDevice = {
  nodes: () => call(request, { url: '/cloud-device/nodes' }),
  models: nodeId => call(request, { url: '/cloud-device/models', params: { nodeId } }),
  list: params => call(request, { url: '/cloud-device/bindings', params }),
  save: data => call(request, { url: '/cloud-device/bindings', method: 'post', data }),
  toggle: row => call(request, { url: `/cloud-device/bindings/${encodeURIComponent(row.id)}/enabled`, method: 'put', data: { revision: row.revision, enabled: !row.enabled } }),
  async bind(selection) {
    const identity = authManager.getIdentitySnapshot()
    const grant = await call(wvpRequest, { url: '/video/cloud-ai/grants', method: 'post', data: selection })
    if (!authManager.isIdentityCurrent(identity)) throw new CanceledError('登录身份已变化')
    return call(request, { url: '/cloud-device/sources', method: 'post', data: { token: grant.grantToken } })
  }
}
