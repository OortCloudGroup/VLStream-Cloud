import test from 'node:test'
import assert from 'node:assert/strict'
import { createEventLocationResolver } from './eventLocationResolver.mjs'
import { eventLocationCoordinate } from './eventLocation.mjs'
const coordinate = (lng = 114.1177) => ({ lng, lat: 22.5543, coordinateSystem: 'WGS84' })
const tick = () => new Promise(resolve => setImmediate(resolve))

test('deduplicates same typed coordinates and caches successful provider address', async () => {
  let calls = 0
  const lookup = createEventLocationResolver(async value => { calls++; assert.deepEqual(value, coordinate()); return '模拟地址' })
  const [first, second] = await Promise.all([lookup(coordinate()), lookup(coordinate())])
  assert.equal(first.address, '模拟地址'); assert.equal(second.address, first.address)
  assert.equal((await lookup(coordinate())).address, first.address)
  assert.equal(calls, 1)
})
test('limits concurrency and skips rows that were left before their request starts', async () => {
  let calls = 0, release
  const lookup = createEventLocationResolver(() => { calls++; return new Promise(resolve => { release = resolve }) }, { concurrency: 1 })
  const active = lookup(coordinate()), cancelled = lookup(coordinate(115), () => false)
  await tick(); assert.equal(calls, 1)
  release('模拟地址'); await active
  assert.equal((await cancelled).status, 'CANCELLED'); assert.equal(calls, 1)
})
test('failure/timeout are bounded and cached with a one-minute provider cooldown', async () => {
  let calls = 0, now = 1000
  const lookup = createEventLocationResolver(async () => { calls++; throw Error('mock quota failure') }, { now: () => now })
  assert.equal((await lookup(coordinate())).status, 'UNAVAILABLE')
  await lookup(coordinate()); await lookup(coordinate(115)); assert.equal(calls, 1)
  now += 61000; await lookup(coordinate()); assert.equal(calls, 2)
  const timeoutLookup = createEventLocationResolver(() => new Promise(() => {}), { timeout: 5 })
  assert.equal((await timeoutLookup(coordinate())).status, 'UNAVAILABLE')
})
test('does not confuse coordinate systems or parse a localized display string', async () => {
  let calls = 0
  const lookup = createEventLocationResolver(async () => { calls++; return 'fixture' })
  await lookup(coordinate())
  await lookup({ ...coordinate(), coordinateSystem: 'GCJ02' })
  assert.equal(calls, 2)
  assert.equal((await lookup({ ...coordinate(), lng: '经度114.1177' })).status, 'INVALID')
  assert.equal(eventLocationCoordinate({ point: { lng: 0, lat: 0, coord_system_type: 1 } }).lng, 0)
  assert.equal(eventLocationCoordinate({ point: { lng: 0, lat: 0, coord_system_type: 0 } }), null)
  assert.equal(eventLocationCoordinate({ point: { address: '事件已有地址', lng: 114, lat: 22, coord_system_type: 1 } }), null)
})
