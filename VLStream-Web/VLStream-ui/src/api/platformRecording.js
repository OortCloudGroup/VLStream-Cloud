import request from '@/utils/request'
import wvpRequest from '@/utils/wvpRequest'
import { authManager } from '@/utils/auth'
import { CanceledError } from 'axios'
import { translatePhrase } from '@/i18n'

const part = value => encodeURIComponent(String(value))
async function call(client, config) {
  const identity = authManager.getIdentitySnapshot()
  const check = () => { if (!authManager.isIdentityCurrent(identity)) throw new CanceledError('登录身份已变化') }
  check()
  const response = await client({ ...config, silentError: true, localAuthToken: identity.token,
    transformRequest: [value => { check(); return value }, ...[].concat(client.defaults.transformRequest || [])] })
  check()
  return response?.data ?? response
}
export const platformRecording = {
  runtime: () => call(request, { url: '/platform-recordings/runtime' }),
  sources: (protocol, rowId) => call(request, { url: '/platform-recordings/sources', params: { protocol, rowId } }),
  choices: (protocol, rowId) => call(wvpRequest, { url: `/video/cloud-ai/sources/${part(protocol)}/${part(rowId)}` }),
  async bind(selection) {
    const identity = authManager.getIdentitySnapshot()
    const grant = await call(wvpRequest, { url: '/video/cloud-ai/grants', method: 'post', data: selection })
    if (!authManager.isIdentityCurrent(identity)) throw new CanceledError('登录身份已变化')
    return call(request, { url: '/platform-recordings/sources', method: 'post', data: { token: grant.grantToken } })
  },
  manual: (id, enabled) => call(request, { url: `/platform-recordings/sources/${part(id)}/manual`, method: 'put', data: { enabled } }),
  plan: (id, plan) => call(request, { url: `/platform-recordings/sources/${part(id)}/plan`, method: 'put', data: plan }),
  segments: (id, date) => call(request, { url: `/platform-recordings/sources/${part(id)}/segments`, params: { date } }),
  file: id => call(request, { url: `/platform-recordings/segments/${part(id)}/file`, responseType: 'blob', timeout: 120000 }),
}
export const recordingError = error => error?.response?.status === 404 ? translatePhrase('后台录像接口尚未就绪，请更新并重启本地后端') : error?.response?.data?.msg || error?.data?.msg || error?.message || translatePhrase('请求失败')
export const recordingState = row => ({ IDLE: '未录制', STARTING: '正在连接', RECORDING: '正在录制', STOPPING: '正在停止', RETRYING: '录制异常，等待重试', WAITING: '等待录像资源', UNKNOWN: '录像状态待确认' }[row?.state] || '未录制')
