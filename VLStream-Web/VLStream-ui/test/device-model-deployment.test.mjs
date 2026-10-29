import test from 'node:test'
import assert from 'node:assert/strict'
import { deviceDeploymentRows } from '../src/utils/deviceModelDeployment.mjs'
import { deploymentStatus, deploymentTag } from '../src/utils/modelDispatchBatch.mjs'

test('device deployment status is scoped to the selected device and expires to unknown', () => {
  const now = 1_800_000_000_000
  const rows = deviceDeploymentRows([
    { requestId:'pending', deviceId:17, fileName:'model-a', dispatchStatus:'PUBLISHED', downloadExpiresAt:Math.floor(now / 1000) + 60 },
    { requestId:'expired', deviceId:'17', trainingId:'training-b', dispatchStatus:'DOWNLOADED', downloadExpiresAt:Math.floor(now / 1000) - 1 },
    { requestId:'other-device', deviceId:18, fileName:'model-c', dispatchStatus:'SUCCESS' }
  ], '17', now)

  assert.equal(rows.length, 1)
  assert.equal(rows[0].requestId, 'pending')
  assert.equal(rows[0].modelName, 'model-a')
  assert.equal(rows[0].label, deploymentStatus({ state:'PUBLISHED' }))
  assert.equal(rows[0].type, deploymentTag({ state:'PUBLISHED' }))
  assert.equal(rows.some(row => row.requestId === 'expired'), false)
})
