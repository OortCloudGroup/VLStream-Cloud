import { currentIdentityRequest, computePathPart } from '@/utils/computeIdentityRequest'

const call = (url, method = 'get', data) => currentIdentityRequest({ url: `/vlsCompute/billing${url}`, method, data })
export const getComputeBillingCapabilities = () => call('/capabilities')
// Explicit user action: the trusted platform can grant continuing billing scopes on this POST.
export const checkComputeBillingAccount = () => call('/account/check', 'post', { acceptBillingPermissions: true })
export const quoteComputePaidRun = nodeId => call(`/nodes/${computePathPart(nodeId)}/quote`, 'post')
export const listComputePaidRuns = () => call('/runs')
export const acceptComputePaidRun = runId => call(`/runs/${computePathPart(runId)}/accept`, 'post', { acceptBillingPermissions: true })
export const stopComputePaidRun = runId => call(`/runs/${computePathPart(runId)}/stop`, 'post')
