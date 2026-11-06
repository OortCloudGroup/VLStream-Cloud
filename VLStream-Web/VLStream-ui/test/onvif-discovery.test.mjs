import test from 'node:test'
import assert from 'node:assert/strict'
import { discoveryRows, authenticateRow, invalidateAuthentication, canRegister, registerRows } from '../src/utils/onvifDiscovery.mjs'

const found = [{ ip: '192.0.2.11', hostName: 'http://192.0.2.11/onvif/device_service' }, { ip: '192.0.2.12' }]
const response = { code: 200, data: { streamUris: { main: 'rtsp://192.0.2.12/main' } } }
const account = row => Object.assign(row, { username: 'fixture', password: 'fixture-password' })
const defer = () => { let resolve; const promise = new Promise(done => { resolve = done }); return { promise, resolve } }

test('rediscovery hydrates authorized existing devices, normalizes endpoints and deduplicates discoveries', () => {
  const rows = discoveryRows([...found, found[0]], [{ id: 29, ip: '192.0.2.11:80', name: 'Saved', userName: 'fixture', password: 'saved' }])
  assert.equal(rows.length, 2)
  assert.equal(rows[0].existingId, 29)
  assert.equal(rows[0].name, 'Saved')
  assert.equal(rows[0].password, 'saved')
  assert.equal(canRegister(rows[0]), false)
  assert.equal(discoveryRows([{ ip: '192.0.2.11:8080' }], [{ id: 29, ip: '192.0.2.11' }])[0].existingId, undefined)
})

test('invalid credentials and missing media never become addable', async () => {
  const row = account(discoveryRows(found, [])[0])
  await authenticateRow(row, async () => ({ code: 500, msg: '认证失败' }))
  assert.equal(row.state, 'failed'); assert.equal(canRegister(row), false)
  await authenticateRow(row, async () => ({ code: 200, data: {} }))
  assert.equal(row.state, 'failed'); assert.equal(canRegister(row), false)
})

test('credential edits and stale responses cannot reuse an earlier successful authentication', async () => {
  const row = account(discoveryRows(found, [])[0])
  const pending = defer()
  const task = authenticateRow(row, () => pending.promise)
  row.password = 'changed'; invalidateAuthentication(row)
  pending.resolve(response); await task
  assert.equal(row.result, null); assert.equal(canRegister(row), false)
  await authenticateRow(row, async () => response)
  assert.equal(canRegister(row), true)
  row.auth = '2'
  assert.equal(canRegister(row), false)
})

test('closed discovery generation ignores late authentication', async () => {
  const row = account(discoveryRows(found, [])[0]); const pending = defer(); let current = true
  const task = authenticateRow(row, () => pending.promise, () => current)
  current = false; pending.resolve(response); await task
  assert.equal(canRegister(row), false); assert.equal(row.result, null)
})

test('batch registration isolates failures, skips existing and unauthenticated rows, and never resubmits success', async () => {
  const rows = discoveryRows([...found, { ip: '192.0.2.13' }, { ip: '192.0.2.14' }], [{ id: 29, ip: '192.0.2.11' }])
  for (const row of rows.slice(1, 3)) { account(row); await authenticateRow(row, async () => response) }
  const writes = []
  const save = async data => { writes.push(data); if (data.ip.endsWith('.12')) throw new Error('fixture failure'); return { code: 200 } }
  assert.deepEqual(await registerRows(rows, save), { added: 1, failed: 1, skipped: 2 })
  assert.equal(writes.length, 2); assert.equal(rows[1].state, 'saveFailed'); assert.equal(rows[2].state, 'added')
  assert.deepEqual(await registerRows([rows[2]], save), { added: 0, failed: 0, skipped: 1 })
  assert.equal(writes.length, 2)
})
