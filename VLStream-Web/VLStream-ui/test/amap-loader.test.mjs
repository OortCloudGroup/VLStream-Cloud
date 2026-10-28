import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/utils/amap.js', import.meta.url), 'utf8')
  .replace(/^import .*$/gm, '').replace(/export /g, '')
const loadModule = new Function('AMapLoader', 'getConfigKey', 'window', 'setTimeout', 'clearTimeout',
  source + '\nreturn {loadAMap, reverseGeocode}')

function fixture(load) {
  const calls = [], timers = new Map(), browser = {}
  let nextTimer = 0
  const api = loadModule({ load }, async key => {
    calls.push(key)
    return { msg: key === 'amap.key' ? 'fixture-key-never-sent' : 'fixture-security-never-sent' }
  }, browser, callback => { const id = ++nextTimer; timers.set(id, callback); return id }, id => timers.delete(id))
  return { api, calls, timers, browser }
}

test('shared SDK loader requests existing configuration only once', async () => {
  let sdkCalls = 0
  const SDK = {}
  const h = fixture(async () => { sdkCalls++; return SDK })
  assert.equal(h.api.loadAMap(), h.api.loadAMap())
  assert.equal(await h.api.loadAMap(), SDK)
  assert.deepEqual(h.calls, ['amap.security.key', 'amap.key'])
  assert.equal(sdkCalls, 1)
  assert.equal(h.timers.size, 0)
})

test('SDK/configuration failure clears shared pending state for retry', async () => {
  let attempt = 0
  const h = fixture(async () => { if (++attempt === 1) throw Error('fixture failure'); return {} })
  await assert.rejects(h.api.loadAMap(), /fixture failure/)
  await h.api.loadAMap()
  assert.equal(attempt, 2)
  assert.equal(h.timers.size, 0)
})

test('stalled SDK load has a deadline rather than a permanent loading state', async () => {
  const h = fixture(() => new Promise(() => {}))
  const pending = h.api.loadAMap()
  for (const callback of h.timers.values()) callback()
  await assert.rejects(pending, /地图服务加载超时/)
  assert.equal(h.timers.size, 0)
})

test('reverse geocoder uses the original SDK and accepts zero coordinates', async () => {
  let coordinates
  const h = fixture(async () => ({ Geocoder: class {
    getAddress(value, callback) {
      coordinates = value
      callback('complete', { info: 'OK', regeocode: { formattedAddress: ' 受控地址文本 ' } })
    }
  } }))
  assert.equal(await h.api.reverseGeocode({ lng: 0, lat: 0 }), '受控地址文本')
  assert.deepEqual(coordinates, [0, 0])
  assert.equal(h.timers.size, 0)
})

test('WGS84 and BD09 convert once before geocoding, existing GCJ02 callers never convert', async () => {
  const calls = []
  const h = fixture(async () => ({
    convertFrom(value, source, callback) {
      calls.push(['convert', value, source]); callback('complete', { info: 'ok', locations: [[114.12, 22.55]] })
    },
    Geocoder: class { getAddress(value, callback) {
      calls.push(['address', value]); callback('complete', { info: 'OK', regeocode: { formattedAddress: 'fixture' } })
    } }
  }))
  await h.api.reverseGeocode({ lng: 114.1177, lat: 22.5543, coordinateSystem: 'WGS84' })
  await h.api.reverseGeocode({ lng: 114.1177, lat: 22.5543, coordinateSystem: 'BD09' })
  await h.api.reverseGeocode({ lng: 0, lat: 0 })
  assert.deepEqual(calls.map(item => item[0]), ['convert', 'address', 'convert', 'address', 'address'])
  assert.equal(calls[0][2], 'gps'); assert.equal(calls[2][2], 'baidu')
  assert.deepEqual(calls[1][1], [114.12, 22.55]); assert.deepEqual(calls[4][1], [0, 0])
  assert.equal(h.timers.size, 0)
})
