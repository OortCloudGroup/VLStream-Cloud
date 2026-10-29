import { deploymentStatus, deploymentTag } from './modelDispatchBatch.mjs'

// Dispatch progress and the device's running-model inventory are separate facts.
export function deviceDeploymentRows(tasks, deviceId, now = Date.now()) {
  const activeStates = new Set(['QUEUED', 'CHECKING', 'CREATED', 'PENDING', 'PUBLISHED', 'RECEIVED', 'DOWNLOADING', 'DOWNLOADED', 'VERIFYING', 'DEPLOYING'])
  return tasks.filter(task => String(task.deviceId) === String(deviceId)
    && activeStates.has(String(task.dispatchStatus || '').toUpperCase())
    && !(Number(task.downloadExpiresAt) > 0 && Number(task.downloadExpiresAt) * 1000 <= now)).map(task => {
    const state = String(task.dispatchStatus || '').toUpperCase()
    const item = { state }
    return {
      requestId: task.requestId,
      modelName: task.fileName || task.trainingId || task.algorithmId || '-',
      label: deploymentStatus(item),
      type: deploymentTag(item),
      reason: task.failureReason || ''
    }
  })
}
