import { deploymentStatus, deploymentTag } from './modelDispatchBatch.mjs'

// Dispatch progress and the device's running-model inventory are separate facts.
export function deviceDeploymentRows(tasks, deviceId, now = Date.now()) {
  return tasks.filter(task => String(task.deviceId) === String(deviceId)).map(task => {
    const state = String(task.dispatchStatus || '').toUpperCase()
    const expires = Number(task.downloadExpiresAt)
    const pending = !['SUCCESS', 'FAILED', 'UNKNOWN'].includes(state)
    const effectiveState = pending && expires > 0 && expires * 1000 <= now ? 'UNKNOWN' : state
    const item = { state: effectiveState }
    return {
      requestId: task.requestId,
      modelName: task.fileName || task.trainingId || task.algorithmId || '-',
      label: deploymentStatus(item),
      type: deploymentTag(item),
      reason: task.failureReason || ''
    }
  })
}
