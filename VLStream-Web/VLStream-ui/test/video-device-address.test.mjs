import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createVideoAddressResolver, savedVideoDeviceAddress } from '../src/utils/videoDeviceAddress.mjs'

test('persisted address wins and needs no coordinates or geocoder', async () => {
  let calls = 0
  const resolve = createVideoAddressResolver(() => { calls++; throw new Error('unexpected') })
  assert.equal(savedVideoDeviceAddress({ address: '  ', addressMap: ' 中文地址 ' }), '中文地址')
  assert.deepEqual(await resolve({ address: ' 已维护地址 ', longitude: 0, latitude: 0 }), { status: 'saved', address: '已维护地址' })
  assert.equal(calls, 0)
})

test('missing and invalid coordinates never request a location', async () => {
  const resolve = createVideoAddressResolver(() => assert.fail('unexpected request'))
  for (const coordinates of [{}, { longitude: '', latitude: 0 }, { longitude: null, latitude: 0 },
    { longitude: 181, latitude: 0 }, { longitude: 0, latitude: 91 }, { longitude: 'bad', latitude: 0 },
    { longitude: false, latitude: 0 }]) assert.equal((await resolve(coordinates)).status, 'missing')
})

test('zero is legal, concurrent and repeated requests share the cache', async () => {
  let calls = 0
  let finish
  const resolve = createVideoAddressResolver(coords => {
    calls++
    assert.deepEqual(coords, { lng: 0, lat: 0 })
    return new Promise(done => { finish = done })
  })
  const first = resolve({ longitude: 0, latitude: '0' })
  const second = resolve({ longitude: '0.0', latitude: 0 })
  assert.equal(first, second)
  await Promise.resolve()
  finish(' 真实服务结果 ')
  assert.equal((await first).address, '真实服务结果')
  assert.equal((await resolve({ longitude: 0, latitude: 0 })).status, 'resolved')
  assert.equal(calls, 1)
})

test('failure, empty result and timeout return explicit failure with cooldown', async () => {
  for (const lookup of [() => Promise.reject(new Error('offline')), () => '', () => new Promise(() => {})]) {
    let calls = 0
    const resolve = createVideoAddressResolver(() => { calls++; return lookup() }, 5)
    const device = { longitude: 1, latitude: 2 }
    assert.equal((await resolve(device)).status, 'failed')
    assert.equal((await resolve(device)).status, 'failed')
    assert.equal(calls, 1)
  }
})

test('out-of-order completions and updated saved addresses stay isolated', async () => {
  const pending = new Map()
  const resolve = createVideoAddressResolver(({ lng }) => new Promise(done => pending.set(lng, done)))
  const a = resolve({ longitude: 1, latitude: 2 })
  const b = resolve({ longitude: 3, latitude: 4 })
  await Promise.resolve()
  pending.get(3)('B')
  assert.equal((await b).address, 'B')
  assert.equal((await resolve({ longitude: 1, latitude: 2, address: '更新地址' })).address, '更新地址')
  pending.get(1)('A')
  assert.equal((await a).address, 'A')
})
