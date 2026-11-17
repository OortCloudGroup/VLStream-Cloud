export const pendingPublication = state => state?.receiptState !== 'FAILED' && (state?.receiptState === 'PROCESSING' || ['PROCESSING', 'PUBLISHING', 'UPLOADING', 'UNCERTAIN', 'UNKNOWN'].includes(state?.publishState))
export const confirmedPublication = state => state?.publishState === 'PUBLISHED' && (!state.receiptState || state.receiptState === 'PUBLISHED')
export const uploadAllowed = state => state?.publishable === true && (['DRAFT', 'CHANGED'].includes(state.publishState) || (state.publishState === 'FAILED' && state.retryAllowed === true))
export const publicationPollDelay = (attempt, hidden = false) => hidden ? 120000 : Math.min(30000, 5000 * (2 ** Math.min(Math.max(attempt, 0), 3)))
export function publicationLabel(state, reconnect = false, submitting = false) {
  if (reconnect) return '重新连接'
  if (state?.receiptState === 'FAILED' || ['FAILED', 'REMOTE_FAILED'].includes(state?.publishState)) return '发布失败'
  if (submitting || ['PROCESSING','PUBLISHING','UPLOADING'].includes(state?.publishState)) return '发布中'
  if (confirmedPublication(state)) return '已发布'
  if (['UNCERTAIN','UNKNOWN'].includes(state?.publishState)) return '核实状态'
  return '发布'
}
export const resumeAllowed = state => state?.resumeAllowed === true && state?.receiptState === 'NOT_FOUND' && ['UNCERTAIN', 'UNKNOWN'].includes(state?.publishState)
