import test from 'node:test'
import assert from 'node:assert/strict'
import { loadSafetyRuns, stopSafetyRun } from '../src/utils/computeSafety.mjs'

test('safety list accepts only valid string IDs and projects minimal fields', async () => {
  const identity = { assertCurrent() {} }
  const rows = await loadSafetyRuns(async () => ({ code: 200, data: [{ runId: '9007199254740993', nodeId: 'n', state: 'RUNNING', secret: 'hidden' }] }), identity)
  assert.deepEqual(rows, [{ runId: '9007199254740993', nodeId: 'n', state: 'RUNNING' }])
  await assert.rejects(loadSafetyRuns(async () => ({ code: 200, data: [{ runId: 9007199254740993, nodeId: 'n', state: 'RUNNING' }] }), identity))
  await assert.rejects(loadSafetyRuns(async () => ({ code: 200, data: [{ runId: '1', nodeId: '', state: 'RUNNING' }] }), identity))
  await assert.rejects(loadSafetyRuns(async () => ({ code: 500, data: [] }), identity))
})

test('stop requires confirmation, retains the listed identity and does not claim a failed request succeeded', async () => {
  let valid = true
  let calls = 0
  const identity = { assertCurrent() { if (!valid) throw new Error('identity changed') } }
  const row = { runId: '9007199254740993' }
  await assert.rejects(stopSafetyRun(row, identity, async () => { throw 'cancel' }, async () => { calls++; return { code: 200 } }))
  await assert.rejects(stopSafetyRun(row, identity, async () => { valid = false }, async () => { calls++; return { code: 200 } }))
  assert.equal(calls, 0)
  valid = true
  await assert.rejects(stopSafetyRun(row, identity, async () => {}, async id => { calls++; assert.equal(id, row.runId); return { code: 500 } }))
  await assert.rejects(stopSafetyRun(row, identity, async () => {}, async () => ({ code: 200, data: false })))
  await stopSafetyRun(row, identity, async () => {}, async id => { calls++; assert.equal(id, row.runId); return { code: 200, data: true } })
  assert.equal(calls, 2)
})

test('a late list or stop response cannot succeed after an identity change', async () => {
  let valid = true
  const identity = { assertCurrent() { if (!valid) throw new Error('identity changed') } }
  await assert.rejects(loadSafetyRuns(async () => { valid = false; return { code: 200, data: [] } }, identity))
  valid = true
  await assert.rejects(stopSafetyRun({ runId: '1' }, identity, async () => {}, async () => { valid = false; return { code: 200 } }))
})
