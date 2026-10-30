import request from '@/utils/request'
import wvpRequest from '@/utils/wvpRequest'
import { authManager } from '@/utils/auth'
import { CanceledError } from 'axios'

export const cloudData = response => response?.data ?? response
const part = value => encodeURIComponent(String(value))
const guarded = (client, config) => {
    const identity = authManager.getIdentitySnapshot()
    const current = () => {
        if (!authManager.isIdentityCurrent(identity)) throw new CanceledError('登录身份已变化，请刷新页面')
    }
    current()
    return client({ ...config, silentError: true, localAuthToken: identity.token,
        transformRequest: [value => { current(); return value }, ...[].concat(client.defaults.transformRequest || [])]
    }).then(response => { current(); return cloudData(response) })
}
export const cloudAi = {
    nodes: () => guarded(request, { url: '/cloud-ai/nodes' }),
    checkNode: node => guarded(request, { url: `/vlsCompute/${node.shared ? 'nodes' : 'my-nodes'}/${part(node.id)}/${node.shared ? 'health-refresh' : 'probe'}`, method: 'post', timeout: 240000 }),
    models: nodeId => guarded(request, { url: '/cloud-ai/models', params: { nodeId } }),
    archiveModel: id => guarded(request, { url: `/cloud-ai/models/${part(id)}/archive`, method: 'post', timeout: 180000 }),
    list: (type, params) => guarded(request, { url: `/cloud-ai/${part(type)}`, params }),
    get: (type, id) => guarded(request, { url: `/cloud-ai/${part(type)}/${part(id)}` }),
    save: (type, id, data) => guarded(request, { url: `/cloud-ai/${part(type)}${id ? '/' + part(id) : ''}`, method: id ? 'put' : 'post', data }),
    remove: (type, id) => guarded(request, { url: `/cloud-ai/${part(type)}/${part(id)}`, method: 'delete' }),
    enabled: (type, id, enabled) => guarded(request, { url: `/cloud-ai/${part(type)}/${part(id)}/enabled`, method: 'put', data: { enabled } }),
    stop: id => guarded(request, { url: `/cloud-ai/runs/${part(id)}/stop`, method: 'post' }),
    devices: (protocol, params) => guarded(wvpRequest, { url: `/video/cloud-ai/devices/${part(protocol)}`, params }),
    streams: (protocol, id) => guarded(wvpRequest, { url: `/video/cloud-ai/sources/${part(protocol)}/${part(id)}` }),
    async bind(selection) {
        const identity = authManager.getIdentitySnapshot()
        const grant = await guarded(wvpRequest, { url: '/video/cloud-ai/grants', method: 'post', data: selection })
        if (!authManager.isIdentityCurrent(identity)) throw new CanceledError('登录身份已变化')
        return guarded(request, { url: '/cloud-ai/sources', method: 'post', data: { token: grant.grantToken } })
    }
}
export const cloudError = error => error?.data?.msg || error?.response?.data?.msg || error?.message || '请求未完成，请重试'
