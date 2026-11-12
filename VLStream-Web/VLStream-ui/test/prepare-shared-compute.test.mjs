import { test } from 'node:test'
import assert from 'node:assert/strict'
import { prepareSharedCompute } from '../src/utils/prepareSharedCompute.mjs'

const shared = { id: 'free', billingType: 'FREE', schedulingMode: 'SHARED_GPU_QUEUE', canRefreshEnvironment: true, healthState: 'UNKNOWN' }
test('prepares missing free checks without touching ready, paid, private or pending instances', async () => {
  const calls = []
  await prepareSharedCompute([shared, { ...shared, id: 'ready', healthState: 'READY' },
    { ...shared, id: 'paid', billingType: 'POINTS' }, { ...shared, id: 'private', schedulingMode: 'EXCLUSIVE' },
    { ...shared, id: 'pending', operationPending: true }, { ...shared, id: 'denied', canRefreshEnvironment: false }], async id => calls.push(id))
  assert.deepEqual(calls, ['free'])
})
test('one failing host does not discard successful preparation of another host', async () => {
  const calls = []
  assert.equal(await prepareSharedCompute([shared, { ...shared, id: 'other' }], async id => {
    calls.push(id)
    if (id === 'free') throw new Error('offline')
  }), true)
  assert.deepEqual(calls, ['free', 'other'])
  assert.equal(await prepareSharedCompute([], () => assert.fail()), false)
})
