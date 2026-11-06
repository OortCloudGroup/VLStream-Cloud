import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/utils/osmGeocoding.js', import.meta.url), 'utf8')

function createGeocoder({ provider = 'photon', endpoint = 'https://fixture.invalid' } = {}, fetchImpl) {
  const timers = new Map()
  let nextTimer = 0
  const setTimeout = (callback, delay) => {
    const id = ++nextTimer
    if (delay >= 10000) timers.set(id, callback)
    else queueMicrotask(callback)
    return id
  }
  const clearTimeout = id => timers.delete(id)
  const code = source.replaceAll('import.meta.env', `(${JSON.stringify({
    VITE_OSM_GEOCODING_PROVIDER: provider,
    VITE_OSM_GEOCODING_URL: endpoint,
  })})`).replace(/\bexport\s+/g, '')
  const api = new Function('fetch', 'setTimeout', 'clearTimeout', 'AbortController', 'URLSearchParams',
    `${code}\nreturn { hasGeocoding, searchAddress, reverseGeocode }`)(
    fetchImpl, setTimeout, clearTimeout, AbortController, URLSearchParams)
  return { ...api, timers }
}

const photonFeature = {
  geometry: { type: 'Point', coordinates: [120.1, 30.2] },
  properties: { name: 'Office', street: 'Main St', housenumber: '12', city: 'Hangzhou', country: 'China' },
}

test('Photon search normalizes address results and omits browser credentials', async () => {
  const calls = []
  const geocoder = createGeocoder({}, async (url, options) => {
    calls.push({ url: String(url), options })
    return { ok: true, json: async () => ({ features: [photonFeature] }) }
  })
  assert.deepEqual(await geocoder.searchAddress('fixture address'), [{
    lon: 120.1, lat: 30.2, display_name: 'Office, Main St 12, Hangzhou, China',
  }])
  assert.match(calls[0].url, /\/api\/\?q=fixture\+address&limit=5$/)
  assert.equal(calls[0].options.credentials, 'omit')
  assert.equal(geocoder.timers.size, 0)
})

test('duplicate search results use the bounded cache without a second request', async () => {
  let requests = 0
  const geocoder = createGeocoder({}, async () => {
    requests++
    return { ok: true, json: async () => ({ features: [photonFeature] }) }
  })
  const first = await geocoder.searchAddress('same query')
  const second = await geocoder.searchAddress('same query')
  assert.deepEqual(second, first)
  assert.equal(requests, 1)
})

test('reverse lookup accepts only valid WGS84 coordinates', async () => {
  let requests = 0
  const geocoder = createGeocoder({}, async () => {
    requests++
    return { ok: true, json: async () => ({ features: [photonFeature] }) }
  })
  await assert.rejects(geocoder.reverseGeocode({ lng: 1, lat: 2, coordinateSystem: 'GCJ02' }), /WGS84/)
  await assert.rejects(geocoder.reverseGeocode({ lng: 181, lat: 2 }), /坐标无效/)
  await assert.rejects(geocoder.reverseGeocode({ lng: '', lat: 0 }), /坐标无效/)
  assert.equal(requests, 0)
})

test('Nominatim can be selected through build configuration', async () => {
  let requestedUrl
  const geocoder = createGeocoder({ provider: 'nominatim', endpoint: 'https://geo.fixture.invalid/' }, async url => {
    requestedUrl = String(url)
    return { ok: true, json: async () => [{ display_name: 'Fixture location', lat: '30.2', lon: '120.1' }] }
  })
  assert.deepEqual(await geocoder.searchAddress('fixture'), [{
    display_name: 'Fixture location', lat: '30.2', lon: '120.1',
  }])
  assert.match(requestedUrl, /^https:\/\/geo\.fixture\.invalid\/search\?format=jsonv2&accept-language=zh-CN&q=fixture&limit=5$/)
})
