import { currentIdentityRequest, computePathPart, captureComputeIdentity } from '@/utils/computeIdentityRequest'
import axios from 'axios'

const call = (url, method = 'get', data) => {
  const identity = captureComputeIdentity()
  return currentIdentityRequest({
    url: `/vlsCompute/billing${url}`, method, data, sensitiveData: true, silentError: true,
    // The shared identity helper pins both tokens. Check again after asynchronous
    // Axios interceptors, before transport can combine them with a changed tenant.
    transformRequest: [value => {
      try { identity.assertCurrent() } catch { throw new axios.CanceledError('计费身份已变化，请重新确认') }
      return value
    }, ...[].concat(axios.defaults.transformRequest || [])]
  })
}
export const getComputeBillingCapabilities = () => call('/capabilities')
// Explicit user action: the trusted platform can grant continuing billing scopes on this POST.
export const checkComputeBillingAccount = () => call('/account/check', 'post', { acceptBillingPermissions: true })
export const quoteComputePaidRun = nodeId => call(`/nodes/${computePathPart(nodeId)}/quote`, 'post')
export const quoteComputeArchiveRecovery = sourceRunId => call(`/runs/${computePathPart(sourceRunId)}/archive-recovery-quote`, 'post')
export const listComputePaidRuns = () => call('/runs')
export const acceptComputePaidRun = runId => call(`/runs/${computePathPart(runId)}/accept`, 'post', { acceptBillingPermissions: true })
export const stopComputePaidRun = runId => call(`/runs/${computePathPart(runId)}/stop`, 'post')
